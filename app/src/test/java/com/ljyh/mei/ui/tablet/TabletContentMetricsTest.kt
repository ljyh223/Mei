package com.ljyh.mei.ui.tablet

import org.junit.Assert.*
import org.junit.Test

class TabletContentMetricsTest {
    @Test fun twoPanesRequireSpaceForBothInformationAndTrackColumns() {
        for (width in listOf(320f, 600f, 800f, 839f)) assertFalse(tabletContentMetrics(width, 1f).twoPanes)
        for (width in listOf(840f, 1024f, 1280f, 1920f)) {
            val result = tabletContentMetrics(width, 1f)
            assertTrue(result.twoPanes)
            assertTrue(result.sideWidthDp in 336f..420f)
            assertTrue(width - result.sideWidthDp - 24f >= 480f)
        }
    }
    @Test fun largerTextCanReturnTheSameWindowToSinglePane() {
        assertTrue(tabletContentMetrics(1024f, 1f).twoPanes)
        assertFalse(tabletContentMetrics(1024f, 1.5f).twoPanes)
        assertTrue(tabletContentMetrics(1248f, 1.5f).twoPanes)
    }
    @Test fun invalidMeasurementsDoNotReachLayoutAsNonFiniteSizes() {
        for (width in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f)) {
            val result = tabletContentMetrics(width, 1f)
            assertFalse(result.twoPanes); assertTrue(result.sideWidthDp.isFinite())
        }
        assertTrue(tabletContentMetrics(840f, Float.NaN).twoPanes)
    }
    @Test fun artworkCellsGrowWithFontsInsteadOfOnlyAddingMoreColumns() {
        assertEquals(2, tabletArtworkColumns(480f, 1f, 28f))
        assertTrue(tabletArtworkColumns(900f, 1f, 28f) > tabletArtworkColumns(900f, 1.5f, 28f))
    }
    @Test fun largerLibraryArtworkStillLeavesRoomForTheContentPane() {
        for (scale in listOf(1f, 1.5f)) {
            for (width in listOf(840f, 1024f, 1248f, 1280f, 1920f)) {
                val result = tabletContentMetrics(width, scale, sideWidthFraction = 0.4f, maxSideWidthDp = 480f)
                if (result.twoPanes) {
                    assertTrue(result.sideWidthDp >= 336f * scale)
                    assertTrue(result.sideWidthDp <= 480f * scale)
                    assertTrue(width - result.sideWidthDp - 24f >= 480f * scale)
                }
            }
        }
        assertTrue(tabletContentMetrics(1280f, 1f, 0.4f, 480f).sideWidthDp > tabletContentMetrics(1280f, 1f).sideWidthDp)
    }

}
