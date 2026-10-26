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
        assertEquals(GlassAppearance.Balanced.opacity, value.opacity, 0f)
        assertEquals(GlassAppearance.Balanced.blur, value.blur, 0f)
        assertEquals(0f, value.refraction, 0f)
        assertEquals(1f, value.highlight, 0f)
        assertEquals(GlassAppearance.Balanced.saturation, value.saturation, 0f)
    }

    @Test
    fun opacityAndBlurStayWithinSupportedBounds() {
        val value = GlassAppearance(opacity = 4f, blur = -3f, saturation = 9f).normalized()
        assertEquals(1f, value.opacity, 0f)
        assertEquals(0f, value.blur, 0f)
        assertEquals(1f, value.saturation, 0f)
    }

    @Test
    fun transparencyAndForegroundEmphasisAreIndependent() {
        for (live in listOf(false, true)) {
            val normal = GlassAppearance.Balanced
            assertEquals(normal.tintOpacity(live), normal.copy(strongContent = false).tintOpacity(live), 0f)
            if (live) assertTrue(normal.copy(reduceTransparency = true).tintOpacity(live) > normal.tintOpacity(live))
            assertEquals(1f, normal.copy(reduceTransparency = true).tintOpacity(live), 0f)
        }
    }

    @Test
    fun onlyLiveGlassUsesSavedOpacity() {
        val appearance = GlassAppearance()
        assertTrue(appearance.copy(opacity = 0.2f).tintOpacity(true) <
            appearance.copy(opacity = 0.8f).tintOpacity(true))
        assertEquals(0f, GlassAppearance.Clear.tintOpacity(liveGlass = true), 0f)
        for (opacity in listOf(0f, 0.2f, 0.8f, 1f, Float.NaN)) {
            assertEquals(1f, appearance.copy(opacity = opacity).tintOpacity(liveGlass = false), 0f)
        }
    }

    @Test
    fun solidControlsRemainReadableOverArtworkInBothThemes() {
        val backdrops = listOf(Color.White, Color.Black, Color.Red, Color.Cyan)
        for (dark in listOf(false, true)) for (accent in listOf(Color.Yellow, Color.Cyan, Color.Red, Color.Blue)) {
            val scheme = meiColorScheme(if (dark) darkColorScheme(primary = accent)
                else lightColorScheme(primary = accent), dark)
            for (appearance in listOf(GlassAppearance.Clear, GlassAppearance.Readable,
                GlassAppearance.Balanced.copy(strongContent = false))) {
                // Covers the shared control surface, the playback strip and selected navigation.
                val containers = listOf(scheme.surfaceContainer, scheme.secondaryContainer,
                    appearance.selectionColor(scheme, liveGlass = false))
                for (container in containers) for (backdrop in backdrops) {
                    val surface = container.copy(alpha = appearance.tintOpacity(false)).compositeOver(backdrop)
                    assertEquals("Artwork must not change the solid colour", container, surface)
                    for (secondary in listOf(false, true)) {
                        val ink = appearance.contentColor(scheme, secondary, liveGlass = false)
                        val a = ink.luminance()
                        val b = surface.luminance()
                        val ratio = (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
                        assertTrue("Solid text contrast $ratio below 4.5, dark=$dark", ratio >= 4.5f)
                    }
                }
            }
        }
    }

    @Test
    fun reducedTransparencyOverridesOpticsAndOpacityWithoutChangingTheMaterial() {
        val custom = GlassAppearance(opacity = 0.13f, blur = 17f, refraction = 0.42f,
            highlight = 0.71f, saturation = 0.56f, strongContent = false)
        val reduced = custom.copy(reduceTransparency = true)
        for (available in listOf(false, true)) for (opacity in listOf(0f, 0.2f, 0.8f, 1f)) {
            assertEquals(false, reduced.usesOptics(available))
            assertEquals(1f, reduced.copy(opacity = opacity).tintOpacity(available), 0f)
        }
        val restored = reduced.copy(reduceTransparency = false)
        assertEquals(custom, restored)
        assertEquals(true, restored.usesOptics(available = true))
        assertEquals(false, restored.usesOptics(available = false))
        assertEquals(custom.opacity, restored.tintOpacity(liveGlass = true), 0f)
    }

    @Test
    fun choosingMaterialPresetPreservesIndependentReadabilityOptions() {
        val custom = GlassAppearance(reduceTransparency = true, strongContent = false)
        assertEquals(GlassAppearance.Clear.copy(reduceTransparency = true, strongContent = false),
            custom.withMaterial(GlassAppearance.Clear))
    }

    @Test
    fun reducedTransparencyRetainsTextContrastOverExtremeBackdropColorsInBothModes() {
        val backgrounds = listOf(Color.White, Color.Black, Color.Red, Color.Cyan, Color.Yellow)
        for (dark in listOf(false, true)) for (accent in listOf(Color.Yellow, Color.Cyan, Color.Red)) {
            val scheme = meiColorScheme(if (dark) darkColorScheme(primary = accent)
                else lightColorScheme(primary = accent), dark)
            for (live in listOf(false, true)) for (background in backgrounds) {
                val surface = scheme.surfaceContainerLow.copy(alpha = GlassAppearance.Balanced.copy(reduceTransparency = true).tintOpacity(live))
                    .compositeOver(background)
                val readingSurfaces = listOf(surface,
                    scheme.surfaceContainerHighest.copy(alpha = 0.18f).compositeOver(surface))
                for (readingSurface in readingSurfaces) for (secondary in listOf(false, true)) {
                    val a = GlassAppearance.Balanced.contentColor(scheme, secondary).luminance()
                    val b = readingSurface.luminance()
                    val ratio = (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
                    assertTrue("Contrast $ratio below 4.5 for live=$live, dark=$dark", ratio >= 4.5f)
                }
            }
        }
    }

    @Test
    fun strongForegroundKeepsOpaquePrimaryAndSecondaryHierarchyWithoutChangingTheTheme() {
        for (dark in listOf(false, true)) {
            val scheme = meiColorScheme(if (dark) darkColorScheme() else lightColorScheme(), dark)
            val appearance = GlassAppearance.Balanced
            val strong = if (dark) Color.White else Color.Black
            assertEquals(strong, appearance.contentColor(scheme))
            val secondary = appearance.contentColor(scheme, secondary = true)
            assertEquals(1f, secondary.alpha, 0f)
            assertTrue(if (dark) secondary.luminance() < strong.luminance()
                else secondary.luminance() > strong.luminance())
            assertTrue(if (dark) secondary.luminance() > scheme.onSurfaceVariant.luminance()
                else secondary.luminance() < scheme.onSurfaceVariant.luminance())
            assertEquals(scheme.onSurface, appearance.copy(strongContent = false).contentColor(scheme))
            assertEquals(scheme.onSurfaceVariant,
                appearance.copy(strongContent = false).contentColor(scheme, secondary = true))
        }
    }

    @Test
    fun balancedGlassImprovesTintOnlyContrastOnTheOppositeThemeBackdrop() {
        for (dark in listOf(false, true)) {
            val scheme = meiColorScheme(if (dark) darkColorScheme() else lightColorScheme(), dark)
            val backdrop = if (dark) Color.White else Color.Black
            fun ratio(ink: Color, tintOpacity: Float): Float {
                val surface = scheme.surfaceContainerLow.copy(alpha = tintOpacity).compositeOver(backdrop)
                val a = ink.luminance()
                val b = surface.luminance()
                return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
            }
            // Optical grading is deliberately excluded: this checks just foreground + neutral tint.
            val before = ratio(scheme.onSurface, 0.08f)
            val after = ratio(GlassAppearance.Balanced.contentColor(scheme),
                GlassAppearance.Balanced.tintOpacity(liveGlass = true))
            assertTrue("Tint-only contrast should increase, dark=$dark", after > before)
        }
    }

    @Test
    fun navigationAccentRetainsContrastForExtremeThemeColors() {
        val accents = listOf(Color.Yellow, Color.Cyan, Color.Red, Color.Blue, Color.White,
            Color.Black, Color.Transparent)
        for (dark in listOf(false, true)) for (accent in accents) for (live in listOf(false, true)) {
            val scheme = meiColorScheme(if (dark) darkColorScheme(primary = accent)
                else lightColorScheme(primary = accent), dark)
            val ink = GlassAppearance.Balanced.accentColor(scheme, liveGlass = live)
            val surface = (if (live) scheme.surfaceContainerLow else scheme.surfaceContainer).copy(alpha = 1f)
            val backgrounds = listOf(surface,
                GlassAppearance.Balanced.selectionColor(scheme, liveGlass = live).compositeOver(surface))
            assertEquals(1f, ink.alpha, 0f)
            for (background in backgrounds) {
                val a = ink.luminance()
                val b = background.luminance()
                val ratio = (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
                assertTrue("Accent contrast $ratio below 4.5, dark=$dark, accent=$accent", ratio >= 4.5f)
            }
        }
    }

    @Test
    fun readableThemeAccentIsPreservedExactly() {
        val light = meiColorScheme(lightColorScheme(primary = Color(0xFF2458BE)), false)
        val dark = meiColorScheme(darkColorScheme(primary = Color(0xFFB5C8FF)), true)
        for (scheme in listOf(light, dark)) {
            assertEquals(scheme.primary, GlassAppearance.Balanced.accentColor(scheme))
        }
    }

}
