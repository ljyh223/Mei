package com.ljyh.mei.playback.transition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartTransitionPlannerTest {
    @Test fun fallsBackWhenNoAudioWasMeasured() {
        assertEquals(TransitionPlan(6_000L), SmartTransitionPlanner.plan(emptyList(), emptyList(), 10_000L))
    }

    @Test fun shortensQuietOutroAndSkipsSilentIntro() {
        val outro = List(80) { 0.01f }
        val intro = List(15) { 0f } + List(40) { 0.09f }
        val plan = SmartTransitionPlanner.plan(outro, intro, 9_000L)
        assertEquals(3_500L, plan.durationMs)
        assertEquals(1_300L, plan.incomingStartMs)
    }

    @Test fun usesCompatiblePulseForPhraseLength() {
        val rhythmic = List(120) { if (it % 5 == 0) 0.4f else 0.12f }
        assertEquals(500L, SmartTransitionPlanner.estimateBeatMs(rhythmic))
        val plan = SmartTransitionPlanner.plan(rhythmic, rhythmic, 12_000L)
        assertEquals(8_000L, plan.durationMs)
    }

    @Test fun boundsOverlapByRemainingTrackTime() {
        val dense = List(80) { 0.25f }
        val plan = SmartTransitionPlanner.plan(dense, dense, 5_000L)
        assertTrue(plan.durationMs <= 4_500L)
    }
}
