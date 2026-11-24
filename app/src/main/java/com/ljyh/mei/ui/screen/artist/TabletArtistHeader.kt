package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.shimmer.skeleton

@Composable
internal fun TabletArtistHeader(
    state: ArtistDetailUiState,
    artwork: AppleArtistArtwork?,
    heroHeight: Dp,
    profileTopPadding: Dp,
    actions: ArtistDetailActions,
    onInfo: () -> Unit,
    onPresentationChanged: (ArtistArtworkPresentation) -> Unit,
    artworkModifier: Modifier,
) {
    val report by rememberUpdatedState(onPresentationChanged)
    val fallback: @Composable () -> Unit = {
        TabletNeteaseArtistHeader(state, actions, onInfo, profileTopPadding, artworkModifier)
    }
    if (artwork == null) {
        LaunchedEffect(state.id) { report(ArtistArtworkPresentation()) }
        fallback()
    } else TabletAppleArtistHeader(state, artwork, heroHeight, profileTopPadding, actions, onInfo,
        onPresentationChanged, artworkModifier, fallback)
}

@Composable
private fun TabletAppleArtistHeader(
    state: ArtistDetailUiState,
    artwork: AppleArtistArtwork,
    heroHeight: Dp,
    topPadding: Dp,
    actions: ArtistDetailActions,
    onInfo: () -> Unit,
    onPresentationChanged: (ArtistArtworkPresentation) -> Unit,
    artworkModifier: Modifier,
    fallback: @Composable () -> Unit,
) {
    val banner = artwork.banner?.takeIf { it.width.toFloat() / it.height in 1.4f..3f }
    var bannerFailed by remember(artwork) { mutableStateOf(false) }
    val picture = if (bannerFailed) artwork.portrait else banner ?: artwork.portrait
    var wordmark by remember(state.id, artwork, picture.url) { mutableStateOf(
        if (artwork.logo == null) ArtistArtworkLoadState.Unavailable else ArtistArtworkLoadState.Loading,
    ) }
    val report by rememberUpdatedState(onPresentationChanged)
    SubcomposeAsyncImage(model = picture.url, contentDescription = "歌手照片", modifier = Modifier.fillMaxWidth()) image@{
        val result by painter.state.collectAsState()
        val status = result.artistArtworkLoadState()
        LaunchedEffect(status, wordmark, picture.url) {
            report(ArtistArtworkPresentation(status, wordmark))
            if (status == ArtistArtworkLoadState.Error && picture == banner) bannerFailed = true
        }
        if (status != ArtistArtworkLoadState.Success) fallback() else {
            val background = MaterialTheme.colorScheme.background
            Box(Modifier.fillMaxWidth().heightIn(min = heroHeight).then(artworkModifier)
                .clipToBounds().background(background)) {
                BoxWithConstraints(Modifier.matchParentSize()) {
                    val wide = picture == banner
                    val ratio = picture.width.toFloat() / picture.height
                    val imageWidth = if (wide) maxWidth else (maxHeight * ratio).coerceAtMost(maxWidth)
                    Box(Modifier.align(Alignment.TopCenter).width(imageWidth).fillMaxHeight()) {
                        this@image.SubcomposeAsyncImageContent(Modifier.fillMaxSize(),
                            contentScale = if (wide) ContentScale.Crop else ContentScale.Fit,
                            alignment = Alignment.TopCenter)
                        if (!wide) Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                            0f to background, 0.12f to Color.Transparent,
                            0.88f to Color.Transparent, 1f to background,
                        )))
                    }
                }
                // Preserve the face and torso; only the lower section becomes the text backdrop.
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.22f),
                    0.22f to Color.Transparent, 0.48f to Color.Transparent,
                    0.62f to background.copy(alpha = 0.32f),
                    0.76f to background.copy(alpha = 0.88f), 0.94f to background, 1f to background,
                )))
                Column(Modifier.align(Alignment.BottomCenter).widthIn(max = 376.dp).fillMaxWidth()
                    .padding(meiPageHorizontalPadding())
                    .padding(top = topPadding + heroHeight * 0.42f, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ArtistWordmark(artwork, state.artist?.name ?: artwork.name, compact = true) { wordmark = it }
                    if (state.artist != null) ArtistPrimaryActions(state.canPlay && state.songs.isNotEmpty(),
                        state.subscription, onInfo, actions.onPlayHotSongs, actions.onToggleSubscription)
                }
            }
        }
    }
}

@Composable
internal fun TabletArtistPhotoPlaceholder(loading: Boolean) {
    SkeletonShimmerHost(loading, Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center) {
            if (loading) Spacer(Modifier.matchParentSize().skeleton(true, RoundedCornerShape(0.dp)))
            else Icon(Icons.Rounded.Person, null, Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
