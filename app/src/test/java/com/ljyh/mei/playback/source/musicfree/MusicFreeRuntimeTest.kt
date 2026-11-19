package com.ljyh.mei.playback.source.musicfree

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.InetSocketAddress

class MusicFreeRuntimeTest {
    private fun runtime() = MusicFreeRuntime(
        File("src/main/assets/lx/preload.js").readText(),
        File("src/main/assets/musicfree/modules.js").readText(),
        File("src/main/assets/musicfree/preload.js").readText(), "test",
    )

    @Test fun commonJsSearchPreservesOpaqueFieldsAndUsesPluginVariables() = runBlocking {
        val code = """
            module.exports = {
              platform:'Test',version:'1',userVariables:[{key:'server',name:'Server'}],
              async search(query,page,type) {
                return {isEnd:page === 2,data:[{id:'opaque-id',title:query,artist:type,duration:123,
                  platform:'Test',extra:{ticket:'opaque'},artwork:'https://example.com/cover.jpg'}]};
              },
              async getMediaSource(item,quality) {
                if(item.extra.ticket !== 'opaque') throw new Error('lost fields');
                return {url:env.getUserVariables().server+'/'+quality,headers:{Referer:'https://example.com/'}};
              }
            };
        """.trimIndent()
        runtime().use(code, mapOf("server" to "https://example.com/audio")) {
            assertTrue(inspect().getValue("canSearch").jsonPrimitive.boolean)
            val tracks = musicFreePage("plugin", call("search", buildJsonArray { add("Hello"); add(2); add("music") }))
            assertTrue(tracks.isEnd)
            assertEquals(123_000L, tracks.tracks.single().metadata().duration)
            val audio = call("getMediaSource", buildJsonArray { add(tracks.tracks.single().data); add("standard") }).jsonObject
            assertEquals("https://example.com/audio/standard", audio.text("url"))
            assertEquals("https://example.com/", audio.getValue("headers").jsonObject.text("Referer"))
        }
    }

    @Test fun bundledModulesRunRealCryptoHtmlAndQueryOperations() = runBlocking {
        val code = """
            const CryptoJS=require('crypto-js'),cheerio=require('cheerio'),qs=require('qs'),
              he=require('he'),dayjs=require('dayjs'),integer=require('big-integer');
            module.exports={platform:'Modules',async search(){
              if(CryptoJS.MD5('abc').toString()!=='900150983cd24fb0d6963f7d28e17f72') throw new Error('md5');
              if(CryptoJS.lib.WordArray.random(16).sigBytes!==16) throw new Error('random');
              const key=CryptoJS.enc.Utf8.parse('1234567890123456');
              const encrypted=CryptoJS.AES.encrypt('test',key,{mode:CryptoJS.mode.ECB,padding:CryptoJS.pad.Pkcs7});
              if(CryptoJS.AES.decrypt(encrypted,key,{mode:CryptoJS.mode.ECB,padding:CryptoJS.pad.Pkcs7}).toString(CryptoJS.enc.Utf8)!=='test') throw new Error('aes');
              if(cheerio.load('<p>你好</p>')('p').text()!=='你好') throw new Error('html');
              if(qs.stringify({q:'a b'})!=='q=a%20b') throw new Error('qs');
              if(he.decode('&amp;')!=='&') throw new Error('he');
              if(dayjs('2020-01-01').year()!==2020) throw new Error('date');
              if(integer('9007199254740993').add(1).toString()!=='9007199254740994') throw new Error('integer');
              if(Buffer.from('你好').toString()!=='你好') throw new Error('buffer');
              return {isEnd:true,data:[]};
            }};
        """.trimIndent()
        runtime().use(code) { assertTrue(musicFreePage("test", call("search", JsonArray(emptyList()))).isEnd) }
    }

    @Test fun axiosSupportsQueryPostJsonWithoutAppCookies() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/search") { exchange ->
            assertNull(exchange.requestHeaders.getFirst("Cookie"))
            assertEquals("plugin-header", exchange.requestHeaders.getFirst("X-Plugin"))
            assertTrue(exchange.requestURI.query.contains("keyword=hello"))
            assertTrue(exchange.requestBody.bufferedReader().readText().contains("page"))
            val data = """{"isEnd":true,"data":[{"id":"1","title":"Song"}]}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, data.size.toLong())
            exchange.responseBody.use { it.write(data) }
        }
        server.start()
        try {
            val endpoint = "http://127.0.0.1:" + server.address.port
            val code = """
                const axios=require('axios').create({baseURL:'$endpoint',headers:{'X-Plugin':'plugin-header'}});
                module.exports={platform:'HTTP',async search(q,page){
                  return (await axios.post('/search',{page},{params:{keyword:q}})).data;
                }};
            """.trimIndent()
            runtime().use(code) {
                assertEquals("Song", musicFreePage("http", call("search", buildJsonArray { add("hello"); add(1) })).tracks.single().metadata().title)
            }
        } finally { server.stop(0) }
    }

    @Test fun rejectedCallDoesNotPoisonSessionAndTimersComplete() = runBlocking {
        runtime().use("""
            module.exports={platform:'Promises',async search(q){
              if(q==='bad') throw new Error('no');
              await new Promise(resolve=>setTimeout(resolve,5));
              return {data:[],isEnd:true};
            }};
        """.trimIndent()) {
            assertTrue(runCatching { call("search", buildJsonArray { add("bad") }) }.isFailure)
            assertTrue(musicFreePage("test", call("search", buildJsonArray { add("good") })).isEnd)
        }
    }

    @Test fun nativeModulesAndUnboundedExecutionAreRejected() = runBlocking {
        assertTrue(runCatching { runtime().use("require('fs')") {} }.isFailure)
        assertTrue(runCatching { runtime().use("while(true){}") {} }.isFailure)
    }

    @Test fun cancellationStopsPendingHttp() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val gate = java.util.concurrent.CountDownLatch(1)
        val entered = CompletableDeferred<Unit>()
        server.createContext("/wait") { exchange ->
            entered.complete(Unit)
            gate.await(5, java.util.concurrent.TimeUnit.SECONDS)
            runCatching { exchange.sendResponseHeaders(200, 0); exchange.close() }
        }
        server.start()
        try {
            val endpoint = "http://127.0.0.1:" + server.address.port + "/wait"
            val code = """
                const axios=require('axios');
                module.exports={platform:'Cancel',async search(){return (await axios.get('$endpoint')).data}};
            """.trimIndent()
            val job = launch { runtime().use(code) { call("search", JsonArray(emptyList())) } }
            withTimeout(5000) { entered.await() }
            withTimeout(2000) { job.cancelAndJoin() }
            assertTrue(job.isCancelled)
        } finally { gate.countDown(); server.stop(0) }
    }
}
