package com.ljyh.mei.ui.screen.playlist.component

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertTrue
import org.junit.Test

class CollageCoverScaleTest {
    @Test
    fun rotatedCollageCoversEveryViewportCornerAtDifferentSizes() {
        val angle = Math.toRadians(30.0)
        val cosine = cos(angle)
        val sine = sin(angle)
        for ((width, height) in listOf(120f to 120f, 220f to 220f, 360f to 360f, 240f to 320f)) {
            val scale = collageCoverScale(width, height)
            for (x in listOf(-width / 2, width / 2)) {
                for (y in listOf(-height / 2, height / 2)) {
                    val localX = (x * cosine - y * sine) / scale
                    val localY = (x * sine + y * cosine) / scale
                    assertTrue(abs(localX) <= width / 2)
                    assertTrue(abs(localY) <= width * 0.75 / 2)
                }
            }
        }
    }
}
