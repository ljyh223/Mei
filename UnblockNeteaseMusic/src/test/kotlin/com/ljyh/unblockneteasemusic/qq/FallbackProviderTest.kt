package com.ljyh.unblockneteasemusic.qq

import com.ljyh.unblockneteasemusic.kuwo.KuwoMusicProvider
import com.ljyh.unblockneteasemusic.migu.MiguMusicProvider
import com.ljyh.unblockneteasemusic.migu.decodeMiguPayload
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FallbackProviderTest {
    @Test
    fun kuwoSearchAndResolveUseDirectUrl() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            when (request.url.host) {
                "search.kuwo.cn" -> respond(
                    """{"content":[{}, {"musicpage":{"abslist":[{"MUSICRID":"MUSIC_77","SONGNAME":"Song","ARTIST":"Artist","ALBUM":"Album","DURATION":"180"}]}}]}"""
                )
                "antiserver.kuwo.cn" -> respond("https://audio.example/song.mp3")
                else -> {
                    assertEquals("audio.example", request.url.host)
                    assertEquals("bytes=0-8191", request.headers[HttpHeaders.Range])
                    respond(
                        "audio bytes", status = HttpStatusCode.PartialContent,
                        headers = headersOf(HttpHeaders.ContentRange, "bytes 0-8191/3000000"),
                    )
                }
            }
        })
        val provider = KuwoMusicProvider(client)
        val track = provider.search("Song - Artist", 5).single()
        assertEquals("77", track.id.value)
        assertEquals(180_000, track.durationMs)
        assertEquals("https://audio.example/song.mp3", provider.resolve(track)?.url)
        provider.close()
    }

    @Test
    fun kuwoRejectsShortCopyrightNoticeMp3() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            when (request.url.host) {
                "search.kuwo.cn" -> respond(
                    """{"content":[{}, {"musicpage":{"abslist":[{"MUSICRID":"MUSIC_440615","SONGNAME":"花海","ARTIST":"周杰伦","DURATION":"264","tpay":"1"}]}}]}"""
                )
                "antiserver.kuwo.cn" -> respond("https://audio.example/notice.mp3")
                else -> respond(
                    "audio bytes", status = HttpStatusCode.PartialContent,
                    headers = headersOf(
                        HttpHeaders.ContentRange to listOf("bytes 0-8191/181521"),
                        HttpHeaders.ContentType to listOf("audio/mpeg"),
                    ),
                )
            }
        })
        val provider = KuwoMusicProvider(client)
        val track = provider.search("花海 - 周杰伦", 5).single()
        assertNull(provider.resolve(track))
        provider.close()
    }

    @Test
    fun miguAcceptsNumericIdsAndResolvesListenUrl() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            when (request.url.host) {
                "c.musicapp.migu.cn" -> if (request.url.encodedPath.contains("search_all")) respond(
                    """{"songResultData":{"result":[{"contentId":99,"copyrightId":88,"name":"Song","singers":[{"id":7,"name":"Artist"}],"albums":[{"id":8,"name":"Album"}]}]}}"""
                ) else respond(
                    """{"data":{"audioFormatType":"HQ","url":"https://audio.example/song.mp3","song":{"duration":180}}}"""
                )
                else -> {
                    assertEquals("audio.example", request.url.host)
                    respond("audio bytes", status = HttpStatusCode.PartialContent)
                }
            }
        })
        val provider = MiguMusicProvider(client)
        val track = provider.search("Song - Artist", 5).single()
        assertEquals("99", track.id.value)
        assertEquals("88", track.playbackId)
        assertEquals("8", track.album?.id)
        val playable = provider.resolve(track)
        assertEquals("https://audio.example/song.mp3", playable?.url)
        assertEquals(180_000, playable?.track?.durationMs)
        provider.close()
    }

    @Test
    fun miguDecodesSignedPayload() {
        val plain = """{"data":{"url":"https://audio.example/song.mp3"}}"""
        val key = "Jk8qzuePiJ1qE3mDYhLQ3T73DtDoAhLP".toByteArray()
        val seed = 99
        val encoded = byteArrayOf(0xab.toByte(), 0xcd.toByte(), 0x01, seed.toByte()) +
            plain.toByteArray().mapIndexed { index, byte ->
                ((byte.toInt() and 0xff) - seed + (key[index % key.size].toInt() and 0xff)).toByte()
            }.toByteArray()
        assertEquals(plain, decodeMiguPayload(encoded))
    }
}
