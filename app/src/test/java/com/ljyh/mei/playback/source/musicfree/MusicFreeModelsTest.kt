package com.ljyh.mei.playback.source.musicfree

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class MusicFreeModelsTest {
    @Test fun pluginAndSongIdentityStaySeparateFromNetease() {
        fun track(plugin: String, id: String) = MusicFreeTrack(plugin, buildJsonObject {
            put("id", id); put("title", "Song"); put("duration", 123.4)
        })
        val one = track("one", "123")
        assertTrue(one.mediaId < 0)
        assertNotEquals(one.mediaId, track("two", "123").mediaId)
        assertNotEquals(one.mediaId, track("one", "124").mediaId)
        assertEquals(one.mediaId, track("one", "123").mediaId)
        assertEquals(0L, one.metadata().neteaseId)
        assertEquals(123_400L, one.metadata().duration)
    }

    @Test fun missingResponseIsNotAnEmptyPage() {
        assertThrows(IllegalStateException::class.java) { musicFreePage("test", JsonNull) }
        assertThrows(IllegalStateException::class.java) { musicFreePage("test", buildJsonObject {}) }
        assertTrue(musicFreePage("test", buildJsonObject { put("data", JsonArray(emptyList())) }).isEnd)
    }

    @Test fun qualityUsesMusicFreeNames() {
        assertEquals("low", musicFreeQuality("standard"))
        assertEquals("standard", musicFreeQuality("exhigh"))
        assertEquals("high", musicFreeQuality("lossless"))
        assertEquals("super", musicFreeQuality("hires"))
    }
    @Test fun playbackLabelsDoNotConfuseMusicFreeStandardWithCatalogueStandard() {
        assertEquals("standard", musicFreePlaybackQuality("low"))
        assertEquals("exhigh", musicFreePlaybackQuality("standard"))
        assertEquals("lossless", musicFreePlaybackQuality("high"))
        assertEquals("hires", musicFreePlaybackQuality("super"))
        assertNull(musicFreePlaybackQuality("unknown"))
    }

}
