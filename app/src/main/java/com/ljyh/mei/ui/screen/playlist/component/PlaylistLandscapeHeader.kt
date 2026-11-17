package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.artwork.MeiArtworkGlassActions
import com.ljyh.mei.ui.component.artwork.MeiArtworkPlayButton
import com.ljyh.mei.ui.component.layout.meiPageGutter

/** Controls overlay a separate capture of the artwork, so both lenses sample the cover. */
@Composable
internal fun PlaylistLandscapeHeader(
    maxArtworkHeight: Dp,
    canPlay: Boolean,
    onPlayAll: () -> Unit,
    artwork: @Composable (Modifier) -> Unit,
    more: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.padding(horizontal = meiPageGutter(), vertical = 8.dp)) {
        val artworkHeight = minOf(maxArtworkHeight, maxWidth)
        // Very short windows can crop the cover while leaving room for both actions.
        val artworkWidth = maxOf(artworkHeight, 192.dp).coerceAtMost(maxWidth)
        MeiArtworkGlassActions(
            modifier = Modifier.width(artworkWidth).height(artworkHeight),
            artwork = { artwork(Modifier.matchParentSize()) },
        ) {
            MeiArtworkPlayButton(
                enabled = canPlay,
                onPlayAll = onPlayAll,
                modifier = Modifier.weight(1f, fill = false),
            )
            more()
        }
    }
}
