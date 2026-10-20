package com.ljyh.mei.playback.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopLyricsWindowGeometryTest {
    @Test
    fun windowCanReachAllFourScreenCorners() {
        for ((x, y) in listOf(0 to 0, 180 to 0, 0 to 1_900, 180 to 1_900)) {
            assertEquals(
                DesktopLyricsWindowGeometry(width = 900, height = 500, x = x, y = y),
                desktopLyricsWindowGeometry(1_080, 2_400, 900, 500, x, y),
            )
        }
    }

    @Test
    fun draggingBeyondScreenKeepsWindowReachable() {
        assertEquals(
            DesktopLyricsWindowGeometry(900, 500, 0, 0),
            desktopLyricsWindowGeometry(1_080, 2_400, 900, 500, -400, -800),
        )
        assertEquals(
            DesktopLyricsWindowGeometry(900, 500, 180, 1_900),
            desktopLyricsWindowGeometry(1_080, 2_400, 900, 500, 2_000, 3_000),
        )
    }

    @Test
    fun enlargingWindowImmediatelyReclampsBottomRightPosition() {
        val original = desktopLyricsWindowGeometry(1_080, 2_400, 600, 300, 480, 2_100)
        assertEquals(
            DesktopLyricsWindowGeometry(900, 500, 180, 1_900),
            desktopLyricsWindowGeometry(1_080, 2_400, 900, 500, original.x, original.y),
        )
    }

    @Test
    fun rotatingScreenRecoversSavedPositionInsideNewBounds() {
        val landscape = desktopLyricsWindowGeometry(2_400, 1_080, 900, 500, 1_500, 580)
        assertEquals(
            DesktopLyricsWindowGeometry(900, 500, 180, 580),
            desktopLyricsWindowGeometry(1_080, 2_400, 900, 500, landscape.x, landscape.y),
        )
    }

    @Test
    fun oversizedWindowFitsSmallDisplay() {
        assertEquals(
            DesktopLyricsWindowGeometry(720, 320, 0, 0),
            desktopLyricsWindowGeometry(720, 320, 900, 500, 50, 200),
        )
    }
}
