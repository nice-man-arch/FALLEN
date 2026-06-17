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

    // Dedicated fast-fail client specifically for YouTube/Piped streams to bypass dead/slow instances instantly
    private val pipedClient = client.newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    // Multiple piped API instances for high availability as fallbacks (sorted with highest reliability first)
    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.adminforge.de",
        "https://piped-api.lre.yt",
        "https://api.piped.moe",
        "https://pipedapi.astre.me",
        "https://pipedapi.colby.moe",
        "https://pipedapi.synopy.io",
        "https://pipedapi.hostux.net",
        "https://pipedapi.mha.fi",
        "https://piped-api.garudalinux.org",
        "https://pipedapi.reallyawesomedomain.xyz",
        "https://pipedapi.kavin.rocks"
    )

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

    // Multiple Invidious API instances as search and stream fallback
    private val INVIDIOUS_INSTANCES = listOf(
        "https://yewtu.be",
        "https://invidious.projectsegfadd.online",
        "https://invidious.privacydev.net",
        "https://iv.melmac.space",
        "https://invidious.lre.yt",
        "https://invidious.nerdvpn.de",
        "https://invidious.esmailelbob.xyz",
        "https://inv.vern.cc",
        "https://invidious.no-logs.com",
        "https://invidious.slipfox.xyz"
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

    @kotlin.jvm.Volatile
    private var lastWorkingPipedInstance: String? = null

    @kotlin.jvm.Volatile
    private var lastWorkingInvidiousInstance: String? = null

    fun initCachedInstances() {
        val prefs = preferences ?: return
        lastWorkingSaavnHost = prefs.getString("last_working_saavn_host", null)
        lastWorkingPipedInstance = prefs.getString("last_working_piped_instance", null)
        lastWorkingInvidiousInstance = prefs.getString("last_working_invidious_instance", null)
        Log.d("FallenApi", "Loaded cached hosts from SharedPreferences: Saavn=$lastWorkingSaavnHost, Piped=$lastWorkingPipedInstance, Invidious=$lastWorkingInvidiousInstance")
    }

    private fun saveSaavnHost(host: String) {
        lastWorkingSaavnHost = host
        preferences?.edit()?.putString("last_working_saavn_host", host)?.apply()
    }

    private fun savePipedInstance(instance: String) {
        lastWorkingPipedInstance = instance
        preferences?.edit()?.putString("last_working_piped_instance", instance)?.apply()
    }

    private fun saveInvidiousInstance(instance: String) {
        lastWorkingInvidiousInstance = instance
        preferences?.edit()?.putString("last_working_invidious_instance", instance)?.apply()
    }

    fun startBackgroundProbing() {
        Thread {
            try {
                probePipedInstances()
                probeSaavnHosts()
                probeInvidiousInstances()
            } catch (e: Exception) {
                Log.e("FallenApi", "Error during startup background probing: ${e.message}")
            }
        }.start()
    }

    private fun probePipedInstances() {
        val instances = PIPED_INSTANCES
        val executor = java.util.concurrent.Executors.newFixedThreadPool(4)
        val latches = java.util.concurrent.CountDownLatch(1)
        
        for (instance in instances) {
            executor.submit {
                try {
                    val url = "$instance/search?q=test&filter=music_songs"
                    val request = Request.Builder().url(url).build()
                    val probeClient = client.newBuilder()
                        .connectTimeout(1500, TimeUnit.MILLISECONDS)
                        .readTimeout(1500, TimeUnit.MILLISECONDS)
                        .build()
                    probeClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful && latches.count > 0) {
                            val body = response.body?.string() ?: ""
                            if (body.contains("items")) {
                                savePipedInstance(instance)
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

    private fun probeInvidiousInstances() {
        val instances = INVIDIOUS_INSTANCES
        val executor = java.util.concurrent.Executors.newFixedThreadPool(4)
        val latches = java.util.concurrent.CountDownLatch(1)
        for (instance in instances) {
            executor.submit {
                try {
                    val url = "$instance/api/v1/search?q=test&type=video"
                    val request = Request.Builder().url(url).build()
                    val probeClient = client.newBuilder()
                        .connectTimeout(1500, TimeUnit.MILLISECONDS)
                        .readTimeout(1500, TimeUnit.MILLISECONDS)
                        .build()
                    probeClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful && latches.count > 0) {
                            val body = response.body?.string() ?: ""
                            if (body.contains("videoId")) {
                                saveInvidiousInstance(instance)
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

    private fun getSortedPipedInstances(): List<String> {
        val list = mutableListOf<String>()
        val cached = lastWorkingPipedInstance
        if (cached != null) {
            list.add(cached)
        }
        list.addAll(PIPED_INSTANCES.filter { it != cached })
        return list
    }

    private fun getSortedInvidiousInstances(): List<String> {
        val list = mutableListOf<String>()
        val cached = lastWorkingInvidiousInstance
        if (cached != null) {
            list.add(cached)
        }
        list.addAll(INVIDIOUS_INSTANCES.filter { it != cached })
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
                    pipedClient.newCall(request).execute().use { response ->
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
                    pipedClient.newCall(request).execute().use { response ->
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
                    pipedClient.newCall(request).execute().use { response ->
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

    // YouTube / YouTube Music Searches via Piped API with instance fallbacks
    fun searchYoutube(query: String, filterSongsOnly: Boolean): List<SongModel> {
        val list = mutableListOf<SongModel>()
        val filter = if (filterSongsOnly) "music_songs" else "videos"
        val encodedQuery = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        
        for (instance in getSortedPipedInstances()) {
            try {
                val url = "$instance/search?q=$encodedQuery&filter=$filter"
                val request = Request.Builder().url(url).build()
                pipedClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    
                    val rootObj = JSONObject(body)
                    val itemsArray = rootObj.optJSONArray("items") ?: return@use
                    
                    for (i in 0 until itemsArray.length()) {
                        val item = itemsArray.getJSONObject(i)
                        val watchUrl = item.optString("url", "")
                        var videoId = item.optString("id", "")
                        if (videoId.isEmpty() && watchUrl.contains("v=")) {
                            videoId = watchUrl.substringAfter("v=").substringBefore("&")
                        }
                        
                        val title = item.optString("name", "Unknown YouTube Video")
                        val artist = item.optString("uploaderName", item.optString("uploader", "YouTube Music"))
                        val album = if (filterSongsOnly) "YT Music" else "YouTube"
                        var imageUrl = item.optString("thumbnail", "")
                        if (imageUrl.startsWith("http://")) {
                            imageUrl = imageUrl.replace("http://", "https://")
                        }
                        val durationSeconds = item.optLong("duration", 210L)
                        val durationMs = if (durationSeconds > 0) durationSeconds * 1000L else 210000L
                        
                        if (videoId.isNotEmpty()) {
                            list.add(
                                SongModel(
                                    id = videoId,
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    thumbnailUrl = imageUrl,
                                    streamUrl = null, // Dynamically resolved on PlayTime
                                    source = if (filterSongsOnly) MusicSource.YOUTUBE_MUSIC else MusicSource.YOUTUBE,
                                    durationMs = durationMs
                                )
                            )
                        }
                    }
                }
                if (list.isNotEmpty()) {
                    savePipedInstance(instance)
                    return list
                }
            } catch (e: Exception) {
                Log.e("FallenApi", "Youtube Piped search error on $instance: ${e.message}")
            }
        }
        
        // Fallback to Invidious search if all Piped instances fail or rate limit
        if (list.isEmpty()) {
            Log.d("FallenApi", "YouTube Piped searches returned empty. Performing high-reliability Invidious search fallback...")
            for (instance in getSortedInvidiousInstances()) {
                try {
                    val url = "$instance/api/v1/search?q=$encodedQuery&type=video"
                    val request = Request.Builder().url(url).build()
                    pipedClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use
                        val body = response.body?.string() ?: return@use
                        
                        val jsonArray = JSONArray(body)
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            val videoId = item.optString("videoId", "")
                            if (videoId.isNotEmpty()) {
                                val title = item.optString("title", "Unknown")
                                val artist = item.optString("author", "YouTube")
                                val durationSeconds = item.optLong("lengthSeconds", 210L)
                                val durationMs = durationSeconds * 1000L
                                
                                var imageUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                                val thumbArray = item.optJSONArray("videoThumbnails")
                                if (thumbArray != null && thumbArray.length() > 0) {
                                    val firstThumb = thumbArray.getJSONObject(0)
                                    val thumbUrl = firstThumb.optString("url", "")
                                    if (thumbUrl.isNotEmpty() && !thumbUrl.startsWith("/")) {
                                        imageUrl = thumbUrl
                                    }
                                }
                                if (imageUrl.startsWith("http://")) {
                                    imageUrl = imageUrl.replace("http://", "https://")
                                }
                                
                                list.add(
                                    SongModel(
                                        id = videoId,
                                        title = title,
                                        artist = artist,
                                        album = if (filterSongsOnly) "YT Music (Invid)" else "YouTube (Invid)",
                                        thumbnailUrl = imageUrl,
                                        streamUrl = null,
                                        source = if (filterSongsOnly) MusicSource.YOUTUBE_MUSIC else MusicSource.YOUTUBE,
                                        durationMs = durationMs
                                    )
                                )
                            }
                        }
                    }
                    if (list.isNotEmpty()) {
                        saveInvidiousInstance(instance)
                        Log.d("FallenApi", "Successfully retrieved ${list.size} results from invidious: $instance")
                        return list
                    }
                } catch (ex: Exception) {
                    Log.e("FallenApi", "Invidious search error on $instance: ${ex.message}")
                }
            }
        }
        return list
    }

    private fun getPlayablePipedUrl(stream: JSONObject, instanceUrl: String): String {
        var proxyUrl = stream.optString("proxyUrl", "")
        if (proxyUrl.isNotEmpty()) {
            if (!proxyUrl.startsWith("http://") && !proxyUrl.startsWith("https://")) {
                val cleanInstance = instanceUrl.removeSuffix("/")
                val rel = if (proxyUrl.startsWith("/")) proxyUrl else "/$proxyUrl"
                proxyUrl = "$cleanInstance$rel"
            }
            Log.d("FallenApi", "Resolved absolute Piped proxyUrl: $proxyUrl")
            return proxyUrl
        }
        
        var directUrl = stream.optString("url", "")
        if (directUrl.isNotEmpty()) {
            if (!directUrl.startsWith("http://") && !directUrl.startsWith("https://")) {
                val cleanInstance = instanceUrl.removeSuffix("/")
                val rel = if (directUrl.startsWith("/")) directUrl else "/$directUrl"
                directUrl = "$cleanInstance$rel"
            }
        }
        
        if (directUrl.contains("googlevideo.com/videoplayback")) {
            try {
                val index = directUrl.indexOf("/videoplayback")
                if (index != -1) {
                    val pathAndQuery = directUrl.substring(index)
                    val cleanInstance = instanceUrl.removeSuffix("/")
                    val constructedProxy = "$cleanInstance/proxy$pathAndQuery"
                    Log.d("FallenApi", "Constructed manual Piped proxy URL: $constructedProxy")
                    return constructedProxy
                }
            } catch (e: Exception) {
                Log.e("FallenApi", "Failed to construct manual Piped proxy: ${e.message}")
            }
        }
        return directUrl
    }

    private fun getPlayableInvidiousUrl(streamUrl: String, instanceUrl: String): String {
        var resolvedUrl = streamUrl
        if (resolvedUrl.isNotEmpty()) {
            if (!resolvedUrl.startsWith("http://") && !resolvedUrl.startsWith("https://")) {
                val cleanInstance = instanceUrl.removeSuffix("/")
                val rel = if (resolvedUrl.startsWith("/")) resolvedUrl else "/$resolvedUrl"
                resolvedUrl = "$cleanInstance$rel"
            }
        }
        
        if (resolvedUrl.contains("googlevideo.com/videoplayback")) {
            try {
                val index = resolvedUrl.indexOf("/videoplayback")
                if (index != -1) {
                    val pathAndQuery = resolvedUrl.substring(index)
                    val cleanInstance = instanceUrl.removeSuffix("/")
                    val separator = if (pathAndQuery.contains("?")) "&" else "?"
                    val constructedProxy = "$cleanInstance$pathAndQuery$separator" + "local=true"
                    Log.d("FallenApi", "Constructed manual Invidious proxy URL: $constructedProxy")
                    return constructedProxy
                }
            } catch (e: Exception) {
                Log.e("FallenApi", "Failed to construct manual Invidious proxy: ${e.message}")
            }
        }
        return resolvedUrl
    }

    // Resolve direct Audio Stream URL for YouTube item using Video ID
    fun resolveYoutubeStreamUrl(videoId: String): String? {
        Log.d("FallenApi", "Resolving youtube stream in parallel for videoId: $videoId")
        
        // Try Piped instances first in parallel (taking up to 6 instances to avoid overwhelming pool)
        val pipedInstances = getSortedPipedInstances().take(6)
        val resolvedUrl = runBlocking {
            val successfulResult = CompletableDeferred<String>()
            val jobs = mutableListOf<Job>()
            var activeCount = pipedInstances.size
            if (activeCount == 0) {
                successfulResult.complete("")
            }
            
            pipedInstances.forEach { instance ->
                val job = launch(Dispatchers.IO) {
                    try {
                        val url = "$instance/streams/$videoId"
                        val request = Request.Builder().url(url).build()
                        pipedClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string()
                                if (body != null) {
                                    val rootObj = JSONObject(body)
                                    val audioStreams = rootObj.optJSONArray("audioStreams")
                                    if (audioStreams != null) {
                                        var bestUrl: String? = null
                                        var maxBitrate = -1
                                        
                                        // First pass: compatible AAC / M4A streams
                                        for (i in 0 until audioStreams.length()) {
                                            val stream = audioStreams.getJSONObject(i)
                                            val streamUrl = getPlayablePipedUrl(stream, instance)
                                            val bitrate = stream.optInt("bitrate", -1)
                                            val mimeType = stream.optString("mimeType", "")
                                            val format = stream.optString("format", "")
                                            val codec = stream.optString("codec", "")
                                            
                                            val isSupported = mimeType.contains("mp4", ignoreCase = true) || 
                                                              format.contains("m4a", ignoreCase = true) ||
                                                              codec.contains("mp4a", ignoreCase = true)
                                            
                                            if (isSupported && streamUrl.isNotEmpty() && bitrate > maxBitrate) {
                                                maxBitrate = bitrate
                                                bestUrl = streamUrl
                                            }
                                        }
                                        
                                        // Second pass backup
                                        if (bestUrl == null) {
                                            maxBitrate = -1
                                            for (i in 0 until audioStreams.length()) {
                                                val stream = audioStreams.getJSONObject(i)
                                                val streamUrl = getPlayablePipedUrl(stream, instance)
                                                val bitrate = stream.optInt("bitrate", -1)
                                                if (streamUrl.isNotEmpty() && bitrate > maxBitrate) {
                                                    maxBitrate = bitrate
                                                    bestUrl = streamUrl
                                                }
                                            }
                                        }
                                        
                                        if (bestUrl != null) {
                                            savePipedInstance(instance)
                                            Log.d("FallenApi", "Successfully resolved streaming URL from Piped: $bestUrl on $instance")
                                            successfulResult.complete(bestUrl)
                                            return@use
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FallenApi", "Parallel Piped stream resolve failed on $instance: ${e.message}")
                        if (instance == lastWorkingPipedInstance) {
                            lastWorkingPipedInstance = null
                            preferences?.edit()?.remove("last_working_piped_instance")?.apply()
                        }
                    } finally {
                        synchronized(successfulResult) {
                            activeCount--
                            if (activeCount == 0 && !successfulResult.isCompleted) {
                                successfulResult.complete("")
                            }
                        }
                    }
                }
                jobs.add(job)
            }
            
            val finalUrl = try {
                withTimeoutOrNull(5000) {
                    successfulResult.await()
                }
            } catch (e: Exception) {
                null
            }
            jobs.forEach { it.cancel() }
            if (finalUrl.isNullOrEmpty()) null else finalUrl
        }
        
        if (resolvedUrl != null) return resolvedUrl
        
        // Fallback 1: Try Invidious API in parallel
        Log.d("FallenApi", "All parallel Piped instances failed. Trying parallel Invidious API...")
        val invidiousInstances = getSortedInvidiousInstances().take(5)
        val resolvedInvidiousUrl = runBlocking {
            val successfulResult = CompletableDeferred<String>()
            val jobs = mutableListOf<Job>()
            var activeCount = invidiousInstances.size
            if (activeCount == 0) {
                successfulResult.complete("")
            }
            
            invidiousInstances.forEach { instance ->
                val job = launch(Dispatchers.IO) {
                    try {
                        val url = "$instance/api/v1/videos/$videoId"
                        val request = Request.Builder().url(url).build()
                        pipedClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string()
                                if (body != null) {
                                    val rootObj = JSONObject(body)
                                    val adaptiveFormats = rootObj.optJSONArray("adaptiveFormats")
                                    if (adaptiveFormats != null) {
                                        var bestUrl: String? = null
                                        var maxBitrate = -1
                                        for (i in 0 until adaptiveFormats.length()) {
                                            val formatObj = adaptiveFormats.getJSONObject(i)
                                            val streamUrl = formatObj.optString("url")
                                            val type = formatObj.optString("type", "")
                                            val container = formatObj.optString("container", "")
                                            val bitrateStr = formatObj.optString("bitrate", "")
                                            val bitrate = bitrateStr.toIntOrNull() ?: -1
                                            
                                            val isAudio = type.contains("audio", ignoreCase = true)
                                            val isSupported = type.contains("mp4", ignoreCase = true) || container.contains("m4a", ignoreCase = true) || type.contains("mp4a", ignoreCase = true)
                                            
                                            if (isAudio && streamUrl.isNotEmpty()) {
                                                val playableUrl = getPlayableInvidiousUrl(streamUrl, instance)
                                                if (isSupported && bitrate > maxBitrate) {
                                                    maxBitrate = bitrate
                                                    bestUrl = playableUrl
                                                } else if (bestUrl == null) {
                                                    bestUrl = playableUrl
                                                }
                                            }
                                        }
                                        if (bestUrl != null) {
                                            saveInvidiousInstance(instance)
                                            Log.d("FallenApi", "Successfully resolved streaming URL from Invidious API: $bestUrl on $instance")
                                            successfulResult.complete(bestUrl)
                                            return@use
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FallenApi", "Parallel Invidious API resolve failed on $instance: ${e.message}")
                        if (instance == lastWorkingInvidiousInstance) {
                            lastWorkingInvidiousInstance = null
                            preferences?.edit()?.remove("last_working_invidious_instance")?.apply()
                        }
                    } finally {
                        synchronized(successfulResult) {
                            activeCount--
                            if (activeCount == 0 && !successfulResult.isCompleted) {
                                successfulResult.complete("")
                            }
                        }
                    }
                }
                jobs.add(job)
            }
            
            val finalUrl = try {
                withTimeoutOrNull(5000) {
                    successfulResult.await()
                }
            } catch (e: Exception) {
                null
            }
            jobs.forEach { it.cancel() }
            if (finalUrl.isNullOrEmpty()) null else finalUrl
        }
        
        if (resolvedInvidiousUrl != null) return resolvedInvidiousUrl
        
        // Fallback 2: Try Invidious direct latest_version proxied stream in parallel
        Log.d("FallenApi", "Invidious API failed. Trying manual redirections in parallel...")
        val directInstances = getSortedInvidiousInstances().take(4)
        val resolvedRedirectUrl = runBlocking {
            val successfulResult = CompletableDeferred<String>()
            val jobs = mutableListOf<Job>()
            var activeCount = directInstances.size
            if (activeCount == 0) {
                successfulResult.complete("")
            }
            
            directInstances.forEach { instance ->
                val job = launch(Dispatchers.IO) {
                    try {
                        val cleanInstance = instance.removeSuffix("/")
                        val proxiedUrl = "$cleanInstance/latest_version?id=$videoId&itag=140&local=true"
                        val request = Request.Builder().url(proxiedUrl).head().build()
                        pipedClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful || response.code == 200 || response.code == 206) {
                                saveInvidiousInstance(instance)
                                Log.d("FallenApi", "Successfully verified Invidious proxied stream: $proxiedUrl on $instance")
                                successfulResult.complete(proxiedUrl)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FallenApi", "Invidious stream verification failed on $instance: ${e.message}")
                        if (instance == lastWorkingInvidiousInstance) {
                            lastWorkingInvidiousInstance = null
                            preferences?.edit()?.remove("last_working_invidious_instance")?.apply()
                        }
                    } finally {
                        synchronized(successfulResult) {
                            activeCount--
                            if (activeCount == 0 && !successfulResult.isCompleted) {
                                successfulResult.complete("")
                            }
                        }
                    }
                }
                jobs.add(job)
            }
            
            val finalUrl = try {
                withTimeoutOrNull(5000) {
                    successfulResult.await()
                }
            } catch (e: Exception) {
                null
            }
            jobs.forEach { it.cancel() }
            if (finalUrl.isNullOrEmpty()) null else finalUrl
        }
        
        return resolvedRedirectUrl
    }

    // Fetches scrolling time-synced or raw lyrics
    fun fetchLyrics(artist: String, title: String): Pair<String, Boolean> {
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
            println("SOURCE ERROR: $e\n${Log.getStackTraceString(e)}")
            Log.e("FallenApi", "SOURCE ERROR: $e\n${Log.getStackTraceString(e)}")
        }
        return Pair("No lyrics found for this track. Enjoy the wave! 🎵", false)
    }
}
