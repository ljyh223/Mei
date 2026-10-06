package com.ljyh.mei.ui.component.player.component.applemusic

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortraitMotionBackdropTest {
    @Test
    fun `gray mask starts only when most sampled pixels are nearly white`() {
        val white = 0xfff0f0f0.toInt()
        val red = 0xffff8080.toInt()
        assertTrue(shouldUseGrayMask(IntArray(6) { white } + IntArray(4) { red }, false))
        assertTrue(!shouldUseGrayMask(IntArray(4) { white } + IntArray(6) { red }, false))
        assertTrue(!shouldUseGrayMask(IntArray(10) { 0xffe9dfbf.toInt() }, false))
    }

    @Test
    fun `gray mask stays stable near the white threshold`() {
        val white = 0xffffffff.toInt()
        val dark = 0xff222222.toInt()
        assertTrue(shouldUseGrayMask(IntArray(4) { white } + IntArray(6) { dark }, true))
        assertTrue(!shouldUseGrayMask(IntArray(3) { white } + IntArray(7) { dark }, true))
    }

    @Test
    fun `color follows video unless white mask is active`() {
        val red = Color(0xFFE99494)
        assertEquals(red, motionControlBackground(red, false))
        val whiteBackground = motionControlBackground(Color.White, true)
        assertEquals(whiteBackground.red, whiteBackground.green, 0.01f)
        assertTrue(whiteBackground.red < 0.7f)
    }

    @Test
    fun `video edge average preserves both dominant colors`() {
        val color = averageMotionColor(intArrayOf(0xffff0000.toInt(), 0xff0000ff.toInt()))
        assertEquals(0.5f, color.red, 0.01f)
        assertEquals(0f, color.green, 0.01f)
        assertEquals(0.5f, color.blue, 0.01f)
    }
}
