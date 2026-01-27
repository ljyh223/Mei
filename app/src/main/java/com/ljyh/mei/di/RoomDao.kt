package com.ljyh.mei.di

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.ljyh.mei.data.model.room.AlbumArtistCrossRef
import com.ljyh.mei.data.model.room.AlbumEntity
import com.ljyh.mei.data.model.room.AlbumWithArtists
import com.ljyh.mei.data.model.room.ArtistEntity
import com.ljyh.mei.data.model.room.CacheColor
import com.ljyh.mei.data.model.room.HistoryItem
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.PlaybackHistory
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.model.room.Song
import kotlinx.coroutines.flow.Flow


@Dao
interface ColorDao {
    @Query("SELECT * FROM color where url=:url")
    fun getColor(url: String): CacheColor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColor(color: CacheColor)
}

@Dao
interface QQSongDao {
    @Query("SELECT * FROM qqSong where id=:id")
    fun getSong(id: String): Flow<QQSong?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(qqSong: QQSong)

    @Query("DELETE FROM qqSong WHERE id = :id")
    suspend fun deleteSongById(id: String)
}

@Dao
interface SongDao {
    @Query("SELECT * FROM song where id=:id")
    fun getSong(id: String): Flow<Song?>

    @Query("SELECT * FROM song")
    fun getAllSong(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist where id=:id")
    suspend fun getPlaylist(id: String): Playlist?

    @Query("SELECT * FROM playlist where author=:author")
    suspend fun getPlaylistByAuthor(author: String): List<Playlist>

    @Query("SELECT * FROM playlist")
    fun getAllPlaylist(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<Playlist>)

    @Query("DELETE FROM playlist where id=:id")
    suspend fun deletePlaylistById(id: String)
}

@Dao
interface LikeDao {
    @Query("SELECT * FROM `like` where id=:id")
    suspend fun getLike(id: String): Like?


    @Query("SELECT * FROM `like`")
    suspend fun getALlLike(): List<Like>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: Like)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateALlLike(likes: List<Like>)


    @Query("DELETE FROM `like` where id=:id")
    suspend fun deleteLike(id: String)
}


@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistory)

    @Transaction
    suspend fun addSongToHistory(song: Song) {
        insertOrUpdateSong(song)
        insertHistory(PlaybackHistory(songId = song.id, playedAt = System.currentTimeMillis()))
    }

    @Query(
        """
        SELECT song.*, history.playedAt 
        FROM playback_history AS history
        INNER JOIN song ON history.songId = song.id
        ORDER BY history.playedAt DESC
    """
    )
    fun getHistory(): Flow<List<HistoryItem>>

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("DELETE FROM playback_history WHERE songId = :songId")
    suspend fun deleteHistoryBySongId(songId: String)
}

@Dao
interface AlbumsDao {
    @Transaction
    @Query("SELECT * FROM albums WHERE albumId = :id")
    suspend fun getAlbumWithArtists(id: Long): AlbumWithArtists

    @Transaction
    @Query(
        """
        SELECT * FROM albums
        INNER JOIN album_artist_cross_ref
        ON albums.albumId = album_artist_cross_ref.albumId
        WHERE album_artist_cross_ref.artistId = :artistId"""
    )
    suspend fun getAlbumsByArtist(artistId: Long): List<AlbumEntity>




    @Transaction
    suspend fun insertAlbumWithArtists(
        album: AlbumEntity,
        artists: List<ArtistEntity>
    ) {
        // 1. 插入 album
        insertAlbum(album)

        // 2. 插入 artists（已存在的会被忽略）
        insertArtists(artists)

        // 3. 建立关联
        val refs = artists.map {
            AlbumArtistCrossRef(
                albumId = album.albumId,
                artistId = it.artistId
            )
        }
        insertAlbumArtistRefs(refs)
    }

