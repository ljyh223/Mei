package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImagePainter
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
    photoHeight: Dp,
    actions: ArtistDetailActions,
    onInfo: () -> Unit,
    onPresentationChanged: (ArtistArtworkPresentation) -> Unit,
    artworkModifier: Modifier,
) {
    var photo by remember(state.id, artwork) { mutableStateOf(
        if (artwork == null) ArtistArtworkLoadState.Unavailable else ArtistArtworkLoadState.Loading,
    ) }
    var wordmark by remember(state.id, artwork) { mutableStateOf(
        if (artwork?.logo == null) ArtistArtworkLoadState.Unavailable else ArtistArtworkLoadState.Loading,
    ) }
    val report by rememberUpdatedState(onPresentationChanged)
    LaunchedEffect(photo, wordmark, artwork) { report(ArtistArtworkPresentation(photo, wordmark)) }
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally) {
        val fallback: @Composable () -> Unit = {
            val artist = state.artist
            TabletNeteaseArtistPhoto(artist?.cover?.takeIf(String::isNotBlank) ?: artist?.avatar,
                state.detailLoading, photoHeight, artworkModifier)
        }
        if (artwork == null) fallback() else TabletAppleArtistPhoto(
            artwork, photoHeight, artworkModifier, onStateChanged = {
                photo = it
                if (it != ArtistArtworkLoadState.Success) wordmark = if (artwork.logo == null)
                    ArtistArtworkLoadState.Unavailable else ArtistArtworkLoadState.Loading
            }, fallback = fallback)
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(meiPageHorizontalPadding())
            .padding(top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            val name = state.artist?.name ?: if (state.detailLoading) "正在加载" else "歌手"
            if (artwork != null && photo == ArtistArtworkLoadState.Success) {
                ArtistWordmark(artwork, name, compact = false) { wordmark = it }
            } else {
                Text(name, modifier = Modifier.fillMaxWidth().skeleton(state.detailLoading),
                    style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (state.artist != null) ArtistPrimaryActions(state.canPlay && state.songs.isNotEmpty(),
                state.subscription, onInfo, actions.onPlayHotSongs, actions.onToggleSubscription)
        }
    }
}

@Composable
private fun TabletAppleArtistPhoto(
    artwork: AppleArtistArtwork,
    photoHeight: Dp,
    artworkModifier: Modifier,
    onStateChanged: (ArtistArtworkLoadState) -> Unit,
    fallback: @Composable () -> Unit,
) {
    val banner = artwork.banner?.takeIf { it.width.toFloat() / it.height in 1.4f..3f }
    var bannerFailed by remember(artwork) { mutableStateOf(false) }
    val picture = if (bannerFailed) artwork.portrait else banner ?: artwork.portrait
    val report by rememberUpdatedState(onStateChanged)
    SubcomposeAsyncImage(model = picture.url, contentDescription = "歌手照片", modifier = Modifier.fillMaxWidth()) image@{
        val result by painter.state.collectAsState()
        val status = result.artistArtworkLoadState()
        LaunchedEffect(status, picture.url) {
            report(status)
            if (status == ArtistArtworkLoadState.Error && picture == banner) bannerFailed = true
        }
        if (status != ArtistArtworkLoadState.Success) fallback() else {
            val wide = picture == banner
            TabletArtistPhotoFrame(picture.width.toFloat() / picture.height, wide, photoHeight, artworkModifier) {
                this@image.SubcomposeAsyncImageContent(Modifier.fillMaxSize(),
                    contentScale = if (wide) ContentScale.Crop else ContentScale.Fit,
                    alignment = Alignment.TopCenter)
            }
        }
    }
}

@Composable
private fun TabletNeteaseArtistPhoto(url: String?, loading: Boolean, photoHeight: Dp, artworkModifier: Modifier) {
    if (url.isNullOrBlank()) {
        TabletArtistPhotoFrame(1f, false, photoHeight, artworkModifier) { TabletArtistPhotoPlaceholder(loading) }
    } else SubcomposeAsyncImage(model = url, contentDescription = "歌手照片", modifier = Modifier.fillMaxWidth()) image@{
        val result by painter.state.collectAsState()
        val success = result as? AsyncImagePainter.State.Success
        val ratio = success?.result?.image?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: 1f
        TabletArtistPhotoFrame(ratio, false, photoHeight, artworkModifier) {
            if (success != null) this@image.SubcomposeAsyncImageContent(Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit, alignment = Alignment.TopCenter)
            else TabletArtistPhotoPlaceholder(result !is AsyncImagePainter.State.Error)
        }
    }
}

/** Full-width colour field; portrait proportions stay intact within its bounded image area. */
@Composable
private fun TabletArtistPhotoFrame(
    aspectRatio: Float,
    wide: Boolean,
    photoHeight: Dp,
    artworkModifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val ratio = aspectRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
        val imageWidth = if (wide) maxWidth else (photoHeight * ratio).coerceAtMost(maxWidth)
        Box(Modifier.fillMaxWidth().height(photoHeight).then(artworkModifier).background(background)) {
            Box(Modifier.align(Alignment.TopCenter).width(imageWidth).fillMaxHeight()) {
                content()
                if (!wide) Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                    0f to background, 0.1f to Color.Transparent,
                    0.9f to Color.Transparent, 1f to background,
                )))
            }
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                0f to Color.Transparent, 0.55f to Color.Transparent,
                0.82f to background.copy(alpha = 0.55f), 1f to background,
            )))
        }
    }
}

@Composable
private fun TabletArtistPhotoPlaceholder(loading: Boolean) {
    SkeletonShimmerHost(loading, Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center) {
            if (loading) Spacer(Modifier.matchParentSize().skeleton(true, RoundedCornerShape(0.dp)))
            else Icon(Icons.Rounded.Person, null, Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
