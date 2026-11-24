package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.shimmer.skeleton

@Composable
internal fun TabletArtistHeader(
    state: ArtistDetailUiState,
    artwork: AppleArtistArtwork?,
    presentation: ArtistArtworkPresentation,
    portraitPainter: Painter?,
    heroHeight: Dp,
    profileTopPadding: Dp,
    actions: ArtistDetailActions,
    onInfo: () -> Unit,
    onWordmarkChanged: (ArtistArtworkLoadState) -> Unit,
    artworkModifier: Modifier,
) {
    if (artwork == null || !presentation.usesApplePhoto) {
        TabletNeteaseArtistHeader(state, actions, onInfo, profileTopPadding, artworkModifier)
        return
    }
    if (portraitPainter != null) {
        // Ordinary artwork is a foreground portrait, not a panoramic hero. Its painter is
        // shared with the page backdrop so returning or scrolling cannot start another load.
        Box(Modifier.fillMaxWidth().then(artworkModifier), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 416.dp).fillMaxWidth()
                .padding(meiPageHorizontalPadding())
                .padding(top = profileTopPadding, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Image(portraitPainter, contentDescription = "歌手照片",
                    modifier = Modifier.size(192.dp).clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), CircleShape),
                    contentScale = ContentScale.Crop)
                Spacer(Modifier.height(32.dp))
                ArtistWordmark(artwork, state.artist?.name ?: artwork.name, compact = true,
                    logoHeight = 64.dp, onLoadStateChanged = onWordmarkChanged)
                Spacer(Modifier.height(16.dp))
                if (state.artist != null) ArtistPrimaryActions(state.canPlay && state.songs.isNotEmpty(),
                    state.subscription, onInfo, actions.onPlayHotSongs, actions.onToggleSubscription)
            }
        }
        return
    }
    Box(Modifier.fillMaxWidth().heightIn(min = heroHeight).then(artworkModifier)) {
        Column(Modifier.align(Alignment.BottomCenter).widthIn(max = 416.dp).fillMaxWidth()
            .padding(meiPageHorizontalPadding())
            .padding(top = profileTopPadding + heroHeight * 0.42f, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ArtistWordmark(artwork, state.artist?.name ?: artwork.name, compact = true,
                logoHeight = 64.dp, onLoadStateChanged = onWordmarkChanged)
            if (state.artist != null) ArtistPrimaryActions(state.canPlay && state.songs.isNotEmpty(),
                state.subscription, onInfo, actions.onPlayHotSongs, actions.onToggleSubscription)
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
