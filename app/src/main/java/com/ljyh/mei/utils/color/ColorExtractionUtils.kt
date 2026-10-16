package com.ljyh.mei.utils.color

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

/** Keep monochrome artwork monochrome instead of replacing it with an unrelated accent. */
fun extractCoverColor(bitmap: Bitmap): Color? = Palette.from(bitmap)
    .clearFilters()
    .maximumColorCount(32)
    .generate()
    .dominantSwatch
    ?.rgb
    ?.let(::Color)

// --- Extensions identical to AppleMusicFluidBackground's logic for standalone use ---
fun Color.isGrayscale(threshold: Float = 0.15f): Boolean {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    return hsl[1] < threshold 
}

fun Color.boostSaturation(multiplier: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    if (hsl[2] < 0.2f) hsl[2] = 0.2f
    hsl[1] = (hsl[1] * multiplier).coerceIn(0.2f, 1f) 
    return Color(ColorUtils.HSLToColor(hsl))
}

fun Color.darken(factor: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[2] = (hsl[2] * (1f - factor)).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

fun Color.lighten(factor: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[2] = (hsl[2] + (1f - hsl[2]) * factor).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

fun Color.shiftHue(amount: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[0] = (hsl[0] + amount).mod(360f)
    return Color(ColorUtils.HSLToColor(hsl))
}
