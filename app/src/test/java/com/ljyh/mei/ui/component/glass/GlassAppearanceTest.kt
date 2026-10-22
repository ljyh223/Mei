package com.ljyh.mei.ui.component.glass

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.ljyh.mei.ui.theme.meiColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassAppearanceTest {
    @Test
    fun invalidRestoredValuesCannotReachTheRenderer() {
        val value = GlassAppearance(opacity = Float.NaN, blur = Float.POSITIVE_INFINITY,
            refraction = -2f, highlight = 8f, saturation = Float.NEGATIVE_INFINITY).normalized()
        assertEquals(0.60f, value.opacity, 0f)
        assertEquals(20f, value.blur, 0f)
        assertEquals(0f, value.refraction, 0f)
        assertEquals(1f, value.highlight, 0f)
        assertEquals(0.65f, value.saturation, 0f)
    }

    @Test
    fun opacityAndBlurStayWithinSupportedBounds() {
        val value = GlassAppearance(opacity = 4f, blur = -3f, saturation = 9f).normalized()
        assertEquals(1f, value.opacity, 0f)
        assertEquals(0f, value.blur, 0f)
        assertEquals(1f, value.saturation, 0f)
    }

    @Test
    fun contrastModePreservesOpacityControlAndOpaqueEndpoint() {
        val low = GlassAppearance(opacity = 0.2f).opacityFor(MeiGlassRole.Navigation)
        val high = GlassAppearance(opacity = 0.8f).opacityFor(MeiGlassRole.Navigation)
        assertTrue(low < high)
        assertEquals(1f, GlassAppearance(opacity = 1f).opacityFor(MeiGlassRole.Navigation), 0f)
        assertEquals(0f, GlassAppearance(opacity = 0f, strongContent = false)
            .opacityFor(MeiGlassRole.Navigation), 0f)
    }

    @Test
    fun defaultForegroundRetainsContrastOverExtremeBackdropColors() {
        val backgrounds = listOf(Color.White, Color.Black, Color.Red, Color.Cyan, Color.Yellow)
        val roles = listOf(MeiGlassRole.Navigation, MeiGlassRole.MiniPlayer, MeiGlassRole.Group)
        for (dark in listOf(false, true)) for (accent in listOf(Color.Yellow, Color.Cyan, Color.Red)) {
            val scheme = meiColorScheme(if (dark) darkColorScheme(primary = accent)
                else lightColorScheme(primary = accent), dark)
            for (role in roles) for (background in backgrounds) {
                val surface = scheme.surfaceContainerLow.copy(alpha = GlassAppearance.Balanced.opacityFor(role))
                    .compositeOver(background)
                val a = scheme.onSurface.luminance()
                val b = surface.luminance()
                val ratio = (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
                assertTrue("Contrast $ratio below 4.5 for $role, dark=$dark", ratio >= 4.5f)
            }
        }
    }
}
