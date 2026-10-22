package com.ljyh.mei.data.network

import com.ljyh.mei.data.model.domain.DiscoverPlaylistQuery
import com.ljyh.mei.data.network.api.DiscoverApiService
import com.ljyh.mei.data.repository.DiscoverRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class DiscoverApiServiceTest {
    @Test
    fun verifiedPathsUseExplicitCryptoAndMapOnlyRequiredFields() = runBlocking {
        val paths = mutableSetOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            val path = request.url.encodedPath
            assertEquals(if (path.startsWith("/eapi/")) "eapi" else "weapi", request.headers["X-Netease-Crypto"])
            val body = when (path) {
                "/eapi/playlist/catalogue" -> """{"code":200,"categories":{"0":"语种","1":"风格"},"sub":[{"name":"电子","category":1},{"name":"华语","category":0},{"name":"无分组","category":8}],"unused":true}"""
                "/weapi/playlist/hottags" -> """{"code":200,"tags":[{"name":"电子","id":9},{"name":"电子"},{"name":""}]}"""
                "/weapi/playlist/list" -> {
                    val params = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                    assertEquals("24", params.getValue("offset").jsonPrimitive.content)
                    assertEquals("全部", params.getValue("cat").jsonPrimitive.content)
                    assertEquals("hot", params.getValue("order").jsonPrimitive.content)
                    """{"code":200,"more":true,"playlists":[{"id":9,"name":"歌单","coverImgUrl":"cover","playCount":25331042304},{"id":0,"name":"无效"}]}"""
                }
                "/weapi/discovery/newAlbum" -> """{"code":200,"albums":[{"id":4,"name":"专辑","picUrl":"cover","artists":[{"name":"A"},{"name":"B"}]}]}"""
                "/weapi/toplist/detail" -> """{"code":200,"list":[{"id":8,"name":"榜单","tracks":[{"first":"歌名","second":"歌手"}]},{"id":7,"name":"无摘要","tracks":null,"updateFrequency":null}]}"""
                else -> error("Unexpected path $path")
            }
            respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val repo = DiscoverRepository(DiscoverApiService(NeteaseHttp(client, Json { ignoreUnknownKeys = true })))
            assertEquals(listOf("语种", "风格"), (repo.categories() as Resource.Success).data.map { it.group })
            assertEquals(listOf("电子"), (repo.hotCategories() as Resource.Success).data)
            val page = (repo.playlists(DiscoverPlaylistQuery(), 24) as Resource.Success).data
            assertEquals(25_331_042_304L, page.items.single().playCount)
            assertEquals(26, page.nextOffset)
            assertTrue(page.hasMore)
            assertEquals("A / B", (repo.albums() as Resource.Success).data.single().artist)
            val charts = (repo.charts() as Resource.Success).data
            assertEquals(listOf("歌名 · 歌手"), charts.first().tracks)
            assertTrue(charts.last().tracks.isEmpty())
            assertEquals(5, paths.size)
        } finally { client.close() }
    }

    @Test
    fun serverErrorCodesAreNotMisrepresentedAsEmptySuccessfulLists() = runBlocking {
        val client = HttpClient(MockEngine {
            respond("""{"code":301,"message":"login"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val repo = DiscoverRepository(DiscoverApiService(NeteaseHttp(client, Json { ignoreUnknownKeys = true })))
            assertTrue(repo.albums() is Resource.Error)
            assertTrue(repo.charts() is Resource.Error)
            assertTrue(repo.playlists(DiscoverPlaylistQuery(), 0) is Resource.Error)
        } finally { client.close() }
    }
}
