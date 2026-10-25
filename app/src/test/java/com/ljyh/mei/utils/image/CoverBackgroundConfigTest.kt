package com.ljyh.mei.utils.image

import org.junit.Assert.*
import org.junit.Test

class CoverBackgroundConfigTest {
    @Test fun standardKeepsExistingThresholds() {
        val standard = CoverBackgroundPreset.Standard.config!!
        assertEquals(CoverBackgroundConfig(), standard)
        assertEquals(0.008f, standard.maximumBandTexture, 0f)
        assertEquals(0.045f, standard.maximumBandGradient, 0f)
        assertEquals(0.55f, standard.minimumBandShare, 0f)
        assertEquals(0.30f, standard.minimumWholeShare, 0f)
        assertTrue(standard.allowSingleOccupiedSide)
        assertTrue(standard.allowThreeStrongSides)
    }

    @Test fun presetsTightenThresholdsInTheExpectedDirection() {
        val loose = CoverBackgroundPreset.Relaxed.config!!
        val standard = CoverBackgroundPreset.Standard.config!!
        val strict = CoverBackgroundPreset.Strict.config!!
        assertTrue(loose.colorTolerance > standard.colorTolerance && standard.colorTolerance > strict.colorTolerance)
        assertTrue(loose.maximumBandTexture > standard.maximumBandTexture && standard.maximumBandTexture > strict.maximumBandTexture)
        assertTrue(loose.maximumBandGradient > standard.maximumBandGradient && standard.maximumBandGradient > strict.maximumBandGradient)
        assertTrue(loose.minimumBackgroundConfidence < standard.minimumBackgroundConfidence && standard.minimumBackgroundConfidence < strict.minimumBackgroundConfidence)
        assertTrue(loose.minimumBandShare < standard.minimumBandShare && standard.minimumBandShare < strict.minimumBandShare)
        assertTrue(loose.softEdgeMatch < standard.softEdgeMatch && standard.softEdgeMatch < strict.softEdgeMatch)
        assertTrue(loose.maximumSoftGap > standard.maximumSoftGap && standard.maximumSoftGap > strict.maximumSoftGap)
        assertFalse(strict.allowSingleOccupiedSide)
        assertFalse(strict.allowThreeStrongSides)
        CoverBackgroundPreset.entries.mapNotNull { it.config }.forEach { assertEquals(it, it.normalized()) }
    }

    @Test fun invalidValuesCannotReachTheAnalyzer() {
        val config = CoverBackgroundConfig(
            colorTolerance = Float.NaN, maximumBandTexture = Float.POSITIVE_INFINITY,
            maximumBandGradient = -1f, minimumBackgroundConfidence = 9f,
            solidEdgeMatch = 0.6f, softEdgeMatch = 0.9f, minimumSingleEdgeMatch = 0.8f,
            maximumSolidGap = 0.8f, maximumSoftGap = 0.2f,
        ).normalized()
        assertEquals(0.075f, config.colorTolerance, 0f)
        assertEquals(0.008f, config.maximumBandTexture, 0f)
        assertEquals(0f, config.maximumBandGradient, 0f)
        assertEquals(1f, config.minimumBackgroundConfidence, 0f)
        assertEquals(0.6f, config.softEdgeMatch, 0f)
        assertEquals(0.6f, config.minimumSingleEdgeMatch, 0f)
        assertEquals(0.2f, config.maximumSolidGap, 0f)
    }
}
