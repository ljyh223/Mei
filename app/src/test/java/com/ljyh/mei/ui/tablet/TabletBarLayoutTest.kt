package com.ljyh.mei.ui.tablet

import com.ljyh.mei.ui.screen.Index
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabletBarLayoutTest {
    @Test
    fun everyDestinationKeepsAMinimumTouchTarget() {
        for (width in listOf(544f, 664f, 744f, 1024f, 1536f)) {
            for (scale in listOf(1f, 1.3f, 1.5f, 2f, 3f)) {
                val layout = tabletBarLayout(width, scale)
                val itemWidth = (layout.navigationWidth.value - 12f - (Index.entries.size - 1) * 4f) / Index.entries.size
                assertTrue("Navigation item is too narrow at $width / $scale", itemWidth >= 48f)
                assertTrue(layout.controlHeight.value - 12f >= 48f)
            }
        }
    }

    @Test
    fun navigationAndSearchLeaveRoomForPlaybackAtAllSupportedWidths() {
        // Widths exclude the shell's 28dp gutters. Search is 64dp; the two gaps are 12dp each.
        for (width in listOf(544f, 664f, 719f, 720f, 744f, 968f)) {
            for (scale in listOf(1f, 1.4f, 2f, 3f)) {
                val layout = tabletBarLayout(width, scale)
                val available = width - layout.navigationWidth.value - 64f - 24f
                assertTrue("Playback slot collapsed at $width / $scale", layout.playerWidth.value >= 220f)
                assertTrue("Playback overlaps search at $width / $scale", layout.playerWidth.value <= available)
            }
        }
    }

    @Test
    fun wideWindowsLeaveExtraSpaceOutsideThePlayer() {
        for (width in listOf(968f, 1200f, 1536f)) {
            val layout = tabletBarLayout(width, 1f)
            assertEquals(340f, layout.playerWidth.value, 0f)
            val gap = width - layout.navigationWidth.value - layout.playerWidth.value - 64f - 24f
            assertTrue("Extra window space must not stretch the playback bar", gap > 0f)
        }
    }

    @Test
    fun textLabelsOnlyAppearWhenTheyFitTheNavigationSlots() {
        for (width in listOf(544f, 744f, 1024f)) {
            for (scale in listOf(1f, 1.3f, 2f, Float.NaN)) {
                val layout = tabletBarLayout(width, scale)
                if (layout.showNavigationLabels) {
                    val effectiveScale = scale.takeIf { it.isFinite() } ?: 1f
                    val itemWidth = (layout.navigationWidth.value - 24f) / 4f
                    assertTrue(itemWidth >= 3f * 12f * effectiveScale)
                }
            }
        }
    }
}
