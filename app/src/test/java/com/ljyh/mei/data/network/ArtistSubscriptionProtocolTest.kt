package com.ljyh.mei.data.network

import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.repository.ArtistRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class ArtistSubscriptionProtocolTest {
    @Test fun subscribeAndUnsubscribeUseTheirOwnWeapiPathAndArtistFields() = runBlocking {
        val paths = mutableListOf<String>()
        fixture { path, body ->
            paths += path
            assertEquals("7", body.getValue("artistId").jsonPrimitive.content)
            assertEquals("[7]", body.getValue("artistIds").jsonPrimitive.content)
            """{"code":200}"""
        }.use { f ->
            assertTrue(f.repo.setSubscribed("7", true) is Resource.Success)
            assertTrue(f.repo.setSubscribed("7", false) is Resource.Success)
        }
        assertEquals(listOf("/weapi/artist/sub", "/weapi/artist/unsub"), paths)
    }

    @Test fun scansBeyondFirstPageUsingTheActualReturnedCount() = runBlocking {
        val offsets = mutableListOf<Int>()
        fixture { path, body ->
            assertEquals("/weapi/artist/sublist", path)
            assertEquals("100", body.getValue("limit").jsonPrimitive.content)
            assertEquals("true", body.getValue("total").jsonPrimitive.content)
            val offset = body.getValue("offset").jsonPrimitive.int
            offsets += offset
            if (offset == 0) """{"code":200,"data":[{"id":1},{"id":2}],"hasMore":true}"""
            else """{"code":200,"data":[{"id":7}],"hasMore":false}"""
        }.use { assertEquals(Resource.Success(true), it.repo.isSubscribed("7")) }
        assertEquals(listOf(0, 2), offsets)
    }

    @Test fun finalPageAndEmptyListResolveToNotSubscribed() = runBlocking {
        for (response in listOf(
            """{"code":200,"data":[],"hasMore":false}""",
            """{"code":200,"data":[{"id":1}],"more":false}""",
            """{"code":200,"data":[{"id":1}],"count":1}""",
        )) fixture { _, _ -> response }.use { assertEquals(Resource.Success(false), it.repo.isSubscribed("7")) }
    }

    @Test fun invalidOrIncompleteResponsesDoNotPretendToBeAnUnselectedStar() = runBlocking {
        for (response in listOf(
            """{"code":301,"data":[],"hasMore":false}""",
            """{"code":200,"hasMore":false}""",
            """{"code":200,"data":[{}],"hasMore":false}""",
            """{"code":200,"data":[],"hasMore":true}""",
            """{"code":200,"data":[{"id":1}]}""",
        )) fixture { _, _ -> response }.use { assertTrue(it.repo.isSubscribed("7") is Resource.Error) }
    }

    @Test fun repeatedPagesAreRejectedRatherThanLoopingForever() = runBlocking {
        var calls = 0
        fixture { _, _ ->
            calls++
            """{"code":200,"data":[{"id":1}],"hasMore":true}"""
        }.use { assertTrue(it.repo.isSubscribed("7") is Resource.Error) }
        assertEquals(2, calls)
    }

    @Test fun rejectedMutationIsNotReportedAsSuccessAndInvalidIdsNeverReachTransport() = runBlocking {
        var calls = 0
        fixture { _, _ -> calls++; """{"code":301}""" }.use { f ->
            assertTrue(f.repo.setSubscribed("7", true) is Resource.Error)
            for (id in listOf("0", "-1", "invalid")) {
                assertTrue(f.repo.setSubscribed(id, true) is Resource.Error)
                assertTrue(f.repo.isSubscribed(id) is Resource.Error)
            }
        }
        assertEquals(1, calls)
    }

    private fun fixture(response: (String, JsonObject) -> String): Fixture = Fixture(response)

    private class Fixture(response: (String, JsonObject) -> String) : AutoCloseable {
        private val client = HttpClient(MockEngine { request ->
            assertEquals("music.163.com", request.url.host)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            respond(response(request.url.encodedPath, body))
        })
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        private val http = NeteaseHttp(client, json)
        val repo = ArtistRepository(ApiService(http, XeApiClient(RuntimeEnvironment.getApplication(), http, json)), WeApiService(http))
        override fun close() = client.close()
    }
}
