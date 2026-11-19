package com.ljyh.mei.data.network

import androidx.room.Room
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.repository.LocalPlaylistRepository
import com.ljyh.mei.data.model.api.ManipulateTrackResult
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.ui.screen.playlist.PlaylistTrackAddOutcome
import com.ljyh.mei.ui.screen.playlist.toPlaylistTrackAddOutcome
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class PlaylistTrackMutationTest {
    @Test fun deletionAcceptsCodeOnlyAndPartialSuccessResponses() = runBlocking {
        for (body in listOf(
            """{"code":200}""",
            """{"code":200,"count":0}""",
            """{"code":200,"cloudCount":null,"count":null,"trackIds":null}""",
        )) {
            val result = mutate("del", body)
            assertTrue("Response $body became $result", result is Resource.Success)
            assertEquals(200, (result as Resource.Success).data.code)
        }
    }

    @Test fun rejectionRetainsTheBusinessCodeWithoutRequiringSuccessMetadata() = runBlocking {
        for (code in listOf(301, 403, 502, 512)) {
            val result = mutate("del", """{"code":$code,"message":"拒绝操作"}""")
            assertTrue(result is Resource.Success)
            val response = (result as Resource.Success).data
            assertEquals(code, response.code)
            assertNotEquals(200, response.code)
            assertEquals("拒绝操作", response.message)
        }
    }

    @Test fun missingStatusOrMalformedResponseStillFails() = runBlocking {
        assertTrue(mutate("del", """{"count":0}""") is Resource.Error)
        assertTrue(mutate("del", "invalid json") is Resource.Error)
    }

    @Test fun onlyAnExplicitServerDuplicateResponseIsReportedAsAlreadyPresent() = runBlocking {
        val added = mutate("add", """{"code":200,"cloudCount":0,"count":11,"trackIds":"[1]"}""")
        assertEquals(PlaylistTrackAddOutcome.Added, added.toPlaylistTrackAddOutcome())
        val unchanged = mutate("add", """{"code":200,"cloudCount":0,"count":10,"trackIds":"[1]"}""")
        assertEquals(PlaylistTrackAddOutcome.Added, unchanged.toPlaylistTrackAddOutcome())
        val duplicate = mutate("add", """{"code":502,"message":"歌单内歌曲重复"}""")
        assertEquals(PlaylistTrackAddOutcome.AlreadyExists, duplicate.toPlaylistTrackAddOutcome())
    }

    @Test fun codeOnlyAddSuccessDoesNotInventAnUnchangedCount() = runBlocking {
        assertEquals(
            PlaylistTrackAddOutcome.Added,
            mutate("add", """{"code":200}""").toPlaylistTrackAddOutcome(),
        )
        assertEquals(
            PlaylistTrackAddOutcome.Failed,
            mutate("add", """{"code":403}""").toPlaylistTrackAddOutcome(),
        )
    }

    private suspend fun mutate(op: String, response: String): Resource<ManipulateTrackResult> {
        var requests = 0
        val client = HttpClient(MockEngine { request ->
            requests++
            assertEquals("interface.music.163.com", request.url.host)
            assertEquals("/api/playlist/manipulate/tracks", request.url.encodedPath)
            assertEquals("POST", request.method.value)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(op, body.getValue("op").jsonPrimitive.content)
            assertEquals("123", body.getValue("pid").jsonPrimitive.content)
            assertEquals("""["456","789"]""", body.getValue("trackIds").jsonPrimitive.content)
            assertEquals("true", body.getValue("imme").jsonPrimitive.content)
            respond(response, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
            isLenient = true
        }
        val http = NeteaseHttp(client, json)
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = PlaylistRepository(
            ApiService(http, XeApiClient(RuntimeEnvironment.getApplication(), http, json)),
            WeApiService(http),
            EApiService(http),
            LocalPlaylistRepository(db.playlistDao(), db.playlistSongCrossRefDao()),
        )
        return try {
            repo.manipulateTrack(op, "123", "456,789").also {
                assertEquals("A mutation must not be retried on a decoding failure", 1, requests)
            }
        } finally {
            client.close()
            db.close()
        }
    }
}
