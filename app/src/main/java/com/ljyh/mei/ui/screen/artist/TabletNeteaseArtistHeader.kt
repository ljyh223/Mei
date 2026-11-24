package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.shimmer.skeleton

/** A profile header for ordinary NetEase avatars, aligned with the catalogue below. */
@Composable
internal fun TabletNeteaseArtistHeader(
    state: ArtistDetailUiState,
    actions: ArtistDetailActions,
    onInfo: () -> Unit,
    topPadding: Dp,
    artworkModifier: Modifier,
) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 960.dp).fillMaxWidth()
            .padding(meiPageHorizontalPadding()).padding(top = topPadding, bottom = 24.dp)) {
            val stacked = maxWidth < 640.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
            val portraitSize = if (stacked) (maxWidth * 0.42f).coerceIn(160.dp, 220.dp)
                else (maxWidth * 0.28f).coerceIn(200.dp, 260.dp)
            if (stacked) {
                Column(Modifier.fillMaxWidth().then(artworkModifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    NeteaseArtistPortrait(state, Modifier.size(portraitSize))
                    NeteaseArtistIdentity(state, actions, onInfo, centered = true)
                }
            } else {
                Row(Modifier.fillMaxWidth().then(artworkModifier),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    NeteaseArtistPortrait(state, Modifier.size(portraitSize))
                    Column(Modifier.weight(1f)) {
                        NeteaseArtistIdentity(state, actions, onInfo, centered = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun NeteaseArtistIdentity(
    state: ArtistDetailUiState,
    actions: ArtistDetailActions,
    onInfo: () -> Unit,
    centered: Boolean,
) {
    Column(Modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(state.artist?.name ?: if (state.detailLoading) "正在加载" else "歌手",
            modifier = Modifier.fillMaxWidth().skeleton(state.detailLoading).semantics { heading() },
            style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (state.artist != null) ArtistPrimaryActions(state.canPlay && state.songs.isNotEmpty(),
            state.subscription, onInfo, actions.onPlayHotSongs, actions.onToggleSubscription)
    }
}

@Composable
private fun NeteaseArtistPortrait(state: ArtistDetailUiState, modifier: Modifier) {
    val artist = state.artist
    val sources = remember(artist?.avatar, artist?.cover) {
        listOfNotNull(artist?.avatar?.takeIf(String::isNotBlank), artist?.cover?.takeIf(String::isNotBlank)).distinct()
    }
    var sourceIndex by remember(state.id, sources) { mutableIntStateOf(0) }
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        if (sources.isEmpty()) TabletArtistPhotoPlaceholder(state.detailLoading)
        else key(sources[sourceIndex]) {
            SubcomposeAsyncImage(model = sources[sourceIndex], contentDescription = "歌手照片",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) {
                val result by painter.state.collectAsState()
                LaunchedEffect(result is AsyncImagePainter.State.Error) {
                    if (result is AsyncImagePainter.State.Error && sourceIndex < sources.lastIndex) sourceIndex++
                }
                if (result is AsyncImagePainter.State.Success) SubcomposeAsyncImageContent()
                else TabletArtistPhotoPlaceholder(result !is AsyncImagePainter.State.Error)
            }
        }
    }
}
