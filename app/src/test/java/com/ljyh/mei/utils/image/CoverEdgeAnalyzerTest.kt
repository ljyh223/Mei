package com.ljyh.mei.utils.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverEdgeAnalyzerTest {
    private val side = 128
    private val background = 0xffe8d9c2.toInt()
    private val foreground = 0xff204060.toInt()

    @Test
    fun centeredArtworkOnFlatBackgroundExtendsDirectly() {
        val pixels = IntArray(side * side) { background }
        for (y in 30 until 98) for (x in 36 until 92) pixels[y * side + x] = foreground

        val result = requireNotNull(analyzeCoverBackground(pixels, side, side))
        assertEquals(background, result.color)
        assertTrue(result.isSolidBackground)
        assertEquals(CoverExtensionMode.SOLID, result.mode)
    }

    @Test
    fun foregroundTouchingOneEdgeStillHasDetectableBackground() {
        val pixels = IntArray(side * side) { background }
        for (y in 28 until 100) for (x in 65 until side) pixels[y * side + x] = foreground

        val result = requireNotNull(analyzeCoverBackground(pixels, side, side))
        assertEquals(background, result.color)
        assertTrue(result.isSolidBackground)
        assertTrue(result.sideMatches[1] < 0.5f)
        assertEquals(CoverExtensionMode.NONE, result.mode)
    }

    @Test
    fun shortInterruptionAtTopUsesSoftTransition() {
        val pixels = IntArray(side * side) { background }
        for (y in 0 until 25) for (x in 52 until 76) pixels[y * side + x] = foreground

        val result = requireNotNull(analyzeCoverBackground(pixels, side, side))
        assertTrue(result.isSolidBackground)
        assertEquals(CoverExtensionMode.SOFT, result.mode)
    }

    @Test
    fun thinFlatFrameAroundBusyArtworkIsRejected() {
        val pixels = IntArray(side * side) { background }
        for (y in 4 until side - 4) for (x in 4 until side - 4) {
            pixels[y * side + x] = if ((x + y) % 2 == 0) foreground else 0xffa73521.toInt()
        }

        val result = requireNotNull(analyzeCoverBackground(pixels, side, side))
        assertFalse(result.isSolidBackground)
        assertEquals(CoverExtensionMode.NONE, result.mode)
    }

    @Test
    fun busyEdgesAreRejectedWithoutScanningTheInterior() {
        val pixels = IntArray(side * side) {
            if (((it / side) + (it % side)) % 2 == 0) background else foreground
        }
        val result = requireNotNull(analyzeCoverBackground(pixels, side, side))
        assertFalse(result.isSolidBackground)
        assertEquals(CoverExtensionMode.NONE, result.mode)
    }

    @Test
    fun confidenceThresholdCanBeAdjusted() {
        val pixels = IntArray(side * side) { background }
        for (y in 30 until 98) for (x in 36 until 92) pixels[y * side + x] = foreground

        val strict = requireNotNull(analyzeCoverBackground(
            pixels, side, side, CoverBackgroundConfig(minimumBackgroundConfidence = 0.98f),
        ))
        assertFalse(strict.isSolidBackground)
        assertEquals(CoverExtensionMode.NONE, strict.mode)
    }
}
