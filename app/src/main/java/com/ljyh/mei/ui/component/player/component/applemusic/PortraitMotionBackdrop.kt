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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

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

/** Keep the controls connected to the video while retaining contrast for white labels. */
internal fun motionControlsColor(paletteColor: Color, edgeColor: Color?): Color {
    val edgeWeight = if (paletteColor.luminance() > 0.6f) 0.2f else 0.45f
    val sampledColor = edgeColor?.let { lerp(paletteColor, it, edgeWeight) } ?: paletteColor
    val brightness = sampledColor.luminance()
    if (brightness <= 0.38f) return sampledColor

    // Keep light artwork palettes near the medium gray used behind Apple's motion controls.
    var low = 0f
    var high = 1f
    repeat(8) {
        val fraction = (low + high) / 2f
        if (lerp(Color.Black, sampledColor, fraction).luminance() > 0.38f) {
            high = fraction
        } else {
            low = fraction
        }
    }
    return lerp(Color.Black, sampledColor, low)
}

/** Holds the video edge color; only readers of [edgeColor] recompose. */
internal class PortraitMotionBackdropState {
    var edgeColor by mutableStateOf<Color?>(null)
        private set

    fun accept(bitmap: Bitmap) {
        val rows = minOf(3, bitmap.height)
        val pixels = IntArray(bitmap.width * rows)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, bitmap.height - rows, bitmap.width, rows)
        edgeColor = averageMotionColor(pixels)
        bitmap.recycle()
    }
}

/** Diffuse the video's sampled edge color without scaling a frame into a visible blue shape. */
@Composable
internal fun PortraitMotionColorWash(color: Color, artworkHeight: Dp, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Box(modifier.drawBehind {
        val center = Offset(size.width / 2f, with(density) { artworkHeight.toPx() })
        drawRect(
            brush = Brush.radialGradient(
                0f to color.copy(alpha = 0.70f),
                0.38f to color.copy(alpha = 0.34f),
                1f to Color.Transparent,
                center = center,
                radius = size.width * 0.52f,
            ),
        )
    })
}

@Composable
internal fun PortraitMotionTopScrim() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.56f),
                0.2f to Color.Transparent,
                1f to Color.Transparent,
            )
        )
    )
}
