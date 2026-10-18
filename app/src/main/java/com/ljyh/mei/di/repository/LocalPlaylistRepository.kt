package com.ljyh.mei.di.repository

import com.ljyh.mei.data.model.domain.UserPlaylistItem
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.PlaylistType
import com.ljyh.mei.data.model.room.PlaylistSongCrossRef
import com.ljyh.mei.di.dao.PlaylistDao
import com.ljyh.mei.di.dao.PlaylistSongCrossRefDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LocalPlaylistRepository @Inject constructor(private val playlistDao: PlaylistDao) {
    suspend fun getPlaylist(id: String): Playlist? = playlistDao.getPlaylist(id)
    suspend fun getPlaylistByAuthor(author: String): List<Playlist> = playlistDao.getPlaylistByAuthor(author)
    fun getAllPlaylist(): Flow<List<Playlist>> = playlistDao.getAllPlaylist()
    suspend fun insertPlaylist(playlist: Playlist) = playlistDao.insertPlaylist(playlist)
    suspend fun insertPlaylists(playlists: List<Playlist>) = playlistDao.insertPlaylists(playlists)

    suspend fun mergeUserPlaylists(remote: List<UserPlaylistItem>) {
        val existing = playlistDao.getAllPlaylist().first().associateBy(Playlist::id)
        val now = System.currentTimeMillis()
        playlistDao.insertPlaylists(remote.map { item ->
            val cached = existing[item.id.toString()]
            Playlist(
                id = item.id.toString(),
                title = item.name,
                cover = item.coverImgUrl,
                author = item.creator.userId.toString(),
                authorName = item.creator.nickname,
                authorAvatar = item.creator.avatarUrl,
                count = item.trackCount,
                type = cached?.type ?: PlaylistType.NETEAST,
                description = cached?.description,
                createdAt = cached?.createdAt ?: now,
                updatedAt = cached?.updatedAt ?: now,
                playCount = item.playCount,
                lastPlayTime = cached?.lastPlayTime ?: 0L,
                localPlayCount = cached?.localPlayCount ?: 0,
            )
        })
    }
    suspend fun deletePlaylistById(id: String) = playlistDao.deletePlaylistById(id)
    suspend fun touchPlaylist(id: String, timestamp: Long) = playlistDao.touchPlaylist(id, timestamp)
}

class PlaylistSongCrossRefRepository @Inject constructor(private val dao: PlaylistSongCrossRefDao) {
    fun getSongIdsByPlaylist(playlistId: String): Flow<List<String>> = dao.getSongIdsByPlaylist(playlistId)
    suspend fun insert(crossRef: PlaylistSongCrossRef) = dao.insert(crossRef)
    suspend fun insertAll(crossRefs: List<PlaylistSongCrossRef>) = dao.insertAll(crossRefs)
    suspend fun deleteByPlaylist(playlistId: String) = dao.deleteByPlaylist(playlistId)
    suspend fun delete(playlistId: String, songId: String) = dao.delete(playlistId, songId)
}
