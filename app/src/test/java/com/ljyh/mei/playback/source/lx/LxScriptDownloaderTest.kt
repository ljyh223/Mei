package com.ljyh.mei.playback.source.lx

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class LxScriptDownloaderTest {
    private lateinit var server: HttpServer
    private val script = "/** @name Test */\nconst testSource = true;".toByteArray()
    private val downloader = LxScriptDownloader()
    private fun url(path: String) = "http://127.0.0.1:${server.address.port}$path"

    @Before fun start() { server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0); server.start() }
    @After fun stop() { server.stop(0) }
    private fun HttpExchange.reply(status: Int, body: ByteArray, type: String = "application/javascript", chunked: Boolean = false) {
        responseHeaders.add("Content-Type", type)
        sendResponseHeaders(status, if (chunked) 0 else body.size.toLong())
        responseBody.use { it.write(body) }
    }

    @Test fun downloadsRawScriptWithoutAccountHeaders() = runBlocking {
        val cookie = AtomicReference<String?>()
        val authorization = AtomicReference<String?>()
        server.createContext("/source.js") {
            cookie.set(it.requestHeaders.getFirst("Cookie"))
            authorization.set(it.requestHeaders.getFirst("Authorization"))
            it.reply(200, script, "text/plain; charset=utf-8")
        }
        val result = downloader.download("  ${url("/source.js")}  ")
        assertArrayEquals(script, result.bytes)
        assertEquals("source.js", result.filename)
        assertNull(cookie.get())
        assertNull(authorization.get())
    }

    @Test fun followsRedirectAndUsesFinalFilename() = runBlocking {
        server.createContext("/share") { it.responseHeaders.add("Location", "/actual.js"); it.sendResponseHeaders(302, -1); it.close() }
        server.createContext("/actual.js") { it.reply(200, script) }
        val result = downloader.download(url("/share"))
        assertArrayEquals(script, result.bytes)
        assertEquals("actual.js", result.filename)
    }

    @Test fun acceptsDirectDownloadEndpointsWithoutJsSuffix() = runBlocking {
        server.createContext("/download") { it.reply(200, script, "application/octet-stream") }
        assertEquals("网络音源.js", downloader.download(url("/download")).filename)
    }

    @Test fun rejectsInvalidUrlsWithoutMakingRequests() = runBlocking {
        for (input in listOf("", "file:///tmp/source.js", "ftp://example.com/source.js", "not a url")) {
            assertTrue(runCatching { downloader.download(input) }.exceptionOrNull() is LxScriptImportException)
        }
        assertTrue(runCatching { downloader.download("https://user:password@example.com/source.js") }.exceptionOrNull() is LxScriptImportException)
    }

    @Test fun rejectsWebPagesJsonAndEmptyResponses() = runBlocking {
        server.createContext("/html") { it.reply(200, "<!DOCTYPE html><html>Share page</html>".toByteArray(), "text/plain") }
        server.createContext("/json") { it.reply(200, "{}".toByteArray(), "application/json") }
        server.createContext("/empty") { it.sendResponseHeaders(204, -1); it.close() }
        for (path in listOf("/html", "/json", "/empty")) {
            assertTrue(runCatching { downloader.download(url(path)) }.exceptionOrNull() is LxScriptImportException)
        }
    }

    @Test fun httpErrorsAreActionableWithoutRepeatingTheUrl() = runBlocking {
        server.createContext("/missing") { it.reply(404, "not found".toByteArray(), "text/plain") }
        val error = runCatching { downloader.download(url("/missing?key=example")) }.exceptionOrNull()
        assertTrue(error is LxScriptImportException)
        assertTrue(error!!.message!!.contains("404"))
        assertFalse(error.message!!.contains("key="))
    }

    @Test fun enforcesSizeLimitForFixedAndChunkedBodies() = runBlocking {
        val oversized = ByteArray(LxScriptStore.MAX_SCRIPT_BYTES + 32) { 65 }
        server.createContext("/large") { runCatching { it.reply(200, oversized) }; it.close() }
        server.createContext("/stream") { runCatching { it.reply(200, oversized, chunked = true) }; it.close() }
        for (path in listOf("/large", "/stream")) {
            val error = runCatching { downloader.download(url(path)) }.exceptionOrNull()
            assertTrue(error is LxScriptImportException)
            assertTrue(error!!.message!!.contains("1 MB"))
        }
    }

    @Test fun cancellationDoesNotWaitForTheDownloadTimeout() = runBlocking {
        val started = CountDownLatch(1)
        val gate = CountDownLatch(1)
        server.createContext("/held") {
            started.countDown()
            gate.await(4, TimeUnit.SECONDS)
            runCatching { it.reply(200, script) }; it.close()
        }
        val task = async(Dispatchers.Default) { downloader.download(url("/held")) }
        try {
            assertTrue(started.await(2, TimeUnit.SECONDS))
            withTimeout(1000) { task.cancelAndJoin() }
            assertTrue(task.isCancelled)
        } finally { gate.countDown() }
    }
}
