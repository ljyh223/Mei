package com.ljyh.mei.di.repository

import com.ljyh.mei.data.model.domain.UserPlaylistItem
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.PlaylistType
import com.ljyh.mei.data.model.room.PlaylistSongCrossRef
import com.ljyh.mei.di.dao.PlaylistDao
import com.ljyh.mei.di.dao.PlaylistSongCrossRefDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class LocalPlaylistRepository @Inject constructor(private val playlistDao: PlaylistDao) {
    private val writes = Mutex()
    private var revision = 0L
    private val contentRevisions = mutableMapOf<String, Long>()

    suspend fun getPlaylist(id: String): Playlist? = playlistDao.getPlaylist(id)
    suspend fun getPlaylistByAuthor(author: String): List<Playlist> = playlistDao.getPlaylistByAuthor(author)
    fun getAllPlaylist(): Flow<List<Playlist>> = playlistDao.getAllPlaylist()
    suspend fun insertPlaylist(playlist: Playlist) = writes.withLock { playlistDao.insertPlaylist(playlist) }
    suspend fun insertPlaylists(playlists: List<Playlist>) = writes.withLock { playlistDao.insertPlaylists(playlists) }

    /** Capture before the remote read so an older response cannot undo a confirmed edit. */
    suspend fun playlistSyncRevision(): Long = writes.withLock { revision }

    suspend fun mergeUserPlaylists(remote: List<UserPlaylistItem>, requestedAtRevision: Long) = writes.withLock {
        val existing = playlistDao.getAllPlaylist().first().associateBy(Playlist::id)
        val now = System.currentTimeMillis()
        playlistDao.insertPlaylists(remote.map { item ->
            val id = item.id.toString()
            val cached = existing[id]
            val keepContent = cached != null && (contentRevisions[id] ?: 0L) > requestedAtRevision
            Playlist(
                id = id,
                title = item.name,
                cover = if (keepContent) cached!!.cover else item.coverImgUrl,
                author = item.creator.userId.toString(),
                authorName = item.creator.nickname,
                authorAvatar = item.creator.avatarUrl,
                count = if (keepContent) cached!!.count else item.trackCount,
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

    /** Publish an authoritative mutation count immediately, without guessing a cover or delta. */
    suspend fun recordTrackMutation(id: String, count: Int?): Long = writes.withLock {
        val next = ++revision
        contentRevisions[id] = next
        playlistDao.updatePlaylistContent(id, count?.takeIf { it >= 0 }, null)
        next
    }

    suspend fun updatePlaylistSummary(
        id: String,
        count: Int,
        cover: String,
        requestedAtRevision: Long,
    ): Boolean = writes.withLock {
        if (count < 0 || (contentRevisions[id] ?: 0L) > requestedAtRevision) return@withLock false
        if (playlistDao.getPlaylist(id) == null) return@withLock false
        playlistDao.updatePlaylistContent(id, count, cover)
        contentRevisions[id] = ++revision
        true
    }

    suspend fun deletePlaylistById(id: String) = writes.withLock { playlistDao.deletePlaylistById(id) }
    suspend fun touchPlaylist(id: String, timestamp: Long) =
        writes.withLock { playlistDao.touchPlaylist(id, timestamp) }
}

class PlaylistSongCrossRefRepository @Inject constructor(private val dao: PlaylistSongCrossRefDao) {
    fun getSongIdsByPlaylist(playlistId: String): Flow<List<String>> = dao.getSongIdsByPlaylist(playlistId)
    suspend fun insert(crossRef: PlaylistSongCrossRef) = dao.insert(crossRef)
    suspend fun insertAll(crossRefs: List<PlaylistSongCrossRef>) = dao.insertAll(crossRefs)
    suspend fun deleteByPlaylist(playlistId: String) = dao.deleteByPlaylist(playlistId)
    suspend fun delete(playlistId: String, songId: String) = dao.delete(playlistId, songId)
}
