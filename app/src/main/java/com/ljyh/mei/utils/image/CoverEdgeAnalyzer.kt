package com.ljyh.mei.utils.image

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal enum class CoverExtensionMode { NONE, SOFT, SOLID }

internal data class AlbumBackgroundAnalysis(
    val color: Int,
    val isSolidBackground: Boolean,
    val backgroundConfidence: Float,
    val extensionConfidence: Float,
    val edgeConsistency: Float,
    val sideMatches: List<Float>, // top, right, bottom, left
    val mode: CoverExtensionMode,
)

private data class Lab(val l: Float, val a: Float, val b: Float)

private data class ColorBin(var count: Int = 0, var l: Float = 0f, var a: Float = 0f, var b: Float = 0f) {
    fun add(value: Lab) {
        count++
        l += value.l
        a += value.a
        b += value.b
    }

    fun center() = Lab(l / count, a / count, b / count)
}

/**
 * Finds a dominant flat color without requiring a continuous border on all four sides.
 *
 * Only the edge band is converted to OKLab up front; the interior is converted lazily and only
 * when the band already looks like a solid background. Patterned covers therefore skip the full
 * image pass entirely, and no per-pixel Lab objects are allocated.
 */
internal fun analyzeCoverBackground(
    pixels: IntArray,
    width: Int,
    height: Int,
    config: CoverBackgroundConfig = CoverBackgroundConfig(),
): AlbumBackgroundAnalysis? {
    if (width < 32 || height < 32 || pixels.size < width * height) return null
    val total = width * height
    val labL = FloatArray(total)
    val labA = FloatArray(total)
    val labB = FloatArray(total)
    val valid = BooleanArray(total)
    fun ensure(index: Int): Boolean {
        if (valid[index]) return true
        val pixel = pixels[index]
        if (pixel ushr 24 < 248) return false
        val color = toOKLab(pixel)
        labL[index] = color.l
        labA[index] = color.a
        labB[index] = color.b
        valid[index] = true
        return true
    }

    val band = (min(width, height) * 0.08f).roundToInt().coerceAtLeast(3)
    val bandIndices = ArrayList<Int>(total / 3)
    val bins = HashMap<Int, ColorBin>()
    for (y in 0 until height) for (x in 0 until width) {
        if (x >= band && x < width - band && y >= band && y < height - band) continue
        val index = y * width + x
        if (!ensure(index)) continue
        bandIndices.add(index)
        // Coarse bins only provide seeds. The final match uses perceptual distance, so
        // colors near a bin boundary still join the same cluster.
        val l = (labL[index] / 0.04f).roundToInt()
        val a = ((labA[index] + 0.5f) / 0.04f).roundToInt()
        val b = ((labB[index] + 0.5f) / 0.04f).roundToInt()
        bins.getOrPut((l shl 10) or (a shl 5) or b) { ColorBin() }
            .add(Lab(labL[index], labA[index], labB[index]))
    }
    if (bandIndices.size < total / 10 || bins.isEmpty()) return null

    val tolerance = config.colorTolerance.coerceIn(0.025f, 0.15f)
    var bestSeed: Lab? = null
    var bestSupport = -1
    for (bin in bins.values.sortedByDescending { it.count }.take(12)) {
        val seed = bin.center()
        val support = bandIndices.count { index -> distance(labL, labA, labB, index, seed) <= tolerance }
        if (support > bestSupport) {
            bestSupport = support
            bestSeed = seed
        }
    }
    val seed = bestSeed ?: return null

    val matches = BooleanArray(total)
    var bandRed = 0L
    var bandGreen = 0L
    var bandBlue = 0L
    var bandMatches = 0
    var bandDistanceTotal = 0f
    for (index in bandIndices) {
        val distance = distance(labL, labA, labB, index, seed)
        if (distance > tolerance) continue
        matches[index] = true
        bandMatches++
        bandDistanceTotal += distance
        bandRed += (pixels[index] ushr 16) and 0xff
        bandGreen += (pixels[index] ushr 8) and 0xff
        bandBlue += pixels[index] and 0xff
    }
    val bandShare = bandMatches.toFloat() / bandIndices.size
    val sideMatches = sideMatches(matches, width, height)
    val edgeConsistency = sideMatches.average().toFloat()
    val weakestSide = sideMatches.minOrNull() ?: 0f

    // A solid background needs at least 55% of the band; below that the interior pass cannot
    // change the decision, so stop before converting and scanning the whole image.
    if (bandMatches == 0 || bandShare < 0.55f) {
        val color = if (bandMatches == 0) 0 else {
            (0xff shl 24) or
                ((bandRed / bandMatches).toInt() shl 16) or
                ((bandGreen / bandMatches).toInt() shl 8) or (bandBlue / bandMatches).toInt()
        }
        return AlbumBackgroundAnalysis(
            color = color,
            isSolidBackground = false,
            backgroundConfidence = (0.45f * bandShare).coerceIn(0f, 1f),
            extensionConfidence = edgeConsistency,
            edgeConsistency = edgeConsistency,
            sideMatches = sideMatches,
            mode = CoverExtensionMode.NONE,
        )
    }

    var red = 0L
    var green = 0L
    var blue = 0L
    var matchCount = 0
    var distanceTotal = 0f
    for (index in 0 until total) {
        if (!ensure(index)) continue
        val distance = distance(labL, labA, labB, index, seed)
        if (distance > tolerance) continue
        matches[index] = true
        matchCount++
        distanceTotal += distance
        red += (pixels[index] ushr 16) and 0xff
        green += (pixels[index] ushr 8) and 0xff
        blue += pixels[index] and 0xff
    }
    if (matchCount == 0) return null
    val background = (0xff shl 24) or
        ((red / matchCount).toInt() shl 16) or
        ((green / matchCount).toInt() shl 8) or (blue / matchCount).toInt()
    val wholeShare = matchCount.toFloat() / total
    val purity = (1f - distanceTotal / matchCount / tolerance).coerceIn(0f, 1f)
    val backgroundConfidence = (0.45f * bandShare + 0.40f * wholeShare + 0.15f * purity)
        .coerceIn(0f, 1f)
    val solidBackground = bandShare >= 0.55f && wholeShare >= 0.30f &&
        backgroundConfidence >= config.minimumBackgroundConfidence

    val longestGap = longestGap(matches, width, height)
    val extensionConfidence = (edgeConsistency * 0.7f + weakestSide * 0.3f).coerceIn(0f, 1f)
    val mode = when {
        !solidBackground -> CoverExtensionMode.NONE
        extensionConfidence >= config.solidEdgeMatch &&
            weakestSide >= max(config.minimumSingleEdgeMatch, config.solidEdgeMatch - 0.12f) &&
            longestGap <= 0.10f -> CoverExtensionMode.SOLID
        extensionConfidence >= config.softEdgeMatch &&
            weakestSide >= config.minimumSingleEdgeMatch && longestGap <= 0.35f ->
            CoverExtensionMode.SOFT
        else -> CoverExtensionMode.NONE
    }
    return AlbumBackgroundAnalysis(
        color = background,
        isSolidBackground = solidBackground,
        backgroundConfidence = backgroundConfidence,
        extensionConfidence = extensionConfidence,
        edgeConsistency = edgeConsistency,
        sideMatches = sideMatches,
        mode = mode,
    )
}

