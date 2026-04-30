package com.ljyh.mei.ui.component.player.component.mesh

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private fun noise(x: Float, y: Float): Float {
    return fract(sin(x * 12.9898f + y * 78.233f) * 43758.5453f)
}

private fun fract(x: Float): Float = x - floor(x)

private fun smoothNoise(x: Float, y: Float): Float {
    val x0 = floor(x)
    val y0 = floor(y)
    val x1 = x0 + 1f
    val y1 = y0 + 1f

    val xf = x - x0
    val yf = y - y0

    val u = xf * xf * (3f - 2f * xf)
    val v = yf * yf * (3f - 2f * yf)

    val n00 = noise(x0, y0)
    val n10 = noise(x1, y0)
    val n01 = noise(x0, y1)
    val n11 = noise(x1, y1)

    val nx0 = n00 * (1f - u) + n10 * u
    val nx1 = n01 * (1f - u) + n11 * u

    return nx0 * (1f - v) + nx1 * v
}

private fun computeNoiseGradient(
    x: Float, y: Float,
    epsilon: Float = 0.001f
): FloatArray {
    val n1 = smoothNoise(x + epsilon, y)
    val n2 = smoothNoise(x - epsilon, y)
    val n3 = smoothNoise(x, y + epsilon)
    val n4 = smoothNoise(x, y - epsilon)
    val dx = (n1 - n2) / (2f * epsilon)
    val dy = (n3 - n4) / (2f * epsilon)
    val len = sqrt(dx * dx + dy * dy).let { if (it < 0.0001f) 1f else it }
    return floatArrayOf(dx / len, dy / len)
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun smoothifyControlPoints(
    conf: MutableList<ControlPointConf>,
    w: Int, h: Int,
    iterations: Int = 2,
    factor: Float = 0.5f,
    factorIterationModifier: Float = 0.1f
) {
    var grid = Array(h) { j -> Array(w) { i -> conf[j * w + i] } }
    var f = factor

    for (iter in 0 until iterations) {
        val newGrid = Array(h) { j -> Array(w) { i ->
            if (i == 0 || i == w - 1 || j == 0 || j == h - 1) {
                grid[j][i]
            } else {
                var sumX = 0f; var sumY = 0f
                var sumUR = 0f; var sumVR = 0f; var sumUP = 0f; var sumVP = 0f
                val kernel = arrayOf(
                    intArrayOf(1, 2, 1), intArrayOf(2, 4, 2), intArrayOf(1, 2, 1)
                )
                for (dj in -1..1) {
                    for (di in -1..1) {
                        val weight = kernel[dj + 1][di + 1]
                        val nb = grid[j + dj][i + di]
                        sumX += nb.x * weight; sumY += nb.y * weight
                        sumUR += nb.ur * weight; sumVR += nb.vr * weight
                        sumUP += nb.up * weight; sumVP += nb.vp * weight
                    }
                }
                val cur = grid[j][i]
                ControlPointConf(
                    cx = cur.cx, cy = cur.cy,
                    x = cur.x * (1f - f) + (sumX / 16f) * f,
                    y = cur.y * (1f - f) + (sumY / 16f) * f,
                    ur = cur.ur * (1f - f) + (sumUR / 16f) * f,
                    vr = cur.vr * (1f - f) + (sumVR / 16f) * f,
                    up = cur.up * (1f - f) + (sumUP / 16f) * f,
                    vp = cur.vp * (1f - f) + (sumVP / 16f) * f
                )
            }
        }}
        grid = newGrid
        f = (f + factorIterationModifier).coerceIn(0f, 1f)
    }

    for (j in 0 until h) {
        for (i in 0 until w) {
            conf[j * w + i] = grid[j][i]
        }
    }
}

fun generateControlPoints(
    random: Random = Random.Default,
    width: Int = 5,
    height: Int = 5,
    variationFraction: Float = randomRange(random, 0.4f, 0.6f),
    normalOffset: Float = randomRange(random, 0.3f, 0.6f),
    blendFactor: Float = 0.8f,
    smoothIters: Int = random.nextInt(3, 6),
    smoothFactor: Float = randomRange(random, 0.2f, 0.3f),
    smoothModifier: Float = randomRange(random, -0.1f, -0.05f)
): ControlPointPreset {
    val w = width
    val h = height
    val dx = if (w == 1) 0f else 2f / (w - 1)
    val dy = if (h == 1) 0f else 2f / (h - 1)

    val conf = mutableListOf<ControlPointConf>()

    for (j in 0 until h) {
        for (i in 0 until w) {
            val baseX = if (w == 1) 0f else i.toFloat() / (w - 1) * 2f - 1f
            val baseY = if (h == 1) 0f else j.toFloat() / (h - 1) * 2f - 1f

            val isBorder = i == 0 || i == w - 1 || j == 0 || j == h - 1

            val pertX = if (isBorder) 0f else randomRange(random, -variationFraction * dx, variationFraction * dx)
            val pertY = if (isBorder) 0f else randomRange(random, -variationFraction * dy, variationFraction * dy)
            var x = baseX + pertX
            var y = baseY + pertY

            val ur = if (isBorder) 0f else randomRange(random, -60f, 60f)
            val vr = if (isBorder) 0f else randomRange(random, -60f, 60f)
            val up = if (isBorder) 1f else randomRange(random, 0.8f, 1.2f)
            val vp = if (isBorder) 1f else randomRange(random, 0.8f, 1.2f)

            if (!isBorder) {
                val u = (baseX + 1f) / 2f
                val v = (baseY + 1f) / 2f

                val grad = computeNoiseGradient(u, v, 0.001f)
                var offsetX = grad[0] * normalOffset
                var offsetY = grad[1] * normalOffset

                val distToBorder = minOf(u, 1f - u, v, 1f - v)
                val weight = smoothstep(0f, 1f, distToBorder)

                x = x * (1f - blendFactor) + (x + offsetX) * blendFactor * weight +
                    x * blendFactor * (1f - weight)
                y = y * (1f - blendFactor) + (y + offsetY) * blendFactor * weight +
                    y * blendFactor * (1f - weight)
            }

            conf.add(ControlPointConf(i, j, x, y, ur, vr, up, vp))
        }
    }

    smoothifyControlPoints(conf, w, h, smoothIters, smoothFactor, smoothModifier)

    return ControlPointPreset(w, h, conf.toList())
}

private fun randomRange(random: Random, min: Float, max: Float): Float {
    return min + random.nextFloat() * (max - min)
}