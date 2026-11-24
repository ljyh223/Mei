package com.ljyh.mei.ui.screen.profile

import com.ljyh.mei.data.model.response.ArchiveSongDto
import com.ljyh.mei.data.model.response.ListeningTrack
import com.ljyh.mei.data.network.Resource
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ListeningArchiveViewModelTest {
    @Test fun reentryAndPeriodSwitchesReusePerSessionCaches() = fixture { vm, source ->
        vm.bind("100", "test-session")
        repeat(3) { vm.bind("100", "test-session") }
        vm.selectPeriod(true)
        vm.selectPeriod(false)
        assertEquals(1, source.calls["rank-100-false"])
        assertEquals(1, source.calls["rank-100-true"])
        assertEquals(1, source.calls["recent"])
        assertEquals("100-false", (vm.state.value.rank as Resource.Success).data.single().song.title)
    }

    @Test fun cookieChangeAndLogoutClearAllOldAccountContent() = fixture { vm, source ->
        vm.bind("100", "test-session")
        vm.bind("100", "different-test-session")
        assertEquals(2, source.calls["rank-100-false"])
        vm.bind(null, "")
        assertSame(Resource.Loading, vm.state.value.rank)
        assertSame(Resource.Loading, vm.state.value.recent)
        vm.refresh()
        assertEquals(2, source.calls["recent"])
    }

    @Test fun failedInitialLoadsWaitForExplicitRetry() = fixture { vm, source ->
        source.fail = true
        vm.bind("100", "test-session")
        vm.bind("100", "test-session")
        assertEquals(1, source.calls["recent"])
        assertTrue(vm.state.value.rank is Resource.Error)
        source.fail = false
        vm.refresh()
        assertTrue(vm.state.value.rank is Resource.Success)
        assertEquals(2, source.calls["recent"])
    }

    @Test fun refreshKeepsExistingContentWhilePendingAndAfterFailure() = fixture { vm, source ->
        vm.bind("100", "test-session")
        val original = vm.state.value.rank
        source.barrier = CompletableDeferred()
        source.fail = true
        vm.refresh()
        assertTrue(vm.state.value.refreshing)
        assertSame(original, vm.state.value.rank)
        vm.refresh()
        assertEquals(2, source.calls["rank-100-false"])
        source.barrier!!.complete(Unit)
        yield()
        assertFalse(vm.state.value.refreshing)
        assertSame(original, vm.state.value.rank)
        assertNotNull(vm.state.value.refreshError)
    }

    @Test fun oldRequestsCannotWriteIntoAnotherAccountEvenIfTheyIgnoreCancellation() = fixture { vm, source ->
        val old = CompletableDeferred<Unit>()
        source.barrier = old
        vm.bind("100", "test-session")
        source.barrier = null
        vm.bind("200", "new-test-session")
        old.complete(Unit)
        yield()
        assertEquals("200-false", (vm.state.value.rank as Resource.Success).data.single().song.title)
        assertFalse(vm.state.value.refreshing)
    }

    @Test fun historyRefreshReusesRankAndUpdatesTheSharedRecentCache() = fixture { vm, source ->
        vm.bind("100", "test-session")
        val originalRank = vm.state.value.rank
        vm.refreshRecent()
        assertEquals(1, source.calls["rank-100-false"])
        assertEquals(2, source.calls["recent"])
        assertSame(originalRank, vm.state.value.rank)
        // Returning to the archive consumes the refreshed cache, without another request.
        vm.bind("100", "test-session")
        assertEquals(2, source.calls["recent"])
    }

    @Test fun historyRefreshKeepsRowsAfterFailureAndCannotFetchAfterLogout() = fixture { vm, source ->
        vm.bind("100", "test-session")
        val originalRecent = vm.state.value.recent
        source.barrier = CompletableDeferred()
        source.fail = true
        vm.refreshRecent()
        vm.refreshRecent()
        assertSame(originalRecent, vm.state.value.recent)
        assertTrue(vm.state.value.refreshing)
        assertEquals(2, source.calls["recent"])
        source.barrier!!.complete(Unit)
        yield()
        assertSame(originalRecent, vm.state.value.recent)
        assertEquals("最近收听刷新失败，已保留原内容", vm.state.value.refreshError)
        assertFalse(vm.state.value.refreshing)
        vm.bind(null, "")
        vm.refreshRecent()
        assertSame(Resource.Loading, vm.state.value.recent)
        assertEquals(2, source.calls["recent"])
    }

    private fun fixture(block: suspend CoroutineScope.(ListeningArchiveViewModel, FakeSource) -> Unit) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val source = FakeSource()
        try { block(ListeningArchiveViewModel(source, scope), source) } finally { scope.cancel() }
    }

    private class FakeSource : ListeningArchiveDataSource {
        val calls = mutableMapOf<String, Int>()
        var fail = false
        var barrier: CompletableDeferred<Unit>? = null
        private suspend fun result(key: String): Resource<List<ListeningTrack>> {
            calls[key] = calls.getOrDefault(key, 0) + 1
            barrier?.let { withContext(NonCancellable) { it.await() } }
            return if (fail) Resource.Error("offline") else Resource.Success(listOf(ListeningTrack(ArchiveSongDto(1, key).metadata()!!)))
        }
        override suspend fun rank(uid: String, allTime: Boolean): Resource<List<ListeningTrack>> {
            val result = result("rank-$uid-$allTime")
            return if (result is Resource.Success) Resource.Success(result.data.map { it.copy(song = it.song.copy(title = "$uid-$allTime")) }) else result
        }
        override suspend fun recent() = result("recent")
    }
}
