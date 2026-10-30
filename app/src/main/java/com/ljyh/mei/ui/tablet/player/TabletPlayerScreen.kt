package com.ljyh.mei.ui.tablet.player

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.CoverBackgroundEnabledKey
import com.ljyh.mei.constants.TabletAnimationStyle
import com.ljyh.mei.constants.TabletAnimationStyleKey
import com.ljyh.mei.ui.component.player.*
import com.ljyh.mei.ui.component.player.controls.PlayerControlsSection
import com.ljyh.mei.ui.component.player.controls.PlayerActionToolbar
import com.ljyh.mei.ui.model.PlayerAction
import com.ljyh.mei.ui.component.player.cover.*
import com.ljyh.mei.ui.component.player.overlay.*
import com.ljyh.mei.ui.component.player.sheet.PlaylistContent
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.component.sheet.*
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.motion.PlayerMotionSpec
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.LyricsAnchor
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.LyricsFade

/** Binds the existing shared session to independent tablet presentation and engine-backed slots. */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun TabletPlayerScreen(
    sheet: BottomSheetState,
    container: PlayerStateContainer,
    overlays: PlayerOverlayHandler,
    modifier: Modifier = Modifier,
) {
    val metadata by container.mediaMetadata
    val playing by container.isPlaying
    val playbackState by container.playbackState
    val liked by container.isLiked
    val dynamic by container.playerViewModel.dynamicCover.collectAsState()
    val panel by overlays.panel
    val animation by rememberEnumPreference(TabletAnimationStyleKey, TabletAnimationStyle.FLIP_3D)
    val extensionEnabled by rememberPreference(CoverBackgroundEnabledKey, false)
    val edge = if (extensionEnabled && sheet.progress > .05f && dynamic?.songId != metadata?.id)
        rememberCoverEdgeArtwork(metadata?.coverUrl) else null
    var readyArtwork by remember { mutableStateOf<String?>(null) }
    val edgeVisible = edge != null && readyArtwork == metadata?.coverUrl
    val edgeAlpha by animateFloatAsState(if (edgeVisible) 1f else 0f,
        animationSpec = PlayerMotionSpec.tween(220), label = "TabletCoverExtension")
    val foreground = if (edgeAlpha > .5f) playerForegroundForBackground(edge?.controlsColor) else Color.White
    var artworkBounds by remember { mutableStateOf(Rect.Zero) }
    var rootBounds by remember { mutableStateOf(Rect.Zero) }
    val detailsVisible by overlays.detailsVisible
    val panelStates = rememberSaveableStateHolder()
    val context = LocalContext.current
    val nav = LocalNavController.current
    fun showNowPlaying() = overlays.showLyrics()
    val selectedAction = when (panel) {
        PlayerPanel.Queue -> PlayerAction.QUEUE
        PlayerPanel.Comments -> PlayerAction.COMMENTS
        PlayerPanel.Lyrics -> null
    }

    BottomSheet(
        state = sheet, modifier = modifier, backgroundColor = Color(0xFF202024),
        containerMotion = BottomSheetContainerMotion.Sheet,
        transparentCollapsedContainer = true,
        onDismiss = { container.playerConnection.player.stop(); container.playerConnection.player.clearMediaItems() },
        collapsedContent = {},
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF202024)).onGloballyPositioned { rootBounds = it.boundsInRoot() }) {
            // The same retained mesh renderer used by the phone avoids an empty frame during song swaps.
            FluidBackground(metadata?.coverUrl, playing)
            if (edge != null && artworkBounds.width > 0f) CoverEdgeExtensionBackdrop(
                edge, edgeAlpha, artworkBounds.left - rootBounds.left, artworkBounds.top - rootBounds.top,
                artworkBounds.width,
            )
            CompositionLocalProvider(LocalPlayerForegroundColor provides foreground, LocalContentColor provides foreground) {
                TabletPlayerContent(
                    state = TabletPlayerUiState(
                        metadata?.title.orEmpty(), metadata?.artists?.joinToString(" / ") { it.name }.orEmpty(),
                        liked, panel, detailsVisible, foreground, sheet.isExpanded, animation,
                    ),
                    actions = TabletPlayerActions(
                        onCollapse = sheet::collapseSoft,
                        onTitle = { metadata?.let { overlays.showAlbumArtist(it.album, it.artists, it.coverUrl) } },
                        onLike = { metadata?.let { container.playerViewModel.like(it.id.toString()) } },
                        onMore = overlays::showMoreAction,
                        onShowNowPlaying = ::showNowPlaying,
                    ),
                    slots = TabletPlayerSlots(
                        artwork = { artworkModifier ->
                            TabletPlayerArtwork(metadata, playing, dynamic,
                                artworkModifier.onGloballyPositioned { artworkBounds = it.boundsInRoot() },
                                onReady = { readyArtwork = it },
                                onDynamicError = { metadata?.let(container.playerViewModel::fallbackDynamicCover) },
                                edgeArtwork = edge, edgeAlpha = edgeAlpha,
                                onPrevious = container.playerConnection::seekToPrevious,
                                onNext = container.playerConnection::seekToNext)
                        },
                        controls = { controlsModifier, compact ->
                            Box(controlsModifier) {
                                PlayerControlsSection(
                                    sliderPosition = container.sliderPosition, duration = container.duration,
                                    isPlaying = playing, playbackState = playbackState, playerConnection = container.playerConnection,
                                    onLyricClick = {},
                                    onAction = { overlays.handlePlayerAction(it) },
                                    actionProfile = overlays.actionProfile, selectedAction = selectedAction,
                                    onPositionPreview = container::previewSeek, onPositionChange = container::commitSeek,
                                    onMusicQualityClick = overlays::showMusicQualitySelection,
                                    isCompact = compact, horizontalPadding = 0.dp, includeNavigationBarPadding = false,
                                )
                            }
                        },
                        actions = { actionModifier ->
                            PlayerActionToolbar(
                                onLyricClick = {}, onAction = { overlays.handlePlayerAction(it) },
                                profile = overlays.actionProfile, selectedAction = selectedAction, modifier = actionModifier,
                            )
                        },
                        panel = { visible, panelModifier ->
                            panelStates.SaveableStateProvider(visible.name) {
                                val scheme = MaterialTheme.colorScheme.copy(
                                    onSurface = foreground, onSurfaceVariant = foreground.copy(alpha = .7f),
                                    outline = foreground.copy(alpha = .5f), primary = foreground,
                                )
                                MaterialTheme(colorScheme = scheme) {
                                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge.copy(color = foreground)) {
                                        when (visible) {
                                            PlayerPanel.Lyrics -> LyricScreen(
                                                lyricData = container.lyricLine, playerConnection = container.playerConnection,
                                                modifier = panelModifier,
                                                onClick = { metadata?.let { if (overlays.currentOverlayValue == OverlayState.None) overlays.showLyricSourceSelection(it) } },
                                                onLongClick = { source -> if (source == LyricSource.QQMusic) metadata?.let {
                                                    container.playerViewModel.deleteSongById(it.id.toString())
                                                    Toast.makeText(context, "已删除QQ音乐歌词", Toast.LENGTH_SHORT).show()
                                                } },
                                                controlsVisible = false, onToggleControls = {},
                                                anchor = LyricsAnchor.Fraction(.4f), bottomFade = LyricsFade.Fraction(.2f),
                                            )
                                            PlayerPanel.Queue -> PlaylistContent(onDismiss = ::showNowPlaying,
                                                modifier = panelModifier, showTitleBar = true)
                                            PlayerPanel.Comments -> PlayerCommentsContent(
                                                songId = metadata?.id?.toString().orEmpty(), modifier = panelModifier,
                                                foreground = foreground,
                                                onViewAllComments = { metadata?.let { song ->
                                                    showNowPlaying()
                                                    sheet.collapseThen { Screen.Comment.navigate(nav) { addPath(song.id.toString()) } }
                                                } },
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    ),
                )
            }
        }
    }
}
