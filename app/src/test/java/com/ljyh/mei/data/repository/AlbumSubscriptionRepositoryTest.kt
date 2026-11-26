package com.ljyh.mei.data.repository

import android.app.Application
import androidx.datastore.preferences.core.preferencesOf
import androidx.room.Room
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.response.AlbumDynamic
import com.ljyh.mei.data.model.room.AlbumSubscription
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.di.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE, application = Application::class)
class AlbumSubscriptionRepositoryTest {
    @Test fun cachedStateAppearsBeforeTheNetworkAndIsRevalidated() = runBlocking {
        fixture { f ->
            f.cache(true)
            val release = CompletableDeferred<Unit>()
            f.load = { release.await(); Resource.Success(AlbumDynamic(200, false)) }
            f.observe()
            f.awaitState(Resource.Success(true))
            assertFalse(release.isCompleted)
            release.complete(Unit)
            f.awaitState(Resource.Success(false))
            assertEquals(false, f.dao.get("123", 7)?.subscribed)
        }
    }

    @Test fun offlineRevalidationKeepsBothPositiveAndNegativeCacheEntries() = runBlocking {
        for (subscribed in listOf(true, false)) fixture { f ->
            f.cache(subscribed)
            val finished = CompletableDeferred<Unit>()
            f.load = { finished.complete(Unit); Resource.Error("offline") }
            f.observe()
            f.awaitState(Resource.Success(subscribed))
            withTimeout(5000) { finished.await() }
            assertEquals(subscribed, f.dao.get("123", 7)?.subscribed)
            assertNull(withTimeoutOrNull(100) { f.events.receive() })
        }
    }

    @Test fun unknownOfflineStateDoesNotBecomeANegativeCacheEntry() = runBlocking {
        fixture { f ->
            f.load = { Resource.Error("offline") }
            f.observe()
            f.awaitState(Resource.Error("offline"))
            assertNull(f.dao.get("123", 7))
        }
    }

    @Test fun confirmedSubscribeAndUnsubscribeUpdateTheObservedDatabaseState() = runBlocking {
        fixture { f ->
            f.cache(false)
            f.observe()
            f.awaitState(Resource.Success(false))
            assertTrue(f.repository.setSubscribed(7, true) is Resource.Success)
            f.awaitState(Resource.Success(true))
            assertEquals(true, f.dao.get("123", 7)?.subscribed)
            assertTrue(f.repository.setSubscribed(7, false) is Resource.Success)
            f.awaitState(Resource.Success(false))
            assertEquals(false, f.dao.get("123", 7)?.subscribed)
            assertTrue(requireNotNull(f.dao.get("123", 7)).updatedAt > 0)
        }
    }

    @Test fun rejectedAndFailedMutationsNeverOverwriteTheConfirmedCache() = runBlocking {
        fixture { f ->
            f.cache(true)
            f.change = { Resource.Success(BaseResponse(502)) }
            assertTrue(f.repository.setSubscribed(7, false) is Resource.Error)
            assertEquals(true, f.dao.get("123", 7)?.subscribed)
            f.change = { Resource.Error("offline") }
            assertTrue(f.repository.setSubscribed(7, false) is Resource.Error)
            assertEquals(true, f.dao.get("123", 7)?.subscribed)
        }
    }

    @Test fun guestNeitherReadsTheNetworkNorSeesAnotherAccountsCache() = runBlocking {
        fixture { f ->
            f.cache(true)
            f.preferences.value = account("")
            f.observe()
            f.awaitState(Resource.Success(false))
            assertTrue(f.repository.setSubscribed(7, false) is Resource.Error)
            assertEquals(0, f.reads.get())
            assertEquals(0, f.writes.get())
            assertEquals(true, f.dao.get("123", 7)?.subscribed)
        }
    }

