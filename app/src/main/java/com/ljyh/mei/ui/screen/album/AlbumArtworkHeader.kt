package com.ljyh.mei.ui.screen.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Icon
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.artwork.album.*
import com.ljyh.mei.ui.component.glass.meiTopBarContentClearance
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.player.cover.DynamicCoverView

@Composable
internal fun AlbumHeader(
    state: AlbumDetailUiState,
    actions: AlbumDetailActions,
    artists: List<MediaMetadata.Artist>,
    onShowArtists: () -> Unit,
    artworkModifier: Modifier = Modifier,
) {
    val compact = LocalPageStartInset.current > 0.dp
    val album = state.album
    val kind = albumArtworkKind(state.artworkMode, album.publishTime, state.motionArtwork != null, state.modelMode, allowMotion = !LocalPhoneLandscape.current)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (kind == AlbumArtworkKind.Model) {
            val stageHeight = minOf(maxWidth / AlbumMediaArtworkRatio, if (compact) 240.dp else 460.dp)
            val clearance = albumModelTopClearanceDp(
                meiTopBarContentClearance().value, stageHeight.value, albumArtworkModelStyle(state.artworkMode, album.publishTime, state.modelMode),
            ).dp
            Column(Modifier.fillMaxWidth().padding(top = clearance)) {
                AlbumArtworkVisual(state, actions, Modifier.fillMaxWidth().height(stageHeight).then(artworkModifier))
                AlbumIdentity(state, artists, onShowArtists, actions.onArtistClick)
            }
        } else {
            val height = if (compact) 300.dp else (maxWidth * 1.4f).coerceIn(400.dp, 680.dp)
            AlbumCoverHero(
                minimumHeight = height,
                artworkModifier = artworkModifier,
                artwork = { identityStart ->
                    AlbumArtworkVisual(state, actions, Modifier.fillMaxSize(),
                        identityStartFraction = identityStart, portrait = !compact)
                },
                identity = { AlbumIdentity(state, artists, onShowArtists, actions.onArtistClick) },
            )
        }
    }
}

/** Measure text first: long names and font scaling cannot introduce a separate opaque panel. */
@Composable
internal fun AlbumCoverHero(
    minimumHeight: Dp,
    artworkModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
    artwork: @Composable (identityStartFraction: Float) -> Unit,
    identity: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier.fillMaxWidth()) { constraints ->
        val width = constraints.maxWidth
        val identityPlaceable = subcompose("identity", identity).single()
            .measure(Constraints.fixedWidth(width))
        // Keep a useful unobscured image area even with a multi-line album name.
        val height = constraints.constrainHeight(maxOf(
            minimumHeight.roundToPx(), identityPlaceable.height + (width * .75f).toInt(),
        ))
        val identityTop = (height - identityPlaceable.height).coerceAtLeast(0)
        val image = subcompose("artwork") { artwork(identityTop.toFloat() / height.coerceAtLeast(1)) }
            .single().measure(Constraints.fixed(width, height))
        // The toolbar follows the image/text boundary, rather than the whole metadata block.
        val boundary = subcompose("boundary") { Spacer(artworkModifier) }.single()
            .measure(Constraints.fixed(width, identityTop))
        layout(width, height) {
            image.place(0, 0)
            identityPlaceable.place(0, identityTop)
            boundary.place(0, 0)
        }
    }
}

@Composable
private fun AlbumIdentity(
    state: AlbumDetailUiState,
    artists: List<MediaMetadata.Artist>,
    onShowArtists: () -> Unit,
    onArtistClick: (Long) -> Unit,
) {
    val album = state.album
    val palette = LocalAlbumArtworkColors.current
    Column(Modifier.fillMaxWidth().padding(meiPageHorizontalPadding()).padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(album.name, Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
        artists.firstOrNull()?.let { first ->
            Text(if (artists.size > 1) "${first.name} 等" else first.name,
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable(enabled = artists.size > 1 || first.id > 0) {
                        if (artists.size > 1) onShowArtists() else onArtistClick(first.id)
                    }.wrapContentHeight(Alignment.CenterVertically),
                style = MaterialTheme.typography.titleMedium, color = palette?.secondary ?: MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(listOfNotNull(albumReleaseYear(album.publishTime)?.toString(),
            "${album.size.takeIf { it > 0 } ?: state.songs.size} 首").joinToString(" · "),
            style = MaterialTheme.typography.bodySmall, color = palette?.tertiary ?: MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
internal fun AlbumArtworkVisual(
    state: AlbumDetailUiState,
    actions: AlbumDetailActions,
    modifier: Modifier = Modifier,
    identityStartFraction: Float? = null,
    portrait: Boolean = true,
    fillModelSpace: Boolean = false,
) {
    val album = state.album
    val kind = albumArtworkKind(state.artworkMode, album.publishTime, state.motionArtwork != null, state.modelMode, allowMotion = !LocalPhoneLandscape.current)
    val background = MaterialTheme.colorScheme.surface
    if (kind == AlbumArtworkKind.Model) {
        val tracks = remember(state.songs) { albumPackageTracks(state.songs) }
        AlbumMediaArtwork(album.picUrl, album.publishTime, album.name, album.artistNames.joinToString(" / "),
            modifier, interactive = true, tracks = tracks,
            style = albumArtworkModelStyle(state.artworkMode, album.publishTime, state.modelMode),
            fillAvailableSpace = fillModelSpace)
    } else {
        Box(modifier.background(background)) {
            if (kind == AlbumArtworkKind.Motion) {
                var resumed by remember { mutableStateOf(false) }
                LifecycleResumeEffect(Unit) {
                    resumed = true
                    onPauseOrDispose { resumed = false }
                }
                val motion = state.motionArtwork?.let { if (portrait) it.portraitVariant() ?: it else it }
                DynamicCoverView(imageUrl = album.picUrl, cover = motion, playing = resumed,
                    modifier = Modifier.matchParentSize(), onPlaybackError = { actions.onArtworkError() })
            } else {
                SubcomposeAsyncImage(model = album.picUrl, contentDescription = "专辑《${album.name}》封面",
                    contentScale = ContentScale.Crop, alignment = Alignment.TopCenter,
                    modifier = Modifier.matchParentSize()) {
                    val image by painter.state.collectAsState()
                    when (image) {
                        is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                        is AsyncImagePainter.State.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Album, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> Box(Modifier.fillMaxSize().skeleton(true))
                    }
                }
            }
            if (identityStartFraction != null) Box(Modifier.matchParentSize().drawWithCache {
                val identityTop = size.height * identityStartFraction.coerceIn(0f, 1f)
                // Preserve the subject above the title. Blend across the title's lower image edge,
                // rather than washing out the portrait with a large band before the text starts.
                val start = (identityTop - 24.dp.toPx()).coerceAtLeast(0f)
                val end = (identityTop + 72.dp.toPx()).coerceAtMost(size.height)
                // One continuous transition, with zero slope at both ends and the exact page RGB.
                val stops = Array(33) { index ->
                    val t = index / 32f
                    val alpha = t * t * t * (t * (t * 6f - 15f) + 10f)
                    t to background.copy(alpha = alpha.coerceIn(0f, 1f))
                }
                val fade = Brush.verticalGradient(*stops, startY = start, endY = maxOf(end, start + 1f))
                onDrawBehind { drawRect(fade) }
            })
        }
    }
}
