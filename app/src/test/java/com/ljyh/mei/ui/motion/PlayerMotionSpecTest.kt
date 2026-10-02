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
    fun bottomNavigationReturnsBeforeTheSheetSettles() {
        val exit = PlayerMotionSpec.BottomNavigationExit

        assertEquals(1f, exit.reverse(0f))
        assertEquals(1f, exit.reverse(0.04f))
        assertEquals(0.5f, exit.reverse(0.13f), 0.0001f)
        assertEquals(0f, exit.reverse(0.22f))
        assertEquals(0f, exit.reverse(1f))
    }

    @Test
    fun playerBackgroundExpansionKeepsItsOwnTiming() {
        val expansion = PlayerMotionSpec.PlayerBackgroundExpansion

        assertEquals(0f, expansion.transform(0f))
        assertEquals(0.5f, expansion.transform(0.09f))
        assertEquals(1f, expansion.transform(0.18f))
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
