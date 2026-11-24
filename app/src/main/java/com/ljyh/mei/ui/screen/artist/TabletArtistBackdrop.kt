package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import com.ljyh.mei.ui.screen.artist.component.artistBackdropEffects

/** A page-owned photographic background. Coil decodes once; the same painter draws its reflection. */
@Composable
internal fun TabletArtistBackdrop(
    artwork: AppleArtistArtwork,
    scrollPixels: () -> Float,
    onPhotoChanged: (ArtistArtworkLoadState, String?) -> Unit,
) {
    val banner = artwork.banner?.takeIf { it.width.toFloat() / it.height in 1.4f..3f }
    var bannerFailed by remember(artwork) { mutableStateOf(false) }
    val picture = if (bannerFailed) artwork.portrait else banner ?: artwork.portrait
    val report by rememberUpdatedState(onPhotoChanged)
    SubcomposeAsyncImage(model = picture.url, contentDescription = null, modifier = Modifier.fillMaxSize()) {
        val result by painter.state.collectAsState()
        val status = result.artistArtworkLoadState()
        LaunchedEffect(status, picture.url) {
            report(status, picture.background)
            if (status == ArtistArtworkLoadState.Error && picture == banner) bannerFailed = true
        }
        if (status == ArtistArtworkLoadState.Success) {
            val imagePainter = painter
            val background = MaterialTheme.colorScheme.background
            val fit = picture.width.toFloat() / picture.height < 1.4f
            // MusicUI bounds the background to 2H = W. Do not filter invisible pixels
            // across the full height of a portrait tablet window.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxWidth().height(minOf(maxWidth, maxHeight))
                    .artistBackdropEffects(scrollPixels)) {
                    val frame = Size(size.width, size.width * 0.5f)
                    drawArtworkFrame(imagePainter, frame, fit, background)
                    // MusicUI.ReflectionContainerView uses scale(1, -3, 1) below the original.
                    withTransform({
                        translate(0f, frame.height * 4f)
                        scale(1f, -3f, Offset.Zero)
                    }) { drawArtworkFrame(imagePainter, frame, fit, background) }
                }
            }
        }
    }
}

private fun DrawScope.drawArtworkFrame(painter: Painter, frame: Size, fit: Boolean, background: Color) {
    val intrinsic = painter.intrinsicSize.takeIf { it.width > 0 && it.height > 0 } ?: frame
    val scale = (if (fit) ContentScale.Fit else ContentScale.Crop).computeScaleFactor(intrinsic, frame)
    val image = Size(intrinsic.width * scale.scaleX, intrinsic.height * scale.scaleY)
    clipRect(0f, 0f, frame.width, frame.height) {
        drawRect(background, size = frame)
        withTransform({ translate((frame.width - image.width) / 2f, (frame.height - image.height) / 2f) }) {
            with(painter) { draw(image) }
        }
        if (fit) {
            // An ordinary portrait is not a panoramic asset. Keep the face intact and soften
            // its side boundaries, rather than cropping it into a 2:1 strip or repeating faces.
            val left = (frame.width - image.width) / 2f
            drawRect(Brush.horizontalGradient(
                0f to background, 0.12f to Color.Transparent,
                0.88f to Color.Transparent, 1f to background,
                startX = left, endX = left + image.width,
            ), topLeft = Offset(left, (frame.height - image.height) / 2f), size = image)
        }
    }
}
