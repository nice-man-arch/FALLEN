package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.io.Serializable

enum class MusicSource {
    YOUTUBE,
    YOUTUBE_MUSIC,
    JIO_SAAVN
}

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val thumbnailUrl: String,
    val streamUrl: String?,
    val source: String, // Matches MusicSource enum
    val durationMs: Long,
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val addedToHistoryAt: Long? = null
) : Serializable {
    fun toSongModel(): SongModel {
        var cleanThumb = thumbnailUrl
        if (cleanThumb.startsWith("http://")) {
            cleanThumb = cleanThumb.replace("http://", "https://")
        }
        if (cleanThumb.contains("saavncdn.com")) {
            cleanThumb = cleanThumb
                .replace("150x150", "500x500")
                .replace("50x50", "500x500")
                .replace("250x250", "500x500")
                .replace("350x350", "500x500")
        } else if (cleanThumb.contains("img.youtube.com")) {
            cleanThumb = cleanThumb.replace("mqdefault.jpg", "hqdefault.jpg")
        }
        return SongModel(
            id = id,
            title = title,
            artist = artist,
            album = album,
            thumbnailUrl = cleanThumb,
            streamUrl = streamUrl,
            source = MusicSource.valueOf(source),
            durationMs = durationMs,
            isLiked = isLiked,
            isDownloaded = isDownloaded,
            localFilePath = localFilePath
        )
    }
}

data class SongModel(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val thumbnailUrl: String,
    val streamUrl: String?,
    val source: MusicSource,
    val durationMs: Long,
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null
) : Serializable {
    fun toEntity(): SongEntity {
        return SongEntity(
            id = id,
            title = title,
            artist = artist,
            album = album,
            thumbnailUrl = thumbnailUrl,
            streamUrl = streamUrl,
            source = source.name,
            durationMs = durationMs,
            isLiked = isLiked,
            isDownloaded = isDownloaded,
            localFilePath = localFilePath
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "playlist_song_cross_ref", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String
)

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<SongEntity>
)

@Dao
interface MusicDao {
    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE isLiked = 1")
    fun getLikedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isDownloaded = 1")
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE addedToHistoryAt IS NOT NULL ORDER BY addedToHistoryAt DESC LIMIT 20")
    fun getRecentlyPlayedFlow(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    // Upsert mechanism that retains specific flags like isLiked / isDownloaded if already exists
    @Transaction
    suspend fun upsertSong(song: SongEntity) {
        val existing = getSongById(song.id)
        if (existing != null) {
            val merged = song.copy(
                isLiked = existing.isLiked || song.isLiked,
                isDownloaded = existing.isDownloaded || song.isDownloaded,
                localFilePath = existing.localFilePath ?: song.localFilePath,
                addedToHistoryAt = song.addedToHistoryAt ?: existing.addedToHistoryAt
            )
            insertSong(merged)
        } else {
            insertSong(song)
        }
    }

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :id")
    suspend fun updateLikedStatus(id: String, isLiked: Boolean)

    @Query("UPDATE songs SET isDownloaded = :isDownloaded, localFilePath = :localFilePath WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, localFilePath: String?)

    @Query("UPDATE songs SET addedToHistoryAt = :timestamp WHERE id = :id")
    suspend fun updateHistoryTimestamp(id: String, timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)

    @Query("SELECT * FROM playlists")
    fun getPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithSongsFlow(playlistId: Long): Flow<PlaylistWithSongs?>
}

@Database(entities = [SongEntity::class, PlaylistEntity::class, PlaylistSongCrossRef::class], version = 1, exportSchema = false)
abstract class FallenDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: FallenDatabase? = null

        fun getDatabase(context: Context): FallenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FallenDatabase::class.java,
                    "fallen_music_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                updateInstance(instance)
                instance
            }
        }

        private fun updateInstance(db: FallenDatabase) {
            INSTANCE = db
        }
    }
}
