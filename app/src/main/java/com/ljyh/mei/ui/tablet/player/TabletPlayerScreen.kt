package com.ljyh.mei.ui.tablet.player

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import com.ljyh.mei.ui.tablet.component.TabletMiniPlayer
import com.ljyh.mei.constants.CoverStyle
import com.ljyh.mei.constants.CoverStyleKey
import androidx.compose.ui.graphics.Color
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
    expansionOrigin: PlayerExpansionOrigin? = null,
    miniArtwork: Painter? = null,
    shortWindowLayout: Boolean = false,
) {
    val metadata by container.mediaMetadata
    val playing by container.isPlaying
    val playbackState by container.playbackState
    val liked by container.isLiked
    val dynamic by container.playerViewModel.dynamicCover.collectAsState()
    val coverUrl = metadata?.coverUrl
    val activeDynamicCover = dynamic?.takeIf { sheet.isExpanded && it.songId == metadata?.id }
    val panel by overlays.panel
    val density = LocalDensity.current
    val coverStyle by rememberEnumPreference(CoverStyleKey, CoverStyle.Square)
    val playbackScale by animateFloatAsState(if (playing) 1f else .9f,
        animationSpec = PlayerMotionSpec.CoverPlayStateSpring, label = "TabletCoverScale")
    var fullArtwork by remember { mutableStateOf<Pair<String, Painter>?>(null) }
    var readyFluidUrl by remember { mutableStateOf<String?>(null) }
    val fluidReveal by animateFloatAsState(if (readyFluidUrl != null) 1f else 0f,
        animationSpec = PlayerMotionSpec.tween(PlayerMotionSpec.CoverSwapDurationMillis), label = "TabletFluidReveal")
    val fullPainter = fullArtwork?.takeIf { it.first == coverUrl }?.second
    val sharedPainter = fullPainter ?: miniArtwork
    val sideBarOrigin = expansionOrigin.takeIf { shortWindowLayout }
    val dockOrigin = expansionOrigin.takeUnless { shortWindowLayout }
    val sharedArtworkVisible = expansionOrigin != null && !sheet.isExpanded
    val animation by rememberEnumPreference(TabletAnimationStyleKey, TabletAnimationStyle.FLIP_3D)
    val extensionEnabled by rememberPreference(CoverBackgroundEnabledKey, false)
    val edge = if (extensionEnabled && sheet.progress > .05f && activeDynamicCover == null) {
        rememberCoverEdgeArtwork(coverUrl)
    } else null
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

    val targetArtwork = artworkBounds.translate(-rootBounds.topLeft)
    val visibleArtwork = if (expansionOrigin != null) sharedPlayerArtworkBounds(
        expansionOrigin.artwork, targetArtwork, playbackScale, sheet.progress,
    ) else sharedPlayerArtworkBounds(targetArtwork, targetArtwork, playbackScale, 1f)
    val edgeVisible = edge != null && fullArtwork?.first == coverUrl && visibleArtwork.width > 0f
    val edgeReveal by key(coverUrl) {
        animateFloatAsState(if (edgeVisible) 1f else 0f,
            animationSpec = PlayerMotionSpec.tween(220), label = "TabletCoverExtension")
    }
    val edgeAlpha = if (edge != null) edgeReveal else 0f
    val foreground = if (edgeAlpha > .5f) playerForegroundForBackground(edge?.controlsColor) else Color.White

    BottomSheet(
        state = sheet, modifier = modifier,
        backgroundColor = Color(0xFF202024),
        expansionBounds = expansionOrigin?.surface,
        expansionRadius = sideBarOrigin?.let { with(density) { (it.surface.size.minDimension / 2f).toDp() } } ?: 24.dp,
        morphSpec = dockOrigin?.let { origin -> BottomSheetMorphSpec(
            collapsedHeight = with(density) { origin.surface.height.toDp() },
            collapsedBottomMargin = sheet.expandedBound - sheet.collapsedBound - with(density) { origin.surface.top.toDp() },
        ) },
        containerMotion = BottomSheetContainerMotion.Sheet,
        keepExpandedContentComposed = dockOrigin != null || (sideBarOrigin != null && sheet.progress > 0f),
        transparentCollapsedContainer = sideBarOrigin == null,
        onDismiss = { container.playerConnection.player.stop(); container.playerConnection.player.clearMediaItems() },
        collapsedContent = {
            dockOrigin?.let { origin ->
                TabletMiniPlayer(
                    metadata = metadata, playing = playing, canSkipNext = container.canSkipNext.value,
                    enabled = sheet.isCollapsed, onOpen = sheet::expandSoft,
                    onPlayPause = {
                        val player = container.playerConnection.player
                        if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                            player.seekTo(0, 0); player.playWhenReady = true
                        } else if (playing) player.pause() else player.play()
                    },
                    onNext = container.playerConnection::seekToNext,
                    modifier = Modifier.align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset(x = with(density) { origin.surface.left.toDp() })
                        .size(with(density) { origin.surface.width.toDp() }, with(density) { origin.surface.height.toDp() }),
                    drawCover = false,
                )
            }
        },
        overlayContent = {
            if (sharedArtworkVisible) {
                val radius = with(density) {
                    val targetRadius = (if (coverStyle == CoverStyle.Circle) targetArtwork.width / 2f else 12.dp.toPx()) *
                        playbackScale * (1f - edgeAlpha)
                    val sourceRadius = sideBarOrigin?.artwork?.size?.minDimension?.div(2f) ?: 12.dp.toPx()
                    (sourceRadius + (targetRadius - sourceRadius) * sheet.progress).toDp()
                }
                Box(Modifier.align(AbsoluteAlignment.TopLeft).graphicsLayer {
                    translationX = visibleArtwork.left; translationY = visibleArtwork.top
                    shape = RoundedCornerShape(radius); clip = true
                    shadowElevation = (12.dp * playbackScale * sheet.progress * (1f - edgeAlpha)).toPx()
                }.size(with(density) { visibleArtwork.width.toDp() }, with(density) { visibleArtwork.height.toDp() })
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .coverEdgeFeather(edge, edgeAlpha * sheet.progress), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    if (sharedPainter != null) Image(sharedPainter, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().fillArtworkEdges())
                    else Icon(Icons.Rounded.MusicNote, null)
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize()
            .background(if (expansionOrigin != null) Color.Transparent else Color(0xFF202024))
            .onGloballyPositioned { rootBounds = Rect(it.positionInRoot(), it.size.toSize()) }) {
            if (sheet.progress > 0f) FluidBackground(coverUrl, playing,
                renderAlpha = fluidReveal * (1f - edgeAlpha) * (if (expansionOrigin != null)
                    PlayerMotionSpec.BackgroundReveal.transform(sheet.progress) else 1f),
                onRenderedArtworkChange = { readyFluidUrl = it }, clipToContainer = true)
            if (edge != null && visibleArtwork.width > 0f) CoverEdgeExtensionBackdrop(
                edge, edgeAlpha, visibleArtwork.left, visibleArtwork.top, visibleArtwork.width,
            )
            CompositionLocalProvider(LocalPlayerForegroundColor provides foreground, LocalContentColor provides foreground) {
                TabletPlayerContent(
                    shortWindowLayout = shortWindowLayout,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (expansionOrigin != null) PlayerMotionSpec.ExpandedUiReveal.transform(sheet.progress) else 1f
                    },
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
                            TabletPlayerArtwork(metadata, playing, activeDynamicCover,
                                artworkModifier.onGloballyPositioned {
                                    artworkBounds = Rect(it.positionInRoot(), it.size.toSize())
                                }.graphicsLayer { alpha = if (sharedArtworkVisible) 0f else 1f },
                                onPainterReady = { url, painter -> if (url == coverUrl) fullArtwork = url to painter },
                                playbackScale = playbackScale, interactive = sheet.isExpanded,
                                keepCoverVisible = shortWindowLayout,
                                poster = sharedPainter.takeIf { shortWindowLayout },
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
                                onLyricClick = { overlays.selectPanel(PlayerPanel.Lyrics) },
                                    onAction = { overlays.handlePlayerAction(it) },
                                    actionProfile = overlays.actionProfile, selectedAction = selectedAction,
                                    onPositionPreview = container::previewSeek, onPositionChange = container::commitSeek,
                                    onMusicQualityClick = overlays::showMusicQualitySelection,
                                isCompact = compact, horizontalPadding = 0.dp, includeNavigationBarPadding = false,
                                tightControls = compact,
                                )
                            }
                        },
                        actions = { actionModifier ->
                            PlayerActionToolbar(
                                onLyricClick = { overlays.selectPanel(PlayerPanel.Lyrics) }, onAction = { overlays.handlePlayerAction(it) },
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
                                                anchor = LyricsAnchor.Fraction(if (shortWindowLayout) .25f else .4f),
                                                bottomFade = LyricsFade.Fraction(if (shortWindowLayout) .10f else .2f),
                                                lyricsVerticalPadding = if (shortWindowLayout) 0.dp else 8.dp,
                                                onBackToControls = if (shortWindowLayout) ::showNowPlaying else null,
                                            )
                                            PlayerPanel.Queue -> PlaylistContent(
                                                onDismiss = ::showNowPlaying, modifier = panelModifier,
                                                titleTopPadding = if (shortWindowLayout) 0.dp else 8.dp,
                                            )
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
