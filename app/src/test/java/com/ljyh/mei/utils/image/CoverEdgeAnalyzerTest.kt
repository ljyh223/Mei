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
        // One occupied side still leaves a flat rim on the other three, so a soft blend is fine.
        assertEquals(CoverExtensionMode.SOFT, result.mode)
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
    fun fullyOccupiedBottomEdgeStillExtendsSoftly() {
        val pixels = IntArray(side * side) { background }
        for (y in 100 until side) for (x in 0 until side) pixels[y * side + x] = foreground
        val result = requireNotNull(analyzeCoverBackground(pixels, side, side))
        assertTrue(result.isSolidBackground)
        assertEquals(0f, result.sideMatches[2], 0.01f)
        assertEquals(CoverExtensionMode.SOFT, result.mode)
    }

    @Test
    fun fineGrainTextureIsRejected() {
        var seed = 7
        val pixels = IntArray(side * side) {
            seed = seed * 1103515245 + 12345
            val jitter = ((seed shr 16) and 0x7) - 3
            val r = ((background shr 16) and 0xff) + jitter
            val g = ((background shr 8) and 0xff) + jitter
            val b = (background and 0xff) + jitter
            (0xff shl 24) or ((r and 0xff) shl 16) or ((g and 0xff) shl 8) or (b and 0xff)
        }
        val result = requireNotNull(analyzeCoverBackground(pixels, side, side))
        assertEquals(CoverExtensionMode.NONE, result.mode)
    }

    @Test
    fun smoothGradientIsRejected() {
        val from = 0xff1f3a6e.toInt()
        val to = 0xff4f7fb8.toInt()
        val pixels = IntArray(side * side) { index ->
            val x = index % side
            val t = x.toFloat() / (side - 1)
            val r = (((from shr 16) and 0xff) * (1 - t) + ((to shr 16) and 0xff) * t).toInt()
            val g = (((from shr 8) and 0xff) * (1 - t) + ((to shr 8) and 0xff) * t).toInt()
            val b = ((from and 0xff) * (1 - t) + (to and 0xff) * t).toInt()
            (0xff shl 24) or (r shl 16) or (g shl 8) or b
        }
        val result = requireNotNull(analyzeCoverBackground(pixels, side, side))
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

    @Test
    fun measurementsReportActualEdgesAndLeaveSkippedInteriorUnknown() {
        val clean = requireNotNull(analyzeCoverBackground(IntArray(side * side) { background }, side, side))
        assertEquals(1f, clean.bandShare, 0f)
        assertEquals(1f, clean.wholeShare!!, 0f)
        assertEquals(0f, clean.bandTexture, 0f)
        assertEquals(0f, clean.bandGradient, 0f)
        assertEquals(0f, clean.longestGap, 0f)
        val busy = IntArray(side * side) { if (((it / side) + (it % side)) % 2 == 0) background else foreground }
        val rejected = requireNotNull(analyzeCoverBackground(busy, side, side))
        assertEquals(null, rejected.wholeShare)
    }

    @Test
    fun singleOccupiedSideExceptionCanBeDisabled() {
        val pixels = IntArray(side * side) { background }
        for (y in 100 until side) for (x in 0 until side) pixels[y * side + x] = foreground
        val defaults = requireNotNull(analyzeCoverBackground(pixels, side, side))
        val disabled = requireNotNull(analyzeCoverBackground(pixels, side, side,
            CoverBackgroundConfig(allowSingleOccupiedSide = false, allowThreeStrongSides = false)))
        assertEquals(CoverExtensionMode.SOFT, defaults.mode)
        assertEquals(CoverExtensionMode.NONE, disabled.mode)
    }

    @Test
    fun threeStrongSidesExceptionCanBeDisabledIndependently() {
        val pixels = IntArray(side * side) { background }
        for (y in 28 until 100) for (x in 65 until side) pixels[y * side + x] = foreground
        val config = CoverBackgroundConfig(allowSingleOccupiedSide = false)
        val allowed = requireNotNull(analyzeCoverBackground(pixels, side, side, config))
        val disabled = requireNotNull(analyzeCoverBackground(pixels, side, side, config.copy(allowThreeStrongSides = false)))
        assertEquals(CoverExtensionMode.SOFT, allowed.mode)
        assertEquals(CoverExtensionMode.NONE, disabled.mode)
    }

    @Test
    fun maximumGapChangesDirectExtensionDecision() {
        val pixels = IntArray(side * side) { background }
        for (y in 0 until 25) for (x in 52 until 76) pixels[y * side + x] = foreground
        val default = requireNotNull(analyzeCoverBackground(pixels, side, side))
        val relaxed = requireNotNull(analyzeCoverBackground(pixels, side, side, CoverBackgroundConfig(maximumSolidGap = 0.25f)))
        assertEquals(CoverExtensionMode.SOFT, default.mode)
        assertEquals(CoverExtensionMode.SOLID, relaxed.mode)
    }

    @Test
    fun textureThresholdControlsGrainRejection() {
        var seed = 7
        val pixels = IntArray(side * side) {
            seed = seed * 1103515245 + 12345
            val jitter = ((seed shr 16) and 0x7) - 3
            val r = ((background shr 16) and 0xff) + jitter
            val g = ((background shr 8) and 0xff) + jitter
            val b = (background and 0xff) + jitter
            (0xff shl 24) or ((r and 0xff) shl 16) or ((g and 0xff) shl 8) or (b and 0xff)
        }
        val default = requireNotNull(analyzeCoverBackground(pixels, side, side))
        val relaxed = requireNotNull(analyzeCoverBackground(pixels, side, side, CoverBackgroundConfig(maximumBandTexture = 0.1f)))
        assertTrue(default.bandTexture > CoverBackgroundConfig().maximumBandTexture)
        assertEquals(CoverExtensionMode.NONE, default.mode)
        assertEquals(CoverExtensionMode.SOLID, relaxed.mode)
    }

}
