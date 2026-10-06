package com.ljyh.unblockneteasemusic.qq

import com.ljyh.unblockneteasemusic.model.LyricRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QqMusicProviderTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun searchMapsOnlyTheFieldsNeededByTheApp() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals("u.y.qq.com", request.url.host)
            respond(
                """{"request":{"data":{"body":{"item_song":[{"id":42,"mid":"songmid","name":"Song","interval":187,"singer":[{"id":7,"name":"Artist"}],"album":{"id":9,"name":"Album","pmid":"abc"},"unused":"ignored"}]}}}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) { install(ContentNegotiation) { json(json) } }
        val provider = QqMusicProvider(client)

        val track = provider.search("Song").single()
        assertEquals("42", track.id.value)
        assertEquals("songmid", track.playbackId)
        assertEquals("Song", track.title)
        assertEquals(187_000, track.durationMs)
        assertEquals("Artist", track.artists.single().name)
        assertTrue(track.album!!.coverUrl!!.endsWith("abc.jpg"))
        provider.close()
    }

    @Test
    fun vkeyResolvesAndProbesPlayableUrl() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            if (request.url.host == "u.y.qq.com") {
                respond(
                    """{"req_0":{"data":{"sip":["https://audio.example/"],"midurlinfo":[{"purl":"song.mp3"}]}}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                assertEquals("audio.example", request.url.host)
                assertEquals("bytes=0-8191", request.headers[HttpHeaders.Range])
                respond("audio bytes", status = HttpStatusCode.PartialContent)
            }
        }) { install(ContentNegotiation) { json(json) } }
        val provider = QqMusicProvider(client)
        val track = com.ljyh.unblockneteasemusic.model.MusicTrack(
            com.ljyh.unblockneteasemusic.model.TrackId("qq", "42"),
            "Song", emptyList(), null, 180_000, playbackId = "songmid",
        )
        assertEquals("https://audio.example/song.mp3", provider.resolve(track)?.url)
        provider.close()
    }

    @Test
    fun searchForwardsConfiguredQqCookie() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals("uin=123; qqmusic_key=test", request.headers[HttpHeaders.Cookie])
            respond(
                """{"request":{"data":{"body":{"item_song":[]}}}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) { install(ContentNegotiation) { json(json) } }
        val provider = QqMusicProvider(client) { "uin=123; qqmusic_key=test" }
        assertTrue(provider.search("Song").isEmpty())
        provider.close()
    }

    @Test
    fun searchFallsBackToDesktopProtocolWhenLiteReturnsNoSongs() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            if (request.method.value == "POST") {
                respond(
                    """{"request":{"data":{"body":{"item_song":[]}}}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                assertTrue(request.url.parameters["data"].orEmpty().contains("DoSearchForQQMusicDesktop"))
                respond(
                    """{"search":{"data":{"body":{"song":{"list":[{"id":42,"mid":"mid42","name":"Song","interval":180,"singer":[{"id":7,"name":"Artist"}]}]}}}}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }) { install(ContentNegotiation) { json(json) } }
        val provider = QqMusicProvider(client)
        assertEquals("mid42", provider.search("Song - Artist").single().playbackId)
        provider.close()
    }

    @Test
    fun lyricRequestPreservesTheRequiredWireFields() {
        val encoded = json.encodeToString(
            LyricRequestDto(call = LyricCall(param = LyricParams(
                albumName = "QQ==",
                interval = 187,
                singerName = "QQ==",
                songID = 42,
                songName = "QQ==",
                qrc = 0,
            )))
        )
        assertTrue(encoded.contains("\"music.musichallSong.PlayLyricInfo.GetPlayLyricInfo\""))
        assertTrue(encoded.contains("\"qrc\":0"))
        assertTrue(encoded.contains("\"songID\":42"))
        assertTrue(encoded.contains("\"tmeAppID\":\"qqmusiclight\""))
    }
}
