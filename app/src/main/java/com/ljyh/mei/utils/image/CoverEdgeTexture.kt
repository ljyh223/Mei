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
    sideMatches: List<Float> = listOf(1f, 1f, 1f, 1f),
): CoverEdgeTexturePixels {
    require(width > 0 && height > 0 && pixels.size >= width * height)
    val extension = pixels.copyOf(width * height)
    // CLAMP samples these rim pixels outside the square cover.
    for (y in 0 until height) for (x in 0 until width) {
        if (x < 2 || x >= width - 2 || y < 2 || y >= height - 2) {
            extension[y * width + x] = backgroundColor
        }
    }

    val baseWidth = (min(width, height) * when (mode) {
        CoverExtensionMode.SOFT -> 0.06f
        else -> 0.03f
    }).roundToInt().coerceAtLeast(3)
    // A side covered by artwork gets a narrow feather: it still hides the flat rim, but does not
    // wash the subject with the background color.
    val widths = listOf(0, 1, 2, 3).map { side ->
        if ((sideMatches.getOrNull(side) ?: 1f) >= 0.75f) baseWidth
        else (min(width, height) * 0.02f).roundToInt().coerceAtLeast(2)
    }
    val feather = IntArray(width * height)
    val rgb = backgroundColor and 0x00ffffff
    for (y in 0 until height) for (x in 0 until width) {
        val distances = intArrayOf(y, width - 1 - x, height - 1 - y, x)
        var alpha = 0f
        for (side in 0..3) {
            val width = widths[side]
            val distance = distances[side]
            if (distance >= width) continue
            val fraction = (1f - distance.toFloat() / width).coerceIn(0f, 1f)
            alpha = maxOf(alpha, fraction * fraction)
        }
        if (alpha <= 0f) continue
        feather[y * width + x] = ((alpha * 255f).roundToInt() shl 24) or rgb
    }
    return CoverEdgeTexturePixels(extension, feather, backgroundColor)
}
