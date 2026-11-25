package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding

@Composable
internal fun AppleArtistHeader(
    state: ArtistDetailUiState,
    artwork: AppleArtistArtwork,
    actions: ArtistDetailActions,
    onInfo: () -> Unit,
    onPresentationChanged: (ArtistArtworkPresentation) -> Unit,
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
    compact: Boolean = false,
    fallback: @Composable () -> Unit,
) {
    // Only a photographic landscape asset is suitable here; ultra-wide store promos stay unused.
    val wideImage = artwork.banner?.takeIf { compact && it.width.toFloat() / it.height in 1.4f..2.4f }
    var bannerFailed by remember(artwork) { mutableStateOf(false) }
    val picture = if (bannerFailed) artwork.portrait else wideImage ?: artwork.portrait
    var wordmarkState by remember(artwork, picture.url) {
        mutableStateOf(if (artwork.logo == null) ArtistArtworkLoadState.Unavailable else ArtistArtworkLoadState.Loading)
    }
    val onPresentation by rememberUpdatedState(onPresentationChanged)
    SubcomposeAsyncImage(
        model = picture.url, contentDescription = null, modifier = modifier.fillMaxWidth(),
        contentScale = ContentScale.Crop, alignment = Alignment.TopCenter,
    ) image@{
        val result by painter.state.collectAsState()
        val photoState = result.artistArtworkLoadState()
        val loaded = photoState == ArtistArtworkLoadState.Success
        val presentation = ArtistArtworkPresentation(photoState, wordmarkState)
        LaunchedEffect(presentation, picture.url) {
            onPresentation(presentation)
            if (photoState == ArtistArtworkLoadState.Error && picture == wideImage) bannerFailed = true
        }
        if (!loaded) {
            fallback()
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val height = if (compact) {
                    if (constraints.hasBoundedHeight) maxHeight.coerceAtMost(360.dp) else 300.dp
                } else (maxWidth * 1.48f).coerceIn(460.dp, 660.dp) + 16.dp
                val background = MaterialTheme.colorScheme.background
                val foreground = MaterialTheme.colorScheme.onBackground
                Box(Modifier.fillMaxWidth().height(height).background(background)) {
                    this@image.SubcomposeAsyncImageContent(
                        modifier = Modifier.fillMaxWidth().height(height * 0.88f).then(artworkModifier),
                        contentScale = ContentScale.Crop, alignment = Alignment.TopCenter,
                    )
                    Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                        0f to Color.Transparent, 0.34f to Color.Transparent,
                        0.68f to background.copy(alpha = 0.72f), 0.88f to background, 1f to background,
                    )))
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(meiPageHorizontalPadding()).padding(bottom = if (compact) 12.dp else 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 28.dp)) {
                        ArtistWordmark(artwork, state.artist?.name ?: artwork.name, compact) { wordmarkState = it }
                        ArtistPrimaryActions(
                            canPlay = state.canPlay && state.songs.isNotEmpty(), subscription = state.subscription,
                            onInfo = onInfo, onPlay = actions.onPlayHotSongs,
                            onToggleSubscription = actions.onToggleSubscription, compact = compact,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ArtistWordmark(
    artwork: AppleArtistArtwork,
    name: String,
    compact: Boolean,
    logoHeight: Dp = if (compact) 48.dp else 96.dp,
    nameStyle: TextStyle? = null,
    onLoadStateChanged: (ArtistArtworkLoadState) -> Unit,
) {
    val onStateChanged by rememberUpdatedState(onLoadStateChanged)
    Box(Modifier.fillMaxWidth().heightIn(min = if (compact) 40.dp else 68.dp)
        .clearAndSetSemantics { contentDescription = name; heading() }, contentAlignment = Alignment.Center) {
        val text: @Composable () -> Unit = {
            Text(name, style = nameStyle ?: if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        val logo = artwork.logo
        if (logo == null) text() else SubcomposeAsyncImage(
            model = logo.url, contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(logoHeight),
            contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
        ) {
            val result by painter.state.collectAsState()
            LaunchedEffect(result) { onStateChanged(result.artistArtworkLoadState()) }
            if (result is AsyncImagePainter.State.Success) SubcomposeAsyncImageContent() else text()
        }
    }
}

internal fun AsyncImagePainter.State.artistArtworkLoadState(): ArtistArtworkLoadState = when (this) {
    is AsyncImagePainter.State.Success -> ArtistArtworkLoadState.Success
    is AsyncImagePainter.State.Error -> ArtistArtworkLoadState.Error
    else -> ArtistArtworkLoadState.Loading
}
