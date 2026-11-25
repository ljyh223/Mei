package com.ljyh.mei.data.repository

import android.content.Context
import com.ljyh.mei.data.network.NeteaseHttp
import com.ljyh.mei.data.network.api.EApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class AdDeviceIdProviderTest {
    @Test fun coldIdentityReturnsImmediatelyWhileOneBackgroundRequestIsPending() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences("home_ad_device_id", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val scope = CoroutineScope(coroutineContext + Job())
        val started = CompletableDeferred<Unit>()
        val barrier = CompletableDeferred<Unit>()
        val calls = AtomicInteger()
        val client = HttpClient(MockEngine {
            calls.incrementAndGet()
            started.complete(Unit)
            barrier.await()
            respond("""{"code":200,"data":{"adDeviceId":"test-id"}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val provider = AdDeviceIdProvider(context, EApiService(NeteaseHttp(client, Json { ignoreUnknownKeys = true })), scope)
            assertNull(provider.cachedAndRefresh())
            withTimeout(5_000) { started.await() }
            repeat(5) { assertNull(provider.cachedAndRefresh()) }
            assertEquals(1, calls.get())
            barrier.complete(Unit)
            withTimeout(5_000) { while (preferences.getString("id", null) != "test-id") yield() }
            assertEquals("test-id", provider.cachedAndRefresh())
            assertEquals(1, calls.get())
        } finally {
            barrier.complete(Unit)
            scope.cancel()
            client.close()
            preferences.edit().clear().commit()
        }
    }

    @Test fun expiredIdentityRemainsAvailableAndFailureDoesNotStartARequestOnEveryPage() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences("home_ad_device_id", Context.MODE_PRIVATE)
        preferences.edit().clear().putString("id", "old-id").putLong("time", 0).commit()
        val scope = CoroutineScope(coroutineContext + Job())
        val calls = AtomicInteger()
        val client = HttpClient(MockEngine { calls.incrementAndGet(); throw IOException("offline") })
        try {
            val provider = AdDeviceIdProvider(context, EApiService(NeteaseHttp(client, Json { ignoreUnknownKeys = true })), scope)
            assertEquals("old-id", provider.get())
            repeat(5) { assertEquals("old-id", provider.cachedAndRefresh()) }
            yield()
            assertEquals(1, calls.get())
        } finally { scope.cancel(); client.close(); preferences.edit().clear().commit() }
    }
}
