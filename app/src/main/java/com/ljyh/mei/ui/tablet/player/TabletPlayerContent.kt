package com.ljyh.mei.ui.tablet.player

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.player.applemusic.Title
import com.ljyh.mei.ui.component.player.controls.PlayerControlButton
import com.ljyh.mei.ui.component.player.overlay.PlayerPanel
import com.ljyh.mei.ui.component.layout.meiPageGutter

/** Pure presentation: no ViewModel, navigation, playback connection or device classification. */
@Composable
internal fun TabletPlayerContent(
    state: TabletPlayerUiState,
    actions: TabletPlayerActions,
    slots: TabletPlayerSlots,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = meiPageGutter()),
            verticalAlignment = Alignment.CenterVertically) {
            PlayerControlButton(actions.onCollapse, foreground = state.foreground, size = 48.dp) {
                Icon(Icons.Rounded.KeyboardArrowDown, "收起播放器", tint = state.foreground)
            }
            if (state.detailsVisible) TextButton(actions.onShowNowPlaying,
                colors = ButtonDefaults.textButtonColors(contentColor = state.foreground)) { Text("正在播放") }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            BoxWithConstraints(Modifier.widthIn(max = 1400.dp).fillMaxSize()
                .padding(horizontal = meiPageGutter(), vertical = 12.dp)) {
                val viewportHeight = maxHeight
                val layout = tabletPlayerLayout(maxWidth.value, viewportHeight.value, LocalDensity.current.fontScale)
                BackHandler(enabled = state.expanded &&
                    (state.panel != PlayerPanel.Lyrics || (!layout.twoPanes && state.detailsVisible))) {
                    actions.onShowNowPlaying()
                }
                if (layout.twoPanes) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(layout.paneGapDp.dp)) {
                        TabletNowPlayingPane(state, actions, slots, layout,
                            Modifier.width(layout.nowPlayingWidthDp.dp).fillMaxHeight(), viewportHeight)
                        TabletPlayerDetailPane(state, slots, Modifier.weight(1f).fillMaxHeight())
                    }
                } else if (state.detailsVisible) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            slots.artwork(Modifier.size(56.dp))
                            TabletPlayerTitle(state, actions, Modifier.weight(1f), compact = true)
                        }
                        TabletPlayerDetailPane(state, slots, Modifier.weight(1f).fillMaxWidth())
                        if (state.panel == PlayerPanel.Lyrics && layout.showCompactControls) slots.controls(Modifier.widthIn(max = 460.dp).fillMaxWidth(), true)
                        slots.actions(Modifier.widthIn(max = 460.dp).fillMaxWidth())
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        TabletNowPlayingPane(state, actions, slots, layout,
                            Modifier.width(layout.nowPlayingWidthDp.dp).fillMaxHeight(), viewportHeight)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletNowPlayingPane(
    state: TabletPlayerUiState,
    actions: TabletPlayerActions,
    slots: TabletPlayerSlots,
    layout: TabletPlayerLayout,
    modifier: Modifier,
    viewportHeight: androidx.compose.ui.unit.Dp,
) {
    Column(modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.width(layout.artworkSizeDp.dp).heightIn(min = viewportHeight),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            slots.artwork(Modifier.size(layout.artworkSizeDp.dp))
            Spacer(Modifier.height(24.dp))
            TabletPlayerTitle(state, actions, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            slots.controls(Modifier.fillMaxWidth(), false)
        }
    }
}

@Composable
private fun TabletPlayerTitle(
    state: TabletPlayerUiState, actions: TabletPlayerActions, modifier: Modifier, compact: Boolean = false,
) {
    Title(
        title = state.title, subTitle = state.artist, isLiked = state.isLiked,
        onLikeClick = actions.onLike, onMoreClick = actions.onMore, onTitleClick = actions.onTitle,
        modifier = modifier,
        titleStyle = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
        subTitleStyle = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleMedium,
        needShadow = false, titleColor = state.foreground,
        subTitleColor = state.foreground.copy(alpha = state.foreground.alpha * .7f), iconColor = state.foreground,
    )
}

@Composable
private fun TabletPlayerDetailPane(
    state: TabletPlayerUiState, slots: TabletPlayerSlots, modifier: Modifier,
) {
    TabletPlayerPanels(state.panel, state.animation, modifier, slots.panel)
}
