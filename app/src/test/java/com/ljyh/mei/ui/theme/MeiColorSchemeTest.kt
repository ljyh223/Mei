package com.ljyh.mei.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class MeiColorSchemeTest {
    private val accents = listOf(Color(0xFF0061A4), Color(0xFFD04B65), Color(0xFF7654C7), Color.Yellow, Color.Cyan)

    @Test
    fun readingTextRetainsContrastAcrossSurfacesAndAlbumAccents() {
        for (dark in listOf(false, true)) for (accent in accents) {
            val scheme = meiColorScheme(
                if (dark) darkColorScheme(primary = accent) else lightColorScheme(primary = accent), dark,
            )
            val surfaces = listOf(scheme.background, scheme.surfaceContainerLow,
                scheme.surfaceContainer, scheme.surfaceContainerHigh, scheme.surfaceContainerHighest)
            for (surface in surfaces) {
                assertContrast(scheme.onSurface, surface, 4.5f)
                assertContrast(scheme.onSurfaceVariant, surface, 4.5f)
            }
        }
    }

    @Test
    fun navigationSelectionKeepsReadableLabelsForBrightAndDarkAccents() {
        for (dark in listOf(false, true)) for (accent in accents) {
            val scheme = meiColorScheme(
                if (dark) darkColorScheme(primary = accent) else lightColorScheme(primary = accent), dark,
            )
            assertContrast(scheme.onSecondaryContainer, scheme.secondaryContainer, 4.5f)
        }
    }

    @Test
    fun categorySymbolsHaveContrastOnEveryCategoryColor() {
        MeiCategory.entries.forEach { assertContrast(Color.White, it.color, 3f) }
    }

    private fun assertContrast(ink: Color, surface: Color, minimum: Float) {
        val a = ink.luminance()
        val b = surface.luminance()
        val ratio = (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
        assertTrue("Contrast $ratio is below $minimum for $ink on $surface", ratio >= minimum)
    }
}
