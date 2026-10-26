package com.ljyh.mei.ui.component.glass

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class GlassAppearance(
    val opacity: Float = 0.28f,
    val blur: Float = 2f,
    val refraction: Float = 0.85f,
    val highlight: Float = 0.55f,
    val saturation: Float = 1f,
    val strongContent: Boolean = true,
    val reduceTransparency: Boolean = false,
) {
    // Persisted values may come from backups or older builds; never pass invalid values to Haze.
    fun normalized(): GlassAppearance = copy(
        opacity = opacity.finiteOr(0.28f).coerceIn(0f, 1f),
        blur = blur.finiteOr(2f).coerceIn(0f, 40f),
        refraction = refraction.finiteOr(0.85f).coerceIn(0f, 1f),
        highlight = highlight.finiteOr(0.55f).coerceIn(0f, 1f),
        saturation = saturation.finiteOr(1f).coerceIn(0f, 1f),
    )

    /** One tint policy for every floating control, including the non-shader fallback. */
    fun tintOpacity(liveGlass: Boolean): Float {
        val value = normalized()
        return when {
            value.reduceTransparency -> 1f
            liveGlass -> value.opacity
            // Solid appearance does not depend on saved glass opacity.
            else -> 1f
        }
    }

    /** Keep the user's liquid-glass preference; reduced transparency temporarily uses solids. */
    fun usesOptics(available: Boolean): Boolean = available && !reduceTransparency

    /** Secondary copy stays opaque, so its hierarchy is independent of the sampled artwork. */
    fun contentColor(colors: ColorScheme, secondary: Boolean = false, liveGlass: Boolean = true): Color {
        if (!usesOptics(liveGlass)) return if (secondary) colors.onSurfaceVariant else colors.onSurface
        val primary = if (strongContent) {
            if (colors.surface.luminance() < 0.5f) Color.White else Color.Black
        } else colors.onSurface
        return when {
            !secondary -> primary
            strongContent -> lerp(primary, colors.surfaceContainerLow, 0.18f).copy(alpha = 1f)
            else -> colors.onSurfaceVariant
        }
    }

    fun selectionColor(colors: ColorScheme, liveGlass: Boolean = true): Color =
        if (usesOptics(liveGlass)) colors.surfaceContainerHighest.copy(alpha = 0.18f)
        else colors.secondaryContainer.copy(alpha = 1f)

    /** Retain the theme accent while correcting extremely pale/dark accents for navigation. */
    fun accentColor(colors: ColorScheme, liveGlass: Boolean = true): Color {
        val surface = (if (usesOptics(liveGlass)) colors.surfaceContainerLow
            else colors.surfaceContainer).copy(alpha = 1f)
        val backgrounds = listOf(surface, selectionColor(colors, liveGlass).compositeOver(surface))
        val accent = colors.primary.compositeOver(surface)
        fun readable(color: Color) = backgrounds.all { color.contrastAgainst(it) >= 4.5f }
        if (readable(accent)) return accent
        val ink = if (colors.surface.luminance() < 0.5f) Color.White else Color.Black
        var low = 0f
        var high = 1f
        repeat(10) {
            val fraction = (low + high) / 2f
            if (readable(lerp(accent, ink, fraction))) high = fraction else low = fraction
        }
        return lerp(accent, ink, high)
    }

    /** Presets change the material without changing the independent readability choices. */
    fun withMaterial(preset: GlassAppearance): GlassAppearance = preset.copy(
        strongContent = strongContent,
        reduceTransparency = reduceTransparency,
    )

    companion object {
        val Clear = GlassAppearance(opacity = 0f)
        val Balanced = GlassAppearance()
        val Readable = GlassAppearance(opacity = 0.45f, blur = 14f, refraction = 0.60f,
            highlight = 0.35f, saturation = 0.75f)
    }
}

private fun Color.contrastAgainst(other: Color): Float {
    val a = luminance()
    val b = other.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

private fun Float.finiteOr(fallback: Float) = if (isFinite()) this else fallback

val LocalMeiGlassAppearance = staticCompositionLocalOf { GlassAppearance.Balanced }
