package com.ljyh.mei.download

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadCoverCacheTest {
    @Test
    fun reusesAlbumCoverForLaterSongs() = runBlocking {
        var fetches = 0
        val cache = DownloadCoverCache(fetch = { fetches++; byteArrayOf(1, 2) })
        assertArrayEquals(byteArrayOf(1, 2), cache.get("album-cover"))
        assertArrayEquals(byteArrayOf(1, 2), cache.get("album-cover"))
        assertEquals(1, fetches)
    }

    @Test
    fun evictsOldCoversAndRetriesFailedRequests() = runBlocking {
        var fetches = 0
        val cache = DownloadCoverCache(
            fetch = { url ->
                fetches++
                if (url == "missing" && fetches == 1) null else byteArrayOf(1, 2)
            },
            maxEntries = 1,
            maxBytes = 2,
        )
        assertNull(cache.get("missing"))
        assertArrayEquals(byteArrayOf(1, 2), cache.get("missing"))
        cache.get("other")
        cache.get("missing")
        assertEquals(4, fetches)
    }
}
