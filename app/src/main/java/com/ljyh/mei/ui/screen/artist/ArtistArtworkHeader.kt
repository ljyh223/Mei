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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.theme.LocalPageLightStatusBar

/** The whole new presentation activates only after the main photograph is usable. */
@Composable
internal fun ArtistArtworkTheme(artwork: AppleArtistArtwork?, ready: Boolean, content: @Composable () -> Unit) {
    val original = MaterialTheme.colorScheme
    val background = artwork?.portrait?.background?.let { value ->
        runCatching { Color(android.graphics.Color.parseColor("#$value")) }.getOrNull()
    } ?: original.background
    val foreground = if (background.luminance() > 0.179f) Color.Black else Color.White
    val colors = if (ready) original.copy(
        background = background, onBackground = foreground,
        surface = background, onSurface = foreground, onSurfaceVariant = foreground.copy(alpha = 0.74f),
        primary = foreground, onPrimary = background, secondary = foreground,
        surfaceVariant = lerp(background, foreground, 0.10f),
        surfaceContainerLowest = background,
        surfaceContainerLow = lerp(background, foreground, 0.04f),
        surfaceContainer = lerp(background, foreground, 0.08f),
        surfaceContainerHigh = lerp(background, foreground, 0.12f),
        surfaceContainerHighest = lerp(background, foreground, 0.16f),
        outline = foreground.copy(alpha = 0.35f), outlineVariant = foreground.copy(alpha = 0.16f),
    ) else original
    val owner = remember { Any() }
    val statusBar = LocalPageLightStatusBar.current
    LifecycleResumeEffect(ready, background) {
        statusBar(owner, if (ready) background.luminance() > 0.179f else null)
        onPauseOrDispose { statusBar(owner, null) }
    }
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}

@Composable
internal fun AppleArtistHeader(
    state: ArtistDetailUiState,
    artwork: AppleArtistArtwork,
    actions: ArtistDetailActions,
    onInfo: () -> Unit,
    onImageReady: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
    compact: Boolean = false,
    fallback: @Composable () -> Unit,
) {
    // Only a photographic landscape asset is suitable here; ultra-wide store promos stay unused.
    val wideImage = artwork.banner?.takeIf { compact && it.width.toFloat() / it.height in 1.4f..2.4f }
    var bannerFailed by remember(artwork) { mutableStateOf(false) }
    val picture = if (bannerFailed) artwork.portrait else wideImage ?: artwork.portrait
    val onReady by rememberUpdatedState(onImageReady)
    SubcomposeAsyncImage(
        model = picture.url, contentDescription = null, modifier = modifier.fillMaxWidth(),
        contentScale = ContentScale.Crop, alignment = Alignment.TopCenter,
    ) image@{
        val result by painter.state.collectAsState()
        val loaded = result is AsyncImagePainter.State.Success
        LaunchedEffect(loaded, picture.url, result is AsyncImagePainter.State.Error) {
            onReady(loaded)
            if (result is AsyncImagePainter.State.Error && picture == wideImage) bannerFailed = true
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
                        ArtistWordmark(artwork, state.artist?.name ?: artwork.name, compact)
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
private fun ArtistWordmark(artwork: AppleArtistArtwork, name: String, compact: Boolean) {
    Box(Modifier.fillMaxWidth().heightIn(min = if (compact) 40.dp else 68.dp)
        .clearAndSetSemantics { contentDescription = name; heading() }, contentAlignment = Alignment.Center) {
        val text: @Composable () -> Unit = {
            Text(name, style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        val logo = artwork.logo
        if (logo == null) text() else SubcomposeAsyncImage(
            model = logo.url, contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 96.dp),
            contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
        ) {
            val result by painter.state.collectAsState()
            if (result is AsyncImagePainter.State.Success) SubcomposeAsyncImageContent() else text()
        }
    }
}
