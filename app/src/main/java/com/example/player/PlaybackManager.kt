package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.util.Log
import com.example.api.FallenApi
import com.example.data.FallenDatabase
import com.example.data.MusicSource
import com.example.data.SongModel
import com.example.util.getSafeAttributionContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

enum class RepeatMode { OFF, ONE, ALL }

object PlaybackManager {
    private const val TAG = "PlaybackManager"
    
    private var mediaPlayer: MediaPlayer? = null
    private var nextMediaPlayer: MediaPlayer? = null
    var equalizer: Equalizer? = null
        private set
        
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var outgoingPlayer: MediaPlayer? = null
    private var crossfadeJob: Job? = null
    val crossfadeDurationMs = MutableStateFlow(0L) // 0 = crossfade off, set externally by UI layer
    private var sleepTimerJob: Job? = null
    private var sleepAfterCurrentSong = false
    private var fallbackCount = 0
    @Volatile
    private var playerShouldPlay = true

    // Player Live States
    val currentSong = MutableStateFlow<SongModel?>(null)
    val isPlaying = MutableStateFlow(false)
    val isBuffering = MutableStateFlow(false)
    val playbackPosition = MutableStateFlow(0)
    val playbackDuration = MutableStateFlow(0)
    val shuffleEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(RepeatMode.OFF)
    val sleepTimerActive = MutableStateFlow(false)
    val sleepTimerRemainingMs = MutableStateFlow(0L)

    // Equalizer States
    val eqAvailable = MutableStateFlow(false)
    val eqBandsCount = MutableStateFlow(0)
    val eqBandLevels = MutableStateFlow<Map<Short, Short>>(emptyMap()) // Band ID -> Level in Millibels
    val currentPresetName = MutableStateFlow("Flat")

    // Queue management
    private val originalQueue = mutableListOf<SongModel>()
    val currentQueue = MutableStateFlow<List<SongModel>>(emptyList())
    private var currentIndex = -1

