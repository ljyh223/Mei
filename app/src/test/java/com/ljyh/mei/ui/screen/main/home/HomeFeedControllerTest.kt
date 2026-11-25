package com.ljyh.mei.ui.screen.main.home

import com.ljyh.mei.data.model.eapi.HomePageResourceShow.Data
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.HomeFeedSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class HomeFeedControllerTest {
    @Test fun cachedCardsArePublishedBeforeTheNetworkFinishesAndPagingWaits() = fixture { state, source ->
        source.cached["a"] = page("cached")
        state.load(userId = "a")
        state.content.first { it.names() == listOf("cached") }
        assertTrue(state.refreshing.value)
        state.loadMore()
        assertEquals(0, source.pageCalls)
        source.requests[0].complete(Resource.Success(page("fresh")))
        state.content.first { it.names() == listOf("fresh") }
        await { !state.refreshing.value }
    }

    @Test fun returningToThePageSharesAnActiveLoadAndReusesTheCompletedFeed() = fixture { state, source ->
        state.load(userId = "a")
        await { source.calls == 1 }
        repeat(4) { state.load(userId = "a") }
        assertEquals(1, source.calls)
        source.requests[0].complete(Resource.Success(page("fresh")))
        state.content.first { it.names() == listOf("fresh") }
        repeat(4) { state.load(userId = "a") }
        assertEquals(1, source.calls)
        assertEquals(1, source.cacheCalls)
    }

    @Test fun returningAfterTheRecommendationDayChangesRefreshesOnceAndKeepsTheOldCardsVisible() = fixture { state, source ->
        state.load(userId = "a")
        await { source.calls == 1 }
        source.requests[0].complete(Resource.Success(page("yesterday")))
        await { !state.refreshing.value }
        state.load(userId = "a")
        assertEquals(1, source.calls)
        source.cacheFresh = false
        state.load(userId = "a")
        await { source.calls == 2 }
        assertEquals(listOf("yesterday"), state.content.value.names())
        repeat(3) { state.load(userId = "a") }
        assertEquals(2, source.calls)
        source.cacheFresh = true
        source.requests[1].complete(Resource.Success(page("today")))
        state.content.first { it.names() == listOf("today") }
        state.load(userId = "a")
        assertEquals(2, source.calls)
    }

    @Test fun pagingAfterTheDailyCutoffLoadsANewFirstPageInsteadOfContinuingYesterdaysCursor() = fixture { state, source ->
        state.load(userId = "a")
        await { source.calls == 1 }
        source.requests[0].complete(Resource.Success(page("yesterday")))
        await { !state.refreshing.value }
        source.cacheFresh = false
        state.loadMore()
        await { source.calls == 2 }
        assertEquals(0, source.pageCalls)
        source.requests[1].complete(Resource.Success(page("today")))
        state.content.first { it.names() == listOf("today") }
    }

    @Test fun anExpiredCacheRemainsUsableWhenRefreshFailsAndExplicitRetryStillWorks() = fixture { state, source ->
        source.cached["a"] = page("cached")
        state.load(userId = "a")
        state.content.first { it.names() == listOf("cached") }
        source.requests[0].complete(Resource.Error("offline"))
        await { !state.refreshing.value }
        assertEquals(listOf("cached"), state.content.value.names())
        assertTrue(state.refreshError.value)
        state.load(refresh = true, userId = "a")
        await { source.calls == 2 }
        assertEquals(listOf("cached"), state.content.value.names())
        assertEquals(1, source.cacheCalls)
        assertEquals(listOf(false, true), source.refreshes)
        source.requests[1].complete(Resource.Success(page("updated")))
        state.content.first { it.names() == listOf("updated") }
        await { !state.refreshing.value }
        assertFalse(state.refreshError.value)
    }

    @Test fun cacheReadFailureDoesNotPreventTheNetworkRequest() = fixture { state, source ->
        source.cacheFailure = true
        state.load(userId = "a")
        await { source.calls == 1 }
        source.requests[0].complete(Resource.Success(page("fresh")))
        state.content.first { it.names() == listOf("fresh") }
    }

    @Test fun aLateAccountResponseCannotReplaceTheNewAccountOrClearItsLoadingFlag() = fixture { state, source ->
        source.ignoreCancellation = true
        source.cached["a"] = page("account-a")
        source.cached["b"] = page("account-b")
        state.load(userId = "a")
        state.content.first { it.names() == listOf("account-a") }
        state.load(userId = "b")
        state.content.first { it.names() == listOf("account-b") }
        await { source.calls == 2 }
        assertEquals(listOf(false, false), source.refreshes)
        source.requests[0].complete(Resource.Success(page("late-a")))
        await { source.completed == 1 }
        assertEquals(listOf("account-b"), state.content.value.names())
        assertTrue(state.refreshing.value)
        source.requests[1].complete(Resource.Success(page("fresh-b")))
        state.content.first { it.names() == listOf("fresh-b") }
    }

    @Test fun refreshCannotBeOverwrittenByAnOlderPageRequest() = fixture { state, source ->
        source.ignoreCancellation = true
        state.load(userId = "a")
        await { source.calls == 1 }
        source.requests[0].complete(Resource.Success(page("first")))
        await { !state.refreshing.value }
        state.loadMore()
        await { source.pageCalls == 1 }
        state.load(refresh = true, userId = "a")
        await { source.calls == 2 }
        source.pageRequest.complete(Resource.Success(page("late-page")))
        await { source.completed == 2 }
        assertEquals(listOf("first"), state.content.value.names())
        assertTrue(state.refreshing.value)
        source.requests[1].complete(Resource.Success(page("refreshed")))
        state.content.first { it.names() == listOf("refreshed") }
    }

    private fun fixture(block: suspend CoroutineScope.(HomeFeedController, Source) -> Unit) = runBlocking {
        val scope = CoroutineScope(coroutineContext + Job())
        val source = Source()
        try { withTimeout(5_000) { block(HomeFeedController(source, scope), source) } }
        finally {
            source.requests.forEach { it.complete(Resource.Error("closed")) }
            source.pageRequest.complete(Resource.Error("closed"))
            scope.cancel()
        }
    }

    private class Source : HomeFeedSource {
        val cached = mutableMapOf<String, Data>()
        val requests = mutableListOf<CompletableDeferred<Resource<Data>>>()
        val pageRequest = CompletableDeferred<Resource<Data>>()
        var cacheFresh = true
        override fun isHomePageCacheFresh(userId: String) = cacheFresh
        val refreshes = mutableListOf<Boolean>()
        var calls = 0
        var cacheCalls = 0
        var pageCalls = 0
        var completed = 0
        var ignoreCancellation = false
        var cacheFailure = false
        override suspend fun getCachedHomePageResourceShow(userId: String): Data? {
            cacheCalls++
            if (cacheFailure) throw IOException("unreadable cache")
            return cached[userId]
        }
        override suspend fun getHomePageResourceShow(refresh: Boolean): Resource<Data> {
            calls++
            refreshes += refresh
            val response = CompletableDeferred<Resource<Data>>().also(requests::add)
            return read(response).also { completed++ }
        }
        override suspend fun loadMore(previous: Data): Resource<Data> {
            pageCalls++
            return read(pageRequest).also { completed++ }
        }
        private suspend fun read(value: CompletableDeferred<Resource<Data>>) =
            if (ignoreCancellation) withContext(NonCancellable) { value.await() } else value.await()
    }

    private suspend fun await(condition: () -> Boolean) = withTimeout(5_000) { while (!condition()) yield() }
    private fun page(code: String) = Data(listOf(Data.Block(positionCode = code)), cursor = 10, hasMore = true)
    private fun Resource<List<Data.Block>>.names() = (this as? Resource.Success)?.data?.map { it.positionCode }
}
