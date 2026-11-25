package com.ljyh.mei.ui.screen.album

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ljyh.mei.ui.component.artwork.MeiArtworkGlassActions
import com.ljyh.mei.ui.component.artwork.MeiArtworkPlayButton

/** Phone landscape has a still artwork stage and two controls that sample that stage only. */
@Composable
internal fun AlbumLandscapeArtworkPane(
    state: AlbumDetailUiState,
    actions: AlbumDetailActions,
    modifier: Modifier = Modifier,
    more: @Composable RowScope.() -> Unit,
) {
    val model = albumArtworkKind(state.artworkMode, state.album.publishTime, false,
        state.modelMode, allowMotion = false) == AlbumArtworkKind.Model
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val frame = if (model) Modifier.fillMaxSize() else Modifier.size(minOf(maxWidth, maxHeight))
        MeiArtworkGlassActions(
            modifier = frame.semantics { contentDescription = "专辑封面" },
            artwork = {
                AlbumArtworkVisual(state, actions, Modifier.fillMaxSize(), portrait = false,
                    fillModelSpace = true)
            },
            actions = {
                MeiArtworkPlayButton(enabled = state.songs.isNotEmpty() && state.canPlay,
                    onPlayAll = actions.onPlayAll, label = "播放")
                more()
            },
        )
    }
}
