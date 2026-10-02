package com.ljyh.mei.ui.component.player.component.applemusic

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal fun averageMotionColor(pixels: IntArray): Color {
    if (pixels.isEmpty()) return Color.Transparent
    var red = 0L
    var green = 0L
    var blue = 0L
    for (pixel in pixels) {
        red += (pixel shr 16) and 0xff
        green += (pixel shr 8) and 0xff
        blue += pixel and 0xff
    }
    return Color(
        red / pixels.size / 255f,
        green / pixels.size / 255f,
        blue / pixels.size / 255f
    )
}

/** Holds the video edge color; only readers of [edgeColor] recompose. */
internal class PortraitMotionBackdropState {
    var edgeColor by mutableStateOf<Color?>(null)
        private set

    fun accept(bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        bitmap.recycle()
        edgeColor = averageMotionColor(pixels)
    }
}

@Composable
internal fun PortraitMotionTopScrim() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.22f),
                0.16f to Color.Transparent,
                1f to Color.Transparent,
            )
        )
    )
}
