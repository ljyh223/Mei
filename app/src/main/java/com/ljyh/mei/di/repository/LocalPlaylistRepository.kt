package com.ljyh.mei.di.repository

import com.ljyh.mei.data.model.domain.UserPlaylistItem
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.PlaylistType
import com.ljyh.mei.data.model.room.PlaylistSongCrossRef
import com.ljyh.mei.di.dao.PlaylistDao
import com.ljyh.mei.di.dao.PlaylistSongCrossRefDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class LocalPlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val trackDao: PlaylistSongCrossRefDao,
) {
    private val writes = Mutex()
    private var revision = 0L
    private val contentRevisions = mutableMapOf<String, Long>()

    suspend fun getPlaylist(id: String): Playlist? = playlistDao.getPlaylist(id)
    suspend fun getPlaylistByAuthor(author: String): List<Playlist> = playlistDao.getPlaylistByAuthor(author)
    fun getAllPlaylist(): Flow<List<Playlist>> = playlistDao.getAllPlaylist()
    fun playlistsContaining(songId: Long): Flow<Set<String>> =
        trackDao.getPlaylistIdsWithSong(songId.toString()).map { it.toSet() }
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
    suspend fun recordTrackMutation(
        id: String,
        count: Int?,
        addedTrackIds: List<Long> = emptyList(),
        removedTrackIds: List<Long> = emptyList(),
    ): Long = writes.withLock {
        val next = ++revision
        contentRevisions[id] = next
        playlistDao.updatePlaylistContent(id, count?.takeIf { it >= 0 }, null)
        if (addedTrackIds.isNotEmpty()) mergeKnownTracks(id, addedTrackIds)
        if (removedTrackIds.isNotEmpty()) trackDao.deleteTracks(id, removedTrackIds.map(Long::toString))
        next
    }

    suspend fun updatePlaylistSummary(
        id: String,
        count: Int,
        cover: String,
        requestedAtRevision: Long,
        trackIds: List<Long>? = null,
    ): Boolean = writes.withLock {
        if (count < 0 || (contentRevisions[id] ?: 0L) > requestedAtRevision) return@withLock false
        val hasSummary = playlistDao.getPlaylist(id) != null
        if (!hasSummary && trackIds == null) return@withLock false
        if (hasSummary) playlistDao.updatePlaylistContent(id, count, cover)
        if (trackIds != null) storeTracks(id, trackIds, complete = trackIds.distinct().size == count)
        contentRevisions[id] = ++revision
        true
    }

    suspend fun updatePlaylistMembership(
        id: String,
        trackIds: List<Long>,
        complete: Boolean,
        requestedAtRevision: Long,
    ): Boolean = writes.withLock {
        if ((contentRevisions[id] ?: 0L) > requestedAtRevision) return@withLock false
        storeTracks(id, trackIds, complete)
        contentRevisions[id] = ++revision
        true
    }

    private suspend fun storeTracks(id: String, trackIds: List<Long>, complete: Boolean) {
        if (complete && trackIds.all { it > 0 }) {
            trackDao.replaceTracks(id, trackIds.distinct().mapIndexed { index, songId ->
                PlaylistSongCrossRef(id, songId.toString(), sortOrder = index)
            })
        } else {
            // Partial reads can confirm presence, but cannot prove an old song was removed.
            mergeKnownTracks(id, trackIds)
        }
    }

    private suspend fun mergeKnownTracks(id: String, trackIds: List<Long>) {
        val existing = trackDao.getSongIdsByPlaylist(id).first().toSet()
        trackDao.insertAll(trackIds.distinct().filter { it > 0 && it.toString() !in existing }
            .mapIndexed { index, songId -> PlaylistSongCrossRef(id, songId.toString(), existing.size + index) })
    }

    suspend fun deletePlaylistById(id: String) = writes.withLock {
        trackDao.deleteByPlaylist(id)
        playlistDao.deletePlaylistById(id)
    }
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
