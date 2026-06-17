package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.FallenApi
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Failure(val message: String) : UiState<Nothing>
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FallenDatabase.getDatabase(application)
    private val dao = db.musicDao()
    private val prefs = application.getSharedPreferences("fallen_prefs", Context.MODE_PRIVATE)

    // Source manager
    val activeSource = MutableStateFlow(MusicSource.JIO_SAAVN)

    // Search term and result state
    val searchTerms = MutableStateFlow("")
    val searchState = MutableStateFlow<UiState<List<SongModel>>>(UiState.Idle)
    private var searchJob: Job? = null

    // Saved queries for recent searches list
    val recentSearches = MutableStateFlow<List<String>>(emptyList())

    // UI custom styling configurations
    val amoledTheme = MutableStateFlow(false)
    val lightTheme = MutableStateFlow(false)
    val accentColorIndex = MutableStateFlow(0) // 0: Purple, 1: Pink, 2: Blue, 3: Mint, 4: Orange, 5: Green
    val backgroundType = MutableStateFlow(0) // 0: Classic, 1: AMOLED, 2: Deep Purple, 3: Midnight Blue, 4: Forest Green, 5: Titanium Slate, 6: Crimson Wine, 7: Custom Solid Color, 8: Custom Gradient Color
    val backgroundOpacity = MutableStateFlow(0.85f) // Transparency overlay for cards / glassmorphism
    val libraryPlacement = MutableStateFlow("tabs") // "tabs" (Bottom Bar), "home_top" (Top of Home Screen), "home_bottom" (Bottom of Home Screen)
    val customBgColor = MutableStateFlow(0xFF121212) // custom solid color hex
    val customBgGradientStart = MutableStateFlow(0xFF1D103D) // custom gradient start
    val customBgGradientEnd = MutableStateFlow(0xFF050512) // custom gradient end

    // Settings config values
    val audioQuality = MutableStateFlow("High") // Auto, Low, High
    val crossfadeDuration = MutableStateFlow(0) // 0-10s
    val defaultSourceConfig = MutableStateFlow("JioSaavn")

    // Home Screen Trends
    val trendingSongsState = MutableStateFlow<UiState<List<SongModel>>>(UiState.Idle)

    // Lyrics fetch state
    val songLyrics = MutableStateFlow<Pair<String, Boolean>>(Pair("", false)) // (Payload, isSynced)
    val isLyricsLoading = MutableStateFlow(false)

    init {
        // Set up FallenApi preferences and perform background probing
        FallenApi.preferences = prefs
        FallenApi.initCachedInstances()
        FallenApi.startBackgroundProbing()

        // Load initial states from SharedPreferences
        val savedSourceName = prefs.getString("active_source", MusicSource.JIO_SAAVN.name) ?: MusicSource.JIO_SAAVN.name
        activeSource.value = try { MusicSource.valueOf(savedSourceName) } catch (e: Exception) { MusicSource.JIO_SAAVN }
        
        amoledTheme.value = prefs.getBoolean("theme_amoled", false)
        lightTheme.value = prefs.getBoolean("theme_light", false)
        accentColorIndex.value = prefs.getInt("accent_color_idx", 0)
        backgroundType.value = prefs.getInt("theme_bg_type", 0)
        backgroundOpacity.value = prefs.getFloat("theme_bg_opacity", 0.85f)
        libraryPlacement.value = prefs.getString("library_placement", "tabs") ?: "tabs"
        customBgColor.value = prefs.getLong("custom_bg_col", 0xFF121212)
        customBgGradientStart.value = prefs.getLong("custom_bg_grad_start", 0xFF1D103D)
        customBgGradientEnd.value = prefs.getLong("custom_bg_grad_end", 0xFF050512)
        audioQuality.value = prefs.getString("audio_quality", "High") ?: "High"
        crossfadeDuration.value = prefs.getInt("crossfade", 0)
        defaultSourceConfig.value = prefs.getString("default_source_cfg", "JioSaavn") ?: "JioSaavn"
        
        // Load recent searches string list
        val recentStr = prefs.getString("recent_searches_csv", "") ?: ""
        if (recentStr.isNotEmpty()) {
            recentSearches.value = recentStr.split(",")
        }

        // Trigger loading home screen trends as first action
        reloadTrendingNow()
    }

    // Reactive lists from database
    val likedSongs: StateFlow<List<SongModel>> = dao.getLikedSongs()
        .map { entities -> entities.map { it.toSongModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedSongs: StateFlow<List<SongModel>> = dao.getDownloadedSongs()
        .map { entities -> entities.map { it.toSongModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<SongModel>> = dao.getRecentlyPlayedFlow()
        .map { entities -> entities.map { it.toSongModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = dao.getPlaylistsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playlistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?> {
        return dao.getPlaylistWithSongsFlow(playlistId)
    }

    // Settings adjustments
    fun setSource(source: MusicSource) {
        activeSource.value = source
        prefs.edit().putString("active_source", source.name).apply()
        // Reload trends on source change
        reloadTrendingNow()
        // If query is not empty, re-run search instantly on source toggle
        val q = searchTerms.value
        if (q.isNotEmpty()) {
            triggerSearch(q, forceInstant = true)
        }
    }

    fun setAmoledTheme(enabled: Boolean) {
        amoledTheme.value = enabled
        if (enabled) {
            lightTheme.value = false
        }
        prefs.edit().putBoolean("theme_amoled", enabled).putBoolean("theme_light", lightTheme.value).apply()
    }

    fun setLightTheme(enabled: Boolean) {
        lightTheme.value = enabled
        if (enabled) {
            amoledTheme.value = false
        }
        prefs.edit().putBoolean("theme_light", enabled).putBoolean("theme_amoled", amoledTheme.value).apply()
    }

    fun setAccentColorIndex(idx: Int) {
        accentColorIndex.value = idx
        prefs.edit().putInt("accent_color_idx", idx).apply()
    }

    fun setBackgroundType(type: Int) {
        backgroundType.value = type
        prefs.edit().putInt("theme_bg_type", type).apply()
        // If they chose AMOLED black, set amoledTheme state to true, otherwise false
        if (type == 1) {
            amoledTheme.value = true
            lightTheme.value = false
            prefs.edit().putBoolean("theme_amoled", true).putBoolean("theme_light", false).apply()
        } else {
            if (amoledTheme.value) {
                amoledTheme.value = false
                prefs.edit().putBoolean("theme_amoled", false).apply()
            }
        }
    }

    fun setCustomBgColor(colorHex: Long) {
        customBgColor.value = colorHex
        prefs.edit().putLong("custom_bg_col", colorHex).apply()
    }

    fun setCustomBgGradient(startHex: Long, endHex: Long) {
        customBgGradientStart.value = startHex
        customBgGradientEnd.value = endHex
        prefs.edit()
            .putLong("custom_bg_grad_start", startHex)
            .putLong("custom_bg_grad_end", endHex)
            .apply()
    }

    fun setBackgroundOpacity(opacity: Float) {
        backgroundOpacity.value = opacity
        prefs.edit().putFloat("theme_bg_opacity", opacity).apply()
    }

    fun setLibraryPlacement(placement: String) {
        libraryPlacement.value = placement
        prefs.edit().putString("library_placement", placement).apply()
    }

    fun setAudioQuality(quality: String) {
        audioQuality.value = quality
        prefs.edit().putString("audio_quality", quality).apply()
    }

    fun setCrossfadeDuration(duration: Int) {
        crossfadeDuration.value = duration
        prefs.edit().putInt("crossfade", duration).apply()
    }

    // Trending Loader
    fun reloadTrendingNow() {
        viewModelScope.launch {
            trendingSongsState.value = UiState.Loading
            try {
                val songs = withContext(Dispatchers.IO) {
                    when (activeSource.value) {
                        MusicSource.JIO_SAAVN -> FallenApi.fetchJioSaavnCharts()
                        MusicSource.YOUTUBE -> FallenApi.searchYoutube("Top Trending Global Music Hits", false)
                        MusicSource.YOUTUBE_MUSIC -> FallenApi.searchYoutube("Top Songs", true)
                    }
                }
                trendingSongsState.value = UiState.Success(songs)
            } catch (e: Exception) {
                trendingSongsState.value = UiState.Failure("Failed to load trending music: ${e.message}")
            }
        }
    }

    // Database Actions
    fun toggleLikeSong(song: SongModel) {
        viewModelScope.launch {
            val currentlyLiked = likedSongs.value.any { it.id == song.id }
            val isLikedNow = !currentlyLiked
            val existing = dao.getSongById(song.id)
            if (existing == null) {
                dao.insertSong(song.toEntity().copy(isLiked = isLikedNow))
            } else {
                dao.updateLikedStatus(song.id, isLikedNow)
            }
        }
    }

    fun removeDownload(songId: String, localPath: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            localPath?.let {
                try {
                    val file = java.io.File(it)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to delete file: ${e.message}")
                }
            }
            dao.updateDownloadStatus(songId, isDownloaded = false, localFilePath = null)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            dao.deletePlaylist(playlistId)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isNotEmpty()) {
                dao.insertPlaylist(PlaylistEntity(name = name))
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: SongModel) {
        viewModelScope.launch {
            dao.upsertSong(song.toEntity())
            dao.insertPlaylistCrossRef(PlaylistSongCrossRef(playlistId, song.id))
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            dao.removeSongFromPlaylist(playlistId, songId)
        }
    }

    // Debounced Search Action
    fun updateSearchQuery(query: String) {
        searchTerms.value = query
        triggerSearch(query, forceInstant = false)
    }

    private fun triggerSearch(query: String, forceInstant: Boolean) {
        searchJob?.cancel()
        if (query.trim().isEmpty()) {
            searchState.value = UiState.Idle
            return
        }

        searchState.value = UiState.Loading
        searchJob = viewModelScope.launch {
            if (!forceInstant) {
                delay(350) // Debounce delay
            }
            try {
                val musicList = withContext(Dispatchers.IO) {
                    when (activeSource.value) {
                        MusicSource.JIO_SAAVN -> FallenApi.searchJioSaavn(query)
                        MusicSource.YOUTUBE -> FallenApi.searchYoutube(query, false)
                        MusicSource.YOUTUBE_MUSIC -> FallenApi.searchYoutube(query, true)
                    }
                }
                searchState.value = UiState.Success(musicList)
                if (musicList.isNotEmpty()) {
                    addRecentSearch(query)
                }
            } catch (e: Exception) {
                searchState.value = UiState.Failure("Search failed: ${e.message}")
            }
        }
    }

    private fun addRecentSearch(query: String) {
        val list = recentSearches.value.toMutableList()
        val formatted = query.trim()
        if (formatted.isEmpty()) return
        
        list.remove(formatted)
        list.add(0, formatted)
        
        val truncated = if (list.size > 8) list.subList(0, 8) else list
        recentSearches.value = truncated
        prefs.edit().putString("recent_searches_csv", truncated.joinToString(",")).apply()
    }

    fun clearRecentSearches() {
        recentSearches.value = emptyList()
        prefs.edit().putString("recent_searches_csv", "").apply()
    }

    // Fetch song lyrics
    fun fetchLyrics(song: SongModel) {
        isLyricsLoading.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    FallenApi.fetchLyrics(song.artist, song.title)
                }
                songLyrics.value = result
            } catch (e: Exception) {
                songLyrics.value = Pair("Lyrics failed to load.", false)
            } finally {
                isLyricsLoading.value = false
            }
        }
    }
}
