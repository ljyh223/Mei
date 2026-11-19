package com.ljyh.mei.playback.source.musicfree

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.repository.SongRepository
import com.ljyh.mei.playback.source.lx.LxScriptDownloader
import com.ljyh.mei.utils.preferences.dataStore
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class MusicFreeSubscriptionStoreTest {
    private val code = """module.exports={platform:'Subscription fixture',version:'1',supportedSearchType:['music'],
        userVariables:[{key:'key'}],search(query){return {data:[{id:'1',title:query}],isEnd:true}}};"""

    @Test fun readingIsAPreviewAndBatchImportsKeepSuccessfulPluginsWhenOtherEntriesFail() = runBlocking {
        fixture { store, server ->
            val downloads = AtomicInteger()
            server.createContext("/share") { it.responseHeaders.add("Location", "/sub/main.json"); it.sendResponseHeaders(302, -1); it.close() }
            server.createContext("/sub/main.json") { it.reply("""{"desc":"Fixture","plugins":[
                {"name":"Working","url":"../working.js","version":"1"},
                {"name":"Incompatible","url":"../bad.js"},
                {"name":"Unavailable","url":"../missing.js"},
                {"name":"Duplicate","url":"../working.js"}
            ]}""", "application/json") }
            server.createContext("/working.js") {
                downloads.incrementAndGet()
                assertNull(it.requestHeaders.getFirst("Cookie"))
                assertNull(it.requestHeaders.getFirst("Authorization"))
                it.reply(code)
            }
            server.createContext("/bad.js") { it.reply("module.exports={}") }
            val document = store.readImportUrl(url(server, "/share")) as MusicFreeImportDocument.Subscription
            assertEquals(3, document.manifest.plugins.size)
            assertEquals(0, downloads.get())
            assertTrue(store.snapshot().isEmpty())
            val progress = mutableListOf<Pair<Int, Int>>()
            val report = store.importSubscription(document.manifest.plugins) { done, total -> progress += done to total }
            assertEquals(listOf("Working"), report.imported.map { it.name })
            assertEquals(listOf("Incompatible", "Unavailable"), report.failures.map { it.entry.name })
            assertTrue(report.failures.last().reason.contains("404"))
            assertEquals(listOf(1 to 3, 2 to 3, 3 to 3), progress)
            val plugin = store.snapshot().single()
            store.configure(plugin.id, mapOf("key" to "fixture-value"))
            store.enable(plugin.id, false)
            store.importSubscription(listOf(document.manifest.plugins.first()))
            assertEquals(1, store.snapshot().size)
            assertEquals(mapOf("key" to "fixture-value"), store.snapshot().single().variables)
            assertFalse(store.snapshot().single().enabled)
            assertTrue(runCatching { store.importSubscription(emptyList()) }.exceptionOrNull() is MusicFreeImportException)
        }
    }

    @Test fun cancellationStopsFurtherDownloadsAndKeepsCompletedImports() = runBlocking {
        fixture { store, server ->
            val started = CountDownLatch(1)
            val gate = CountDownLatch(1)
            val remaining = AtomicInteger()
            server.createContext("/first.js") { it.reply(code) }
            server.createContext("/held.js") {
                started.countDown(); gate.await(4, TimeUnit.SECONDS)
                runCatching { it.reply(code) }; it.close()
            }
            server.createContext("/remaining.js") { remaining.incrementAndGet(); it.reply(code) }
            val task = async(Dispatchers.Default) {
                store.importSubscription(listOf("first", "held", "remaining").map {
                    MusicFreeSubscriptionEntry(it, url(server, "/$it.js"))
                })
            }
            try {
                assertTrue(started.await(3, TimeUnit.SECONDS))
                withTimeout(1000) { task.cancelAndJoin() }
                assertTrue(task.isCancelled)
                assertEquals(1, store.snapshot().size)
                assertEquals(0, remaining.get())
            } finally { gate.countDown() }
        }
    }

    private fun url(server: HttpServer, path: String) = "http://127.0.0.1:${server.address.port}$path"
    private fun HttpExchange.reply(text: String, type: String = "application/javascript") {
        val bytes = text.toByteArray()
        responseHeaders.add("Content-Type", type)
        sendResponseHeaders(200, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }
    private suspend fun fixture(block: suspend (MusicFreeStore, HttpServer) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val key = stringPreferencesKey("musicfree_plugins_v1")
        context.dataStore.edit { it.remove(key) }
        File(context.noBackupFilesDir, "musicfree").deleteRecursively()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply { start() }
        try { block(MusicFreeStore(context, LxScriptDownloader(), SongRepository(db.songDao())), server) }
        finally {
            server.stop(0); db.close()
            context.dataStore.edit { it.remove(key) }
            File(context.noBackupFilesDir, "musicfree").deleteRecursively()
        }
    }
}
