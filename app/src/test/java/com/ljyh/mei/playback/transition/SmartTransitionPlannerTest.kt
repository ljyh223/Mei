package com.ljyh.mei.playback.transition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartTransitionPlannerTest {

    private val rhythmic500 = List(120) { if (it % 5 == 0) 0.4f else 0.12f }
    private val rhythmic800 = List(120) { if (it % 8 == 0) 0.4f else 0.12f }

    @Test
    fun refusesWhenNothingWasMeasured() {
        assertNull(SmartTransitionPlanner.plan(emptyList(), emptyList(), 10_000L))
    }

    @Test
    fun refusesWhenOutgoingAlreadyFadesToSilence() {
        val outro = List(80) { 0.001f }
        assertNull(SmartTransitionPlanner.plan(outro, rhythmic500, 9_000L))
    }

    @Test
    fun quietOutroGetsShortFadeAndSkipsIntroSilence() {
        val outro = List(80) { 0.01f }
        val intro = List(15) { 0f } + List(40) { 0.09f }
        val plan = SmartTransitionPlanner.plan(outro, intro, 9_000L)
        assertNotNull(plan)
        assertEquals(3_000L, plan!!.durationMs)
        assertEquals(1_300L, plan.incomingStartMs)
    }

    @Test
    fun compatiblePulseUsesFourBars() {
        assertEquals(500L, SmartTransitionPlanner.estimateBeatMs(rhythmic500))
        val plan = SmartTransitionPlanner.plan(rhythmic500, rhythmic500, 12_000L)
        assertNotNull(plan)
        assertEquals(8_000L, plan!!.durationMs)
    }

    @Test
    fun incompatibleTempoIsRefused() {
        assertEquals(800L, SmartTransitionPlanner.estimateBeatMs(rhythmic800))
        assertNull(SmartTransitionPlanner.plan(rhythmic500, rhythmic800, 12_000L))
    }

    @Test
    fun denseMaterialWithoutPulseIsRefused() {
        val dense = List(80) { 0.25f }
        assertNull(SmartTransitionPlanner.plan(dense, dense, 12_000L))
    }

    @Test
    fun boundsOverlapByRemainingTrackTime() {
        val plan = SmartTransitionPlanner.plan(rhythmic500, rhythmic500, 5_000L)
        assertNotNull(plan)
        assertTrue(plan!!.durationMs <= 4_500L)
    }

    @Test
    fun refusesWhenLessThanTwoSecondsRemain() {
        assertNull(SmartTransitionPlanner.plan(rhythmic500, rhythmic500, 2_000L))
    }
}
