package com.ljyh.mei.data.network

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class QrLoginClientTest {
    @Test
    fun qrRequestUsesConsistentPcWebFingerprint() {
        val request = QrLoginClient().buildRequest(
            path = "/login/qrcode/unikey",
            payload = mapOf("type" to 1),
        )

        assertEquals("https://music.163.com/weapi/login/qrcode/unikey", request.url)
        assertTrue(request.headers["User-Agent"].orEmpty().contains("Macintosh"))
        assertTrue(request.headers["User-Agent"].orEmpty().contains("Chrome/125"))
        assertEquals("https://music.163.com", request.headers["Referer"])
        assertEquals("os=pc; appver=3.1.17", request.headers["Cookie"])
        assertNull(request.headers["X-Real-IP"])
        assertNull(request.headers["X-Forwarded-For"])
        assertFalse(request.headers["Cookie"].orEmpty().contains("deviceId"))
        assertFalse(request.headers["Cookie"].orEmpty().contains("mobilename"))

        assertEquals(setOf("params", "encSecKey"), request.form.names())
    }

    @Test
    fun successfulQrLoginReadsMusicUFromSetCookie() = runBlocking {
        val requestCookie = AtomicReference<String>()
        val requestForm = AtomicReference<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/weapi/login/qrcode/unikey") { exchange ->
                exchange.requestBody.use { it.readBytes() }
                exchange.responseHeaders.add("Set-Cookie", "__csrf=test-csrf; Path=/")
                exchange.respond("""{"code":200,"unikey":"test-key"}""")
            }
            createContext("/weapi/login/qrcode/client/login") { exchange ->
                requestCookie.set(exchange.requestHeaders.getFirst("Cookie"))
                requestForm.set(exchange.requestBody.use { it.readBytes().decodeToString() })
                exchange.responseHeaders.add("Set-Cookie", "MUSIC_U=test-token; Path=/")
                exchange.respond("""{"code":803,"message":"ok"}""")
            }
            start()
        }
        try {
            val client = QrLoginClient("http://127.0.0.1:${server.address.port}")
            assertEquals("test-key", client.createQrLoginKey().unikey)
            val result = client.checkQrLogin("test-key")

            assertEquals(803, result.code)
            assertEquals("test-token", result.musicU)
            assertTrue(requestCookie.get().contains("__csrf=test-csrf"))
            assertTrue(requestForm.get().contains("params="))
            assertTrue(requestForm.get().contains("encSecKey="))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun lateResponseFromReplacedSessionCannotWriteCookies() = runBlocking {
        val oldCheckStarted = CountDownLatch(1)
        val releaseOldCheck = CountDownLatch(1)
        val keyCounter = AtomicInteger()
        val executor = Executors.newCachedThreadPool()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            this.executor = executor
            createContext("/weapi/login/qrcode/unikey") { exchange ->
                exchange.requestBody.use { it.readBytes() }
                val number = keyCounter.incrementAndGet()
                exchange.responseHeaders.add("Set-Cookie", "NMTID=session-$number; Path=/")
                exchange.respond("""{"code":200,"unikey":"key-$number"}""")
            }
            createContext("/weapi/login/qrcode/client/login") { exchange ->
                exchange.requestBody.use { it.readBytes() }
                oldCheckStarted.countDown()
                releaseOldCheck.await(5, TimeUnit.SECONDS)
                exchange.responseHeaders.add("Set-Cookie", "MUSIC_U=old-token; Path=/")
                exchange.respond("""{"code":803,"message":"ok"}""")
            }
            start()
        }

        try {
            val client = QrLoginClient("http://127.0.0.1:${server.address.port}")
            assertEquals("key-1", client.createQrLoginKey().unikey)

            val oldCheck = async(Dispatchers.IO) {
                runCatching { client.checkQrLogin("key-1") }
            }
            assertTrue(oldCheckStarted.await(5, TimeUnit.SECONDS))

            assertEquals("key-2", client.createQrLoginKey().unikey)
            releaseOldCheck.countDown()

            assertTrue(oldCheck.await().exceptionOrNull() is CancellationException)
            val currentRequest = client.buildRequest(
                path = "/login/qrcode/client/login",
                payload = mapOf("key" to "key-2", "type" to 1),
            )
            assertFalse(currentRequest.headers["Cookie"].orEmpty().contains("MUSIC_U=old-token"))
            assertTrue(currentRequest.headers["Cookie"].orEmpty().contains("NMTID=session-2"))
        } finally {
            releaseOldCheck.countDown()
            server.stop(0)
            executor.shutdownNow()
        }
    }

    private fun HttpExchange.respond(body: String) {
        val bytes = body.toByteArray()
        responseHeaders.add("Content-Type", "application/json")
        sendResponseHeaders(200, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }
}
