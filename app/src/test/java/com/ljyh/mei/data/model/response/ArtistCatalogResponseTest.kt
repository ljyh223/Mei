package com.ljyh.mei.data.model.response

import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.api.ArtistSongsPage
import com.ljyh.mei.data.model.api.GetArtistSongsPage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistCatalogResponseTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun artistSongsPageContainsDisplayableSongsAndPaginationFields() {
        val response = json.decodeFromString<ArtistSongsPage>(
            """{"songs":[{"id":298317,"name":"屋顶","dt":319039,"ar":[{"id":6452,"name":"周杰伦"}],"al":{"id":29597,"name":"有点野","pic":109951170514287417,"picUrl":"https://p2.music.126.net/cover.jpg"}}],"more":true,"total":579,"code":200}"""
        )

        assertEquals(200, response.code)
        assertTrue(response.more)
        assertEquals(298317L, response.songs.single().id)
        assertEquals("https://p2.music.126.net/cover.jpg", response.songs.single().album.picUrl)

        val request = json.encodeToString(GetArtistSongsPage(id = "6452", offset = 50))
        assertTrue(request.contains("\"private_cloud\":\"true\""))
        assertTrue(request.contains("\"offset\":50"))
    }

    @Test
    fun artistAlbumPageContainsAlbumsAndMoreFlag() {
        val response = json.decodeFromString<ArtistAlbum>(
            """{"code":200,"hotAlbums":[{"id":403900711,"name":"太阳之子","picUrl":"https://p1.music.126.net/cover.jpg","size":13,"artists":[{"id":6452,"name":"周杰伦"}]}],"more":true}"""
        )

        assertEquals(200, response.code)
        assertTrue(response.more)
        assertEquals(403900711L, response.hotAlbums.single().id)
        assertEquals("周杰伦", response.hotAlbums.single().artists.single().name)
    }
}
