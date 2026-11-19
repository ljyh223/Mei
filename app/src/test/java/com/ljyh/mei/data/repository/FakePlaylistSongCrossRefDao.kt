package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.room.PlaylistSongCrossRef
import com.ljyh.mei.di.dao.PlaylistSongCrossRefDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class FakePlaylistSongCrossRefDao : PlaylistSongCrossRefDao {
    private val rows = MutableStateFlow<Map<Pair<String, String>, PlaylistSongCrossRef>>(emptyMap())
    override fun getSongIdsByPlaylist(playlistId: String) = rows.map { data ->
        data.values.filter { it.playlistId == playlistId }.sortedBy { it.sortOrder }.map { it.songId }
    }
    override fun getPlaylistIdsWithSong(songId: String) = rows.map { data ->
        data.values.filter { it.songId == songId }.map { it.playlistId }
    }
    override suspend fun insert(crossRef: PlaylistSongCrossRef) = insertAll(listOf(crossRef))
    override suspend fun insertAll(crossRefs: List<PlaylistSongCrossRef>) {
        rows.update { old -> old + crossRefs.associateBy { it.playlistId to it.songId } }
    }
    override suspend fun deleteByPlaylist(playlistId: String) {
        rows.update { old -> old.filterValues { it.playlistId != playlistId } }
    }
    override suspend fun deleteTracks(playlistId: String, songIds: List<String>) {
        rows.update { old -> old.filterValues { it.playlistId != playlistId || it.songId !in songIds } }
    }
    override suspend fun delete(playlistId: String, songId: String) = deleteTracks(playlistId, listOf(songId))
}
