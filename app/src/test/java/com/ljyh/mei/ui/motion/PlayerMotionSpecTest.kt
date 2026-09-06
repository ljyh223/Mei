package com.ljyh.mei.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlayerMotionSpecTest {
    @Test
    fun progressWindowClampsAndNormalizes() {
        val window = MotionProgressWindow(start = 0.25f, end = 0.5f)

        assertEquals(0f, window.transform(0.1f))
        assertEquals(0f, window.transform(0.25f))
        assertEquals(0.5f, window.transform(0.375f))
        assertEquals(1f, window.transform(0.5f))
        assertEquals(1f, window.transform(0.8f))
    }

    @Test
    fun reverseProgressMirrorsNormalizedValue() {
        val window = MotionProgressWindow(start = 0f, end = 0.2f)

        assertEquals(1f, window.reverse(0f))
        assertEquals(0.5f, window.reverse(0.1f))
        assertEquals(0f, window.reverse(0.2f))
    }

    @Test
    fun progressWindowRejectsEmptyOrReversedRanges() {
        assertThrows(IllegalArgumentException::class.java) {
            MotionProgressWindow(start = 0.5f, end = 0.5f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MotionProgressWindow(start = 0.5f, end = 0.25f)
        }
    }
}
