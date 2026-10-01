package com.ljyh.mei.playback.equalizer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ParametricEqualizerTest {
    @Test
    fun peakFilterMatchesRequestedGainAndBypassesAtOtherFrequencies() {
        val filter = EqFilter(1, FilterType.PEAK, 1_000f, 6f, 1f)
        val coefficients = BiquadDesign.coefficients(filter, 48_000)
        assertEquals(6.0, coefficients.magnitudeDb(1_000.0, 48_000), 0.05)
        assertTrue(coefficients.magnitudeDb(20.0, 48_000) < 0.1)
        assertTrue(coefficients.magnitudeDb(18_000.0, 48_000) < 0.1)
    }

    @Test
    fun shelvesAndPassFiltersHaveExpectedDirection() {
        val lowShelf = BiquadDesign.coefficients(EqFilter(1, FilterType.LOW_SHELF, 120f, 4f), 48_000)
        assertTrue(lowShelf.magnitudeDb(30.0, 48_000) > lowShelf.magnitudeDb(8_000.0, 48_000) + 2.0)
        val highPass = BiquadDesign.coefficients(EqFilter(2, FilterType.HIGH_PASS, 100f), 48_000)
        assertTrue(highPass.magnitudeDb(30.0, 48_000) < -10.0)
        assertTrue(highPass.magnitudeDb(2_000.0, 48_000) > -0.1)
    }

    @Test
    fun profileRoundTripsAndSanitizesMalformedFilterValues() {
        val profile = EqualizerProfile(true, "custom", -3f, 1f,
            listOf(EqFilter(7, FilterType.PEAK, 750f, 2.5f, 1.3f, false)))
        assertEquals(profile, EqualizerProfileCodec.decode(EqualizerProfileCodec.encode(profile)))
        val decoded = EqualizerProfileCodec.decode("v1;1;custom;0;0;1,PEAK,NaN,2,0.7,1;2,LOW_PASS,100,0,0.7,1")
        assertNotNull(decoded)
        assertEquals(1, decoded!!.filters.size)
        assertEquals(FilterType.LOW_PASS, decoded.filters.single().type)
    }

    @Test
    fun zeroGainProfileHasFlatResponse() {
        val profile = EqualizerProfile(true, filters = listOf(
            EqFilter(1, FilterType.PEAK, 1_000f), EqFilter(2, FilterType.LOW_SHELF, 120f)))
        for (frequency in listOf(20.0, 80.0, 1_000.0, 12_000.0)) {
            assertEquals(0.0, BiquadDesign.responseDb(profile, frequency), 0.001)
        }
    }

    @Test
    fun processorAppliesGainToPcmAndCanBeDisabledLive() {
        val processor = ParametricEqualizerProcessor()
        processor.configure(AudioProcessor.AudioFormat(48_000, 1, C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.setProfile(EqualizerProfile(enabled = true, inputGainDb = -6f))
        val samples = ByteBuffer.allocateDirect(2_048).order(ByteOrder.LITTLE_ENDIAN)
        repeat(1_024) { samples.putShort(16_000) }
        samples.flip()
        processor.queueInput(samples)
        val output = processor.getOutput().order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(8_019.0, output.getShort(2_046).toDouble(), 10.0)

        processor.setProfile(EqualizerProfile(enabled = false))
        val bypassSamples = ByteBuffer.allocateDirect(4_096).order(ByteOrder.LITTLE_ENDIAN)
        repeat(2_048) { bypassSamples.putShort(16_000) }
        bypassSamples.flip()
        processor.queueInput(bypassSamples)
        val bypassOutput = processor.getOutput().order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(16_000, bypassOutput.getShort(4_094).toInt())
    }
}
