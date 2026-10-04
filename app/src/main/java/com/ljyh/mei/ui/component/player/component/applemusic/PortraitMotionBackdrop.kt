package com.ljyh.mei.ui.component.player.component.applemusic

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

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
    val sampledColor = edgeColor?.let { lerp(paletteColor, it, 0.65f) } ?: paletteColor
    val brightness = sampledColor.luminance()
    if (brightness <= 0.18f) return sampledColor

    // A light previewFrame palette often describes the artwork, not the darker area behind
    // Apple's white playback controls. Preserve the hue and lower only its brightness.
    var low = 0f
    var high = 1f
    repeat(8) {
        val fraction = (low + high) / 2f
        if (lerp(Color.Black, sampledColor, fraction).luminance() > 0.18f) {
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
    var frame by mutableStateOf<ImageBitmap?>(null)
        private set

    fun accept(bitmap: Bitmap) {
        val rows = minOf(3, bitmap.height)
        val pixels = IntArray(bitmap.width * rows)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, bitmap.height - rows, bitmap.width, rows)
        edgeColor = averageMotionColor(pixels)
        frame = bitmap.asImageBitmap()
    }
}

/** A blurred copy of the current video softly takes over where its sharp lower edge fades. */
@Composable
internal fun PortraitMotionBlur(frame: ImageBitmap, modifier: Modifier = Modifier) {
    Image(
        bitmap = frame,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                val mask = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.54f to Color.Transparent,
                    0.77f to Color.White.copy(alpha = 0.82f),
                    1f to Color.Transparent,
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(mask, blendMode = BlendMode.DstIn)
                }
            }
            .blur(28.dp),
    )
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
