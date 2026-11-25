package com.ljyh.mei.ui.screen.album

import android.os.Build

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.Image
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.ljyh.mei.ui.component.artwork.backdrop.ArtworkReflection
import com.ljyh.mei.ui.component.artwork.backdrop.artworkBackdropEffects
import com.ljyh.mei.ui.component.shimmer.skeleton
import kotlinx.coroutines.CancellationException

/** Decode once for the clear square, reflection, variable blur, and shared palette extraction. */
@Composable
internal fun AlbumStaticArtwork(state: AlbumDetailUiState, actions: AlbumDetailActions, modifier: Modifier) {
    val url = state.album.picUrl
    val report by rememberUpdatedState(actions.onStaticArtworkChanged)
    var sampled by remember(url) { mutableStateOf<Image?>(null) }
    SubcomposeAsyncImage(model = url, contentDescription = "专辑《${state.album.name}》封面",
        modifier = modifier, contentScale = ContentScale.Crop) {
        val result by painter.state.collectAsState()
        LaunchedEffect(result, url) {
            val decoded = (result as? AsyncImagePainter.State.Success)?.result?.image
            if (decoded == null || decoded !== sampled) {
                try {
                    report(url, decoded)
                    sampled = decoded
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Palette failure keeps the decoded image and neutral fallback. */ }
            }
        }
        when (val image = result) {
            is AsyncImagePainter.State.Success -> if (Build.VERSION.SDK_INT >= 33) {
                ArtworkReflection(image.painter,
                    Modifier.fillMaxSize().artworkBackdropEffects({ 0f }, clearHeightFraction = .6f,
                        fadeStartFraction = .9f, fadeEndFraction = 1.4f), frameAspectRatio = 1f)
            } else SubcomposeAsyncImageContent(contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
            is AsyncImagePainter.State.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Album, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> Box(Modifier.fillMaxSize().skeleton(true))
        }
    }
}
