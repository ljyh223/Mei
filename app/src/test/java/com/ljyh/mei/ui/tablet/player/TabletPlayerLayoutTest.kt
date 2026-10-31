package com.ljyh.mei.ui.tablet.player

import org.junit.Assert.*
import org.junit.Test

class TabletPlayerLayoutTest {
    @Test fun contentWidthAndFontSizeChooseTheLayoutRatherThanOrientation() {
        assertFalse(tabletPlayerLayout(700f, 1100f, 1f).twoPanes)
        assertFalse(tabletPlayerLayout(800f, 1100f, 1f).twoPanes)
        assertTrue(tabletPlayerLayout(840f, 1100f, 1f).twoPanes)
        assertTrue(tabletPlayerLayout(1200f, 400f, 1f).twoPanes)
        assertFalse(tabletPlayerLayout(1000f, 700f, 1.5f).twoPanes)
    }

    @Test fun bothPanesKeepReadableWidthsAndArtworkNeverExceedsItsOwnColumn() {
        for (scale in listOf(1f, 1.3f, 2f)) for (extra in listOf(0f, 200f, 900f)) {
            val width = 800f * scale + 40f + extra
            val result = tabletPlayerLayout(width, 800f, scale)
            assertTrue(result.twoPanes)
            assertTrue(result.nowPlayingWidthDp >= 400f * scale - .01f)
            assertEquals(result.nowPlayingWidthDp, width - result.nowPlayingWidthDp - result.paneGapDp, .01f)
            assertTrue(result.artworkSizeDp <= result.nowPlayingWidthDp)
        }
    }

    @Test fun shortWindowsKeepAnUsableCoverAndLetTheContentScroll() {
        val tall = tabletPlayerLayout(1100f, 900f, 1f)
        val short = tabletPlayerLayout(1100f, 280f, 1f)
        assertEquals(tall.nowPlayingWidthDp, short.nowPlayingWidthDp)
        assertEquals(280f, short.artworkSizeDp)
        assertTrue(short.artworkSizeDp < tall.artworkSizeDp)
        assertFalse(short.showCompactControls)
        assertTrue(tall.showCompactControls)
        assertFalse(tabletPlayerLayout(700f, 700f, 2f).showCompactControls)
    }

    @Test fun invalidOrTinyConstraintsNeverProduceNegativeOrNonFiniteSizes() {
        for (width in listOf(-1f, 0f, 120f, Float.NaN, Float.POSITIVE_INFINITY)) {
            val result = tabletPlayerLayout(width, Float.NaN, Float.NaN)
            assertFalse(result.twoPanes)
            assertTrue(result.nowPlayingWidthDp.isFinite() && result.nowPlayingWidthDp >= 0f)
            assertTrue(result.artworkSizeDp.isFinite() && result.artworkSizeDp >= 0f)
        }
        assertEquals(tabletPlayerLayout(900f, 700f, 1f), tabletPlayerLayout(900f, 700f, -1f))
    }
}
