package com.ljyh.mei.playback.transition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatNetRhythmTest {
    @Test fun detectsBeatsAndBarStartsFromModelActivations() {
        val activations = FloatArray(3_200) { 0.01f }
        for (frame in 0 until 1_600 step 25) {
            activations[frame * 2] = 0.9f
            if (frame % 100 == 0) activations[frame * 2 + 1] = 0.95f
        }
        val rhythm = BeatNetRhythmPlanner.decode(activations, 68_000L)
        assertNotNull(rhythm)
        val detected = rhythm!!
        assertEquals(120.0, detected.bpm, 1.0)
        assertTrue(detected.confidence > 0.5)
        assertTrue(detected.downbeatsMs.all { (it - 68_000L) % 2_000L == 0L })
        val plan = BeatNetRhythmPlanner.plan(detected, detected, 100_000L)
        assertNotNull(plan)
        assertTrue(plan!!.outgoingStartMs in 91_000L..93_000L)
        assertEquals(0L, plan.incomingStartMs)
        assertEquals(1f, plan.incomingRate)
    }

    @Test fun silenceDoesNotProduceAUsablePlan() {
        val silent = BeatNetRhythmPlanner.decode(FloatArray(3_200), 0L)
        assertNotNull(silent)
        assertEquals(null, BeatNetRhythmPlanner.plan(silent!!, silent, 180_000L))
    }

    @Test fun featureExtractorReturnsZeroForSilence() {
        val features = BeatNetFeatures().extract(FloatArray(22_050), 22_050)
        assertEquals(1_600 * 272, features.size)
        assertTrue(features.all { it == 0f })
    }
}