    @Test fun switchingAccountsIgnoresAnUncancellableOldRead() = runBlocking {
        fixture { f ->
            f.cache(true)
            f.cache(false, "456")
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val completed = CompletableDeferred<Unit>()
            f.load = {
                if (f.reads.get() == 1) {
                    started.complete(Unit)
                    withContext(NonCancellable) { release.await() }
                    completed.complete(Unit)
                    Resource.Success(AlbumDynamic(200, false))
                } else Resource.Success(AlbumDynamic(200, false))
            }
            f.observe()
            f.awaitState(Resource.Success(true))
            withTimeout(5000) { started.await() }
            f.preferences.value = account("456")
            release.complete(Unit)
            f.awaitState(Resource.Success(false))
            withTimeout(5000) { completed.await() }
            assertEquals(true, f.dao.get("123", 7)?.subscribed)
            assertEquals(false, f.dao.get("456", 7)?.subscribed)
        }
    }

    @Test fun oldSessionMutationCannotUpdateEitherAccountsRow() = runBlocking {
        fixture { f ->
            f.cache(true)
            f.cache(false, "456")
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            f.change = { started.complete(Unit); release.await(); Resource.Success(BaseResponse(200)) }
            val write = async(Dispatchers.Default) { f.repository.setSubscribed(7, false) }
            withTimeout(5000) { started.await() }
            f.preferences.value = account("456")
            release.complete(Unit)
            assertTrue(withTimeout(5000) { write.await() } is Resource.Error)
            assertEquals(true, f.dao.get("123", 7)?.subscribed)
            assertEquals(false, f.dao.get("456", 7)?.subscribed)
        }
    }

    @Test fun slowReadCannotOverwriteANewerConfirmedMutation() = runBlocking {
        fixture { f ->
            f.cache(false)
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            f.load = { started.complete(Unit); release.await(); Resource.Success(AlbumDynamic(200, false)) }
            f.observe()
            f.awaitState(Resource.Success(false))
            withTimeout(5000) { started.await() }
            val write = async(Dispatchers.Default) { f.repository.setSubscribed(7, true) }
            release.complete(Unit)
            assertTrue(withTimeout(5000) { write.await() } is Resource.Success)
            f.awaitState(Resource.Success(true))
            assertEquals(true, f.dao.get("123", 7)?.subscribed)
        }
    }

    @Test fun malformedRemoteStatusIsNotCachedAsNotSubscribed() = runBlocking {
        fixture { f ->
            f.load = { Resource.Success(AlbumDynamic(code = 200, isSubscribed = null)) }
            f.observe()
            withTimeout(5000) {
                while (f.events.receive() !is Resource.Error) Unit
            }
            assertNull(f.dao.get("123", 7))
        }
    }

    private suspend fun fixture(block: suspend (Fixture) -> Unit) {
        val f = Fixture()
        try { block(f) } finally { f.scope.cancel(); f.scope.coroutineContext[Job]?.join(); f.db.close() }
    }

    private class Fixture {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val dao = db.albumSubscriptionDao()
        val preferences = MutableStateFlow(account("123"))
        val reads = AtomicInteger()
        val writes = AtomicInteger()
        var load: suspend () -> Resource<AlbumDynamic> = { Resource.Success(AlbumDynamic(200, false)) }
        var change: suspend () -> Resource<BaseResponse> = { Resource.Success(BaseResponse(200)) }
        val repository = AlbumSubscriptionRepository(dao, preferences,
            { reads.incrementAndGet(); load() }, { _, _ -> writes.incrementAndGet(); change() })
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val events = Channel<Resource<Boolean>>(Channel.UNLIMITED)
        fun observe() { scope.launch { repository.observe(7).collect { events.send(it) } } }
        suspend fun cache(subscribed: Boolean, userId: String = "123") {
            dao.upsert(AlbumSubscription(userId, 7, subscribed, 1))
        }
        suspend fun awaitState(expected: Resource<Boolean>) = withTimeout(5000) {
            while (events.receive() != expected) Unit
        }
    }

    companion object {
        private fun account(userId: String) = preferencesOf(UserIdKey to userId,
            CookieKey to if (userId.isBlank()) "" else "test-session-$userId")
    }
}
