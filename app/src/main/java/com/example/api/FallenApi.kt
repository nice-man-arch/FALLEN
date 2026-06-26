package com.example.api

import android.util.Log
import com.example.data.MusicSource
import com.example.data.SongModel
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object FallenApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .build()
            chain.proceed(request)
        }
        .build()

    // Multiple JioSaavn API wrapper hosts for high availability
    private val SAAVN_API_HOSTS = listOf(
        "https://saavn.dev",
        "https://jiosaavn-api-beta-one.vercel.app",
        "https://jiosaavn-api-2.vercel.app",
        "https://jiosaavn-api-sumitkolhe.vercel.app",
        "https://jiosaavn-api-ashutoshg007.vercel.app",
        "https://jiosaavn-api-line.vercel.app",
        "https://jiosaavn-api-eight.vercel.app",
        "https://jiosaavn-api-v2.vercel.app"
    )

    // Enhance JioSaavn image urls to High Resolution
    fun enhanceSaavnImageUrl(imageUrl: String): String {
        val secured = if (imageUrl.startsWith("http://")) {
            imageUrl.replace("http://", "https://")
        } else {
            imageUrl
        }
        return secured
            .replace("150x150", "500x500")
            .replace("50x50", "500x500")
            .replace("250x250", "500x500")
            .replace("350x350", "500x500")
    }

    @kotlin.jvm.Volatile
    var preferences: android.content.SharedPreferences? = null

    @kotlin.jvm.Volatile
    private var lastWorkingSaavnHost: String? = null

    fun initCachedInstances() {
        val prefs = preferences ?: return
        lastWorkingSaavnHost = prefs.getString("last_working_saavn_host", null)
        Log.d("FallenApi", "Loaded cached hosts from SharedPreferences: Saavn=$lastWorkingSaavnHost")
    }

    private fun saveSaavnHost(host: String) {
        lastWorkingSaavnHost = host
        preferences?.edit()?.putString("last_working_saavn_host", host)?.apply()
    }

    fun startBackgroundProbing() {
        Thread {
            try {
                probeSaavnHosts()
            } catch (e: Exception) {
                Log.e("FallenApi", "Error during startup background probing: ${e.message}")
            }
        }.start()
    }

    private fun probeSaavnHosts() {
        val hosts = SAAVN_API_HOSTS
        val executor = java.util.concurrent.Executors.newFixedThreadPool(3)
        val latches = java.util.concurrent.CountDownLatch(1)
        for (host in hosts) {
            executor.submit {
                try {
                    val url = "$host/api/search/songs?query=latest"
                    val request = Request.Builder().url(url).build()
                    val probeClient = client.newBuilder()
                        .connectTimeout(1500, TimeUnit.MILLISECONDS)
                        .readTimeout(1500, TimeUnit.MILLISECONDS)
                        .build()
                    probeClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful && latches.count > 0) {
                            val body = response.body?.string() ?: ""
                            if (body.contains("data") || body.contains("results")) {
                                saveSaavnHost(host)
                                latches.countDown()
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
        }
        try {
            latches.await(3, TimeUnit.SECONDS)
        } catch (e: Exception) {}
        executor.shutdownNow()
    }

    private fun getSortedSaavnHosts(): List<String> {
        val list = mutableListOf<String>()
        val cached = lastWorkingSaavnHost
        if (cached != null) {
            list.add(cached)
        }
        list.addAll(SAAVN_API_HOSTS.filter { it != cached })
        return list
    }

    // Helper to parse songs from JioSaavn API responses
    private fun parseSaavnDevJson(body: String): List<SongModel> {
        val songsList = mutableListOf<SongModel>()
        try {
            val rootObj = JSONObject(body)
            val dataObj = rootObj.optJSONObject("data")
            val resultsArray = if (dataObj != null) {
                dataObj.optJSONArray("results") ?: dataObj.optJSONArray("songs")
            } else {
                rootObj.optJSONArray("results") ?: rootObj.optJSONArray("data") ?: rootObj.optJSONArray("songs")
            }
            if (resultsArray == null) return songsList

            val tempMap = LinkedHashMap<String, Pair<SongModel, Long>>()

            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.getJSONObject(i)
                val id = item.optString("id", "")
                val title = item.optString("name", item.optString("title", item.optString("song", "Unknown track")))
                
                val albumObj = item.optJSONObject("album")
                val album = albumObj?.optString("name", "Singles") ?: item.optString("album", "Singles")
                
                val artistsObj = item.optJSONObject("artists")
                val primaryArtistsArr = artistsObj?.optJSONArray("primary")
                val artist = if (primaryArtistsArr != null && primaryArtistsArr.length() > 0) {
                    val artistsNames = mutableListOf<String>()
                    for (j in 0 until primaryArtistsArr.length()) {
                        artistsNames.add(primaryArtistsArr.getJSONObject(j).optString("name", ""))
                    }
                    artistsNames.filter { it.isNotEmpty() }.joinToString(", ")
                } else {
                    item.optString("primaryArtists", item.optString("singers", item.optString("artists", "Unknown Artist")))
                }
                
                val imageArray = item.optJSONArray("image")
                var thumbnailUrl = ""
                if (imageArray != null && imageArray.length() > 0) {
                    thumbnailUrl = imageArray.getJSONObject(imageArray.length() - 1).optString("url", "")
                    if (thumbnailUrl.isEmpty()) {
                        thumbnailUrl = imageArray.getJSONObject(imageArray.length() - 1).optString("link", "")
                    }
                } else {
                    thumbnailUrl = item.optString("image", "")
                }
                thumbnailUrl = enhanceSaavnImageUrl(thumbnailUrl)
                
                val downloadUrlArray = item.optJSONArray("downloadUrl")
                var streamUrl: String? = null
                if (downloadUrlArray != null && downloadUrlArray.length() > 0) {
                    for (j in 0 until downloadUrlArray.length()) {
                        val dUrlObj = downloadUrlArray.getJSONObject(j)
                        val qual = dUrlObj.optString("quality", "")
                        if (qual == "320kbps") {
                            streamUrl = dUrlObj.optString("url", "")
                            if (streamUrl.isEmpty()) {
                                  streamUrl = dUrlObj.optString("link", "")
                            }
                        }
                    }
                    if (streamUrl.isNullOrEmpty()) {
                        streamUrl = downloadUrlArray.getJSONObject(downloadUrlArray.length() - 1).optString("url", "")
                        if (streamUrl.isNullOrEmpty()) {
                            streamUrl = downloadUrlArray.getJSONObject(downloadUrlArray.length() - 1).optString("link", "")
                        }
                    }
                }
                
                if (streamUrl.isNullOrEmpty()) {
                    streamUrl = item.optString("downloadUrl", "")
                }
                
                val durationStr = item.optString("duration", "0")
                val durationMs = (durationStr.toLongOrNull() ?: 180L) * 1000L
                
                if (id.isNotEmpty()) {
                    val songModel = SongModel(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        thumbnailUrl = thumbnailUrl,
                        streamUrl = streamUrl,
                        source = MusicSource.JIO_SAAVN,
                        durationMs = durationMs
                    )
                    
                    val playCountStr = item.optString("playCount", "0")
                    val playCount = playCountStr.toLongOrNull() ?: item.optLong("playCount", 0L)
                    
                    val normalizedTitle = title.trim().lowercase()
                    val normalizedArtist = artist.trim().lowercase()
                    val key = "$normalizedTitle|$normalizedArtist"
                    
                    val existing = tempMap[key]
                    if (existing == null || playCount > existing.second) {
                        tempMap[key] = Pair(songModel, playCount)
                    }
                }
            }
            songsList.addAll(tempMap.values.map { it.first })
        } catch (e: Exception) {
            println("SOURCE ERROR: $e\n${Log.getStackTraceString(e)}")
            Log.e("FallenApi", "SOURCE ERROR: $e\n${Log.getStackTraceString(e)}")
        }
        return songsList
    }

    // Refresh expired JioSaavn song stream info by ID
    fun fetchJioSaavnSongDetails(id: String): SongModel? {
        for (host in getSortedSaavnHosts()) {
            for (path in listOf("/api/songs", "/songs")) {
                try {
                    val url = "$host$path?id=$id"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use
                        val body = response.body?.string() ?: return@use
                        val list = parseSaavnDevJson(body)
                        if (list.isNotEmpty()) {
                            saveSaavnHost(host)
                            return list[0]
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FallenApi", "Saavn details error on $host$path: ${e.message}")
                }
            }
        }
        return null
    }

    // JioSaavn Searches via modern, ad-free public APIs
    fun searchJioSaavn(query: String): List<SongModel> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        for (host in getSortedSaavnHosts()) {
            for (path in listOf("/api/search/songs", "/search/songs")) {
                try {
                    val url = "$host$path?query=$encodedQuery"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use
                        val body = response.body?.string() ?: return@use
                        val list = parseSaavnDevJson(body)
                        if (list.isNotEmpty()) {
                            saveSaavnHost(host)
                            return list
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FallenApi", "Saavn search error on $host$path: ${e.message}")
                }
            }
        }
        return emptyList()
    }

    // JioSaavn Charts
    fun fetchJioSaavnCharts(): List<SongModel> {
        val songsList = mutableListOf<SongModel>()
        try {
            // Fetch trending/charts via saavn.dev
            for (host in getSortedSaavnHosts()) {
                try {
                    val url = "$host/api/modules?language=english,hindi"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use
                        val body = response.body?.string() ?: return@use
                        val rootObj = JSONObject(body)
                        val dataObj = rootObj.optJSONObject("data") ?: return@use
                        
                        // Try parsing trending or charts
                        val trendingObj = dataObj.optJSONObject("trending")
                        val chartsObj = dataObj.optJSONObject("charts")
                        val targetObj = if (trendingObj?.optJSONArray("songs") != null) trendingObj else chartsObj
                        val songsArray = targetObj?.optJSONArray("songs")
                        if (songsArray != null && songsArray.length() > 0) {
                            val list = parseSaavnDevJson(targetObj.toString())
                            if (list.isNotEmpty()) {
                                saveSaavnHost(host)
                                songsList.addAll(list)
                                return songsList
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FallenApi", "Saavn charts error on $host: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("FallenApi", "Saavn charts generic error: ${e.message}")
        }
        
        // Fallback chart loader: search for dynamic popular tags to guarantee top quality songs!
        if (songsList.isEmpty()) {
            songsList.addAll(searchJioSaavn("Top Trending Hits"))
        }
        return songsList
    }



    fun cleanText(text: String): String {
        return text
            .replace(Regex("(?i)\\(feat\\.? .*?\\)"), "")
            .replace(Regex("(?i)\\[feat\\.? .*?\\]"), "")
            .replace(Regex("(?i)feat\\.? .*"), "")
            .replace(Regex("(?i)ft\\.? .*"), "")
            .replace(Regex("(?i)\\(official video\\)"), "")
            .replace(Regex("(?i)\\(official audio\\)"), "")
            .replace(Regex("(?i)\\(lyric video\\)"), "")
            .replace(Regex("(?i)\\(lyrics\\)"), "")
            .replace(Regex("(?i)\\[official video\\]"), "")
            .replace(Regex("(?i)\\[official audio\\]"), "")
            .replace(Regex("(?i)- Topic"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // Fetches scrolling time-synced or raw lyrics
    fun fetchLyrics(artist: String, title: String): Pair<String, Boolean> {
        val cleanArt = cleanText(artist)
        val cleanTit = cleanText(title)
        
        // 1. Try Exact Get first with cleaned metadata
        try {
            val encodedArtist = URLEncoder.encode(cleanArt, "UTF-8").replace("+", "%20")
            val encodedTitle = URLEncoder.encode(cleanTit, "UTF-8").replace("+", "%20")
            val url = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle"
            
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val rootObj = JSONObject(body)
                    val syncedLyrics = rootObj.optString("syncedLyrics", "")
                    if (syncedLyrics.isNotEmpty()) {
                        return Pair(syncedLyrics, true)
                    }
                    val plainLyrics = rootObj.optString("plainLyrics", "")
                    if (plainLyrics.isNotEmpty()) {
                        return Pair(plainLyrics, false)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("FallenApi", "Clean exact lyrics fetch failed: ${e.message}")
        }

        // 2. Try Exact Get with original artist & title (in case cleanup was too aggressive)
        if (cleanArt != artist || cleanTit != title) {
            try {
                val encodedArtist = URLEncoder.encode(artist, "UTF-8").replace("+", "%20")
                val encodedTitle = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
                val url = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle"
                
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val rootObj = JSONObject(body)
                        val syncedLyrics = rootObj.optString("syncedLyrics", "")
                        if (syncedLyrics.isNotEmpty()) {
                            return Pair(syncedLyrics, true)
                        }
                        val plainLyrics = rootObj.optString("plainLyrics", "")
                        if (plainLyrics.isNotEmpty()) {
                            return Pair(plainLyrics, false)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("FallenApi", "Original exact lyrics fetch failed: ${e.message}")
            }
        }

        // 3. Fallback to LRCLIB Search API (Query search)
        try {
            val query = URLEncoder.encode("$cleanArt $cleanTit", "UTF-8").replace("+", "%20")
            val url = "https://lrclib.net/api/search?q=$query"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val jsonArray = JSONArray(body)
                    if (jsonArray.length() > 0) {
                        // Preference 1: Time-synced lyrics
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val syncedLyrics = obj.optString("syncedLyrics", "")
                            if (syncedLyrics.isNotEmpty()) {
                                return Pair(syncedLyrics, true)
                            }
                        }
                        // Preference 2: Plain text lyrics
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val plainLyrics = obj.optString("plainLyrics", "")
                            if (plainLyrics.isNotEmpty()) {
                                return Pair(plainLyrics, false)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FallenApi", "Search lyrics fallback failed: ${e.message}")
        }

        return Pair("No lyrics found for this track. Enjoy the wave! 🎵", false)
    }
}
