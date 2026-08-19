package com.ljyh.mei.ui.screen.main.library

import com.ljyh.mei.data.model.room.Playlist
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryUiStateTest {
    @Test
    fun contentOnlyContainsPlaylistsOwnedOrCollectedByCurrentUser() {
        val state = buildLibraryUiState(
            profile = LibraryProfileUi(
                userId = "owner",
                nickname = "name",
                avatarUrl = "avatar",
                signature = "signature",
            ),
            section = LibrarySection.Collected,
            playlists = listOf(
                playlist(id = "created", author = "owner"),
                playlist(id = "collected", author = "someone-else"),
            ),
            albums = emptyList(),
            now = 1_000L,
        )

        assertEquals(LibrarySection.Collected, state.section)
        assertEquals(listOf("created"), state.createdPlaylists.map { it.id })
        assertEquals(listOf("collected"), state.collectedPlaylists.map { it.id })
    }

    private fun playlist(id: String, author: String) = Playlist(
        id = id,
        title = id,
        cover = "cover",
        author = author,
        authorName = author,
        authorAvatar = "avatar",
        count = 1,
    )
}
