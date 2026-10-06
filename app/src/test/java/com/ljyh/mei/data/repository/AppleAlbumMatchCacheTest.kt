package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.repository.artwork.AlbumMatchEntry
import com.ljyh.mei.data.repository.artwork.AppleAlbumMatchCache
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleAlbumMatchCacheTest {
    @Test
    fun repeatedAndConcurrentSongsFromOneAlbumShareSearchAndPersistId() = runBlocking {
        val stored = mutableMapOf<String, AlbumMatchEntry>()
        val searches = AtomicInteger()
        fun cache() = AppleAlbumMatchCache(
            read = { stored.toMap() },
            write = { stored.clear(); stored.putAll(it) },
            search = { _, _, _ ->
                searches.incrementAndGet()
                delay(20)
                "album-123"
            },
            now = { 1_000L },
        )
        val first = cache()
        val results = (1..8).map {
            async(Dispatchers.Default) { first.get("The Album", "Artist", "https://example.com/cover.jpg") }
        }.awaitAll()
        assertTrue(results.all { it == "album-123" })
        assertEquals(1, searches.get())
        assertEquals("album-123", cache().get("The Album", "Artist", "https://example.com/cover.jpg"))
        assertEquals(1, searches.get())
    }

    @Test
    fun missingAlbumIsRetriedAfterNegativeCacheExpires() = runBlocking {
        var time = 1_000L
        var searches = 0
        val cache = AppleAlbumMatchCache(
            read = { emptyMap() },
            write = {},
            search = { _, _, _ -> searches++; null },
            now = { time },
        )
        assertEquals(null, cache.get("Missing", "Artist", "https://example.com/cover.jpg"))
        assertEquals(null, cache.get("Missing", "Artist", "https://example.com/cover.jpg"))
        assertEquals(1, searches)
        time += 7L * 60 * 60_000
        assertEquals(null, cache.get("Missing", "Artist", "https://example.com/cover.jpg"))
        assertEquals(2, searches)
    }

    @Test
    fun differentAlbumsCanSearchAtTheSameTime() = runBlocking {
        val started = AtomicInteger()
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        val cache = AppleAlbumMatchCache(
            read = { emptyMap() }, write = {},
            search = { album, _, _ -> started.incrementAndGet(); release.await(); album },
        )
        val first = async { cache.get("First", "Artist", "https://example.com/first.jpg") }
        val second = async { cache.get("Second", "Artist", "https://example.com/second.jpg") }
        kotlinx.coroutines.withTimeout(1_000) {
            while (started.get() < 2) delay(1)
        }
        release.complete(Unit)
        assertEquals(listOf("First", "Second"), awaitAll(first, second))
    }

    @Test
    fun failedImageFetchDoesNotCreateNegativeCacheEntry() = runBlocking {
        var searches = 0
        val cache = AppleAlbumMatchCache(
            read = { emptyMap() }, write = {},
            search = { _, _, _ -> searches++; if (searches == 1) throw java.io.IOException("offline"); "id" },
        )
        runCatching { cache.get("Album", "Artist", "https://example.com/cover.jpg") }
        assertEquals("id", cache.get("Album", "Artist", "https://example.com/cover.jpg"))
        assertEquals(2, searches)
    }

    @Test
    fun cancelledLookupCanBeRetried() = runBlocking {
        val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        var searches = 0
        val cache = AppleAlbumMatchCache(
            read = { emptyMap() }, write = {},
            search = { _, _, _ ->
                searches++
                if (searches == 1) {
                    started.complete(Unit)
                    kotlinx.coroutines.awaitCancellation()
                }
                "id"
            },
        )
        val first = async { cache.get("Album", "Artist", "https://example.com/cover.jpg") }
        started.await()
        first.cancel()
        first.join()
        assertEquals("id", cache.get("Album", "Artist", "https://example.com/cover.jpg"))
        assertEquals(2, searches)
    }

    @Test
    fun englishTitleOptionSkipsOnlyAppleLookupForNonLatinTitles() {
        val english = song("Hello", "World")
        val chinese = song("你好", "World")
        val mixedAlbum = song("Hello", "世界 World")
        val numericAlbum = song("Shake It Off", "1989")
        assertTrue(shouldSearchAppleMusic(english, true))
        assertTrue(shouldSearchAppleMusic(numericAlbum, true))
        assertFalse(shouldSearchAppleMusic(chinese, true))
        assertFalse(shouldSearchAppleMusic(mixedAlbum, true))
        assertTrue(shouldSearchAppleMusic(chinese, false))
    }

    private fun song(title: String, album: String) = MediaMetadata(
        id = 1L,
        title = title,
        coverUrl = "https://example.com/cover.jpg",
        artists = listOf(MediaMetadata.Artist(1L, "Artist")),
        duration = 1000L,
        album = MediaMetadata.Album(1L, album),
    )
}
