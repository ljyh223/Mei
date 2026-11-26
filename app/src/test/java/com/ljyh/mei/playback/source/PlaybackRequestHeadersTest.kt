package com.ljyh.mei.playback.source

import android.app.Application
import androidx.media3.datasource.DataSpec
import androidx.test.core.app.ApplicationProvider
import com.ljyh.mei.constants.UserAgent
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class PlaybackRequestHeadersTest {
    @Test fun playbackSendsExactlyOneUserAgentWithOrWithoutAPluginOverride() {
        val observed = ConcurrentHashMap<String, List<String>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            observed[exchange.requestURI.path] = exchange.requestHeaders["User-Agent"].orEmpty()
            exchange.sendResponseHeaders(200, 1)
            exchange.responseBody.use { it.write(0) }
        }
        server.start()
        try {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val factory = CacheManager.getCacheDataSourceFactory(context)
            fun play(path: String, headers: Map<String, String>) {
                val source = factory.createDataSource()
                try {
                    source.open(DataSpec.Builder().setUri("http://127.0.0.1:${server.address.port}/$path")
                        .setHttpRequestHeaders(headers).build())
                    assertEquals(1, source.read(ByteArray(1), 0, 1))
                } finally {
                    source.close()
                }
            }
            play("plugin", mapOf("user-agent" to "Plugin-Agent"))
            play("default", emptyMap())
            assertEquals(listOf("Plugin-Agent"), observed["/plugin"])
            assertEquals(listOf(UserAgent), observed["/default"])
        } finally {
            CacheManager.release()
            server.stop(0)
        }
    }
}
