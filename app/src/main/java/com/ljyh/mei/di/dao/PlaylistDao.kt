package com.ljyh.mei.di.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ljyh.mei.data.model.room.Playlist
import kotlinx.coroutines.flow.Flow

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
