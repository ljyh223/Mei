package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.network.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ArtistAlbumPreviewTest {
    @Test fun scansPastSinglesPublishesEarlyAndUsesRawPageOffsets() = runBlocking {
        val offsets = mutableListOf<Int>()
        val published = mutableListOf<ArtistAlbum>()
        val result = loadArtistAlbumPreview(loadPage = { offset, limit ->
            offsets += offset
            assertEquals(50, limit)
            Resource.Success(if (offset == 0) ArtistAlbum(
                (1L..15).map { ArtistAlbum.HotAlbum(id = it, type = "EP/Single") }, true, 200,
            ) else ArtistAlbum(
                (15L..27).map { ArtistAlbum.HotAlbum(id = it, type = "专辑") }, true, 200,
            ))
        }, onPage = { published += it }) as Resource.Success
        assertEquals(listOf(0, 15), offsets)
        assertEquals(15, published.first().hotAlbums.size)
        assertEquals(27, result.data.hotAlbums.size)
        assertEquals(15, result.data.hotAlbums.count { it.type == "EP/Single" })
    }

    @Test fun laterFailureKeepsEarlierReleasesAndTheFullCatalogRemainsAvailable() = runBlocking {
        val result = loadArtistAlbumPreview(loadPage = { offset, _ ->
            if (offset == 0) Resource.Success(ArtistAlbum(listOf(ArtistAlbum.HotAlbum(id = 1)), true, 200))
            else Resource.Error("offline")
        }, onPage = {}) as Resource.Success
        assertEquals(1L, result.data.hotAlbums.single().id)
        assertTrue(result.data.more)
        val firstFailure = loadArtistAlbumPreview({ _, _ -> Resource.Error("offline") }, {})
        assertEquals(Resource.Error("offline"), firstFailure)
    }

    @Test fun singleTypeCatalogIsBoundedAndCancellationPropagates() = runBlocking {
        var calls = 0
        loadArtistAlbumPreview({ offset, _ ->
            calls++
            Resource.Success(ArtistAlbum(listOf(ArtistAlbum.HotAlbum(id = offset + 1L, type = "Single")), true, 200))
        }, {})
        assertEquals(4, calls)
        try {
            loadArtistAlbumPreview({ _, _ -> throw CancellationException("stop") }, {})
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
