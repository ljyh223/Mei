package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.glass.LocalMeiHazeState
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.component.layout.MeiPageLayout
import com.ljyh.mei.ui.component.layout.meiPageGutter
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Controls overlay a separate capture of the artwork, so both lenses sample the cover. */
@Composable
internal fun PlaylistLandscapeHeader(
    maxArtworkHeight: Dp,
    canPlay: Boolean,
    onPlayAll: () -> Unit,
    artwork: @Composable (Modifier) -> Unit,
    more: @Composable () -> Unit,
) {
    val source = rememberHazeState()
    BoxWithConstraints(Modifier.padding(horizontal = meiPageGutter(), vertical = 8.dp)) {
        val artworkHeight = minOf(maxArtworkHeight, maxWidth)
        // Very short windows can crop the cover while leaving room for both actions.
        val artworkWidth = maxOf(artworkHeight, 192.dp).coerceAtMost(maxWidth)
        Box(Modifier.width(artworkWidth).height(artworkHeight)) {
            Box(Modifier.matchParentSize().hazeSource(source)) {
                artwork(Modifier.matchParentSize())
            }
            CompositionLocalProvider(LocalMeiHazeState provides source) {
                Row(
                    Modifier.align(Alignment.BottomStart).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MeiGlassSurface(
                        modifier = Modifier.weight(1f, fill = false).alpha(if (canPlay) 1f else 0.5f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Row(
                            Modifier.heightIn(min = MeiPageLayout.ToolbarTouchSize)
                                .clickable(enabled = canPlay, role = Role.Button, onClick = onPlayAll)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Text(
                                "播放全部",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    more()
                }
            }
        }
    }
}
