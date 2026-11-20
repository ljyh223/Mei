package com.ljyh.mei.ui.screen.artist

import com.ljyh.mei.data.network.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class ArtistSubscriptionControllerTest {
    @Test fun guestDoesNotQueryFavoritesAndGetsLoginFeedback() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = ArtistSubscriptionController(scope, { error("guest read") }, { _, _ -> error("guest write") })
            controller.bind("7", "")
            controller.toggle()
            assertFalse(controller.state.value.authenticated)
            assertEquals(false, controller.state.value.subscribed)
            assertEquals("请先登录后收藏歌手", withTimeout(1000) { controller.events.first() })
        } finally { scope.cancel() }
    }

    @Test fun loadedFavoriteCanBeRemovedAndDuplicateTapsDoNotSendAnotherWrite() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val result = CompletableDeferred<Resource<Unit>>()
        var writes = 0
        try {
            val controller = ArtistSubscriptionController(scope, { Resource.Success(true) }, { id, subscribed ->
                assertEquals("7", id); assertFalse(subscribed); writes++; result.await()
            })
            controller.bind("7", "123")
            assertEquals(true, controller.state.value.subscribed)
            controller.toggle(); controller.toggle()
            assertEquals(1, writes)
            assertTrue(controller.state.value.pending)
            assertEquals(false, controller.state.value.subscribed)
            result.complete(Resource.Success(Unit))
            assertFalse(controller.state.value.pending)
            assertEquals(false, controller.state.value.subscribed)
            assertEquals("已取消收藏", withTimeout(1000) { controller.events.first() })
        } finally { scope.cancel() }
    }

    @Test fun failedWriteRestoresThePreviousFavorite() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val result = CompletableDeferred<Resource<Unit>>()
        try {
            val controller = ArtistSubscriptionController(scope, { Resource.Success(false) }, { _, _ -> result.await() })
            controller.bind("7", "123"); controller.toggle()
            assertEquals(true, controller.state.value.subscribed)
            result.complete(Resource.Error("offline"))
            assertEquals(false, controller.state.value.subscribed)
            assertFalse(controller.state.value.pending)
            assertEquals("操作失败，请稍后重试", withTimeout(1000) { controller.events.first() })
        } finally { scope.cancel() }
    }

    @Test fun unknownReadRetriesInsteadOfGuessingAndWriting() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var reads = 0
        try {
            val controller = ArtistSubscriptionController(scope, {
                reads++; if (reads == 1) Resource.Error("offline") else Resource.Success(true)
            }, { _, _ -> error("must first resolve actual state") })
            controller.bind("7", "123")
            assertNull(controller.state.value.subscribed)
            assertTrue(controller.state.value.error)
            controller.toggle()
            assertEquals(2, reads)
            assertEquals(true, controller.state.value.subscribed)
            assertFalse(controller.state.value.error)
            controller.bind("7", "123")
            assertEquals(2, reads)
        } finally { scope.cancel() }
    }

    @Test fun oldAccountReadCannotOverwriteNewAccountState() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val oldRead = CompletableDeferred<Resource<Boolean>>()
        var reads = 0
        try {
            val controller = ArtistSubscriptionController(scope, {
                reads++
                if (reads == 1) withContext(NonCancellable) { oldRead.await() } else Resource.Success(false)
            }, { _, _ -> error("no write") })
            controller.bind("7", "123")
            assertTrue(controller.state.value.loading)
            controller.toggle()
            assertEquals(1, reads)
            controller.bind("7", "456")
            oldRead.complete(Resource.Success(true))
            assertEquals(false, controller.state.value.subscribed)
            assertFalse(controller.state.value.loading)
        } finally { scope.cancel() }
    }

    @Test fun oldWriteCannotChangeLoggedOutStateOrSendSuccessFeedback() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val oldWrite = CompletableDeferred<Resource<Unit>>()
        try {
            val controller = ArtistSubscriptionController(scope, { Resource.Success(false) }, { _, _ ->
                withContext(NonCancellable) { oldWrite.await() }
            })
            controller.bind("7", "123"); controller.toggle(); controller.bind("7", "")
            oldWrite.complete(Resource.Success(Unit))
            assertFalse(controller.state.value.authenticated)
            assertEquals(false, controller.state.value.subscribed)
            assertFalse(controller.state.value.pending)
            assertNull(withTimeoutOrNull(50) { controller.events.first() })
        } finally { scope.cancel() }
    }
}
