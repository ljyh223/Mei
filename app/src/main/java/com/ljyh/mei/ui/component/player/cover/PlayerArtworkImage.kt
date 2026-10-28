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
    Box(modifier) {
        val previous = lastArtwork
        if (previous != null && (previous.first != artworkKey || request == null)) {
            Image(
                painter = previous.second,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
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
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
