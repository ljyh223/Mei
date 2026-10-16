package com.ljyh.mei.playback.sleep

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepConfidenceGateTest {
    @Test
    fun requiresTwoFreshConsecutiveHighConfidenceSamples() {
        val started = 1_000_000L
        val gate = SleepConfidenceGate(started)

        assertFalse(gate.onClassification(started - 1, 95, started + 60_000))
        assertFalse(gate.onClassification(started + 60_000, 85, started + 60_000))
        assertFalse(gate.onClassification(started + 60_000, 90, started + 60_000))
        assertFalse(gate.onClassification(started + 11 * 60_000, 50, started + 11 * 60_000))
        assertFalse(gate.onClassification(started + 21 * 60_000, 81, started + 21 * 60_000))
        assertTrue(gate.onClassification(started + 31 * 60_000, 88, started + 31 * 60_000))
    }

    @Test
    fun ignoresOldOrWidelySeparatedSamples() {
        val gate = SleepConfidenceGate(1_000_000L)
        assertFalse(gate.onClassification(1_060_000L, 90, 1_060_000L))
        assertFalse(gate.onClassification(3_060_000L, 90, 3_060_000L))
        assertFalse(gate.onClassification(3_660_000L, 90, 5_660_001L))
    }
}
