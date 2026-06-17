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
    var equalizer: Equalizer? = null
        private set
        
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var playJob: Job? = null
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
                setOnPreparedListener { mp ->
                    isBuffering.value = false
                    playbackDuration.value = mp.duration
                    initEqualizer(mp.audioSessionId)
                    if (playerShouldPlay) {
                        mp.start()
                        PlaybackManager.isPlaying.value = true
                        startProgressTracker()
                    } else {
                        PlaybackManager.isPlaying.value = false
                    }
                }
                setOnCompletionListener {
                    handleCompletion(safeContext)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    isBuffering.value = false
                    PlaybackManager.isPlaying.value = false
                    // If stream fails, try to fallback automatically
                    triggerFallbackPlay(safeContext)
                    true
                }
            }
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
            } else {
                // Reset expired stream URL for online YouTube tracks to force fresh dynamic resolution
                if (song.source == MusicSource.YOUTUBE || song.source == MusicSource.YOUTUBE_MUSIC) {
                    playUrl = null
                }
            }
            
            // If the song is JioSaavn and not downloaded, refresh the details to get a fresh streaming URL
            if (song.source == MusicSource.JIO_SAAVN && !song.isDownloaded) {
                withContext(Dispatchers.IO) {
                    val refreshed = FallenApi.fetchJioSaavnSongDetails(song.id)
                    if (refreshed != null && !refreshed.streamUrl.isNullOrEmpty()) {
                        playUrl = refreshed.streamUrl
                        Log.d(TAG, "Refreshed JioSaavn stream URL: $playUrl")
                    }
                }
            }
            
            // Resolve stream path if null (YouTube / YouTube Music)
            if (playUrl == null && (song.source == MusicSource.YOUTUBE || song.source == MusicSource.YOUTUBE_MUSIC)) {
                withContext(Dispatchers.IO) {
                    playUrl = FallenApi.resolveYoutubeStreamUrl(song.id)
                }
            }
            
            if (isActive) {
                if (playUrl != null) {
                    loadAndPlay(playUrl)
                } else {
                    // Fail over
                    Log.w(TAG, "Null stream URL, calling fallback finder...")
                    triggerFallbackPlay(context)
                }
            }
        }
    }

    private fun loadAndPlay(url: String) {
        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(url)
            mediaPlayer?.prepareAsync()
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
            withContext(Dispatchers.IO) {
                results = FallenApi.searchJioSaavn(query)
            }
            if (isActive && results.isNotEmpty()) {
                val firstResult = results[0]
                var fallbackUrl: String? = null
                withContext(Dispatchers.IO) {
                    val refreshed = FallenApi.fetchJioSaavnSongDetails(firstResult.id)
                    fallbackUrl = refreshed?.streamUrl
                }
                if (isActive) {
                    if (fallbackUrl != null) {
                        loadAndPlay(fallbackUrl)
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

    private fun handleCompletion(context: Context) {
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
        stopProgressTracker()
        mediaPlayer?.release()
        mediaPlayer = null
        equalizer?.release()
        equalizer = null
        scope.cancel()
    }
}
