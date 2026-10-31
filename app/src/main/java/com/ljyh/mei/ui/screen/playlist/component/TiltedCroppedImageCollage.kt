package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val RotationDegrees = -30f
private const val CollageHeightRatio = 0.75f

/** Scale the rotated 1 × 0.75 collage until all four viewport corners are covered. */
internal fun collageCoverScale(viewportWidth: Float, viewportHeight: Float): Float {
    if (viewportWidth <= 0f || viewportHeight <= 0f) return 1f
    val angle = Math.toRadians(RotationDegrees.toDouble())
    val cosine = abs(cos(angle)).toFloat()
    val sine = abs(sin(angle)).toFloat()
    val widthScale = (viewportWidth * cosine + viewportHeight * sine) / viewportWidth
    val heightScale = (viewportWidth * sine + viewportHeight * cosine) /
        (viewportWidth * CollageHeightRatio)
    // A small overscan covers pixel rounding at the outer edge.
    return max(widthScale, heightScale) + 0.02f
}

@Composable
fun FinalPerfectCollage(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val scale = collageCoverScale(maxWidth.value, maxHeight.value)
        ImageCollageContent(
            imageUrls = imageUrls,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().graphicsLayer {
                rotationZ = RotationDegrees
                scaleX = scale
                scaleY = scale
            },
        )
    }
}
