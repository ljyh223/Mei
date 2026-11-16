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
    shortWindowLayout: Boolean = false,
    immersive: Boolean = false,
    onToggleImmersive: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = meiPageGutter()),
            verticalAlignment = Alignment.CenterVertically) {
            PlayerControlButton(actions.onCollapse, foreground = state.foreground, size = 48.dp) {
                Icon(Icons.Rounded.KeyboardArrowDown, "收起播放器", tint = state.foreground)
            }
            if (state.detailsVisible && !immersive) TextButton(actions.onShowNowPlaying,
                colors = ButtonDefaults.textButtonColors(contentColor = state.foreground)) { Text("正在播放") }
            if (onToggleImmersive != null) {
                Spacer(Modifier.weight(1f))
                TextButton(onToggleImmersive, colors = ButtonDefaults.textButtonColors(contentColor = state.foreground)) {
                    Text(if (immersive) "切换至标准" else "切换至沉浸")
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            BoxWithConstraints(Modifier.widthIn(max = 1400.dp).fillMaxSize()
                .padding(horizontal = meiPageGutter(), vertical = if (shortWindowLayout) 6.dp else 12.dp)) {
                val viewportHeight = maxHeight
                val layout = tabletPlayerLayout(maxWidth.value, viewportHeight.value, LocalDensity.current.fontScale, shortWindowLayout)
                BackHandler(enabled = state.expanded &&
                    (state.panel != PlayerPanel.Lyrics || (!layout.twoPanes && state.detailsVisible))) {
                    actions.onShowNowPlaying()
                }
                if (shortWindowLayout && layout.twoPanes) {
                    PhoneLandscapePlayerPanes(state, actions, slots, layout, immersive, viewportHeight)
                } else if (layout.twoPanes) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(layout.paneGapDp.dp)) {
                        TabletNowPlayingPane(state, actions, slots, layout,
                            Modifier.width(layout.nowPlayingWidthDp.dp).fillMaxHeight(), viewportHeight)
                        TabletPlayerDetailPane(state, slots, Modifier.weight(1f).fillMaxHeight())
                    }
                } else if (state.detailsVisible || (shortWindowLayout && immersive)) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            slots.artwork(Modifier.size(56.dp))
                            TabletPlayerTitle(state, actions, Modifier.weight(1f), compact = true)
                        }
                        TabletPlayerDetailPane(state, slots, Modifier.weight(1f).fillMaxWidth(),
                            panel = if (immersive) PlayerPanel.Lyrics else state.panel)
                        if (!immersive) {
                            if (state.panel == PlayerPanel.Lyrics && layout.showCompactControls) slots.controls(Modifier.widthIn(max = 460.dp).fillMaxWidth(), true)
                            slots.actions(Modifier.widthIn(max = 460.dp).fillMaxWidth())
                        }
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
private fun PhoneLandscapePlayerPanes(
    state: TabletPlayerUiState,
    actions: TabletPlayerActions,
    slots: TabletPlayerSlots,
    layout: TabletPlayerLayout,
    immersive: Boolean,
    viewportHeight: androidx.compose.ui.unit.Dp,
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(layout.paneGapDp.dp)) {
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            slots.artwork(Modifier.size(minOf(viewportHeight, layout.nowPlayingWidthDp.dp)))
        }
        if (immersive || state.detailsVisible) {
            TabletPlayerDetailPane(state, slots, Modifier.weight(1f).fillMaxHeight(),
                panel = if (immersive) PlayerPanel.Lyrics else state.panel)
        } else {
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 460.dp).fillMaxWidth().heightIn(min = viewportHeight),
                    verticalArrangement = Arrangement.Center) {
                    TabletPlayerTitle(state, actions, Modifier.fillMaxWidth(), compact = true)
                    Spacer(Modifier.height(8.dp))
                    slots.controls(Modifier.fillMaxWidth(), true)
                    slots.actions(Modifier.fillMaxWidth())
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
        val shortWindowLayout = layout.fitShortWindow
        val contentWidth = if (shortWindowLayout) minOf(layout.nowPlayingWidthDp.dp, 400.dp) else layout.artworkSizeDp.dp
        Column(Modifier.width(contentWidth).heightIn(min = viewportHeight),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            slots.artwork(Modifier.size(layout.artworkSizeDp.dp))
            Spacer(Modifier.height(if (shortWindowLayout) 8.dp else 24.dp))
            TabletPlayerTitle(state, actions, Modifier.fillMaxWidth(), compact = shortWindowLayout)
            Spacer(Modifier.height(if (shortWindowLayout) 4.dp else 16.dp))
            slots.controls(Modifier.fillMaxWidth(), shortWindowLayout)
            if (shortWindowLayout) slots.actions(Modifier.fillMaxWidth())
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
    panel: PlayerPanel = state.panel,
) {
    TabletPlayerPanels(panel, state.animation, modifier, slots.panel)
}
