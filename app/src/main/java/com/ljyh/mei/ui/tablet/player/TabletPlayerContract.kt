package com.ljyh.mei.ui.tablet.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ljyh.mei.ui.component.player.overlay.PlayerPanel
import com.ljyh.mei.constants.TabletAnimationStyle

internal data class TabletPlayerUiState(
    val title: String,
    val artist: String,
    val isLiked: Boolean,
    val panel: PlayerPanel,
    val detailsVisible: Boolean,
    val foreground: Color = Color.White,
    val expanded: Boolean = true,
    val animation: TabletAnimationStyle = TabletAnimationStyle.FLIP_3D,
)

internal data class TabletPlayerActions(
    val onCollapse: () -> Unit,
    val onTitle: () -> Unit,
    val onLike: () -> Unit,
    val onMore: () -> Unit,
    val onShowNowPlaying: () -> Unit,
)

/** Stateful engine-backed components are supplied by the binding screen, not owned by layouts. */
internal class TabletPlayerSlots(
    val artwork: @Composable (Modifier) -> Unit,
    val controls: @Composable (Modifier, compact: Boolean) -> Unit,
    val panel: @Composable (PlayerPanel, Modifier) -> Unit,
    val actions: @Composable (Modifier) -> Unit,
)
