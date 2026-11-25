package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import com.ljyh.mei.ui.screen.artist.component.ArtistProfileHeader
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding

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
    neteasePortrait: Painter? = null,
    neteaseLoading: Boolean = false,
) {
    if (artwork == null || !presentation.usesApplePhoto) {
        TabletNeteaseArtistHeader(state, actions, onInfo, profileTopPadding, artworkModifier, neteasePortrait,
            neteaseLoading || state.detailLoading)
        return
    }
    if (portraitPainter != null) {
        ArtistProfileHeader(state, actions, onInfo, portraitPainter, false, profileTopPadding, artworkModifier) {
            ArtistWordmark(artwork, state.artist?.name ?: artwork.name, compact = true,
                logoHeight = 64.dp, nameStyle = MaterialTheme.typography.headlineLarge, onLoadStateChanged = onWordmarkChanged)
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
