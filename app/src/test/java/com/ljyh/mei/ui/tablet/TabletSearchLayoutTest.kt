package com.ljyh.mei.ui.tablet

import com.ljyh.mei.ui.tablet.search.tabletSearchLayout
import org.junit.Assert.*
import org.junit.Test

class TabletSearchLayoutTest {
    @Test
    fun narrowWindowsKeepOnePaneAndNeverProduceZeroGridColumns() {
        for (width in listOf(320f, 600f, 800f, 895f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            val layout = tabletSearchLayout(width, 1f, 28f)
            assertFalse(layout.twoPanes)
            assertTrue(layout.primaryWidthDp.isFinite())
            assertTrue(layout.artworkColumns >= 1)
            assertTrue(layout.artistColumns >= 1)
        }
    }

    @Test
    fun bothOverviewPanesReserveReadableContentAndTheirOwnGutters() {
        for (scale in listOf(1f, 1.3f, 1.5f, 2f)) {
            val minimum = 760f * scale + 112f + 24f
            for (width in listOf(minimum, minimum + 100f, minimum + 600f)) {
                val layout = tabletSearchLayout(width, scale, 28f)
                assertTrue(layout.twoPanes)
                assertTrue(layout.primaryWidthDp - 56f >= 480f * scale - 0.01f)
                assertTrue(width - 24f - layout.primaryWidthDp - 56f >= 280f * scale - 0.01f)
            }
        }
    }

    @Test
    fun largerFontsReduceDensityAndTurnOffSongColumnsBeforeTheyBecomeCramped() {
        val normal = tabletSearchLayout(1024f, 1f, 28f)
        val large = tabletSearchLayout(1024f, 1.5f, 28f)
        assertTrue(normal.twoPanes)
        assertFalse(large.twoPanes)
        assertTrue(normal.songColumns)
        assertFalse(large.songColumns)
        assertTrue(large.artworkColumns < normal.artworkColumns)
        assertTrue(large.artistColumns < normal.artistColumns)
    }

    @Test
    fun coverColumnsKeepMinimumCellWidthAndRemainBoundedOnUltrawideWindows() {
        for (width in listOf(600f, 900f, 1280f, 2400f)) {
            for (scale in listOf(1f, 1.3f, 2f)) {
                val layout = tabletSearchLayout(width, scale, 28f)
                val cellWidth = (width - 56f - 24f * (layout.artworkColumns - 1)) / layout.artworkColumns
                if (layout.artworkColumns > 1) assertTrue(cellWidth >= 156f * scale - 0.01f)
                assertTrue(layout.artworkColumns <= 6)
                assertTrue(layout.artistColumns <= 5)
            }
        }
    }

    @Test
    fun albumGridsReserveExtraDiscWidthWithoutShrinkingTheirSquareSleeves() {
        for (width in listOf(600f, 900f, 1280f, 2400f)) for (scale in listOf(1f, 1.3f, 2f)) {
            val layout = tabletSearchLayout(width, scale, 28f)
            assertTrue(layout.albumColumns <= layout.artworkColumns)
            val cell = (width - 56f - 24f * (layout.albumColumns - 1)) / layout.albumColumns
            if (layout.albumColumns > 1) assertTrue(cell / 1.2f >= 156f * scale - .01f)
        }
    }

    @Test
    fun invalidFontScalesUseNormalTextMetrics() {
        val normal = tabletSearchLayout(1024f, 1f, 28f)
        for (scale in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f, 0.8f)) {
            assertEquals(normal, tabletSearchLayout(1024f, scale, 28f))
        }
    }
}
