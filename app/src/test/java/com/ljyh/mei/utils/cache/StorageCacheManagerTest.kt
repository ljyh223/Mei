package com.ljyh.mei.utils.cache

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(UnstableApi::class)
class StorageCacheManagerTest {
    @Test
    fun playbackKeyIsMusicButDynamicCoverIsOther() {
        assertTrue(StorageCacheManager.isMusicKey("12345|lossless"))
        assertTrue(StorageCacheManager.isMusicKey("12345|jymaster"))
        assertFalse(StorageCacheManager.isMusicKey("dynamic:apple:album:portrait"))
        assertFalse(StorageCacheManager.isMusicKey("https://example.com/artwork.m3u8"))
    }

    @Test
    fun totalIsSumOfVisibleCategories() {
        assertEquals(15L, CacheUsage(2, 5, 8).totalBytes)
    }
}
