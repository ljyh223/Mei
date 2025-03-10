package com.ljyh.music.di

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ljyh.music.data.model.room.Color
import com.ljyh.music.data.model.room.Like
import com.ljyh.music.data.model.room.QQSong
import com.ljyh.music.data.model.room.Song
import kotlinx.coroutines.flow.Flow


@Dao
interface ColorDao {
    @Query("SELECT * FROM color where url=:url")
    fun getColor(url: String): Color?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColor(color: Color)
}

@Dao
interface QQSongDao{
    @Query("SELECT * FROM qqSong where id=:id")
    fun getSong(id: String): Flow<QQSong?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(qqSong: QQSong)
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
interface LikeDao{
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


@Database(entities = [Color::class, Song::class, Like::class, QQSong::class], version = 6)
abstract class AppDatabase : RoomDatabase() {
    abstract fun colorDao(): ColorDao
    abstract fun songDao(): SongDao
    abstract fun likeDao(): LikeDao
    abstract fun qqSongDao(): QQSongDao

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