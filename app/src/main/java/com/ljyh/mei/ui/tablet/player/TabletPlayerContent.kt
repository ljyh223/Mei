package com.ljyh.mei.ui.tablet.player

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.ljyh.mei.ui.component.player.applemusic.Title
import com.ljyh.mei.ui.component.player.controls.PlayerControlButton
import com.ljyh.mei.ui.component.player.overlay.PlayerPanel
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.theme.LocalPlayerLightStatusBar
import com.ljyh.mei.ui.motion.PlayerMotionSpec

/** Pure presentation: no ViewModel, navigation, playback connection or device classification. */
@Composable
internal fun TabletPlayerContent(
    state: TabletPlayerUiState,
    actions: TabletPlayerActions,
    slots: TabletPlayerSlots,
    modifier: Modifier = Modifier,
    shortWindowLayout: Boolean = false,
) {
    var consumedInsets by remember { mutableStateOf(WindowInsets(0)) }
    BoxWithConstraints(modifier.fillMaxSize().onConsumedWindowInsetsChanged { consumedInsets = it }) {
        val safeDrawing = WindowInsets.safeDrawing
        val gutter = meiPageGutter()
        val fontScale = LocalDensity.current.fontScale
        val safePadding = safeDrawing.exclude(consumedInsets).asPaddingValues()
        val direction = LocalLayoutDirection.current
        val startInset = safePadding.calculateStartPadding(direction)
        val endInset = safePadding.calculateEndPadding(direction)
        val bodyWidth = minOf(maxWidth - startInset - endInset, 1400.dp) - gutter * 2
        // Reserve the same gap from the actual midpoint with either placement of the cutout.
        val panelInset = ((endInset - startInset) / 2).coerceAtLeast(0.dp)
        val eligibleWidth = bodyWidth - panelInset * 2
        val landscapeArtwork = slots.landscapeArtwork?.takeIf {
            shortWindowLayout && tabletPlayerLayout(eligibleWidth.value, maxHeight.value, fontScale, true).twoPanes
        }
        if (landscapeArtwork != null) {
            val expansion by animateFloatAsState(
                if (landscapeArtwork.expanded) 1f else 0f,
                animationSpec = PlayerMotionSpec.tween(600),
                label = "LandscapeMotionArtworkBounds",
            )
            val safeWidth = maxWidth - startInset - endInset
            val top = safePadding.calculateTopPadding()
            val safeHeight = maxHeight - top - safePadding.calculateBottomPadding()
            val layout = tabletPlayerLayout(bodyWidth.value, safeHeight.value, fontScale, true)
            val normalSize = layout.artworkSizeDp.dp
            val normalStart = startInset + ((safeWidth - 1400.dp) / 2).coerceAtLeast(0.dp) +
                gutter + (layout.nowPlayingWidthDp.dp - normalSize) / 2
            val artworkWidth = lerp(normalSize, maxWidth / 2, expansion)
            val artworkHeight = lerp(normalSize, maxHeight, expansion)
            val artworkStart = lerp(normalStart, 0.dp, expansion)
            val artworkLeft = if (direction == LayoutDirection.Ltr) artworkStart else maxWidth - artworkStart - artworkWidth
            val artworkTop = lerp(top + (safeHeight - normalSize) / 2, 0.dp, expansion)
            landscapeArtwork.content(
                Modifier.absoluteOffset(x = artworkLeft, y = artworkTop).size(artworkWidth, artworkHeight),
                expansion,
            )
            val setPlayerLightStatusBar = LocalPlayerLightStatusBar.current
            if (state.expanded && expansion > 0f) DisposableEffect(setPlayerLightStatusBar) {
                setPlayerLightStatusBar(false)
                onDispose { setPlayerLightStatusBar(null) }
            }
        }
        TabletPlayerBody(
            state, actions, slots, shortWindowLayout, safeDrawing,
            artworkPlacedSeparately = landscapeArtwork != null, panelInset = panelInset,
        )
    }
}

@Composable
private fun TabletPlayerBody(
    state: TabletPlayerUiState,
    actions: TabletPlayerActions,
    slots: TabletPlayerSlots,
    shortWindowLayout: Boolean,
    safeDrawing: WindowInsets,
    artworkPlacedSeparately: Boolean,
    panelInset: Dp,
) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(safeDrawing)) {
        if (!shortWindowLayout) {
            Row(Modifier.fillMaxWidth().padding(horizontal = meiPageGutter()),
                verticalAlignment = Alignment.CenterVertically) {
                PlayerControlButton(actions.onCollapse, foreground = state.foreground, size = 48.dp) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "收起播放器", tint = state.foreground)
                }
                if (state.detailsVisible) {
                    TextButton(
                        onClick = actions.onShowNowPlaying,
                        colors = ButtonDefaults.textButtonColors(contentColor = state.foreground),
                    ) { Text("正在播放") }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            BoxWithConstraints(Modifier.widthIn(max = 1400.dp).fillMaxSize()
                .padding(horizontal = meiPageGutter(), vertical = if (shortWindowLayout) 0.dp else 12.dp)) {
                val viewportHeight = maxHeight
                val layout = tabletPlayerLayout(maxWidth.value, viewportHeight.value, LocalDensity.current.fontScale, shortWindowLayout)
                BackHandler(enabled = state.expanded &&
                    (state.panel != PlayerPanel.Lyrics ||
                        (state.detailsVisible && (shortWindowLayout || !layout.twoPanes)))) {
                    actions.onShowNowPlaying()
                }
                if (shortWindowLayout && layout.twoPanes) {
                    PhoneLandscapePlayerPanes(state, actions, slots, layout, viewportHeight, artworkPlacedSeparately, panelInset)
                } else if (layout.twoPanes) {
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
                        if (state.panel == PlayerPanel.Lyrics && layout.showCompactControls) {
                            slots.controls(Modifier.widthIn(max = 460.dp).fillMaxWidth(), true)
                        }
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
private fun PhoneLandscapePlayerPanes(
    state: TabletPlayerUiState,
    actions: TabletPlayerActions,
    slots: TabletPlayerSlots,
    layout: TabletPlayerLayout,
    viewportHeight: Dp,
    artworkPlacedSeparately: Boolean,
    panelInset: Dp,
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(layout.paneGapDp.dp)) {
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            if (!artworkPlacedSeparately) slots.artwork(Modifier.size(layout.artworkSizeDp.dp))
        }
        TabletPlayerPanels(
            panel = state.panel.takeIf { state.detailsVisible },
            style = state.animation,
            modifier = Modifier.weight(1f).fillMaxHeight()
                .padding(start = if (artworkPlacedSeparately) panelInset else 0.dp).clipToBounds(),
        ) { panel, modifier ->
            if (panel == PlayerPanel.Queue) {
                Box(modifier, contentAlignment = Alignment.Center) {
                    slots.panel(panel, Modifier.fillMaxWidth().height(layout.artworkSizeDp.dp))
                }
            } else if (panel != null) {
                slots.panel(panel, modifier)
            } else {
                Column(
                    modifier.verticalScroll(rememberScrollState()),
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
}

@Composable
private fun TabletNowPlayingPane(
    state: TabletPlayerUiState,
    actions: TabletPlayerActions,
    slots: TabletPlayerSlots,
    layout: TabletPlayerLayout,
    modifier: Modifier,
    viewportHeight: Dp,
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
) {
    TabletPlayerPanels(state.panel, state.animation, modifier, slots.panel)
}
