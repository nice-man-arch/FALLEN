package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.player.PlaybackManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class MusicService : Service() {

    private val binder = MusicBinder()
    private lateinit var mediaSession: MediaSessionCompat
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "fallan_playback_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "com.aistudio.fallan.ACTION_PLAY"
        const val ACTION_PAUSE = "com.aistudio.fallan.ACTION_PAUSE"
        const val ACTION_NEXT = "com.aistudio.fallan.ACTION_NEXT"
        const val ACTION_PREV = "com.aistudio.fallan.ACTION_PREV"
        const val ACTION_STOP = "com.aistudio.fallan.ACTION_STOP"
    }

    inner class MusicBinder : Binder() {
        fun getService() = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "FallanMusicSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { PlaybackManager.resume() }
                override fun onPause() { PlaybackManager.pause() }
                override fun onSkipToNext() { PlaybackManager.skipToNext(this@MusicService) }
                override fun onSkipToPrevious() { PlaybackManager.skipToPrevious(this@MusicService) }
                override fun onSeekTo(pos: Long) { PlaybackManager.seekTo(pos.toInt()) }
                override fun onStop() { stopSelf() }
            })
            isActive = true
        }

        // Lightweight silent placeholder to guarantee immediate foreground status on service start
        val placeholderNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Fallan Wave")
            .setContentText("Ready to ride the wave")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
        startForeground(NOTIFICATION_ID, placeholderNotification)

        observePlaybackState()
    }

    private fun observePlaybackState() {
        serviceScope.launch {
            PlaybackManager.currentSong.collectLatest { song ->
                updateNotification()
            }
        }
        serviceScope.launch {
            PlaybackManager.isPlaying.collectLatest {
                updateNotification()
            }
        }
        serviceScope.launch {
            PlaybackManager.isBuffering.collectLatest {
                updateNotification()
            }
        }
        serviceScope.launch {
            PlaybackManager.playbackDuration.collectLatest {
                updateNotification()
            }
        }
    }

    private var lastThumbnailUrl: String? = null
    private var currentArtwork: Bitmap? = null
    private var imageLoadJob: Job? = null

    private fun updateNotification() {
        val song = PlaybackManager.currentSong.value ?: return
        val isPlaying = PlaybackManager.isPlaying.value
        val isBuffering = PlaybackManager.isBuffering.value
        val duration = PlaybackManager.playbackDuration.value
        val position = PlaybackManager.playbackPosition.value

        // Check if we need to load a new artwork bitmap
        if (song.thumbnailUrl != lastThumbnailUrl) {
            lastThumbnailUrl = song.thumbnailUrl
            currentArtwork = null // clear old
            imageLoadJob?.cancel() // cancel pending
            
            // Start a coroutine to load the new artwork
            imageLoadJob = serviceScope.launch {
                try {
                    val loader = coil.Coil.imageLoader(this@MusicService)
                    val request = coil.request.ImageRequest.Builder(this@MusicService)
                        .data(song.thumbnailUrl)
                        .allowHardware(false) // required for notifications
                        .build()
                    val result = loader.execute(request)
                    if (result is coil.request.SuccessResult) {
                        currentArtwork = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MusicService", "Failed to load artwork: ${e.message}")
                }
                updateNotificationInternal(song, isPlaying, isBuffering, duration, position, currentArtwork)
            }
        } else {
            updateNotificationInternal(song, isPlaying, isBuffering, duration, position, currentArtwork)
        }
    }

    private fun updateNotificationInternal(
        song: com.example.data.SongModel,
        isPlaying: Boolean,
        isBuffering: Boolean,
        duration: Int,
        position: Int,
        artwork: Bitmap?
    ) {
        // Update MediaSession metadata
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration.toLong())
            
        if (artwork != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
        }
        mediaSession.setMetadata(metadataBuilder.build())

        // Update playback state
        val state = when {
            isBuffering -> PlaybackStateCompat.STATE_BUFFERING
            isPlaying -> PlaybackStateCompat.STATE_PLAYING
            else -> PlaybackStateCompat.STATE_PAUSED
        }
        
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, position.toLong(), if (state == PlaybackStateCompat.STATE_PLAYING) 1f else 0f)
        mediaSession.setPlaybackState(stateBuilder.build())

        val notification = buildNotification(song.title, song.artist, isPlaying, artwork)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, artist: String, isPlaying: Boolean, artwork: Bitmap?): Notification {
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun actionIntent(action: String, requestCode: Int): PendingIntent {
            val intent = Intent(this, MusicService::class.java).apply { this.action = action }
            return PendingIntent.getService(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val prevAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous, "Previous",
            actionIntent(ACTION_PREV, 1)
        )
        val playPauseAction = NotificationCompat.Action(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Play",
            actionIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY, 2)
        )
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next, "Next",
            actionIntent(ACTION_NEXT, 3)
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(openAppIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        if (artwork != null) {
            builder.setLargeIcon(artwork)
        }

        return builder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> PlaybackManager.resume()
            ACTION_PAUSE -> PlaybackManager.pause()
            ACTION_NEXT -> PlaybackManager.skipToNext(this)
            ACTION_PREV -> PlaybackManager.skipToPrevious(this)
            ACTION_STOP -> {
                PlaybackManager.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // User swiped app from recents — stop music and kill notification
        PlaybackManager.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fallan Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
