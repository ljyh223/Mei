package com.ljyh.mei.data.network

import androidx.room.Room
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.PlaylistSongCrossRef
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.repository.LocalPlaylistRepository
import com.ljyh.mei.ui.screen.playlist.PlaylistTrackAddOutcome
import com.ljyh.mei.ui.screen.playlist.toPlaylistTrackAddOutcome
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.io.IOException
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class PlaylistMembershipTest {
    @Test fun staleLocalPresenceStillSendsTheAddAndAnOldEqualCountDoesNotMeanDuplicate() = runBlocking {
        fixture { f ->
            f.local.insertPlaylist(f.initial.copy(count = 3))
            f.seed(7)
            assertTrue(f.contains(7))
            f.remoteTracks = listOf(8, 9)
            val result = f.repo.manipulateTrack("add", "123", "7")
            assertEquals(PlaylistTrackAddOutcome.Added, result.toPlaylistTrackAddOutcome())
            assertEquals(1, f.mutations)
            assertTrue(7L in f.remoteTracks)
            assertTrue(f.contains(7))
            assertEquals(3, f.local.getPlaylist("123")!!.count)
        }
    }

    @Test fun explicitBackendDuplicateRepairsMissingLocalPresence() = runBlocking {
        fixture { f ->
            f.remoteTracks = listOf(7, 8)
            f.mutationCode = 502
            val result = f.repo.manipulateTrack("add", "123", "7")
            assertEquals(PlaylistTrackAddOutcome.AlreadyExists, result.toPlaylistTrackAddOutcome())
            assertTrue(f.contains(7))
            assertEquals(1, f.mutations)
        }
    }

    @Test fun successfulAddAndDeleteUpdatePresenceEvenWhenTheReadbackIsOffline() = runBlocking {
        fixture { f ->
            f.failReads = true
            assertEquals(PlaylistTrackAddOutcome.Added,
                f.repo.manipulateTrack("add", "123", "7").toPlaylistTrackAddOutcome())
            assertTrue(f.contains(7))
            assertEquals(200, (f.repo.manipulateTrack("del", "123", "7") as Resource.Success).data.code)
            assertFalse(f.contains(7))
        }
    }

    @Test fun unrelatedRejectionsCannotMarkASongAsAlreadyPresent() = runBlocking {
        fixture { f ->
            f.mutationCode = 403
            assertEquals(PlaylistTrackAddOutcome.Failed,
                f.repo.manipulateTrack("add", "123", "7").toPlaylistTrackAddOutcome())
            assertFalse(f.contains(7))
        }
    }

    @Test fun aFullRemoteSnapshotReplacesStalePositivesAndEmptyPlaylistsClearThem() = runBlocking {
        fixture { f ->
            f.seed(7)
            f.remoteTracks = listOf(8, 9)
            assertTrue(f.repo.refreshPlaylistMembership("123") is Resource.Success)
            assertFalse(f.contains(7))
            assertTrue(f.contains(8))
            f.remoteTracks = emptyList()
            ShadowSystemClock.advanceBy(Duration.ofSeconds(31))
            assertTrue(f.repo.refreshPlaylistMembership("123") is Resource.Success)
            assertFalse(f.contains(8))
        }
    }

    @Test fun incompleteOrFailedReadsCannotEraseKnownMembership() = runBlocking {
        fixture { f ->
            f.seed(7)
            f.remoteTracks = listOf(8)
            f.reportedCount = 10
            assertTrue(f.repo.refreshPlaylistMembership("123") is Resource.Success)
            assertTrue(f.contains(7))
            assertTrue(f.contains(8))
            ShadowSystemClock.advanceBy(Duration.ofSeconds(31))
            f.includeIds = false
            assertTrue(f.repo.refreshPlaylistMembership("123") is Resource.Error)
            assertTrue(f.contains(7))
            f.includeIds = true
            f.reportedCount = null
            f.failReads = true
            assertTrue(f.repo.refreshPlaylistMembership("123") is Resource.Error)
            assertTrue(f.contains(7))
            f.failReads = false
            f.readCode = 301
            assertTrue(f.repo.refreshPlaylistMembership("123") is Resource.Error)
            assertTrue(f.contains(7))
        }
    }

    @Test fun ordinaryPlaylistDetailsAlsoPopulateTheMembershipCache() = runBlocking {
        fixture { f ->
            f.remoteTracks = listOf(7, 8)
            assertTrue(f.repo.getPlaylistDetail("123") is Resource.Success)
            assertTrue(f.contains(7))
            assertTrue(f.contains(8))
        }
    }

    @Test fun aDetailReadBeforeTheLibraryWasLoadedStillCachesMembership() = runBlocking {
        fixture { f ->
            f.local.deletePlaylistById("123")
            f.remoteTracks = listOf(7, 8)
            assertTrue(f.repo.getPlaylistDetail("123") is Resource.Success)
            assertTrue(f.contains(7))
            assertNull(f.local.getPlaylist("123"))
        }
    }

    @Test fun aSlowReadFromBeforeDeletionCannotRestoreTheDeletedMembership() = runBlocking {
        fixture { f ->
            coroutineScope {
                f.seed(7)
                f.remoteTracks = listOf(7, 8)
                f.barrier = CompletableDeferred()
                val pending = async { f.repo.refreshPlaylistMembership("123") }
                f.started.await()
                f.local.recordTrackMutation("123", 1, removedTrackIds = listOf(7))
                f.barrier!!.complete(Unit)
                pending.await()
                assertFalse(f.contains(7))
            }
        }
    }

    @Test fun repeatRequestsShareARecentSnapshotAndParallelReadsAreBounded() = runBlocking {
        fixture { f ->
            coroutineScope {
                f.barrier = CompletableDeferred()
                val reads = (1..6).map { id -> async { f.repo.refreshPlaylistMembership(id.toString()) } }
                f.threeStarted.await()
                assertEquals(3, f.reads.get())
                assertEquals(3, f.maxActive.get())
                f.barrier!!.complete(Unit)
                reads.awaitAll().forEach { assertTrue(it is Resource.Success) }
                assertEquals(6, f.reads.get())
                assertTrue(f.maxActive.get() <= 3)
                f.repo.refreshPlaylistMembership("1")
                assertEquals(6, f.reads.get())
            }
        }
    }

    private suspend fun fixture(block: suspend (Fixture) -> Unit) {
        val f = Fixture()
        try {
            f.local.insertPlaylist(f.initial)
            withTimeout(15_000) { block(f) }
        } finally { f.client.close(); f.db.close() }
    }

    private class Fixture {
        val initial = Playlist("123", "歌单", "cover", "9", "owner", "", 2)
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val local = LocalPlaylistRepository(db.playlistDao(), db.playlistSongCrossRefDao())
        var remoteTracks = listOf(8L, 9L)
        var mutationCode = 200
        var reportedCount: Int? = null
        var includeIds = true
        var readCode = 200
        var failReads = false
        var mutations = 0
        val reads = AtomicInteger()
        private val active = AtomicInteger()
        val maxActive = AtomicInteger()
        val started = CompletableDeferred<Unit>()
        val threeStarted = CompletableDeferred<Unit>()
        var barrier: CompletableDeferred<Unit>? = null
        val client = HttpClient(MockEngine { request ->
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            val response = when (request.url.encodedPath) {
                "/api/playlist/manipulate/tracks" -> {
                    mutations++
                    val ids = Json.parseToJsonElement(body.getValue("trackIds").jsonPrimitive.content).jsonArray
                        .map { it.jsonPrimitive.long }
                    if (mutationCode == 200) {
                        remoteTracks = if (body.getValue("op").jsonPrimitive.content == "add")
                            (remoteTracks + ids).distinct() else remoteTracks.filterNot { it in ids }
                    }
                    """{"code":$mutationCode,"message":"${if (mutationCode == 502) "歌单内歌曲重复" else ""}","count":${remoteTracks.size}}"""
                }
                "/api/v6/playlist/detail" -> {
                    reads.incrementAndGet()
                    val current = active.incrementAndGet()
                    maxActive.accumulateAndGet(current, ::maxOf)
                    val id = body.getValue("id").jsonPrimitive.content
                    val ids = remoteTracks.joinToString(",") { """{"id":$it}""" }
                    val fields = if (includeIds) ""","trackIds":[$ids]""" else ""
                    val detail = if (body.getValue("n").jsonPrimitive.content == "0") {
                        assertEquals("0", body.getValue("s").jsonPrimitive.content)
                        ""
                    } else {
                        ""","name":"歌单","creator":{"userId":9,"nickname":"owner"},"playCount":0,"subscribed":false,"subscribedCount":0,"tracks":[]"""
                    }
                    val snapshot = """{"code":$readCode,"playlist":{"id":$id,"coverImgUrl":"cover","trackCount":${reportedCount ?: remoteTracks.size}$fields$detail}}"""
                    started.complete(Unit)
                    if (current == 3) threeStarted.complete(Unit)
                    try {
                        barrier?.await()
                        if (failReads) throw IOException("offline")
                        snapshot
                    } finally { active.decrementAndGet() }
                }
                else -> error("Unexpected path")
            }
            respond(response, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
        private val http = NeteaseHttp(client, json)
        val repo = PlaylistRepository(ApiService(http, XeApiClient(RuntimeEnvironment.getApplication(), http, json)),
            WeApiService(http), EApiService(http), local)
        suspend fun contains(id: Long) = "123" in local.playlistsContaining(id).first()
        suspend fun seed(id: Long) { db.playlistSongCrossRefDao().insert(PlaylistSongCrossRef("123", id.toString())) }
    }
}
