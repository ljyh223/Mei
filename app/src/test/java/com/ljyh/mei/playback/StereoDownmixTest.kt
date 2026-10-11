package com.ljyh.mei.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class StereoDownmixTest {
    @Test
    fun stereoIsUnchanged() {
        val matrix = stereoDownmixMatrix(2)
        assertEquals(1f, matrix.getMixingCoefficient(0, 0))
        assertEquals(0f, matrix.getMixingCoefficient(0, 1))
        assertEquals(0f, matrix.getMixingCoefficient(1, 0))
        assertEquals(1f, matrix.getMixingCoefficient(1, 1))
    }

    @Test
    fun surroundCenterAndRearReachStereo() {
        val matrix = stereoDownmixMatrix(6)
        assertEquals(2, matrix.outputChannelCount)
        assertEquals(0.7071f, matrix.getMixingCoefficient(2, 0))
        assertEquals(0.7071f, matrix.getMixingCoefficient(2, 1))
        assertEquals(0.7071f, matrix.getMixingCoefficient(4, 0))
        assertEquals(0f, matrix.getMixingCoefficient(4, 1))
        assertEquals(0f, matrix.getMixingCoefficient(5, 0))
        assertEquals(0.7071f, matrix.getMixingCoefficient(5, 1))
    }
}
