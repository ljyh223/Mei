package com.ljyh.mei.utils.image

import kotlin.math.min
import kotlin.math.roundToInt

internal data class CoverEdgeTexturePixels(
    val extension: IntArray,
    val feather: IntArray,
    val controlsColor: Int,
)

/** The sampled background color is the actual extension fill, not just a classification hint. */
internal fun buildCoverEdgeTexture(
    pixels: IntArray,
    width: Int,
    height: Int,
    backgroundColor: Int,
    mode: CoverExtensionMode,
): CoverEdgeTexturePixels {
    require(width > 0 && height > 0 && pixels.size >= width * height)
    val extension = pixels.copyOf(width * height)
    // CLAMP samples these rim pixels outside the square cover.
    for (y in 0 until height) for (x in 0 until width) {
        if (x < 2 || x >= width - 2 || y < 2 || y >= height - 2) {
            extension[y * width + x] = backgroundColor
        }
    }

    val featherWidth = (min(width, height) * when (mode) {
        CoverExtensionMode.SOFT -> 0.15f
        else -> 0.07f
    }).roundToInt().coerceAtLeast(3)
    val feather = IntArray(width * height)
    val rgb = backgroundColor and 0x00ffffff
    for (y in 0 until height) for (x in 0 until width) {
        val distance = min(min(x, width - 1 - x), min(y, height - 1 - y))
        if (distance >= featherWidth) continue
        val fraction = (1f - distance.toFloat() / featherWidth).coerceIn(0f, 1f)
        val alpha = (fraction * fraction * 255f).roundToInt()
        feather[y * width + x] = (alpha shl 24) or rgb
    }
    return CoverEdgeTexturePixels(extension, feather, backgroundColor)
}
