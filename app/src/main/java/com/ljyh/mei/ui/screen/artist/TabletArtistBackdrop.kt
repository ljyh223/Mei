package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import com.ljyh.mei.data.repository.artwork.AppleArtistImage
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import com.ljyh.mei.ui.screen.artist.component.artistBackdropEffects
import com.ljyh.mei.ui.screen.artist.component.ArtistColorBackdrop

internal enum class TabletArtistPhotoKind { Hero, MotionPreview, Portrait }

internal data class TabletArtistPhoto(val image: AppleArtistImage, val kind: TabletArtistPhotoKind) {
    val hasHero: Boolean get() = kind != TabletArtistPhotoKind.Portrait
}

/** Store promos are not hero photos. A failed candidate advances without reloading on scroll. */
internal fun tabletArtistPhotos(artwork: AppleArtistArtwork): List<TabletArtistPhoto> = buildList {
    artwork.motionPreview?.takeIf { it.width.toFloat() / it.height in 1.4f..3f }?.let {
        add(TabletArtistPhoto(it, TabletArtistPhotoKind.MotionPreview))
    }
    artwork.banner?.takeIf { it.width.toFloat() / it.height in 1.4f..3f }?.let {
        add(TabletArtistPhoto(it, TabletArtistPhotoKind.Hero))
    }
    add(TabletArtistPhoto(artwork.portrait, TabletArtistPhotoKind.Portrait))
}.distinctBy { it.image.url }

/** A page-owned photographic background. Coil decodes once; the same painter draws its reflection. */
@Composable
internal fun TabletArtistBackdrop(
    artwork: AppleArtistArtwork,
    scrollPixels: () -> Float,
    onPhotoChanged: (ArtistArtworkLoadState, TabletArtistPhoto, Painter?) -> Unit,
) {
    val photos = remember(artwork) { tabletArtistPhotos(artwork) }
    var photoIndex by remember(artwork) { mutableIntStateOf(0) }
    val photo = photos[photoIndex]
    val picture = photo.image
    val report by rememberUpdatedState(onPhotoChanged)
    SubcomposeAsyncImage(model = picture.url, contentDescription = null, modifier = Modifier.fillMaxSize()) {
        val result by painter.state.collectAsState()
        val status = result.artistArtworkLoadState()
        val decoded = (result as? AsyncImagePainter.State.Success)?.painter
        LaunchedEffect(result, photo) {
            report(status, photo, decoded)
            if (status == ArtistArtworkLoadState.Error && photoIndex < photos.lastIndex) photoIndex++
        }
        if (status == ArtistArtworkLoadState.Success) {
            val imagePainter = checkNotNull(decoded)
            if (!photo.hasHero) {
                ArtistColorBackdrop(imagePainter)
                return@SubcomposeAsyncImage
            }
            // MusicUI bounds the background to 2H = W. Do not filter invisible pixels
            // across the full height of a portrait tablet window.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxWidth().height(minOf(maxWidth, maxHeight))
                    .artistBackdropEffects(scrollPixels)) {
                    val frame = Size(size.width, size.width * 0.5f)
                    drawArtworkFrame(imagePainter, frame)
                    // MusicUI.ReflectionContainerView uses scale(1, -3, 1) below the original.
                    withTransform({
                        translate(0f, frame.height * 4f)
                        scale(1f, -3f, Offset.Zero)
                    }) { drawArtworkFrame(imagePainter, frame) }
                }
            }
        }
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
