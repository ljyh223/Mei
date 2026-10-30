package com.ljyh.mei.ui.screen.main.library

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.ljyh.mei.data.model.domain.*
import com.ljyh.mei.data.model.response.*
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.di.dao.PlaylistDao
import com.ljyh.mei.di.repository.LocalPlaylistRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Count actual calls at the VM's network boundary, including completed and failed requests. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class LibraryLoadingTest {
    @Test fun returningToLibraryAndItsCollectionsDoesNotRequestAgain() = fixture { vm, source ->
        vm.onCookieAvailable("session")
        vm.account.first { it is Resource.Success }
        vm.loadLibrary(profile)?.join()
        vm.loadLikedArtwork("42")?.join()
        val firstCalls = source.calls.toMap()
        assertTrue(firstCalls.isNotEmpty())
        repeat(4) {
            vm.onCookieAvailable("session")
            vm.loadLibrary(profile.copy(nickname = "updated nickname"))?.join()
            vm.loadLikedArtwork("42")?.join()
            // A transient missing playlist while subscribers reconnect must not clear its covers.
            vm.loadLikedArtwork(null)?.join()
        }
        assertEquals(firstCalls, source.calls)
        assertEquals(listOf("cover-1", "cover-2", "cover-3"), vm.likedArtworks.value)
        assertFalse(vm.likedArtworkLoading.value)
    }

    @Test fun failedInitialRequestsWaitForExplicitRefreshRatherThanTabSwitches() = fixture { vm, source ->
        source.fail = true
        vm.loadLibrary(profile)?.join()
        vm.loadLikedArtwork("42")?.join()
        val failedCalls = source.calls.toMap()
        vm.loadLibrary(profile)?.join()
        vm.loadLikedArtwork("42")?.join()
        assertEquals(failedCalls, source.calls)
        source.fail = false
        vm.refresh()?.join()
        assertEquals(2, source.calls["playlists"])
        assertEquals(2, source.calls["artwork"])
        assertEquals("cover-1", vm.likedArtwork.value)
    }

    @Test fun requestsStillInFlightAreSharedByRepeatedEntries() = fixture { vm, source ->
        val barrier = CompletableDeferred<Unit>()
        source.playlistsBarrier = barrier
        source.artworkBarrier = barrier
        val library = vm.loadLibrary(profile)
        val artwork = vm.loadLikedArtwork("42")
        // The repository may suspend for network or disk; navigation must not restart it.
        yield()
        assertSame(library, vm.loadLibrary(profile))
        assertSame(artwork, vm.loadLikedArtwork("42"))
        barrier.complete(Unit)
        library?.join()
        artwork?.join()
        assertEquals(1, source.calls["playlists"])
        assertEquals(1, source.calls["artwork"])
    }

    @Test fun refreshRunsOnceAndKeepsExistingCoversVisibleUntilNewDataArrives() = fixture { vm, source ->
        vm.loadLibrary(profile)?.join()
        vm.loadLikedArtwork("42")?.join()
        val barrier = CompletableDeferred<Unit>()
        source.accountBarrier = barrier
        val refresh = vm.refresh()
        yield()
        assertTrue(vm.isRefreshing.value)
        assertEquals(listOf("cover-1", "cover-2", "cover-3"), vm.likedArtworks.value)
        assertFalse(vm.likedArtworkLoading.value)
        assertSame(refresh, vm.refresh())
        vm.loadLibrary(profile)?.join()
        vm.loadLikedArtwork("42")?.join()
        assertEquals(1, source.calls["playlists"])
        barrier.complete(Unit)
        refresh?.join()
        assertFalse(vm.isRefreshing.value)
        assertEquals(2, source.calls["playlists"])
        assertEquals(2, source.calls["artwork"])
        val callsAfterRefresh = source.calls.toMap()
        vm.loadLibrary(profile)?.join()
        vm.loadLikedArtwork("42")?.join()
        assertEquals(callsAfterRefresh, source.calls)
    }

    @Test fun refreshFailureRetainsCoversAndCollectedAlbumsAndCanBeRetried() = fixture { vm, source ->
        vm.loadLibrary(profile)?.join()
        vm.loadLikedArtwork("42")?.join()
        val content = vm.libraryUiState.filterIsInstance<LibraryUiState.Content>().first { !it.data.albumsLoading }
        assertEquals(1, content.data.albums.size)
        source.fail = true
        vm.refresh()?.join()
        assertFalse(vm.isRefreshing.value)
        assertEquals(listOf("cover-1", "cover-2", "cover-3"), vm.likedArtworks.value)
        val retained = vm.libraryUiState.filterIsInstance<LibraryUiState.Content>().first { it.data.playlistsError != null }
        assertEquals(content.data.albums, retained.data.albums)
        source.fail = false
        vm.refresh()?.join()
        assertEquals(3, source.calls["playlists"])
        assertEquals(3, source.calls["artwork"])
    }

    @Test fun accountChangeDropsOldArtworkAndLogoutDoesNotReuseIt() = fixture { vm, source ->
        vm.onCookieAvailable("first-session")
        assertTrue(vm.signedIn.value)
        vm.account.first { it is Resource.Success }
        vm.loadLibrary(profile)?.join()
        vm.loadLikedArtwork("42")?.join()
        val barrier = CompletableDeferred<Unit>()
        source.accountBarrier = barrier
        source.owner = "10"
        vm.onCookieAvailable("second-session")
        assertTrue(vm.likedArtworks.value.isEmpty())
        // Old preferences can briefly be emitted while the new account request is in flight.
        vm.loadLibrary(profile)?.join()
        assertEquals(1, source.calls["playlists"])
        barrier.complete(Unit)
        vm.account.first { (it as? Resource.Success)?.data?.profile?.userId == 10L }
        vm.loadLibrary(profile.copy(userId = "10"))?.join()
        assertEquals(2, source.calls["playlists"])
        vm.onCookieAvailable("")
        assertFalse(vm.signedIn.value)
        assertTrue(vm.likedArtworks.value.isEmpty())
        assertTrue(vm.account.value is Resource.Loading)
        assertEquals(2, source.calls["account"])
    }

    private fun fixture(block: suspend CoroutineScope.(LibraryViewModel, Source) -> Unit) = runBlocking {
        val scope = CoroutineScope(coroutineContext + Job())
        val source = Source()
        val vm = LibraryViewModel(
            ApplicationProvider.getApplicationContext<Application>(), source,
            LocalPlaylistRepository(Playlists()), scope,
        )
        try { withTimeout(15_000) { block(vm, source) } } finally { scope.cancel() }
    }

    private class Source : LibraryDataSource {
        val calls = mutableMapOf<String, Int>()
        var fail = false
        var owner = "9"
        var accountBarrier: CompletableDeferred<Unit>? = null
        var playlistsBarrier: CompletableDeferred<Unit>? = null
        var artworkBarrier: CompletableDeferred<Unit>? = null
        private fun <T> response(key: String, value: T): Resource<T> {
            calls[key] = (calls[key] ?: 0) + 1
            return if (fail) Resource.Error("offline") else Resource.Success(value)
        }
        override suspend fun getUserAccount(): Resource<UserAccountSummary> {
            val result = response("account", UserAccountSummary(200, AccountProfile(owner.toLong(), "name", "", "")))
            accountBarrier?.await()
            return result
        }
        override suspend fun getUserDetail(uid: String) = response("detail", UserDetail(200, 1, 1, UserDetail.Profile(1, 1)))
        override suspend fun getUserVipInfo(uid: String) = response("vip", UserVipInfo("", 200, null))
        override suspend fun getPhotoAlbum(uid: String) = response("photo", AlbumPhoto())
        override suspend fun getAlbumList() = response("albums", UserAlbumList(200, listOf(UserAlbumList.Data(7, "album"))))
        override suspend fun getListenDataRealtimeReport(type: String) = response("realtime-$type", ListenDataRealtimeResponse(200))
        override suspend fun getListenDataReport(type: String) = response("report-$type", ListenDataReportResponse(200))
        override suspend fun getUserPlaylist(uid: String, limit: Int): Resource<UserPlaylistPage> {
            val result = response("playlists", UserPlaylistPage(listOf(
                UserPlaylistItem(42, "我喜欢的音乐", "", PlaylistCreator(uid.toLong(), "name", ""), 0, 3),
            ), false))
            playlistsBarrier?.await()
            return result
        }
        override suspend fun getPlaylistDetail(id: String): Resource<MiniPlaylistDetail> {
            val result = response("artwork", MiniPlaylistDetail(
            emptyList(), "我喜欢的音乐", null, id.toLong(),
            (1L..3L).map { MediaMetadata(it, "Song $it", "cover-$it", emptyList(), 0, MediaMetadata.Album(1, "")) },
                listOf(1, 2, 3), 3, owner.toLong(), "name", 0, 0, false,
            ))
            artworkBarrier?.await()
            return result
        }
        override suspend fun getSongDetails(ids: List<String>): List<MediaMetadata> {
            calls["songs"] = (calls["songs"] ?: 0) + 1
            return emptyList()
        }
    }

    private class Playlists : PlaylistDao {
        private val rows = MutableStateFlow<List<Playlist>>(emptyList())
        override suspend fun getPlaylist(id: String) = rows.value.firstOrNull { it.id == id }
        override suspend fun getPlaylistByAuthor(author: String) = rows.value.filter { it.author == author }
        override fun getAllPlaylist(): Flow<List<Playlist>> = rows
        override suspend fun insertPlaylist(playlist: Playlist) = insertPlaylists(listOf(playlist))
        override suspend fun insertPlaylists(playlists: List<Playlist>) {
            rows.value = (rows.value + playlists).associateBy { it.id }.values.toList()
        }
        override suspend fun deletePlaylistById(id: String) { rows.value = rows.value.filterNot { it.id == id } }
        override suspend fun touchPlaylist(id: String, timestamp: Long) = Unit
    }

    companion object { private val profile = LibraryProfileUi("9", "name", "", "") }
}
