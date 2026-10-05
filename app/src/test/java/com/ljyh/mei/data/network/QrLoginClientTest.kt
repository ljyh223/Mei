package com.ljyh.mei.data.network

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
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

class QrLoginClientTest {
    @Test
    fun qrRequestUsesConsistentPcWebFingerprint() {
        val request = QrLoginClient().buildRequest(
            path = "/login/qrcode/unikey",
            payload = mapOf("type" to 1),
        )

        assertEquals("https://music.163.com/weapi/login/qrcode/unikey", request.url.toString())
        assertTrue(request.header("User-Agent").orEmpty().contains("Macintosh"))
        assertTrue(request.header("User-Agent").orEmpty().contains("Chrome/125"))
        assertEquals("https://music.163.com", request.header("Referer"))
        assertEquals("os=pc; appver=3.1.17", request.header("Cookie"))
        assertNull(request.header("X-Real-IP"))
        assertNull(request.header("X-Forwarded-For"))
        assertFalse(request.header("Cookie").orEmpty().contains("deviceId"))
        assertFalse(request.header("Cookie").orEmpty().contains("mobilename"))

        val body = request.body as FormBody
        assertEquals(setOf("params", "encSecKey"), (0 until body.size).map(body::name).toSet())
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
            assertFalse(currentRequest.header("Cookie").orEmpty().contains("MUSIC_U=old-token"))
            assertTrue(currentRequest.header("Cookie").orEmpty().contains("NMTID=session-2"))
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
