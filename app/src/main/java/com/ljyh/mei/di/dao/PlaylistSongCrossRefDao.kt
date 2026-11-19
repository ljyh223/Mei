package com.ljyh.mei.di.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ljyh.mei.data.model.room.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistSongCrossRefDao {
    @Query("SELECT songId FROM playlist_song_cross_ref WHERE playlistId = :playlistId ORDER BY sortOrder")
    fun getSongIdsByPlaylist(playlistId: String): Flow<List<String>>

    @Query("SELECT playlistId FROM playlist_song_cross_ref WHERE songId = :songId")
    fun getPlaylistIdsWithSong(songId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: PlaylistSongCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(crossRefs: List<PlaylistSongCrossRef>)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: String)

    @Transaction
    suspend fun replaceTracks(playlistId: String, tracks: List<PlaylistSongCrossRef>) {
        deleteByPlaylist(playlistId)
        insertAll(tracks)
    }

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId IN (:songIds)")
    suspend fun deleteTracks(playlistId: String, songIds: List<String>)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun delete(playlistId: String, songId: String)
}
