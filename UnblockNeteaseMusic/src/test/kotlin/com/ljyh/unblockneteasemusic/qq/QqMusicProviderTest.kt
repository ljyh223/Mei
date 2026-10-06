package com.ljyh.unblockneteasemusic.qq

import com.ljyh.unblockneteasemusic.model.LyricRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
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
                """{"request":{"data":{"body":{"item_song":[{"id":42,"name":"Song","interval":187,"singer":[{"id":7,"name":"Artist"}],"album":{"id":9,"name":"Album","pmid":"abc"},"unused":"ignored"}]}}}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) { install(ContentNegotiation) { json(json) } }
        val provider = QqMusicProvider(client)

        val track = provider.search("Song").single()
        assertEquals("42", track.id.value)
        assertEquals("Song", track.title)
        assertEquals(187_000, track.durationMs)
        assertEquals("Artist", track.artists.single().name)
        assertTrue(track.album!!.coverUrl!!.endsWith("abc.jpg"))
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