    fun initPlayer(context: Context) {
        val safeContext = context.getSafeAttributionContext()
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setupPlayerListeners(this, safeContext)
            }
        }
    }

    private fun setupPlayerListeners(player: MediaPlayer, context: Context) {
        val safeContext = context.getSafeAttributionContext()
        player.setOnPreparedListener { mp ->
            Log.d("PlaybackManager", "MediaPlayer prepared, calling start()")
            isBuffering.value = false
            playbackDuration.value = mp.duration
            initEqualizer(mp.audioSessionId)
            if (playerShouldPlay) {
                mp.start()
                PlaybackManager.isPlaying.value = true
                startProgressTracker()
                prepareNextSong(safeContext)
            } else {
                PlaybackManager.isPlaying.value = false
            }
        }
        player.setOnCompletionListener { completedPlayer ->
            if (completedPlayer != mediaPlayer) {
                Log.d(TAG, "Completed player is not the active mediaPlayer. Ignoring completion.")
                return@setOnCompletionListener
            }
            if (crossfadeDurationMs.value == 0L) {
                // Gapless path — only active when crossfade is off
                var gaplessPromoted = false
                nextMediaPlayer?.let { np ->
                    val oldPlayer = mediaPlayer
                    mediaPlayer = np
                    nextMediaPlayer = null
                    
                    // Clean up old player listeners to avoid any callback leaks/crashes
                    oldPlayer?.setOnPreparedListener(null)
                    oldPlayer?.setOnCompletionListener(null)
                    oldPlayer?.setOnErrorListener(null)
                    oldPlayer?.release()
                    
                    // Setup listeners on the new promoted player
                    setupPlayerListeners(np, safeContext)
                    
                    isBuffering.value = false
                    isPlaying.value = true
                    initEqualizer(np.audioSessionId)
                    playbackDuration.value = np.duration
                    startProgressTracker()
                    
                    val q = currentQueue.value
                    if (q.isNotEmpty()) {
                        val nextIndex = when (repeatMode.value) {
                            RepeatMode.ONE -> currentIndex
                            RepeatMode.ALL -> (currentIndex + 1) % q.size
                            RepeatMode.OFF -> if (currentIndex + 1 < q.size) currentIndex + 1 else -1
                        }
                        if (nextIndex in q.indices) {
                            currentIndex = nextIndex
                            currentSong.value = q[nextIndex]
                            prepareNextSong(safeContext)
                            gaplessPromoted = true
                        }
                    }
                }
                if (!gaplessPromoted) {
                    handleCompletion(safeContext)
                }
            } else {
                // Crossfade is handling the transition — completion of outgoing
                // player during a crossfade is expected, just clean up
                nextMediaPlayer?.release()
                nextMediaPlayer = null
                handleCompletion(safeContext)
            }
        }
        player.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
            isBuffering.value = false
            PlaybackManager.isPlaying.value = false
            // If stream fails, try to fallback automatically
            triggerFallbackPlay(safeContext)
            true
        }
    }

    private fun initEqualizer(audioSessionId: Int) {
        try {
            if (equalizer != null) {
                equalizer?.release()
            }
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
                val numBands = numberOfBands
                eqBandsCount.value = numBands.toInt()
                eqAvailable.value = true
                
                // Read current levels
                val levels = mutableMapOf<Short, Short>()
                for (i in 0 until numBands) {
                    levels[i.toShort()] = getBandLevel(i.toShort())
                }
                eqBandLevels.value = levels
            }
            setPreset("Flat")
        } catch (e: Exception) {
            Log.e(TAG, "Equalizer initialization failed: ${e.message}")
            eqAvailable.value = false
        }
    }

    fun applyBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
            val updated = eqBandLevels.value.toMutableMap()
            updated[band] = level
            eqBandLevels.value = updated
            currentPresetName.value = "Custom"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply band level: ${e.message}")
        }
    }

    fun setPreset(name: String) {
        val bands = eqBandsCount.value
        if (bands <= 0) return
        
        currentPresetName.value = name
        // Custom 5-band gain profiles for standard presets
        val profile = when (name) {
            "Bass Boost" -> listOf(1000, 500, 0, -200, -500)
            "Pop" -> listOf(-200, 300, 1000, 500, -200)
            "Rock" -> listOf(600, 300, -200, 400, 800)
            "Jazz" -> listOf(400, 200, -300, 300, 500)
            "Classical" -> listOf(500, 300, -100, 400, 600)
            else -> listOf(0, 0, 0, 0, 0) // Flat
        }

        try {
            val updated = eqBandLevels.value.toMutableMap()
            for (i in 0 until bands) {
                val band = i.toShort()
                val gain = if (i < profile.size) profile[i].toShort() else 0.toShort()
                equalizer?.setBandLevel(band, gain)
                updated[band] = gain
            }
            eqBandLevels.value = updated
        } catch (e: Exception) {
            Log.e(TAG, "Preset change failed: ${e.message}")
        }
    }

    fun setQueue(queue: List<SongModel>, startIndex: Int) {
        originalQueue.clear()
        originalQueue.addAll(queue)
        
        if (shuffleEnabled.value) {
            val shuffled = originalQueue.shuffled()
            currentQueue.value = shuffled
            currentIndex = if (startIndex in queue.indices) {
                shuffled.indexOf(queue[startIndex])
            } else {
                0
            }
        } else {
            currentQueue.value = ArrayList(originalQueue)
            currentIndex = if (startIndex in queue.indices) startIndex else 0
        }
    }

    fun addToQueue(song: SongModel) {
        originalQueue.add(song)
        val updated = ArrayList(currentQueue.value)
        updated.add(song)
        currentQueue.value = updated
    }

    fun reorderQueue(newQueue: List<SongModel>) {
        currentQueue.value = newQueue
        val newIndex = newQueue.indexOfFirst { it.id == currentSong.value?.id }
        if (newIndex != -1) currentIndex = newIndex
    }

    fun removeFromQueue(index: Int) {
        val updated = currentQueue.value.toMutableList()
        if (index < 0 || index >= updated.size) return
        updated.removeAt(index)
        currentQueue.value = updated
        if (index < currentIndex) {
            currentIndex--
        } else if (index == currentIndex && updated.isNotEmpty()) {
            currentIndex = currentIndex.coerceAtMost(updated.size - 1)
        }
    }

    fun playSongAtIndex(context: Context, index: Int) {
        val q = currentQueue.value
        if (q.isEmpty() || index !in q.indices) return
        currentIndex = index
        playSong(context, q[index])
    }

    fun playSong(context: Context, song: SongModel) {
        val safeContext = context.getSafeAttributionContext()
        initPlayer(safeContext)
        stopProgressTracker()
        
        playJob?.cancel()
        playerShouldPlay = true
        fallbackCount = 0
        
        currentSong.value = song
        isBuffering.value = true
        isPlaying.value = false
        playbackPosition.value = 0
        
        playJob = scope.launch {
            // Save to database as Recently Played
            withContext(Dispatchers.IO) {
                try {
                    val db = FallenDatabase.getDatabase(safeContext)
                    db.musicDao().upsertSong(song.toEntity().copy(addedToHistoryAt = System.currentTimeMillis()))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed history upsert: ${e.message}")
                }
            }

            var playUrl: String? = song.streamUrl
            
            // Check offline mode first
            if (song.isDownloaded && song.localFilePath != null) {
                val localFile = File(song.localFilePath)
                if (localFile.exists()) {
                     playUrl = song.localFilePath
                     Log.d(TAG, "Playing local cached filepath: $playUrl")
                }
            }
            
            // If the song is JioSaavn and not downloaded, refresh the details to get a fresh streaming URL
            if (song.source == MusicSource.JIO_SAAVN && !song.isDownloaded) {
                withContext(Dispatchers.IO) {
                    try {
                        val refreshed = FallenApi.fetchJioSaavnSongDetails(song.id)
                        if (refreshed != null && !refreshed.streamUrl.isNullOrEmpty()) {
                            playUrl = refreshed.streamUrl
                            Log.d(TAG, "Refreshed JioSaavn stream URL: $playUrl")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to refresh JioSaavn details: ${e.message}")
                    }
                }
            }
            


            
            if (isActive) {
                if (playUrl != null) {
                    loadAndPlay(safeContext, playUrl)
                } else {
                    // Fail over
                    Log.w(TAG, "Null stream URL, calling fallback finder...")
                    triggerFallbackPlay(context)
                }
            }
        }
    }

    private fun startCrossfade(context: Context, incomingUrl: String) {
        crossfadeJob?.cancel()

        // Demote current player to outgoing
        outgoingPlayer?.release()
        outgoingPlayer = mediaPlayer
        mediaPlayer = null

        val outgoing = outgoingPlayer ?: return
        val fadeDurationMs = crossfadeDurationMs.value.coerceAtLeast(1000L)

        val incoming = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setVolume(0f, 0f)
            setDataSource(incomingUrl)
            setOnPreparedListener { mp ->
                mediaPlayer = mp
                mp.start()
                PlaybackManager.isBuffering.value = false
                PlaybackManager.playbackDuration.value = mp.duration
                PlaybackManager.playbackPosition.value = 0
                initEqualizer(mp.audioSessionId)
                PlaybackManager.isPlaying.value = true
                startProgressTracker()
                prepareNextSong(context)

                // Volume crossfade coroutine
                crossfadeJob = scope.launch {
                    val steps = 60
                    val stepDelay = fadeDurationMs / steps
                    for (i in 1..steps) {
                        if (!isActive) break
                        val fraction = i.toFloat() / steps.toFloat()
                        val inVol = fraction.coerceIn(0f, 1f)
                        val outVol = (1f - fraction).coerceIn(0f, 1f)
                        withContext(Dispatchers.Main) {
                            mp.setVolume(inVol, inVol)
                            outgoing.setVolume(outVol, outVol)
                        }
                        delay(stepDelay)
                    }
                    withContext(Dispatchers.Main) {
                        try {
                            outgoing.stop()
                            outgoing.release()
                        } catch (e: Exception) {
                            Log.w(TAG, "Outgoing player release error: ${e.message}")
                        }
                        outgoingPlayer = null
                    }
                }
            }
            setOnCompletionListener { completedPlayer ->
                if (completedPlayer != mediaPlayer) {
                    Log.d(TAG, "Completed player is not the active mediaPlayer. Ignoring completion.")
                    return@setOnCompletionListener
                }
                handleCompletion(context.getSafeAttributionContext())
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Crossfade incoming player error: what=$what extra=$extra")
                PlaybackManager.isBuffering.value = false
                true
            }
            prepareAsync()
        }
        mediaPlayer = incoming
        PlaybackManager.isBuffering.value = true
    }

    private fun loadAndPlay(context: Context, url: String) {
        try {
            val alreadyPlaying = mediaPlayer?.isPlaying == true
            val crossfadeActive = crossfadeDurationMs.value > 0L

            if (crossfadeActive && alreadyPlaying) {
                // Crossfade path — outgoing fades out while incoming fades in
                // Cancel any pending gapless next player since crossfade takes over
                nextMediaPlayer?.release()
                nextMediaPlayer = null
                startCrossfade(context, url)
            } else {
                // Standard path (also used when crossfade is off)
                try {
                    mediaPlayer?.reset()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to reset player, recreating: ${e.message}")
                    mediaPlayer?.release()
                    mediaPlayer = null
                    initPlayer(context)
                }
                mediaPlayer?.setDataSource(url)
                mediaPlayer?.prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Load and play failed: ${e.message}")
            isBuffering.value = false
        }
    }

    private fun triggerFallbackPlay(context: Context) {
        val song = currentSong.value ?: return
        if (fallbackCount >= 1) {
            Log.w(TAG, "Already attempted fallback for this song. Stopping to avoid infinite loop.")
            isBuffering.value = false
            isPlaying.value = false
            return
        }
        fallbackCount++
        
        playJob?.cancel()
        playJob = scope.launch {
            val query = "${song.title} ${song.artist}"
            Log.d(TAG, "Triggering automatic JioSaavn fallback for: $query")
            var results: List<SongModel> = emptyList()
            try {
                withContext(Dispatchers.IO) {
                    results = FallenApi.searchJioSaavn(query)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Search fallback failed: ${e.message}")
            }
            
            if (isActive && results.isNotEmpty()) {
                val firstResult = results[0]
                var fallbackUrl: String? = null
                try {
                    withContext(Dispatchers.IO) {
                        val refreshed = FallenApi.fetchJioSaavnSongDetails(firstResult.id)
                        fallbackUrl = refreshed?.streamUrl
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fallback details fetch failed: ${e.message}")
                }
                
                val finalFallbackUrl = fallbackUrl
                if (isActive) {
                    if (finalFallbackUrl != null) {
                        loadAndPlay(context, finalFallbackUrl)
                    } else {
                        isBuffering.value = false
                        isPlaying.value = false
                    }
                }
            } else {
                if (isActive) {
                    isBuffering.value = false
                    isPlaying.value = false
                }
            }
        }
    }

    fun pause() {
        playerShouldPlay = false
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            stopProgressTracker()
        }
        isPlaying.value = false
    }

    fun resume() {
        playerShouldPlay = true
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
            startProgressTracker()
        }
        isPlaying.value = true
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying.value = false
        isBuffering.value = false
    }

    fun skipToNext(context: Context) {
        skipNext(context)
    }

    fun skipToPrevious(context: Context) {
        skipPrevious(context)
    }

    fun setRepeatMode(modeInt: Int) {
        repeatMode.value = when (modeInt) {
            1 -> RepeatMode.ONE
            2 -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }
    }

    fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabled.value = enabled
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        playbackPosition.value = positionMs
    }

    fun skipNext(context: Context) {
        val q = currentQueue.value
        if (q.isEmpty()) return
        
        currentIndex = (currentIndex + 1) % q.size
        playSongAtIndex(context, currentIndex)
    }

    fun skipPrevious(context: Context) {
        val q = currentQueue.value
        if (q.isEmpty()) return
        
        currentIndex = if (currentIndex - 1 < 0) q.size - 1 else currentIndex - 1
        playSongAtIndex(context, currentIndex)
    }

    fun toggleShuffle() {
        shuffleEnabled.value = !shuffleEnabled.value
        val song = currentSong.value
        if (shuffleEnabled.value) {
            val shuffled = originalQueue.shuffled()
            currentQueue.value = shuffled
            if (song != null) {
                currentIndex = shuffled.indexOf(song)
            }
        } else {
            currentQueue.value = ArrayList(originalQueue)
            if (song != null) {
                currentIndex = originalQueue.indexOf(song)
            }
        }
    }

    fun toggleRepeat() {
        repeatMode.value = when (repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.OFF
        }
    }

    fun setSleepTimer(durationMs: Long) {
        cancelSleepTimer()
        if (durationMs == -1L) {
            // Special value: sleep after current song ends
            sleepAfterCurrentSong = true
            sleepTimerActive.value = true
            sleepTimerRemainingMs.value = -1L
            return
        }
        sleepAfterCurrentSong = false
        sleepTimerActive.value = true
        sleepTimerRemainingMs.value = durationMs
        sleepTimerJob = scope.launch {
            val fadeStartMs = 10_000L
            val endTime = System.currentTimeMillis() + durationMs
            while (isActive) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    // Time's up — stop
                    withContext(Dispatchers.Main) {
                        pause()
                        sleepTimerActive.value = false
                        sleepTimerRemainingMs.value = 0L
                    }
                    break
                }
                sleepTimerRemainingMs.value = remaining
                // Fade out volume in the last 10 seconds
                if (remaining <= fadeStartMs) {
                    val fraction = remaining.toFloat() / fadeStartMs.toFloat()
                    val vol = fraction.coerceIn(0f, 1f)
                    withContext(Dispatchers.Main) {
                        mediaPlayer?.setVolume(vol, vol)
                    }
                }
                delay(200)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepAfterCurrentSong = false
        sleepTimerActive.value = false
        sleepTimerRemainingMs.value = 0L
        // Restore full volume
        mediaPlayer?.setVolume(1f, 1f)
    }

    private fun prepareNextSong(context: Context) {
        val q = currentQueue.value
        val nextIndex = when (repeatMode.value) {
            RepeatMode.ONE -> currentIndex
            RepeatMode.ALL -> (currentIndex + 1) % q.size
            RepeatMode.OFF -> if (currentIndex + 1 < q.size) currentIndex + 1 else return
        }
        val nextSong = q[nextIndex]
        scope.launch(Dispatchers.IO) {
            try {
                var nextUrl: String? = nextSong.streamUrl
                if (nextSong.isDownloaded && nextSong.localFilePath != null) {
                    val f = java.io.File(nextSong.localFilePath)
                    if (f.exists()) nextUrl = nextSong.localFilePath
                }
                if (nextUrl == null && nextSong.source == MusicSource.JIO_SAAVN) {
                    nextUrl = FallenApi.fetchJioSaavnSongDetails(nextSong.id)?.streamUrl
                }
                if (nextUrl == null) return@launch
                val np = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(nextUrl)
                    prepare() // blocking prepare is fine here — we're on Dispatchers.IO
                }
                withContext(Dispatchers.Main) {
                    try {
                        nextMediaPlayer?.release()
                        nextMediaPlayer = np
                        mediaPlayer?.setNextMediaPlayer(np)
                        Log.d("PlaybackManager", "Gapless: next song pre-buffered: ${nextSong.title}")
                    } catch (e: Exception) {
                        Log.w("PlaybackManager", "Failed to set next media player: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w("PlaybackManager", "Gapless pre-buffer failed: ${e.message}")
                // Non-fatal — if this fails, playback continues normally without gapless
            }
        }
    }

    private fun handleCompletion(context: Context) {
        // Restore volume in case sleep timer faded it, and handle "sleep after song" mode
        mediaPlayer?.setVolume(1f, 1f)
        if (sleepAfterCurrentSong) {
            sleepAfterCurrentSong = false
            sleepTimerActive.value = false
            isPlaying.value = false
            stopProgressTracker()
            return
        }
        when (repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaPlayer?.start()
                isPlaying.value = true
            }
            RepeatMode.ALL -> {
                skipNext(context)
            }
            RepeatMode.OFF -> {
                val q = currentQueue.value
                if (currentIndex < q.size - 1) {
                    skipNext(context)
                } else {
                    isPlaying.value = false
                    stopProgressTracker()
                    seekTo(0)
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        playbackPosition.value = mp.currentPosition
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        cancelSleepTimer()
        crossfadeJob?.cancel()
        outgoingPlayer?.release()
        outgoingPlayer = null
        stopProgressTracker()
        mediaPlayer?.release()
        mediaPlayer = null
        nextMediaPlayer?.release()
        nextMediaPlayer = null
        equalizer?.release()
        equalizer = null
        scope.cancel()
    }
}
