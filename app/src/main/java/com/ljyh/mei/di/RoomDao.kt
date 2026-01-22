package com.ljyh.mei.di

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
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
    suspend fun deletePlaylistById(id:String)
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

@Database(
    entities = [CacheColor::class, Song::class, Like::class, QQSong::class, Playlist::class, PlaybackHistory::class],
    version = 7
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun colorDao(): ColorDao
    abstract fun songDao(): SongDao
    abstract fun likeDao(): LikeDao
    abstract fun qqSongDao(): QQSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao

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