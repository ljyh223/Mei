package com.ljyh.mei.ui.screen.main.library

import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.model.Album
import org.junit.Assert.*
import org.junit.Test

class LibraryBrowseStateTest {
    private val liked = playlist("liked", "owner", "我的喜欢的音乐")
    private val mine = playlist("mine", "owner", "Night Drive")
    private val saved = playlist("saved", "other", "Morning Jazz")
    private val album = Album(7, "Night Colours", "", 10, listOf(Album.Artist(1, "Aurora")))
    private fun state(created: List<Playlist> = listOf(liked, mine)) = LibraryContentUiState(
        profile = LibraryProfileUi("owner", "昵称", "", ""), section = LibrarySection.Created,
        createdPlaylists = created, collectedPlaylists = listOf(saved), albums = listOf(album),
    )

    @Test fun likedBelongsToCreatedAndIsFirstRegardlessOfSmartRanking() {
        val data = state(listOf(mine, liked, mine))
        assertEquals("liked", data.likedPlaylist()?.id)
        assertEquals(listOf("mine"), data.createdPlaylistOverview().map { it.id })
        assertEquals(listOf("liked", "mine"), data.playlistsIn(LibraryCollection.Created).map { it.id })
        assertEquals(listOf("saved"), data.playlistsIn(LibraryCollection.Collected).map { it.id })
    }

    @Test fun likedEntryRequiresCurrentOwnerAndRecognizedName() {
        assertNull(state(listOf(liked.copy(author = "other"), mine)).likedPlaylist())
        assertEquals(2, state(emptyList()).copy(collectedPlaylists = listOf(liked.copy(author = "other"), saved))
            .playlistsIn(LibraryCollection.Collected).size)
    }

    @Test fun dashboardUsesFirstCreatedAndRecentCollectionsWithoutChangingLowerGridOrder() {
        val second = playlist("second", "owner", "第二张")
        val olderPopular = playlist("older", "other", "常听旧收藏")
        val recent = playlist("recent", "other", "最新收藏")
        val data = state(listOf(mine, second, liked, mine)).copy(
            collectedPlaylists = listOf(olderPopular, recent, saved),
            recentCollectedPlaylists = listOf(recent, saved),
        )
        val dashboard = data.dashboardSelection()
        assertEquals("liked", dashboard.liked?.id)
        assertEquals("mine", dashboard.topCreated?.id)
        assertEquals(listOf("recent", "saved"), dashboard.recentCollected.map { it.id })
        assertEquals(listOf("second"), dashboard.remainingCreated.map { it.id })
        assertEquals(listOf("older", "recent", "saved"), data.playlistsIn(LibraryCollection.Collected).map { it.id })
    }

    @Test fun sparseLibraryDoesNotInventPlaylistsOrUseListeningRankAsCollectionRecency() {
        val sparse = state(listOf(liked)).dashboardSelection()
        assertNotNull(sparse.liked)
        assertNull(sparse.topCreated)
        assertTrue(sparse.recentCollected.isEmpty())
        assertTrue(sparse.remainingCreated.isEmpty())
        val withoutLiked = state(listOf(mine)).dashboardSelection()
        assertNull(withoutLiked.liked)
        assertEquals("mine", withoutLiked.topCreated?.id)
    }

    @Test fun recentCollectionIdsPreserveRemoteOrderAndExcludeOwnedPlaylists() {
        fun remote(id: Long, owner: Long) = com.ljyh.mei.data.model.domain.UserPlaylistItem(
            id, "歌单", "", com.ljyh.mei.data.model.domain.PlaylistCreator(owner, "用户", ""), 0, 1,
        )
        assertEquals(listOf("50", "10"), recentCollectedIds(listOf(
            remote(90, 9), remote(50, 2), remote(50, 2), remote(10, 3), remote(99, 4),
        ), "9"))
    }

    @Test fun searchIgnoresCaseWhitespaceAndMatchesArtistsAndCreators() {
        assertEquals(listOf("mine"), searchLibrary(state(), "  NIGHT drive  ").playlists.map { it.id })
        assertEquals(listOf(7L), searchLibrary(state(), "aurora NIGHT").albums.map { it.id })
        assertEquals(listOf("saved"), searchLibrary(state(), "other").playlists.map { it.id })
    }

    @Test fun searchRequiresEveryTermAndDoesNotInventTracks() {
        assertTrue(searchLibrary(state(), "Night Jazz").playlists.isEmpty())
        assertTrue(searchLibrary(state(), "missing song").albums.isEmpty())
        assertTrue(searchLibrary(state(), "  ").playlists.isEmpty())
        assertTrue(searchLibrary(null, "Night").albums.isEmpty())
    }

    @Test fun repeatedCachedPlaylistHasOnlyOneSearchResult() {
        assertEquals(listOf("mine"), searchLibrary(state(listOf(mine, mine)), "drive").playlists.map { it.id })
    }

    private fun playlist(id: String, owner: String, title: String) = Playlist(
        id = id, title = title, cover = "", author = owner, authorName = owner, authorAvatar = "", count = 1,
    )
}
