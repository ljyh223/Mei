package com.ljyh.mei.playback.equalizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftLimiterTest {

    @Test
    fun quietSamplesPassUnchanged() {
        val threshold = 0.944
        listOf(-0.5, -0.1, 0.0, 0.3, 0.9).forEach { sample ->
            assertEquals(sample, softKneeSample(sample, threshold), 1e-9)
        }
    }

    @Test
    fun loudSamplesStayUnderFullScaleAndKeepSign() {
        val threshold = 0.944
        listOf(1.2, 2.0, 10.0, -1.2, -4.0).forEach { sample ->
            val out = softKneeSample(sample, threshold)
            assertTrue("peak $out", kotlin.math.abs(out) <= 1.0)
            assertEquals(kotlin.math.sign(sample), kotlin.math.sign(out), 0.0)
            assertTrue("monotonic", kotlin.math.abs(out) >= threshold - 1e-9)
        }
    }

    @Test
    fun targetGainOnlyReduces() {
        assertEquals(1.0, limiterTargetGain(0.5, 0.944), 0.0)
        assertEquals(1.0, limiterTargetGain(0.0, 0.944), 0.0)
        assertTrue(limiterTargetGain(2.0, 0.944) < 0.5)
    }

    @Test
    fun softKneeIsMonotonic() {
        val threshold = 0.944
        var previous = 0.0
        for (input in listOf(0.9, 0.95, 1.0, 1.5, 2.0, 5.0, 20.0)) {
            val out = softKneeSample(input, threshold)
            assertTrue("monotonic at $input", out >= previous - 1e-12)
            assertTrue("bounded at $input", out <= 1.0)
            previous = out
        }
    }
}