private fun distance(l: FloatArray, a: FloatArray, b: FloatArray, index: Int, seed: Lab): Float {
    val dl = l[index] - seed.l
    val da = a[index] - seed.a
    val db = b[index] - seed.b
    return sqrt(dl * dl + da * da + db * db)
}

private fun sideMatches(matches: BooleanArray, width: Int, height: Int): List<Float> {
    val sides = listOf(
        IntArray(width) { x -> x },
        IntArray(height) { y -> y * width + width - 1 },
        IntArray(width) { x -> (height - 1) * width + x },
        IntArray(height) { y -> y * width },
    )
    return sides.map { side -> side.count { matches[it] }.toFloat() / side.size }
}

private fun longestGap(matches: BooleanArray, width: Int, height: Int): Float {
    val sides = listOf(
        IntArray(width) { x -> x },
        IntArray(height) { y -> y * width + width - 1 },
        IntArray(width) { x -> (height - 1) * width + x },
        IntArray(height) { y -> y * width },
    )
    return sides.maxOf { side ->
        var current = 0
        var longest = 0
        side.forEach { index ->
            current = if (matches[index]) 0 else current + 1
            longest = max(longest, current)
        }
        longest.toFloat() / side.size
    }
}

private fun toOKLab(pixel: Int): Lab {
    fun linear(value: Int): Double {
        val channel = value / 255.0
        return if (channel <= 0.04045) channel / 12.92
        else Math.pow((channel + 0.055) / 1.055, 2.4)
    }
    val r = linear((pixel ushr 16) and 0xff)
    val g = linear((pixel ushr 8) and 0xff)
    val b = linear(pixel and 0xff)
    val l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    return Lab(
        (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s).toFloat(),
        (1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s).toFloat(),
        (0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s).toFloat(),
    )
}
