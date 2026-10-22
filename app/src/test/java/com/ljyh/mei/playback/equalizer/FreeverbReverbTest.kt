package com.ljyh.mei.playback.equalizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FreeverbReverbTest {

    @Test
    fun disabledProfileIsPassthrough() {
        val core = FreeverbCore(ReverbProfile.Off, channels = 2, sampleRate = 48_000)
        val frame = floatArrayOf(0.25f, -0.5f)
        core.process(frame)
        assertEquals(0.25f, frame[0], 1e-6f)
        assertEquals(-0.5f, frame[1], 1e-6f)
    }

    @Test
    fun impulseProducesDecayingTail() {
        val core = FreeverbCore(
            ReverbProfile.Hall.copy(wet = 0.6f, dry = 0f, preDelayMs = 0f),
            channels = 2,
            sampleRate = 48_000,
        )
        val totalFrames = 48_000
        var earlyEnergy = 0.0
        var lateEnergy = 0.0
        var peak = 0f
        for (index in 0 until totalFrames) {
            val frame = floatArrayOf(if (index == 0) 1f else 0f, 0f)
            core.process(frame)
            assertTrue("non-finite sample at $index", frame[0].isFinite())
            peak = maxOf(peak, abs(frame[0]))
            val energy = frame[0].toDouble() * frame[0]
            if (index in 0 until 4_800) earlyEnergy += energy
            if (index in 24_000 until 48_000) lateEnergy += energy
        }
        assertTrue("no audible tail", earlyEnergy > 1e-6)
        assertTrue("tail must decay", lateEnergy < earlyEnergy)
        assertTrue("unstable feedback", peak < 4f)
    }

    @Test
    fun noiseStaysFiniteAndBounded() {
        val core = FreeverbCore(
            ReverbProfile.Cathedral,
            channels = 2,
            sampleRate = 44_100,
        )
        var seed = 12345
        var peak = 0f
        repeat(44_100) {
            seed = seed * 1103515245 + 12345
            val sample = ((seed shr 16) and 0x7fff) / 32768f - 0.5f
            val frame = floatArrayOf(sample, -sample)
            core.process(frame)
            frame.forEach {
                assertTrue("non-finite output", it.isFinite())
                peak = maxOf(peak, abs(it))
            }
        }
        assertTrue("output must stay bounded, peak=$peak", peak < 8f)
    }

    @Test
    fun codecRoundTrips() {
        val profile = ReverbProfile.Hall.copy(presetId = "hall", wet = 0.31f, preDelayMs = 40f)
        val decoded = ReverbProfileCodec.decode(ReverbProfileCodec.encode(profile))
        assertNotNull(decoded)
        assertEquals(profile.enabled, decoded!!.enabled)
        assertEquals("hall", decoded.presetId)
        assertEquals(profile.roomSize, decoded.roomSize, 1e-5f)
        assertEquals(profile.wet, decoded.wet, 1e-5f)
        assertEquals(40f, decoded.preDelayMs, 1e-5f)
    }

    @Test
    fun malformedCodecFallsBack() {
        assertNull(ReverbProfileCodec.decode(null))
        assertNull(ReverbProfileCodec.decode("v0;1"))
        assertNull(ReverbProfileCodec.decode("v1;1;hall;0.5"))
    }

    @Test
    fun presetsAreNormalized() {
        ReverbPresets.all.forEach { preset ->
            assertEquals(preset.profile, preset.profile.normalized())
        }
    }
}
