package com.ljyh.mei.ui.screen.playlist

import com.ljyh.mei.ui.screen.playlist.layout.PlaylistLayout
import com.ljyh.mei.ui.screen.playlist.layout.playlistLayoutMetrics
import org.junit.Assert.*
import org.junit.Test

class PlaylistLayoutTest {
    @Test fun narrowWindowsUseOnePaneRegardlessOfDeviceOrOrientation() {
        for (width in listOf(320f, 540f, 600f, 800f, 839f, Float.NaN))
            assertEquals(PlaylistLayout.SinglePane, playlistLayoutMetrics(width).layout)
    }
    @Test fun wideWindowsReserveTheMinimumWidthOfBothPanes() {
        for (width in listOf(840f, 880f, 1024f, 1366f, 2048f)) {
            val metrics = playlistLayoutMetrics(width)
            assertEquals(PlaylistLayout.TwoPane, metrics.layout)
            assertTrue(metrics.detailWidthDp >= 336f)
            assertTrue(metrics.detailWidthDp <= 416f)
            assertTrue(width - 24f - metrics.detailWidthDp >= 480f)
        }
    }
    @Test fun largerTextRequiresMoreRoomBeforeEnablingTwoPanes() {
        assertEquals(PlaylistLayout.SinglePane, playlistLayoutMetrics(1024f, 1.5f).layout)
        for (scale in listOf(1f, 1.3f, 2f, 3f)) {
            val width = 80f + 760f * scale
            val metrics = playlistLayoutMetrics(width, scale)
            assertEquals(PlaylistLayout.TwoPane, metrics.layout)
            assertTrue(metrics.detailWidthDp >= 280f * scale + 56f - 0.01f)
            assertTrue(width - 24f - metrics.detailWidthDp >= 480f * scale - 0.01f)
        }
    }
}
