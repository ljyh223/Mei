package com.ljyh.mei.ui.component.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class GlassAppearance(
    val opacity: Float = 0.60f,
    val blur: Float = 20f,
    val refraction: Float = 0.25f,
    val highlight: Float = 0.20f,
    val saturation: Float = 0.65f,
    val strongContent: Boolean = true,
) {
    // Persisted values may come from backups or older builds; never pass invalid values to Haze.
    fun normalized(): GlassAppearance = copy(
        opacity = opacity.finiteOr(0.60f).coerceIn(0f, 1f),
        blur = blur.finiteOr(20f).coerceIn(0f, 40f),
        refraction = refraction.finiteOr(0.25f).coerceIn(0f, 1f),
        highlight = highlight.finiteOr(0.20f).coerceIn(0f, 1f),
        saturation = saturation.finiteOr(0.65f).coerceIn(0f, 1f),
    )

    fun opacityFor(role: MeiGlassRole): Float {
        val value = normalized()
        if (role == MeiGlassRole.PlayerControl) return value.opacity * 0.25f
        // Contrast mode still allows every opacity adjustment to have a visible effect.
        val base = when (role) {
            MeiGlassRole.MiniPlayer -> value.opacity + (1f - value.opacity) * 0.10f
            MeiGlassRole.Group -> value.opacity + (1f - value.opacity) * 0.25f
            else -> value.opacity
        }
        return if (value.strongContent) base + (1f - base) * 0.35f else base
    }

    companion object {
        val Clear = GlassAppearance(opacity = 0.25f, blur = 12f, refraction = 0.45f,
            highlight = 0.30f, saturation = 0.85f, strongContent = false)
        val Balanced = GlassAppearance()
        val Readable = GlassAppearance(opacity = 0.85f, blur = 28f, refraction = 0.10f,
            highlight = 0.10f, saturation = 0.35f)
    }
}

private fun Float.finiteOr(fallback: Float) = if (isFinite()) this else fallback

val LocalMeiGlassAppearance = staticCompositionLocalOf { GlassAppearance.Balanced }
