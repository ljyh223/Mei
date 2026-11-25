package com.ljyh.mei.ui.component.artwork.backdrop

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale

/** A single decoded image serves the clear frame and its vertically stretched reflection. */
@Composable
internal fun ArtworkReflection(painter: Painter, modifier: Modifier, frameAspectRatio: Float = 2f) {
    Canvas(modifier) {
        val frame = Size(size.width, size.width / frameAspectRatio)
        drawArtworkFrame(painter, frame)
        withTransform({
            translate(0f, frame.height * 4f)
            scale(1f, -3f, Offset.Zero)
        }) { drawArtworkFrame(painter, frame) }
    }
}

private fun DrawScope.drawArtworkFrame(painter: Painter, frame: Size) {
    val intrinsic = painter.intrinsicSize.takeIf { it.width > 0 && it.height > 0 } ?: frame
    val scale = ContentScale.Crop.computeScaleFactor(intrinsic, frame)
    val image = Size(intrinsic.width * scale.scaleX, intrinsic.height * scale.scaleY)
    clipRect(0f, 0f, frame.width, frame.height) {
        withTransform({ translate((frame.width - image.width) / 2f, (frame.height - image.height) / 2f) }) {
            with(painter) { draw(image) }
        }
    }
}
