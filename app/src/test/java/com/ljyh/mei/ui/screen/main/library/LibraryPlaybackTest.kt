package com.ljyh.mei.ui.screen.main.library

import com.ljyh.mei.data.model.response.MiniPlaylistDetail
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.playback.queue.QueueSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LibraryPlaybackTest {
    @Test fun incompleteMetadataPreservesFullTrackOrderForServiceResolution() = runBlocking {
        val queue = buildLibraryPlaylistQueue(detail(listOf(30L, 10L, 20L)))!!
        assertEquals(listOf("30", "10", "20"), queue.getInitialStatus().ids.map { it.first })
        assertTrue(queue.getInitialStatus().ids.all { it.second == null })
        assertEquals(QueueSource.Type.PLAYLIST, queue.source.type)
        assertEquals("42", queue.source.id)
        assertEquals("playlist_42", queue.id)
        assertEquals(0, queue.startIndex)
    }

    @Test fun emptyOrInvalidPlaylistDoesNotReplaceCurrentQueue() {
        assertNull(buildLibraryPlaylistQueue(detail(emptyList())))
        assertNull(buildLibraryPlaylistQueue(detail(listOf(1L)).copy(id = 0)))
    }

    @Test fun duplicateIdsDoNotRepeatTracksInTheQueue() = runBlocking {
        val queue = buildLibraryPlaylistQueue(detail(listOf(2L, 1L, 2L)))!!
        assertEquals(listOf("2", "1"), queue.getInitialStatus().ids.map { it.first })
    }

    @Test fun artworkUsesTheFirstTrackIdEvenWhenItsMetadataIsNotInTheDetail() {
        assertEquals(30L, firstLikedTrackId(detail(listOf(30L, 10L))))
        assertNull(firstLikedTrackId(detail(emptyList())))
    }

    @Test fun coverStageUsesThreeTracksInPlaylistOrderRatherThanMetadataOrder() {
        val playlist = detail(listOf(30L, 10L, 20L, 40L)).copy(tracks = listOf(track(20), track(10)))
        assertEquals(listOf(30L, 10L, 20L), likedArtworkTrackIds(playlist))
    }

    @Test fun coverStageFallsBackToAvailableTracksAndHandlesShortOrEmptyPlaylists() {
        assertEquals(listOf(20L, 10L), likedArtworkTrackIds(detail(emptyList()).copy(tracks = listOf(track(20), track(10)))))
        assertEquals(listOf(30L), likedArtworkTrackIds(detail(listOf(30L, 30L))))
        assertEquals(emptyList<Long>(), likedArtworkTrackIds(detail(emptyList())))
    }

    private fun track(id: Long) = MediaMetadata(id, "Song $id", "", emptyList(), 0, MediaMetadata.Album(1, "Album"))

    private fun detail(ids: List<Long>) = MiniPlaylistDetail(
        cover = emptyList(), name = "喜欢的音乐", description = null, id = 42,
        tracks = emptyList(), trackIds = ids, count = ids.size, creatorUserId = 9,
        createUserName = "用户", playCount = 0, subscribedCount = 0, subscribed = false,
    )
}
