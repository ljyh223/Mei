package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.domain.PlaylistCreator
import com.ljyh.mei.data.model.domain.UserPlaylistItem
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.di.dao.PlaylistDao
import com.ljyh.mei.di.repository.LocalPlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalPlaylistRepositoryTest {
    @Test
    fun remoteSyncKeepsUsageForOwnedAndCollectedPlaylists() = runBlocking {
        val dao = FakePlaylistDao(listOf(
            playlist("1", "1", 8, 100L),
            playlist("2", "2", 3, 200L),
        ))
        val remote = listOf(remotePlaylist(1L, 1L, 10L), remotePlaylist(2L, 2L, 20L))

        LocalPlaylistRepository(dao).mergeUserPlaylists(remote)

        assertEquals(8, dao.rows.getValue("1").localPlayCount)
        assertEquals(100L, dao.rows.getValue("1").lastPlayTime)
        assertEquals(3, dao.rows.getValue("2").localPlayCount)
        assertEquals(200L, dao.rows.getValue("2").lastPlayTime)
        assertEquals(10L, dao.rows.getValue("1").playCount)
        assertEquals(20L, dao.rows.getValue("2").playCount)
    }

    private fun playlist(id: String, author: String, count: Int, lastPlayed: Long) = Playlist(
        id = id, title = "cached", cover = "cached-cover", author = author,
        authorName = author, authorAvatar = "avatar", count = 1,
        localPlayCount = count, lastPlayTime = lastPlayed,
    )

    private fun remotePlaylist(id: Long, author: Long, plays: Long) = UserPlaylistItem(
        id = id, name = "remote", coverImgUrl = "remote-cover",
        creator = PlaylistCreator(author, "author", "avatar"),
        playCount = plays, trackCount = 4,
    )

    private class FakePlaylistDao(initial: List<Playlist>) : PlaylistDao {
        val rows = initial.associateBy(Playlist::id).toMutableMap()
        override suspend fun getPlaylist(id: String): Playlist? = rows[id]
        override suspend fun getPlaylistByAuthor(author: String): List<Playlist> =
            rows.values.filter { it.author == author }
        override fun getAllPlaylist(): Flow<List<Playlist>> = flowOf(rows.values.toList())
        override suspend fun insertPlaylist(playlist: Playlist) { rows[playlist.id] = playlist }
        override suspend fun insertPlaylists(playlists: List<Playlist>) {
            playlists.forEach { rows[it.id] = it }
        }
        override suspend fun deletePlaylistById(id: String) { rows.remove(id) }
        override suspend fun touchPlaylist(id: String, timestamp: Long) {
            rows[id]?.let { rows[id] = it.copy(lastPlayTime = timestamp, localPlayCount = it.localPlayCount + 1) }
        }
    }
}
