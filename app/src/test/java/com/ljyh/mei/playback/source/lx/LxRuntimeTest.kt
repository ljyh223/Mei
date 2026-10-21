package com.ljyh.mei.playback.source.lx

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

class LxRuntimeTest {
    private val runtime get() = LxRuntime(File("src/main/assets/lx/preload.js").readText())
    private val script = LxScript("test", "Host test")
    private val candidate = LxCandidate("wy", buildJsonObject { put("songmid", "123") }, 180_000)
    private fun code(handler: String) = """
        lx.on(lx.EVENT_NAMES.request, $handler);
        lx.send(lx.EVENT_NAMES.inited, {sources:{wy:{name:'Test',type:'music',actions:['musicUrl'],qualitys:['128k','320k']}}});
    """.trimIndent()

    @Test fun promiseAndSongMetadataRoundTrip() = runBlocking {
        runtime.use(script, code("async ({source,info}) => 'https://example.com/' + source + '/' + info.musicInfo.songmid + '/' + info.type")) {
            assertEquals(listOf("128k", "320k"), sources.getValue("wy").qualitys)
            assertEquals("https://example.com/wy/123/320k", musicUrl(candidate, "320k"))
        }
    }

    @Test fun rejectedQualityDoesNotPoisonSession() = runBlocking {
        runtime.use(script, code("async ({info}) => { if(info.type==='320k') throw new Error('bad'); return 'https://example.com/full.mp3'; }")) {
            assertTrue(runCatching { musicUrl(candidate, "320k") }.isFailure)
            assertEquals("https://example.com/full.mp3", musicUrl(candidate, "128k"))
        }
    }

    @Test fun utilityAndCancelledTimerWork() = runBlocking {
        val body = """
            async () => {
                const {buffer,crypto}=lx.utils;
                if(crypto.md5('abc')!=='900150983cd24fb0d6963f7d28e17f72') throw new Error('md5');
                if(buffer.bufToString(buffer.from('5L2g5aW9','base64'))!=='你好') throw new Error('utf8');
                const key=buffer.from('2b7e151628aed2a6abf7158809cf4f3c','hex');
                const value=buffer.from('6bc1bee22e409f96e93d7e117393172a','hex');
                if(buffer.bufToString(crypto.aesEncrypt(value,'aes-128-ecb',key),'hex').slice(0,32)!=='3ad77bb40d7a3660a89ecaf32466ef97') throw new Error('aes');
                const timer=setTimeout(()=>{throw new Error('cancelled timer fired')},12000); clearTimeout(timer);
                return await new Promise(resolve=>setTimeout(()=>resolve('https://example.com/full.mp3'),5));
            }
        """.trimIndent()
        withTimeout(2000) { runtime.use(script, code(body)) { assertEquals("https://example.com/full.mp3", musicUrl(candidate, "128k")) } }
    }

    @Test fun httpJsonAndCallbackAreCompatible() = runBlocking {
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/resolve") { exchange ->
            requests.incrementAndGet()
            assertNull(exchange.requestHeaders.getFirst("Cookie"))
            val bytes = "{\"url\":\"https://example.com/full.mp3\"}".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val handler = """() => new Promise((resolve,reject)=>lx.request('http://127.0.0.1:${server.address.port}/resolve',{},(err,resp,body)=> {
                if(err) reject(err); else if(resp.statusCode===200 && body===resp.body) resolve(body.url); else reject(new Error('response'));
            }))"""
            runtime.use(script, code(handler)) { assertEquals("https://example.com/full.mp3", musicUrl(candidate, "128k")) }
            assertEquals(1, requests.get())
        } finally { server.stop(0) }
    }

    @Test fun missingInitializationIsRejected() = runBlocking {
        assertTrue(runCatching { runtime.use(script, "const x=1;") {} }.isFailure)
    }

    @Test fun busyScriptIsInterrupted() = runBlocking {
        val start = System.nanoTime()
        assertTrue(runCatching { withTimeout(500) { runtime.use(script, "while(true){}") {} } }.isFailure)
        assertTrue("Busy evaluation was not interrupted", (System.nanoTime() - start) / 1_000_000 < 4000)
    }

    @Test fun aScriptCannotLoadJavaOrAuthenticatedClients() = runBlocking {
        runtime.use(script, code("async () => { if(typeof Java!=='undefined' || typeof require!=='undefined' || typeof __meiHttp!=='undefined') throw new Error('escaped'); return 'https://example.com/full.mp3'; }")) {
            assertEquals("https://example.com/full.mp3", musicUrl(candidate, "128k"))
        }
    }
    @Test fun immediateHttpAbortDoesNotWaitForRequestTimeout() = runBlocking {
        val gate = java.util.concurrent.CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/held") { exchange ->
            gate.await(4, java.util.concurrent.TimeUnit.SECONDS)
            runCatching { exchange.sendResponseHeaders(200, 0); exchange.close() }
        }
        server.start()
        try {
            val body = """() => new Promise(resolve => {
                const request = lx.request('http://127.0.0.1:${server.address.port}/held', {timeout:10000}, () => { throw new Error('aborted callback'); });
                request.abort();
                setTimeout(() => resolve('https://example.com/full.mp3'), 10);
            })"""
            withTimeout(1500) { runtime.use(script, code(body)) { assertEquals("https://example.com/full.mp3", musicUrl(candidate, "128k")) } }
        } finally { gate.countDown(); server.stop(0) }
    }

    @Test fun oversizedJsAllocationIsRejected() = runBlocking {
        assertTrue(runCatching { runtime.use(script, "const huge=new Uint8Array(64*1024*1024);") {} }.isFailure)
    }

}
