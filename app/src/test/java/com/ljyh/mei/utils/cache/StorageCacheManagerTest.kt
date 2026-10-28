package com.ljyh.mei.utils.cache

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.MusicQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(UnstableApi::class)
class StorageCacheManagerTest {
    @Test
    fun legacyPlaybackKeysRemainMusicForEveryQuality() {
        for (quality in MusicQuality.entries) {
            val key = "12345|${quality.text}"
            assertTrue(key, StorageCacheManager.isMusicKey(key))
        }
    }

    @Test
    fun fullTrackPlaybackKeysAreMusicForEveryQuality() {
        for (quality in MusicQuality.entries) {
            val key = "12345|${quality.text}|full-v1"
            assertTrue(key, StorageCacheManager.isMusicKey(key))
        }
    }

    @Test
    fun lxPlaybackKeysAreMusicRegardlessOfCandidateQuality() {
        for (candidateQuality in listOf("320k", "flac", "flac24bit")) {
            val key = "12345|lossless|full-v1|lx:test-script|wy|67890|$candidateQuality"
            assertTrue(key, StorageCacheManager.isMusicKey(key))
        }
    }

    @Test
    fun unblockPlaybackKeysAreMusic() {
        for (source in listOf("qq", "kuwo")) {
            val key = "12345|exhigh|full-v1|legacy|$source|67890"
            assertTrue(key, StorageCacheManager.isMusicKey(key))
        }
    }

    @Test
    fun nonPlaybackAndMalformedKeysRemainOther() {
        for (key in listOf(
            "dynamic:apple:album:portrait",
            "https://example.com/artwork.m3u8",
            "https://example.com/artwork.m3u8?quality=lossless",
            "dynamic:apple:album|full-v1|lossless",
            "12345|unknown|full-v1|lx:test-script|wy|67890|lossless",
            "12345|lossless|unrelated",
            "|lossless|full-v1",
            "12345",
            "",
        )) {
            assertFalse(key, StorageCacheManager.isMusicKey(key))
        }
    }

    @Test
    fun totalIsSumOfVisibleCategories() {
        assertEquals(15L, CacheUsage(2, 5, 8).totalBytes)
    }
}
