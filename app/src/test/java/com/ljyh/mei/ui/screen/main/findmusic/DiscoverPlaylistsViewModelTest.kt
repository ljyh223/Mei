package com.ljyh.mei.ui.screen.main.findmusic

import com.ljyh.mei.data.model.domain.*
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.DiscoverDataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class DiscoverPlaylistsViewModelTest {
    @Test
    fun lateCategoryResponseCannotReplaceANewerSelectionOrItsCache() = runBlocking {
        val old = CompletableDeferred<Resource<DiscoverPlaylistPage>>()
        val source = FakeSource { query, _ ->
            if (query.category == "华语") withContext(NonCancellable) { old.await() }
            else page(if (query.category == "电子") listOf(8) else listOf(1))
        }
        val vm = DiscoverPlaylistsViewModel(source, androidx.lifecycle.SavedStateHandle(), this)
        yield()
        vm.selectCategory("华语")
        yield()
        vm.selectCategory("电子")
        yield()
        old.complete(page(listOf(9)))
        yield()
        assertEquals("电子", vm.playlists.value.query.category)
        assertEquals(listOf(8L), vm.playlists.value.items.map { it.id })
        vm.selectCategory("电子")
        assertFalse(vm.playlists.value.loading)
        assertEquals(listOf(8L), vm.playlists.value.items.map { it.id })
    }

    @Test
    fun sortingUsesADifferentCacheAndReturningRestoresTheOriginalPage() = runBlocking {
        val source = FakeSource { query, _ -> page(if (query.order == DiscoverOrder.New) listOf(2) else listOf(1)) }
        val vm = DiscoverPlaylistsViewModel(source, androidx.lifecycle.SavedStateHandle(), this)
        yield()
        vm.selectOrder(DiscoverOrder.New)
        yield()
        assertEquals(listOf(2L), vm.playlists.value.items.map { it.id })
        vm.selectOrder(DiscoverOrder.Hot)
        assertEquals(listOf(1L), vm.playlists.value.items.map { it.id })
        assertFalse(vm.playlists.value.loading)
    }

    @Test
    fun paginationDeduplicatesAndStopsWhenTheServerRepeatsTheSamePage() = runBlocking {
        var appendCount = 0
        val source = FakeSource { _, offset ->
            if (offset == 0) page(listOf(1, 2), 24, true)
            else if (++appendCount == 1) page(listOf(2, 3), 48, true)
            else page(listOf(2, 3), 72, true)
        }
        val vm = DiscoverPlaylistsViewModel(source, androidx.lifecycle.SavedStateHandle(), this)
        yield()
        vm.loadMore()
        yield()
        assertEquals(listOf(1L, 2L, 3L), vm.playlists.value.items.map { it.id })
        vm.loadMore()
        yield()
        assertFalse(vm.playlists.value.hasMore)
        vm.loadMore()
        yield()
        assertEquals(2, appendCount)
    }

    @Test
    fun appendFailurePreservesContentAndRetriesTheSameOffset() = runBlocking {
        val offsets = mutableListOf<Int>()
        var attempts = 0
        val source = FakeSource { _, offset ->
            offsets += offset
            if (offset == 0) page(listOf(1), 24, true)
            else if (++attempts == 1) Resource.Error("offline")
            else page(listOf(2), 48, false)
        }
        val vm = DiscoverPlaylistsViewModel(source, androidx.lifecycle.SavedStateHandle(), this)
        yield()
        vm.loadMore()
        yield()
        assertEquals(listOf(1L), vm.playlists.value.items.map { it.id })
        assertTrue(vm.playlists.value.failed)
        vm.retryPlaylists()
        yield()
        assertEquals(listOf(1L, 2L), vm.playlists.value.items.map { it.id })
        assertEquals(listOf(0, 24, 24), offsets)
        assertFalse(vm.playlists.value.failed)
    }

    @Test
    fun navigationArgumentsAndRestoredSortAreUsedOnFirstLoad() = runBlocking {
        val calls = mutableListOf<DiscoverPlaylistQuery>()
        val handle = androidx.lifecycle.SavedStateHandle(mapOf("category" to "R&B/Soul", "order" to "new"))
        val source = FakeSource { query, _ -> calls += query; page(listOf(1)) }
        val vm = DiscoverPlaylistsViewModel(source, handle, this)
        yield()
        assertEquals(DiscoverPlaylistQuery("R&B/Soul", DiscoverOrder.New), calls.single())
        vm.selectCategory("爵士")
        yield()
        assertEquals("爵士", handle.get<String>("category"))
        assertEquals("new", handle.get<String>("order"))
        val restored = DiscoverPlaylistsViewModel(source, handle, this)
        yield()
        assertEquals(vm.playlists.value.query, restored.playlists.value.query)
    }

    @Test
    fun separateNavigationEntriesDoNotShareFiltersOrPages() = runBlocking {
        val source = FakeSource { query, _ -> page(if (query.category == "华语") listOf(1) else listOf(2)) }
        val first = DiscoverPlaylistsViewModel(source, androidx.lifecycle.SavedStateHandle(mapOf("category" to "华语")), this)
        val second = DiscoverPlaylistsViewModel(source, androidx.lifecycle.SavedStateHandle(mapOf("category" to "电子")), this)
        yield()
        first.selectOrder(DiscoverOrder.New)
        yield()
        assertEquals(DiscoverOrder.Hot, second.playlists.value.query.order)
        assertEquals(listOf(2L), second.playlists.value.items.map { it.id })
        assertEquals(listOf(1L), first.playlists.value.items.map { it.id })
    }

    private class FakeSource(
        val fetch: suspend (DiscoverPlaylistQuery, Int) -> Resource<DiscoverPlaylistPage>,
    ) : DiscoverDataSource {
        override suspend fun categories() = Resource.Success(listOf(DiscoverCategory("电子", "风格")))
        override suspend fun hotCategories() = Resource.Success(listOf("电子"))
        override suspend fun playlists(query: DiscoverPlaylistQuery, offset: Int) = fetch(query, offset)
        override suspend fun albums(): Resource<List<DiscoverAlbum>> = Resource.Error("offline")
        override suspend fun charts() = Resource.Success(listOf(DiscoverChart(3, "榜单", "", "每天更新", emptyList())))
    }

    companion object {
        private fun page(ids: List<Int>, next: Int = ids.size, more: Boolean = false) =
            Resource.Success(DiscoverPlaylistPage(ids.map { DiscoverPlaylist(it.toLong(), "歌单", "", 0) }, next, more))
    }
}
