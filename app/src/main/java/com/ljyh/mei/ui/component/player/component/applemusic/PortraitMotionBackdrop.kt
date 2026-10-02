package com.ljyh.mei.ui.component.player.component.applemusic

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal data class PortraitMotionFrame(val image: ImageBitmap, val edgeColor: Color)

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
internal fun PortraitMotionBackdrop(state: PortraitMotionBackdropState, artworkBottomFraction: Float) {
    val sample = state.frame ?: return
    val end = artworkBottomFraction.coerceIn(0f, 1f)
    val start = end * 0.72f
    Image(
        bitmap = sample.image,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize()
            .blur(64.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        start to Color.Transparent,
                        end to Color.Black,
                        1f to Color.Black,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
    )
}

@Composable
internal fun PortraitMotionEdgeGradient(state: PortraitMotionBackdropState, fallbackColor: Color) {
    val bottomColor = if (state.frame == null) fallbackColor else Color.Transparent
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.22f),
                0.16f to Color.Transparent,
                0.55f to Color.Transparent,
                1f to bottomColor,
            )
        )
    )
}
