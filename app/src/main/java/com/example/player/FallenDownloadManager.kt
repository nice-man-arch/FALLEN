package com.example.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.api.FallenApi
import com.example.data.FallenDatabase
import com.example.data.MusicSource
import com.example.data.SongModel
import com.example.util.getSafeAttributionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object FallenDownloadManager {
    private const val TAG = "FallenDownloadManager"
    private const val CHANNEL_ID = "fallen_downloads_channel"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Map of active songId to download percentage (0 to 100)
    val activeDownloads = MutableStateFlow<Map<String, Int>>(emptyMap())

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val safeContext = context.getSafeAttributionContext()
            val name = "Downloads"
            val desc = "Shows progress of offline audio downloads"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = desc
            }
            val manager = safeContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun startDownload(context: Context, song: SongModel) {
        val safeContext = context.getSafeAttributionContext()
        initNotificationChannel(safeContext)
        val currentProgressMap = activeDownloads.value.toMutableMap()
        if (currentProgressMap.containsKey(song.id)) return // Already downloading

        currentProgressMap[song.id] = 0
        activeDownloads.value = currentProgressMap

        scope.launch {
            val notificationManager = safeContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = song.id.hashCode()
            
            val builder = NotificationCompat.Builder(safeContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading ${song.title}")
                .setContentText("Pre-fetching stream...")
                .setProgress(100, 0, true)
                .setOngoing(true)
                .setAutoCancel(false)

            notificationManager.notify(notificationId, builder.build())

            try {
                // 1. Resolve Stream URL if needed
                var streamUrl = song.streamUrl
                if (streamUrl == null) {
                    if (song.source == MusicSource.JIO_SAAVN) {
                        streamUrl = FallenApi.fetchJioSaavnSongDetails(song.id)?.streamUrl
                    }
                }

                if (streamUrl == null) {
                    throw Exception("Could not resolve streaming location for ${song.title}")
                }

                // 2. Clear output destination file
                val storageDir = safeContext.getExternalFilesDir(null) ?: safeContext.filesDir
                val destinationFile = File(storageDir, "fallen_${song.id}.mp3")
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }

                // 3. Initiate chunked stream download
                val request = Request.Builder().url(streamUrl).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Server returned HTTP ${response.code}")
                    val body = response.body ?: throw Exception("Response body empty")
                    val length = body.contentLength()
                    
                    body.byteStream().use { inputStream ->
                        FileOutputStream(destinationFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalRead = 0L

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalRead += bytesRead
                                
                                if (length > 0) {
                                    val percentage = ((totalRead * 100) / length).toInt()
                                    updateProgress(song.id, percentage)
                                    
                                    builder.setProgress(100, percentage, false)
                                        .setContentText("$percentage% Completed")
                                    notificationManager.notify(notificationId, builder.build())
                                }
                            }
                            outputStream.flush()
                        }
                    }

                    // 4. Save metadata state to database
                    val db = FallenDatabase.getDatabase(safeContext)
                    db.musicDao().upsertSong(
                        song.toEntity().copy(
                            isDownloaded = true,
                            localFilePath = destinationFile.absolutePath
                        )
                    )

                    // Complete Notify
                    builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentTitle("Download Complete")
                        .setContentText(song.title)
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                    notificationManager.notify(notificationId, builder.build())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for ${song.title}: ${e.message}")
                builder.setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle("Download Failed")
                    .setContentText("${song.title}: ${e.localizedMessage}")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                notificationManager.notify(notificationId, builder.build())
            } finally {
                // Clear state
                val finalMap = activeDownloads.value.toMutableMap()
                finalMap.remove(song.id)
                activeDownloads.value = finalMap
            }
        }
    }

    private fun updateProgress(songId: String, level: Int) {
        val currentProgressMap = activeDownloads.value.toMutableMap()
        if (currentProgressMap.containsKey(songId)) {
            currentProgressMap[songId] = level
            activeDownloads.value = currentProgressMap
        }
    }
}
