package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class AppleAlbumMatchCacheTest {
    @Test
    fun repeatedAndConcurrentSongsFromOneAlbumShareSearchAndPersistId() = runBlocking {
        val stored = mutableMapOf<String, AlbumMatchEntry>()
        val searches = AtomicInteger()
        fun cache() = AppleAlbumMatchCache(
            read = { stored.toMap() },
            write = { stored.clear(); stored.putAll(it) },
            search = { _, _ ->
                searches.incrementAndGet()
                delay(20)
                "album-123"
            },
            now = { 1_000L },
        )
        val first = cache()
        val results = (1..8).map {
            async(Dispatchers.Default) { first.get("The Album", "Artist") }
        }.awaitAll()
        assertTrue(results.all { it == "album-123" })
        assertEquals(1, searches.get())
        assertEquals("album-123", cache().get("The Album", "Artist"))
        assertEquals(1, searches.get())
    }

    @Test
    fun missingAlbumIsRetriedAfterNegativeCacheExpires() = runBlocking {
        var time = 1_000L
        var searches = 0
        val cache = AppleAlbumMatchCache(
            read = { emptyMap() },
            write = {},
            search = { _, _ -> searches++; null },
            now = { time },
        )
        assertEquals(null, cache.get("Missing", "Artist"))
        assertEquals(null, cache.get("Missing", "Artist"))
        assertEquals(1, searches)
        time += 7L * 60 * 60_000
        assertEquals(null, cache.get("Missing", "Artist"))
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
