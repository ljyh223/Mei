package com.ljyh.mei.ui.component.player.component.applemusic

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal data class PortraitMotionFrame(val image: ImageBitmap, val edgeColor: Color)

private const val MotionBlendAlpha = 0.7f

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

/** Holds the tiny strip copied from the visible video; only readers of [frame] recompose. */
internal class PortraitMotionBackdropState {
    var frame by mutableStateOf<PortraitMotionFrame?>(null)
        private set

    fun accept(bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        frame = PortraitMotionFrame(
            image = bitmap.asImageBitmap(),
            edgeColor = averageMotionColor(pixels)
        )
    }
}

@Composable
internal fun PortraitMotionBackdrop(state: PortraitMotionBackdropState, fallbackColor: Color) {
    val sample = state.frame
    Box(Modifier.fillMaxSize().background(fallbackColor)) {
        if (sample != null) {
            Image(
                bitmap = sample.image,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                alpha = MotionBlendAlpha,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.58f)
                    .blur(64.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
        }
    }
}

@Composable
internal fun PortraitMotionEdgeGradient(state: PortraitMotionBackdropState, fallbackColor: Color) {
    val edgeColor = state.frame?.edgeColor?.let { lerp(fallbackColor, it, MotionBlendAlpha) } ?: fallbackColor
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.22f),
                0.16f to Color.Transparent,
                0.55f to Color.Transparent,
                1f to edgeColor,
            )
        )
    )
}