    // --- 基础方法 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbumArtistRefs(
        refs: List<AlbumArtistCrossRef>
    )
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM albums WHERE albumId = :albumId
        )
    """)
    suspend fun existsAlbum(albumId: Long): Boolean

    /**
     * 删除指定ID的专辑及其关联关系
     * 注意：这会删除专辑，但不会删除关联的艺术家（其他专辑可能还在使用）
     */
    @Transaction
    suspend fun deleteAlbumById(albumId: Long) {
        // 1. 先删除关联关系
        deleteAlbumArtistRefsByAlbumId(albumId)

        // 2. 再删除专辑
        deleteAlbum(albumId)
    }

    /**
     * 批量删除多个专辑及其关联关系
     */
    @Transaction
    suspend fun deleteAlbumsByIds(albumIds: List<Long>) {
        if (albumIds.isEmpty()) return

        // 1. 删除关联关系
        deleteAlbumArtistRefsByAlbumIds(albumIds)

        // 2. 删除专辑
        deleteAlbums(albumIds)
    }

    /**
     * 删除指定艺术家参与的所有专辑关联关系
     * 注意：这会移除该艺术家与专辑的关联，但不会删除专辑本身
     */
    suspend fun deleteArtistFromAllAlbums(artistId: Long) {
        deleteAlbumArtistRefsByArtistId(artistId)
    }

    /**
     * 删除孤立的艺术家（没有被任何专辑引用的艺术家）
     * 用于清理不再使用的艺术家数据
     */
    @Transaction
    suspend fun deleteOrphanedArtists() {
        // 找到没有关联的艺术家
        val orphanedArtistIds = getOrphanedArtistIds()
        if (orphanedArtistIds.isNotEmpty()) {
            deleteArtistsByIds(orphanedArtistIds)
        }
    }


    @Delete
    suspend fun deleteAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE albumId = :albumId")
    suspend fun deleteAlbum(albumId: Long)

    @Query("DELETE FROM albums WHERE albumId IN (:albumIds)")
    suspend fun deleteAlbums(albumIds: List<Long>)

    @Delete
    suspend fun deleteArtist(artist: ArtistEntity)

    @Query("DELETE FROM artists WHERE artistId = :artistId")
    suspend fun deleteArtist(artistId: Long)

    @Query("DELETE FROM artists WHERE artistId IN (:artistIds)")
    suspend fun deleteArtistsByIds(artistIds: List<Long>)

    @Delete
    suspend fun deleteAlbumArtistRef(ref: AlbumArtistCrossRef)

    @Query("DELETE FROM album_artist_cross_ref WHERE albumId = :albumId")
    suspend fun deleteAlbumArtistRefsByAlbumId(albumId: Long)

    @Query("DELETE FROM album_artist_cross_ref WHERE albumId IN (:albumIds)")
    suspend fun deleteAlbumArtistRefsByAlbumIds(albumIds: List<Long>)

    @Query("DELETE FROM album_artist_cross_ref WHERE artistId = :artistId")
    suspend fun deleteAlbumArtistRefsByArtistId(artistId: Long)

    // --- 查询辅助方法 ---

    /**
     * 查找孤立的艺术家ID（没有被任何专辑引用的艺术家）
     */
    @Query("""
        SELECT artistId FROM artists
        WHERE artistId NOT IN (
            SELECT DISTINCT artistId FROM album_artist_cross_ref
        )
    """)
    suspend fun getOrphanedArtistIds(): List<Long>

    /**
     * 检查艺术家是否被任何专辑使用
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM album_artist_cross_ref WHERE artistId = :artistId
        )
    """)
    suspend fun isArtistUsed(artistId: Long): Boolean

}


@Database(
    entities = [CacheColor::class, Song::class, Like::class, QQSong::class, Playlist::class, PlaybackHistory::class, AlbumEntity::class, ArtistEntity::class, AlbumArtistCrossRef::class],
    version = 8
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun colorDao(): ColorDao
    abstract fun songDao(): SongDao
    abstract fun likeDao(): LikeDao
    abstract fun qqSongDao(): QQSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun AlbumsDao(): AlbumsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}