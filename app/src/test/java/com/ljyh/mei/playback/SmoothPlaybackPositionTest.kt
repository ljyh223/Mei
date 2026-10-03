package com.ljyh.mei.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class SmoothPlaybackPositionTest {
    @Test
    fun fillsQuarterSecondGapsWithoutJumpingWhenPlayerUpdates() {
        val clock = SmoothPlaybackPosition()
        assertEquals(1_000, clock.sample(1_000, 0, true, 1f, 10_000))
        assertEquals(1_011, clock.sample(1_000, 11_000_000, true, 1f, 10_000))
        assertEquals(1_249, clock.sample(1_000, 249_000_000, true, 1f, 10_000))
        assertEquals(1_250, clock.sample(1_250, 250_000_000, true, 1f, 10_000))
        assertEquals(1_500, clock.sample(1_500, 500_000_000, true, 1f, 10_000))
    }

    @Test
    fun respectsPauseSeekSpeedAndStalledPosition() {
        val clock = SmoothPlaybackPosition()
        clock.sample(1_000, 0, true, 2f, 10_000)
        assertEquals(1_200, clock.sample(1_000, 100_000_000, true, 2f, 10_000))
        assertEquals(1_200, clock.sample(1_200, 110_000_000, false, 2f, 10_000))
        assertEquals(1_200, clock.sample(1_200, 300_000_000, false, 2f, 10_000))
        clock.reset()
        assertEquals(5_000, clock.sample(5_000, 310_000_000, true, 1f, 10_000))
        assertEquals(5_320, clock.sample(5_000, 710_000_000, true, 1f, 10_000))
    }
}
