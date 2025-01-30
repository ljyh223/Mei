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
import com.ljyh.music.data.model.room.Song
import dagger.Provides
import kotlinx.coroutines.flow.Flow


@Dao
interface ColorDao {
    @Query("SELECT * FROM color where url=:url")
    fun getColor(url: String): Color?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColor(color: Color)
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


@Database(entities = [Color::class, Song::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun colorDao(): ColorDao
    abstract fun songDao(): SongDao

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