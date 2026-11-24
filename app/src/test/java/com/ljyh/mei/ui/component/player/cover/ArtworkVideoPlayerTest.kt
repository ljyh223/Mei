package com.ljyh.mei.ui.component.player.cover

import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class ArtworkVideoPlayerTest {
    @Test fun decorativeVideoDisablesAudioAndKeepsHlsSegmentCacheKeysDistinct() {
        val context = RuntimeEnvironment.getApplication()
        val factory = DefaultMediaSourceFactory(DataSource.Factory { ByteArrayDataSource(byteArrayOf(0)) })
        for (url in listOf("https://example.org/master.m3u8", "https://example.org/cover.mp4")) {
            val player = createArtworkVideoPlayer(context, url, "cover-key", factory)
            try {
                assertEquals(0f, player.volume, 0f)
                assertEquals(Player.REPEAT_MODE_ONE, player.repeatMode)
                assertTrue(C.TRACK_TYPE_AUDIO in player.trackSelectionParameters.disabledTrackTypes)
                assertTrue(C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes)
                assertFalse(player.playWhenReady)
                val media = player.currentMediaItem!!.localConfiguration!!
                if (url.endsWith("m3u8")) {
                    assertEquals(MimeTypes.APPLICATION_M3U8, media.mimeType)
                    assertNull(media.customCacheKey)
                } else assertEquals("cover-key", media.customCacheKey)
            } finally { player.release() }
        }
    }
}
