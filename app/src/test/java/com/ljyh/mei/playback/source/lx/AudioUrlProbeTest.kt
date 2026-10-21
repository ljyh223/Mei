package com.ljyh.mei.playback.source.lx

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class AudioUrlProbeTest {
    @Test fun measuresTheReturnedAudioInsteadOfTheCatalogDuration() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        fun add(path: String, seconds: Int) {
            // MPEG-1 Layer III, 128 kbit/s, 44.1 kHz. Only metadata is parsed; no decoder is involved.
            val bytes = ByteArray(seconds * 16_000)
            for (offset in 0 until bytes.size - 4 step 417) {
                bytes[offset] = 0xff.toByte(); bytes[offset + 1] = 0xfb.toByte()
                bytes[offset + 2] = 0x90.toByte()
            }
            server.createContext(path) { exchange ->
                val range = Regex("bytes=(\\d+)-(\\d+)").matchEntire(exchange.requestHeaders.getFirst("Range").orEmpty())
                val from = range?.groupValues?.get(1)?.toInt() ?: 0
                val to = minOf(range?.groupValues?.get(2)?.toInt() ?: bytes.lastIndex, bytes.lastIndex)
                exchange.responseHeaders.add("Content-Type", "audio/mpeg")
                if (range != null) exchange.responseHeaders.add("Content-Range", "bytes $from-$to/${bytes.size}")
                exchange.sendResponseHeaders(if (range == null) 200 else 206, (to - from + 1).toLong())
                exchange.responseBody.use { it.write(bytes, from, to - from + 1) }
            }
        }
        add("/trial", 30); add("/full", 31)
        server.start()
        try {
            val probe = AudioUrlProbe()
            val trial = probe.durationMs("http://127.0.0.1:${server.address.port}/trial")
            val full = probe.durationMs("http://127.0.0.1:${server.address.port}/full")
            assertEquals(30_000L, trial)
            assertEquals(31_000L, full)
            assertTrue(PreviewPolicy.isPreview(trial))
            assertFalse(PreviewPolicy.isPreview(full))
        } finally { server.stop(0) }
    }
}
