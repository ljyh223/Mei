package com.ljyh.mei.ui.component.navigation

import com.ljyh.mei.constants.MiniPlayerBarHeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomChromeMetricsTest {
    @Test
    fun standardFontPreservesOriginalNavigationAndPlayerPositions() {
        assertEquals(64f, bottomNavigationContentHeight(1f).value, 0f)
        assertEquals(66f, compactPlayerVerticalDrop(1f).value, 0f)
    }

    @Test
    fun largerFontsKeepCompactPlayerAndSideButtonsOnTheSameCentreLine() {
        for (scale in listOf(1f, 1.3f, 1.5f, 2f, 3f)) {
            val bar = bottomNavigationContentHeight(scale).value
            val expandedPlayerCentre = bar + 20f + MiniPlayerBarHeight.value / 2f
            val compactPlayerCentre = expandedPlayerCentre - compactPlayerVerticalDrop(scale).value
            val navigationCentre = 10f + bar / 2f
            assertEquals(navigationCentre, compactPlayerCentre, 0.001f)
            assertTrue(bar >= 24f + 2f + 18f * scale + 12f)
        }
    }

    @Test
    fun smallOrInvalidFontScaleCannotShrinkTouchTargets() {
        for (scale in listOf(0.5f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertEquals(64f, bottomNavigationContentHeight(scale).value, 0f)
        }
    }
}
