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
    fun `light palette is toned toward motion player gray`() {
        val color = motionControlsColor(Color.White, null)
        assertTrue(color.luminance() in 0.35f..0.38f)
    }

    @Test
    fun `bright artwork keeps a medium control background with a dark video edge`() {
        val color = motionControlsColor(Color.White, Color(0xff223060))
        assertTrue(color.luminance() in 0.32f..0.38f)
        assertTrue(color.blue > color.red)
    }

    @Test
    fun `dark motion edge retains its tint`() {
        val color = motionControlsColor(Color(0xff444444), Color(0xff152a56))
        assertTrue(color.blue > color.red)
        assertTrue(color.luminance() <= 0.18f)
    }
}
