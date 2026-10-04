package com.ljyh.mei.ui.component.player.component.applemusic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortraitMotionBackdropTest {
    @Test
    fun `two dominant frame colors retain both channels in the transition color`() {
        val color = averageMotionColor(intArrayOf(0xffff0000.toInt(), 0xff0000ff.toInt()))
        assertEquals(0.5f, color.red, 0.01f)
        assertEquals(0f, color.green, 0.01f)
        assertEquals(0.5f, color.blue, 0.01f)
    }

    @Test
    fun `light palette is darkened enough for white controls`() {
        val color = motionControlsColor(Color.White, null)
        assertTrue(color.luminance() <= 0.18f)
    }

    @Test
    fun `dark motion edge retains its tint`() {
        val color = motionControlsColor(Color(0xff444444), Color(0xff152a56))
        assertTrue(color.blue > color.red)
        assertTrue(color.luminance() <= 0.18f)
    }
}
