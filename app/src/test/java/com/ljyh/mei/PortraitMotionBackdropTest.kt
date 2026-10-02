package com.ljyh.mei.ui.component.player.component.applemusic

import org.junit.Assert.assertEquals
import org.junit.Test

class PortraitMotionBackdropTest {
    @Test
    fun `two dominant frame colors retain both channels in the transition color`() {
        val color = averageMotionColor(intArrayOf(0xffff0000.toInt(), 0xff0000ff.toInt()))
        assertEquals(0.5f, color.red, 0.01f)
        assertEquals(0f, color.green, 0.01f)
        assertEquals(0.5f, color.blue, 0.01f)
    }
}
