package com.ljyh.mei.data.network

import androidx.room.Room
import com.ljyh.mei.data.model.domain.PlaylistCreator
import com.ljyh.mei.data.model.domain.UserPlaylistItem
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.repository.LocalPlaylistRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class PlaylistMetadataSyncTest {
    @Test fun addingUpdatesTheObservedRoomCardWithoutEnteringThePlaylist() = runBlocking {
        fixture { f ->
            coroutineScope {
                val updated = async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeout(5_000) { f.local.getAllPlaylist().first { it.single().cover == "server-custom-cover" } }
                }
                val result = f.repo.manipulateTrack("add", "123", "456")
                assertEquals(200, (result as Resource.Success).data.code)
                val row = updated.await().single()
                assertEquals(5, row.count)
                assertEquals("server-custom-cover", row.cover)
                assertEquals("original title", row.title)
                assertEquals("description", row.description)
                assertEquals(7, row.localPlayCount)
                assertEquals(800L, row.lastPlayTime)
                assertEquals(1, f.mutationRequests)
                assertEquals(1, f.summaryRequests)
                assertEquals(0, f.detailRequests)
            }
        }
    }

    @Test fun deletionRefreshesTheCardAsWellAsTheTrackList() = runBlocking {
        fixture { f ->
            f.count = 3
            assertEquals(200, (f.repo.manipulateTrack("del", "123", "456") as Resource.Success).data.code)
            assertEquals(3, f.local.getPlaylist("123")!!.count)
            assertEquals("server-custom-cover", f.local.getPlaylist("123")!!.cover)
        }
    }

    @Test fun aRejectedMutationDoesNotChangeRoomOrFetchAHeader() = runBlocking {
        fixture { f ->
            f.mutationCode = 502
            assertEquals(502, (f.repo.manipulateTrack("add", "123", "456") as Resource.Success).data.code)
            assertEquals(f.initial, f.local.getPlaylist("123"))
            assertEquals(0, f.summaryRequests)
        }
    }

    @Test fun failedReadbackKeepsTheConfirmedCountAndDoesNotReportTheWriteAsFailed() = runBlocking {
        fixture { f ->
            f.failHeader = true
            val result = f.repo.manipulateTrack("add", "123", "456")
            assertEquals(200, (result as Resource.Success).data.code)
            assertEquals(5, f.local.getPlaylist("123")!!.count)
            assertEquals("old-cover", f.local.getPlaylist("123")!!.cover)
            assertEquals(1, f.mutationRequests)
            // A later explicit detail load also repairs the same Room card.
            f.failHeader = false
            assertTrue(f.repo.getPlaylistDetail("123") is Resource.Success)
            assertEquals("server-custom-cover", f.local.getPlaylist("123")!!.cover)
        }
    }

    @Test fun codeOnlyMutationUsesTheSummaryInsteadOfGuessingAnIncrement() = runBlocking {
        fixture { f ->
            f.includeMutationCount = false
            assertTrue(f.repo.manipulateTrack("add", "123", "456") is Resource.Success)
            assertEquals(5, f.local.getPlaylist("123")!!.count)
        }
    }

    @Test fun anOlderLibraryResponseCannotUndoTheUpdatedCoverOrCount() = runBlocking {
        fixture { f ->
            val beforeEdit = f.local.playlistSyncRevision()
            f.repo.manipulateTrack("add", "123", "456")
            f.local.touchPlaylist("123", 900L)
            f.local.mergeUserPlaylists(listOf(remote(4, "old-cover")), beforeEdit)
            val row = f.local.getPlaylist("123")!!
            assertEquals(5, row.count)
            assertEquals("server-custom-cover", row.cover)
            assertEquals(8, row.localPlayCount)
            assertEquals(900L, row.lastPlayTime)
            // A new request may reflect a later edit made on another device.
            val afterEdit = f.local.playlistSyncRevision()
            f.local.mergeUserPlaylists(listOf(remote(6, "later-cover")), afterEdit)
            assertEquals(6, f.local.getPlaylist("123")!!.count)
            assertEquals("later-cover", f.local.getPlaylist("123")!!.cover)
        }
    }

    @Test fun aSlowHeaderReadCannotOverwriteANewerConfirmedMutation() = runBlocking {
        fixture { f ->
            coroutineScope {
                f.headerBarrier = CompletableDeferred()
                val first = async { f.repo.manipulateTrack("add", "123", "456") }
                f.headerStarted.await()
                val revision = f.local.recordTrackMutation("123", 6)
                f.local.updatePlaylistSummary("123", 6, "newer-cover", revision)
                f.headerBarrier!!.complete(Unit)
                assertTrue(first.await() is Resource.Success)
                assertEquals(6, f.local.getPlaylist("123")!!.count)
                assertEquals("newer-cover", f.local.getPlaylist("123")!!.cover)
            }
        }
    }

    @Test fun aStaleOrWrongHeaderCannotReplaceTheConfirmedCount() = runBlocking {
        fixture { f ->
            f.headerCount = 4
            f.repo.manipulateTrack("add", "123", "456")
            assertEquals(5, f.local.getPlaylist("123")!!.count)
            assertEquals("old-cover", f.local.getPlaylist("123")!!.cover)
            f.headerCount = 5
            f.headerId = 999
            f.repo.manipulateTrack("add", "123", "789")
            assertEquals("old-cover", f.local.getPlaylist("123")!!.cover)
        }
    }

    private suspend fun fixture(block: suspend (Fixture) -> Unit) {
        val f = Fixture()
        try {
            f.local.insertPlaylist(f.initial)
            withTimeout(10_000) { block(f) }
        } finally {
            f.client.close()
            f.db.close()
        }
    }

    private class Fixture {
        val initial = Playlist("123", "original title", "old-cover", "9", "owner", "",
            4, description = "description", lastPlayTime = 800L, localPlayCount = 7)
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val local = LocalPlaylistRepository(db.playlistDao())
        var count = 5
        var mutationCode = 200
        var includeMutationCount = true
        var failHeader = false
        var headerCount: Int? = null
        var headerId = 123L
        var headerBarrier: CompletableDeferred<Unit>? = null
        val headerStarted = CompletableDeferred<Unit>()
        var mutationRequests = 0
        var summaryRequests = 0
        var detailRequests = 0
        val client = HttpClient(MockEngine { request ->
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            val response = when (request.url.encodedPath) {
                "/api/playlist/manipulate/tracks" -> {
                    mutationRequests++
                    assertEquals("123", body.getValue("pid").jsonPrimitive.content)
                    if (includeMutationCount) """{"code":$mutationCode,"count":$count}"""
                    else """{"code":$mutationCode}"""
                }
                "/api/v6/playlist/detail" -> {
                    assertEquals("123", body.getValue("id").jsonPrimitive.content)
                    if (body.getValue("n").jsonPrimitive.content == "0") {
                        summaryRequests++
                        assertEquals("0", body.getValue("s").jsonPrimitive.content)
                        headerStarted.complete(Unit)
                        val snapshot = """{"code":200,"playlist":{"id":$headerId,"coverImgUrl":"server-custom-cover","trackCount":${headerCount ?: count}}}"""
                        headerBarrier?.await()
                        if (failHeader) throw IOException("readback unavailable")
                        snapshot
                    } else {
                        detailRequests++
                        """{"code":200,"playlist":{"id":123,"name":"original title","coverImgUrl":"server-custom-cover",
                        "trackCount":$count,"creator":{"userId":9,"nickname":"owner"},"playCount":0,
                        "subscribed":false,"subscribedCount":0,"trackIds":[],"tracks":[]}}"""
                    }
                }
                else -> error("Unexpected request ${request.url.encodedPath}")
            }
            respond(response, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
        private val http = NeteaseHttp(client, json)
        val repo = PlaylistRepository(
            ApiService(http, XeApiClient(RuntimeEnvironment.getApplication(), http, json)),
            WeApiService(http), EApiService(http), local,
        )
    }

    private fun remote(count: Int, cover: String) = UserPlaylistItem(
        123, "remote title", cover, PlaylistCreator(9, "owner", ""), 10, count,
    )
}
