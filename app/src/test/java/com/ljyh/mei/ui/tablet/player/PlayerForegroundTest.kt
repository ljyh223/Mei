package com.ljyh.mei.ui.tablet.player

import androidx.compose.ui.graphics.Color
import com.ljyh.mei.ui.component.player.playerForegroundForBackground
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerForegroundTest {
    @Test fun brightCoversUseDarkInkAndUnloadedOrDarkCoversUseWhite() {
        assertEquals(Color.Black, playerForegroundForBackground(Color.White))
        assertEquals(Color.Black, playerForegroundForBackground(Color(0xFFE8DAD4)))
        assertEquals(Color.White, playerForegroundForBackground(Color.Black))
        assertEquals(Color.White, playerForegroundForBackground(Color(0xFF303040)))
        assertEquals(Color.White, playerForegroundForBackground(null))
    }
}
