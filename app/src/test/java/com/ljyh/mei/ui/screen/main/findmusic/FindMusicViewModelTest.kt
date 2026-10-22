package com.ljyh.mei.ui.screen.main.findmusic

import com.ljyh.mei.data.model.domain.*
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.DiscoverDataSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class FindMusicViewModelTest {
    @Test
    fun aFailedSectionDoesNotDiscardOtherSections() = runBlocking {
        val source = FakeSource { _, _ -> page(listOf(1)) }
        val vm = FindMusicViewModel(source, this)
        yield()
        assertTrue(vm.albums.value is Resource.Error)
        assertEquals(0, source.chartRequests)
        assertTrue(vm.featured.value is Resource.Success)
        assertTrue(vm.categories.value is Resource.Success)
        assertTrue(vm.hotCategories.value is Resource.Success)
    }

    private class FakeSource(
        val fetch: suspend (DiscoverPlaylistQuery, Int) -> Resource<DiscoverPlaylistPage>,
    ) : DiscoverDataSource {
        override suspend fun categories() = Resource.Success(listOf(DiscoverCategory("电子", "风格")))
        override suspend fun hotCategories() = Resource.Success(listOf("电子"))
        override suspend fun playlists(query: DiscoverPlaylistQuery, offset: Int) = fetch(query, offset)
        override suspend fun albums(): Resource<List<DiscoverAlbum>> = Resource.Error("offline")
        var chartRequests = 0
        override suspend fun charts(): Resource<List<DiscoverChart>> {
            chartRequests++
            return Resource.Success(emptyList())
        }
    }

    companion object {
        private fun page(ids: List<Int>, next: Int = ids.size, more: Boolean = false) =
            Resource.Success(DiscoverPlaylistPage(ids.map { DiscoverPlaylist(it.toLong(), "歌单", "", 0) }, next, more))
    }
}
