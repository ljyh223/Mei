package com.ljyh.mei.ui.component.player.cover

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.Painter
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

/** Retains the last drawn artwork underneath the next request until it succeeds. */
@Composable
internal fun PlayerArtworkImage(
    artworkKey: String,
    request: ImageRequest?,
    modifier: Modifier = Modifier,
    onSuccess: () -> Unit = {},
    onError: () -> Unit = {},
) {
    var lastArtwork by remember { mutableStateOf<Pair<String, Painter>?>(null) }
    Box(modifier.clipToBounds()) {
        val previous = lastArtwork
        if (previous != null && (previous.first != artworkKey || request == null)) {
            Image(
                painter = previous.second,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().fillArtworkEdges(),
            )
        }
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { result ->
                    lastArtwork = artworkKey to result.painter
                    onSuccess()
                },
                onError = {
                    lastArtwork = null
                    onError()
                },
                modifier = Modifier.fillMaxSize().fillArtworkEdges(),
            )
        }
    }
}

/**
 * Keep the bitmap's sampling boundary outside the final clip, including while the cover scales.
 * One drawing pixel per edge is sufficient; the layout and shared cover coordinates stay intact.
 */
private fun Modifier.fillArtworkEdges(): Modifier = drawWithContent {
    if (size.width > 0f && size.height > 0f) {
        scale(
            scaleX = (size.width + 2f) / size.width,
            scaleY = (size.height + 2f) / size.height,
        ) {
            this@drawWithContent.drawContent()
        }
    }
}
