package com.ljyh.mei.ui.screen.artist

import androidx.paging.PagingSource
import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.domain.ArtistReleaseCategory
import com.ljyh.mei.data.network.NeteaseHttp
import com.ljyh.mei.data.network.XeApiClient
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
class ArtistAlbumsPagingSourceTest {
    @Test fun filteredOutPagesAreSkippedUsingTheUnfilteredServerCount() = runBlocking {
        val offsets = mutableListOf<Int>()
        val client = HttpClient(MockEngine { request ->
            assertEquals("/api/artist/albums/7", request.url.encodedPath)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            val offset = body.getValue("offset").jsonPrimitive.int
            offsets += offset
            respond(if (offset == 0)
                """{"code":200,"more":true,"hotAlbums":[{"id":1,"type":"Single"},{"id":2,"type":"EP/Single"}]}"""
            else """{"code":200,"more":false,"hotAlbums":[{"id":3,"type":"专辑","size":1}]}""")
        })
        try {
            val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
            val http = NeteaseHttp(client, json)
            val repo = ArtistRepository(ApiService(http, XeApiClient(RuntimeEnvironment.getApplication(), http, json)), WeApiService(http))
            val source = ArtistAlbumsPagingSource(repo, "7", ArtistReleaseCategory.Albums)
            val first = source.load(PagingSource.LoadParams.Refresh(null, 30, false)) as PagingSource.LoadResult.Page<Int, ArtistAlbum.HotAlbum>
            assertEquals(3L, first.data.single().id)
            assertNull(first.nextKey)
            assertEquals(listOf(0, 2), offsets)
        } finally { client.close() }
    }
}
