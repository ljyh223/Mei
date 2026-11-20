package com.ljyh.mei.data.network

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.ljyh.mei.constants.AppleMusicWebTokenKey
import com.ljyh.mei.constants.AppleMusicWebTokenRefreshAtKey
import com.ljyh.mei.utils.preferences.dataStore
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class AppleMusicCatalogClientTest {
    private fun token(nonce: Int = 0): String {
        fun encode(text: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray())
        return encode("{\"alg\":\"fixture\"}") + "." + encode("{\"exp\":${System.currentTimeMillis()/1000+86400},\"nonce\":$nonce}") + ".fixture"
    }

    @Test fun artistsAndAlbumsShareThePersistedWebTokenWithoutAccountCookies() = runBlocking {
        fixture { context, server, origin ->
            val bundles = AtomicInteger()
            server.createContext("/") { it.reply("<script src=\"/assets/index~fixture.js\"></script>") }
            server.createContext("/assets/") { it.reply("const token='${token(bundles.incrementAndGet())}';") }
            val requests = mutableListOf<String>()
            server.createContext("/v1/") {
                requests += it.requestHeaders.getFirst("Authorization")
                assertEquals(origin, it.requestHeaders.getFirst("Origin"))
                assertNull(it.requestHeaders.getFirst("Cookie"))
                it.reply("{\"data\":[]}")
            }
            val http = OkHttpClient()
            val first = AppleMusicCatalogClient(context, http, origin, origin)
            first.warmToken()
            first.get("$origin/v1/catalog/us/artists/123")
            val recreated = AppleMusicCatalogClient(context, http, origin, origin)
            recreated.get("$origin/v1/catalog/us/albums/456")
            assertEquals(1, bundles.get())
            assertEquals(2, requests.size)
            assertEquals(requests[0], requests[1])
            assertTrue(requests[0].startsWith("Bearer "))
        }
    }

    @Test fun unauthorizedResponseRefreshesTheSharedTokenAndRetriesOnce() = runBlocking {
        fixture { context, server, origin ->
            val bundles = AtomicInteger(); val calls = AtomicInteger()
            server.createContext("/") { it.reply("<script src=\"/assets/index~fixture.js\"></script>") }
            server.createContext("/assets/") { it.reply("const token='${token(bundles.incrementAndGet())}';") }
            server.createContext("/v1/") { it.reply("{\"data\":[]}", if (calls.incrementAndGet() == 1) 401 else 200) }
            AppleMusicCatalogClient(context, OkHttpClient(), origin, origin).get("$origin/v1/catalog/us/artists/123")
            assertEquals(2, calls.get())
            assertEquals(2, bundles.get())
        }
    }

    @Test fun failedRefreshCanReuseAnUnexpiredTokenAndForeignOriginsAreRejected() = runBlocking {
        fixture { context, server, origin ->
            val saved = token()
            context.dataStore.edit { it[AppleMusicWebTokenKey] = saved; it[AppleMusicWebTokenRefreshAtKey] = 0L }
            server.createContext("/") { it.reply("offline", 503) }
            server.createContext("/v1/") {
                assertEquals("Bearer $saved", it.requestHeaders.getFirst("Authorization"))
                it.reply("{\"data\":[]}")
            }
            val client = AppleMusicCatalogClient(context, OkHttpClient(), origin, origin)
            client.get("$origin/v1/catalog/us/artists/123")
            assertEquals(saved, context.dataStore.data.first()[AppleMusicWebTokenKey])
            assertTrue(runCatching { client.get("https://example.com/never-send-token") }.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test fun tokenDownloadIsCancelledWithThePageRequest() = runBlocking {
        fixture { context, server, origin ->
            val started = CountDownLatch(1); val gate = CountDownLatch(1)
            server.createContext("/") {
                started.countDown(); gate.await(4, TimeUnit.SECONDS)
                runCatching { it.reply("offline", 503) }; it.close()
            }
            val client = AppleMusicCatalogClient(context, OkHttpClient(), origin, origin)
            val task = async(Dispatchers.Default) { client.warmToken() }
            try {
                assertTrue(started.await(2, TimeUnit.SECONDS))
                withTimeout(1000) { task.cancelAndJoin() }
                assertNull(context.dataStore.data.first()[AppleMusicWebTokenKey])
            } finally { gate.countDown() }
        }
    }

    private fun HttpExchange.reply(body: String, status: Int = 200) {
        val bytes = body.toByteArray(); sendResponseHeaders(status, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }
    private suspend fun fixture(block: suspend (Application, HttpServer, String) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        suspend fun clear() = context.dataStore.edit { it.remove(AppleMusicWebTokenKey); it.remove(AppleMusicWebTokenRefreshAtKey) }
        clear()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply { start() }
        val origin = "http://127.0.0.1:${server.address.port}"
        try { block(context, server, origin) } finally { server.stop(0); clear() }
    }
}
