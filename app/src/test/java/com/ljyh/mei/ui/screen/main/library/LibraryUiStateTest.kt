package com.ljyh.mei.ui.screen.main.library

import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.response.UserAlbumList
import com.ljyh.mei.data.network.Resource
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryUiStateTest {
    @Test
    fun accountFailureWithoutCachedProfileIsAnError() {
        val state = resolveLibraryUiState(
            accountResource = Resource.Error("offline"),
            profile = null,
            playlists = emptyList(),
            albumResource = Resource.Loading,
            section = LibrarySection.Created,
            now = 1_000L,
        )

        assertEquals(LibraryUiState.Error("offline"), state)
    }

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

    @Test
    fun albumSectionDoesNotReportEmptyWhileItsRequestIsUnfinishedOrFailed() {
        val profile = LibraryProfileUi("owner", "name", "avatar", "")
        fun state(albums: Resource<UserAlbumList>) = resolveLibraryUiState(
            accountResource = Resource.Loading,
            profile = profile,
            playlists = emptyList(),
            albumResource = albums,
            section = LibrarySection.Albums,
            now = 1_000L,
        )

        assertEquals(LibraryUiState.Loading, state(Resource.Loading))
        assertEquals(LibraryUiState.Error("请求失败"), state(Resource.Error("请求失败")))
        assertEquals(
            listOf(42L),
            (state(Resource.Success(UserAlbumList(code = 200, data = listOf(
                UserAlbumList.Data(id = 42, name = "已收藏专辑"),
            )))) as LibraryUiState.Content).data.albums.map { it.id },
        )
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
