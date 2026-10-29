package com.ljyh.mei.ui.screen.search

import com.ljyh.mei.data.model.domain.SearchArtist
import com.ljyh.mei.data.model.domain.SearchResults
import com.ljyh.mei.data.network.Resource
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SearchResultsControllerTest {
    private fun page(vararg ids: Long, offset: Int = ids.size, more: Boolean = false) = SearchResults(
        artists = ids.map { SearchArtist(it, "Artist $it", null, emptyList()) },
        nextOffset = offset, hasMore = more,
    )

    @Test
    fun aLateTabResponseIsCachedWithoutReplacingTheVisibleTab() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val pending = CompletableDeferred<Resource<SearchResults>>()
        var artistCalls = 0
        try {
            val controller = SearchResultsController(scope) { _, type, _ ->
                if (type == SearchType.Song) pending.await()
                else { artistCalls++; Resource.Success(page(2)) }
            }
            controller.initialize("query", SearchType.Song.type)
            controller.select(SearchType.Artist)
            pending.complete(Resource.Success(page(1)))
            assertEquals(2L, controller.page.value.data!!.artists.single().id)
            controller.select(SearchType.Song)
            assertEquals(1L, controller.page.value.data!!.artists.single().id)
            controller.select(SearchType.Artist)
            assertEquals(1, artistCalls)
        } finally { scope.cancel() }
    }

    @Test
    fun changingQueryRejectsLateResponsesEvenWhenTheTransportDoesNotCooperateWithCancellation() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val requests = mutableMapOf<String, Continuation<Resource<SearchResults>>>()
        try {
            val controller = SearchResultsController(scope) { query, type, _ ->
                if (type != SearchType.Artist) Resource.Success(page(3))
                else suspendCoroutine { requests[query] = it }
            }
            controller.initialize("old", 100)
            controller.initialize("new", 100)
            requests.getValue("new").resume(Resource.Success(page(2)))
            requests.getValue("old").resume(Resource.Success(page(1)))
            assertEquals("new", controller.query.value)
            assertEquals(2L, controller.page.value.data!!.artists.single().id)
            controller.select(SearchType.Album)
            controller.select(SearchType.Artist)
            assertEquals(2L, controller.page.value.data!!.artists.single().id)
        } finally { scope.cancel() }
    }

    @Test
    fun paginationFailureRetainsRowsAndRetryUsesTheSameOffsetWithoutDuplicates() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val offsets = mutableListOf<Int>()
        try {
            val controller = SearchResultsController(scope) { _, _, offset ->
                offsets += offset
                when (offsets.size) {
                    1 -> Resource.Success(page(1, 2, offset = 2, more = true))
                    2 -> Resource.Error("offline")
                    3 -> Resource.Success(page(2, 3, offset = 4, more = true))
                    else -> Resource.Success(page(2, 3, offset = 6, more = true))
                }
            }
            controller.initialize("query", 100)
            controller.loadMore()
            assertTrue(controller.page.value.appendError)
            assertEquals(listOf(1L, 2L), controller.page.value.data!!.artists.map { it.id })
            controller.loadMore()
            assertEquals(listOf(0, 2, 2), offsets)
            assertFalse(controller.page.value.appendError)
            assertEquals(listOf(1L, 2L, 3L), controller.page.value.data!!.artists.map { it.id })
            controller.loadMore()
            assertFalse(controller.page.value.data!!.hasMore)
            controller.loadMore()
            assertEquals(4, offsets.size)
        } finally { scope.cancel() }
    }

    @Test
    fun firstPageErrorsAreRetryableAndReturningToTheSameQueryPreservesTheSelectedTab() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var calls = 0
        try {
            val controller = SearchResultsController(scope) { _, _, _ ->
                if (++calls == 1) Resource.Error("offline") else Resource.Success(page(1))
            }
            controller.initialize(" query ", -1)
            assertEquals(SearchType.All, controller.tab.value)
            assertTrue(controller.page.value.error)
            controller.retry()
            assertFalse(controller.page.value.error)
            controller.select(SearchType.Artist)
            controller.initialize("query", SearchType.All.type)
            assertEquals(SearchType.Artist, controller.tab.value)
            assertEquals(3, calls)
        } finally { scope.cancel() }
    }
}
