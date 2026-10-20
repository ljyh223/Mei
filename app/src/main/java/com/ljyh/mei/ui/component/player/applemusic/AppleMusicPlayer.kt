package com.ljyh.mei.ui.component.player.applemusic

import android.app.Activity
import android.content.res.Configuration
import android.view.animation.DecelerateInterpolator
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Size
import com.ljyh.mei.constants.MiniPlayerBarHeight
import com.ljyh.mei.constants.MiniPlayerCornerRadius
import com.ljyh.mei.constants.MiniPlayerCoverSize
import com.ljyh.mei.constants.PlayerHorizontalPadding
import com.ljyh.mei.constants.ThumbnailCornerRadius
import com.ljyh.mei.constants.CoverBackgroundEnabledKey
import com.ljyh.mei.data.repository.DynamicCover
import com.ljyh.mei.utils.image.CoverExtensionMode
import com.ljyh.mei.ui.component.dialog.FullScreenImageViewer
import com.ljyh.mei.ui.component.player.overlay.PlayerOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.component.sheet.BottomSheet
import com.ljyh.mei.ui.component.sheet.BottomSheetContainerMotion
import com.ljyh.mei.ui.component.sheet.BottomSheetMorphSpec
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.sheet.HorizontalSwipeDirection
import com.ljyh.mei.ui.component.utils.lerp
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.motion.PlayerMotionSpec
import com.ljyh.mei.utils.UnitUtils.toPx
import com.ljyh.mei.utils.preferences.rememberPreference
import kotlin.math.min
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.constants.MiniPlayerHorizontalMargin
import com.ljyh.mei.ui.component.player.controls.LocalPlayerControlsBackdrop
import com.kyant.backdrop.Backdrop
import com.ljyh.mei.ui.component.player.cover.rememberCoverEdgeArtwork
import androidx.core.graphics.toColorInt
import com.ljyh.mei.ui.component.player.LocalPlayerForegroundColor
import com.ljyh.mei.ui.component.player.LyricScreen
import com.ljyh.mei.ui.component.player.MiniPlayerBarContent
import com.ljyh.mei.ui.component.player.PlayerCommentsContent
import com.ljyh.mei.ui.component.player.controls.PlayerControlsSection
import com.ljyh.mei.ui.component.player.cover.CoverEdgeExtensionBackdrop
import com.ljyh.mei.ui.component.player.cover.DynamicCoverView
import com.ljyh.mei.ui.component.player.cover.FluidBackground
import com.ljyh.mei.ui.component.player.cover.coverEdgeFeather
import com.ljyh.mei.ui.component.player.overlay.OverlayState


private val PlayerBackgroundSeamOverlap = 1.dp
private val ExpandedControlsBottomLift = 24.dp
private val ExpandedArtworkTitleSpacing = 12.dp
private const val ApplePausedArtworkScale = 0.85f
private val AppleCoverPlaybackInterpolator = DecelerateInterpolator(1.2f)
private val AppleCoverPlaybackEasing = Easing { fraction ->
    AppleCoverPlaybackInterpolator.getInterpolation(fraction)
}
private data class RetainedPortrait(val cover: DynamicCover, val imageUrl: String)

@OptIn(UnstableApi::class)
@Composable
fun AppleMusicPlayer(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    stateContainer: PlayerStateContainer,
    overlayHandler: PlayerOverlayHandler,
    collapsedBottomOffset: Dp = 0.dp,
    miniPlayerBottomInset: Dp = 0.dp,
    backdrop: Backdrop? = null,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val navController = LocalNavController.current
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val configuration = LocalConfiguration.current

    // --- Apple Music 特定状态 ---
    var showLyrics by remember { mutableStateOf(false) }
    val showComments by overlayHandler.phoneComments
    val detailsVisible = showLyrics || showComments
    var showFullImage by remember { mutableStateOf(false) }

    fun toggleLyrics() {
        if (detailsVisible) {
            showLyrics = false
            overlayHandler.showLyrics()
        } else {
            showLyrics = true
        }
    }

    // --- 从状态容器获取数据 ---
    val mediaMetadata by stateContainer.mediaMetadata
    val dynamicCover by stateContainer.playerViewModel.dynamicCover.collectAsState()
    val isPlaying by stateContainer.isPlaying
    val playbackState by stateContainer.playbackState
    val sliderPosition by remember { derivedStateOf { stateContainer.sliderPosition } }
    val duration by remember { derivedStateOf { stateContainer.duration } }
    val isDragging by remember { derivedStateOf { stateContainer.isDragging } }
    val lyricLine by remember { derivedStateOf { stateContainer.lyricLine } }
    val isLiked by stateContainer.isLiked
    val portraitCover = dynamicCover?.takeIf {
        it.source == DynamicCover.Source.APPLE_MUSIC && it.songId == mediaMetadata?.id
    }?.portraitVariant()
    val coverUrl = mediaMetadata?.coverUrl
    val coverBackgroundEnabled by rememberPreference(CoverBackgroundEnabledKey, defaultValue = false)
    val coverEdgeArtwork = if (
        coverBackgroundEnabled && state.progress > 0.05f &&
        dynamicCover?.songId != mediaMetadata?.id
    ) rememberCoverEdgeArtwork(coverUrl) else null
    val coverEdgeColor = coverEdgeArtwork?.controlsColor
    var readyCoverId by remember { mutableStateOf<Long?>(null) }
    var readyFullCover by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var readyFluidUrl by remember { mutableStateOf<String?>(null) }
    var readyPortraitKey by remember { mutableStateOf<String?>(null) }
    var retainedPortrait by remember { mutableStateOf<RetainedPortrait?>(null) }
    val portraitKey = portraitCover?.let { "${it.cacheKey}:${it.url}" }
    val portraitReady = portraitKey != null && readyPortraitKey == portraitKey
    val staticArtworkReady = mediaMetadata != null && readyCoverId == mediaMetadata?.id
    val miniOverlayReady = mediaMetadata?.let { readyFullCover == (it.id to it.coverUrl) } == true
    val fluidReady = coverUrl != null && readyFluidUrl == coverUrl

    // --- Apple Music 特定的 LaunchedEffect ---
    LaunchedEffect(state.isCollapsed) {
        if (state.isCollapsed) {
            showLyrics = false
            overlayHandler.showLyrics()
            readyPortraitKey = null
        }
    }
    LaunchedEffect(showComments) {
        if (showComments) showLyrics = true
    }
    BackHandler(enabled = state.isExpanded && detailsVisible) {
        showLyrics = false
        overlayHandler.showLyrics()
    }


    // --- Animation & Geometry Calculation ---
    val lyricTransition = updateTransition(targetState = detailsVisible, label = "LyricMode")
    val lyricAnimFraction by lyricTransition.animateFloat(
        label = "Fraction",
        transitionSpec = { PlayerMotionSpec.LyricModeSpring }
    ) { if (it) 1f else 0f }
    val playingCoverScale by animateFloatAsState(
        targetValue = when {
            !isPlaying -> 1f
            isDragging -> 0.95f / ApplePausedArtworkScale
            else -> 1f / ApplePausedArtworkScale
        },
        animationSpec = PlayerMotionSpec.tween(300, easing = AppleCoverPlaybackEasing),
        label = "PlayingCoverScale",
    )

    val sheetProgress = state.progress
    val expandedUiAlpha = PlayerMotionSpec.ExpandedUiReveal.transform(sheetProgress)
    val meshBackgroundAlpha = PlayerMotionSpec.BackgroundReveal.transform(sheetProgress)
    val portraitReveal by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (portraitCover != null) {
            if (portraitReady) 1f else 0f
        } else if (retainedPortrait != null && !staticArtworkReady) 1f else 0f,
        animationSpec = PlayerMotionSpec.tween(PlayerMotionSpec.CoverSwapDurationMillis),
        label = "PortraitArtworkReveal",
    )
    val fluidReveal by animateFloatAsState(
        targetValue = if (fluidReady) 1f else 0f,
        animationSpec = PlayerMotionSpec.tween(PlayerMotionSpec.CoverSwapDurationMillis),
        label = "FluidArtworkReveal",
    )
    val displayedPortrait = portraitCover?.let { coverUrl?.let { url -> RetainedPortrait(it, url) } }
        ?: retainedPortrait?.takeIf { !staticArtworkReady || portraitReveal > 0.001f }
    val portraitAlpha = if (displayedPortrait != null &&
        (portraitCover == null || portraitReady)) {
        meshBackgroundAlpha * portraitReveal * (1f - lyricAnimFraction)
    } else 0f
    val edgeExtensionAlpha by key(mediaMetadata?.id, coverUrl) {
        animateFloatAsState(
            targetValue = if (
                coverEdgeArtwork?.mode?.let { it != CoverExtensionMode.NONE } == true &&
                staticArtworkReady && state.isExpanded &&
                !detailsVisible && portraitCover == null && portraitAlpha < 0.001f &&
                dynamicCover?.takeIf { it.songId == mediaMetadata?.id } == null
            ) 1f else 0f,
            animationSpec = PlayerMotionSpec.tween(PlayerMotionSpec.CoverSwapDurationMillis),
            label = "StaticCoverEdgeExtension",
        )
    }
    SideEffect {
        if (portraitCover != null && coverUrl != null) {
            retainedPortrait = RetainedPortrait(portraitCover, coverUrl)
        } else if (staticArtworkReady && portraitReveal <= 0.001f) {
            retainedPortrait = null
        }
    }
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = remember(isSystemInDarkTheme, state.value, state.collapsedBound) {
        if (isSystemInDarkTheme && state.value > state.collapsedBound) {
            lerp(colorScheme.surfaceContainer, Color.Black, state.progress)
        } else {
            colorScheme.surfaceContainer
        }
    }
    val portraitBackdropState = remember(displayedPortrait?.cover?.cacheKey, displayedPortrait?.cover?.url) {
        PortraitMotionBackdropState(displayedPortrait?.cover?.cacheKey)
    }
    val portraitFallbackColor = remember(displayedPortrait?.cover?.palette?.bgColor) {
        displayedPortrait?.cover?.palette?.bgColor?.let { hex ->
            runCatching { Color("#${hex.removePrefix("#")}".toColorInt()) }.getOrNull()
        }
    } ?: colorScheme.surfaceVariant
    DisposableEffect(portraitBackdropState) {
        onDispose { portraitBackdropState.bind(null) }
    }
    val edgeForeground = if (coverEdgeColor?.luminance()?.let { it > 0.179f } == true) {
        Color.Black
    } else Color.White
    val playerForegroundColor = lerp(Color.White, edgeForeground, edgeExtensionAlpha)
    val playerSecondaryColor = playerForegroundColor.copy(
        alpha = lerp(0.7f + 0.12f * portraitAlpha, 0.88f, edgeExtensionAlpha),
    )

    val activity = context as? Activity
    DisposableEffect(activity, isSystemInDarkTheme) {
        onDispose {
            activity?.let {
                WindowCompat.getInsetsController(it.window, it.window.decorView)
                    .isAppearanceLightStatusBars = !isSystemInDarkTheme
            }
        }
    }
    SideEffect {
        activity?.let {
            WindowCompat.getInsetsController(it.window, it.window.decorView)
                .isAppearanceLightStatusBars = if (edgeExtensionAlpha > 0.5f && coverEdgeColor != null) {
                    coverEdgeColor.luminance() > 0.179f
                } else portraitAlpha <= 0.5f && !isSystemInDarkTheme
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        // --- 响应式布局判断 ---
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isCompactHeight = maxHeightPx < with(density) { 600.dp.toPx() }

        // --- 1. 定义关键尺寸参数 ---

        // A. Mini player artwork shares the floating bar inset.
        val miniSize = with(density) { MiniPlayerCoverSize.toPx() }
        val miniStart = with(density) { (MiniPlayerHorizontalMargin + 16.dp).toPx() }
        val miniRadius = with(density) { ThumbnailCornerRadius.toPx() }
        val miniTop = with(density) { 12.dp.toPx() }

        // B. Normal Expanded
        val topSafeArea = with(density) { WindowInsets.statusBars.getTop(this).toFloat() }

        val bottomControlsHeightDp = if (isCompactHeight || isLandscape) 190.dp else 270.dp
        val bottomControlsHeight = with(density) { bottomControlsHeightDp.toPx() }

        // --- 动态计算封面大小 ---
        val normalPaddingH = with(density) { (PlayerHorizontalPadding + 24.dp).toPx() }
        val maxAvailableWidth = maxWidthPx - (normalPaddingH * 2)

        val minTopMargin = with(density) { 16.dp.toPx() }
        val availableVerticalSpace = maxHeightPx - bottomControlsHeight - topSafeArea - minTopMargin

        val normalSize = min(maxAvailableWidth, availableVerticalSpace.coerceAtLeast(0f))

        val realAvailableHeight = (maxHeightPx - bottomControlsHeight - topSafeArea)
        val verticalBias = (realAvailableHeight - normalSize) / 2
        val normalTop = topSafeArea + verticalBias.coerceAtLeast(with(density) { 12.dp.toPx() })

        val normalStart = (maxWidthPx - normalSize) / 2

        // Apple's tall clip is 3:4. Keep its full width instead of widening a taller container
        // and then center-cropping away the lettering and logos at the sides.
        val portraitArtworkHeight = minOf(screenWidth * (4f / 3f), maxHeight * 0.77f)

        // C. Header (Top Left Small)
        val headerSize = with(density) { 52.dp.toPx() }
        val headerTop = topSafeArea + with(density) { 12.dp.toPx() }
        val headerStart = with(density) { PlayerHorizontalPadding.toPx() }
        val headerRadius = with(density) { 4.dp.toPx() }

        // --- 2. 坐标插值 ---
        // Keep the cover on the same continuous progress as the sheet. Forcing this to zero when
        // the collapsed anchor settles creates a visible second shrink on the final frame.
        val coverProgress = sheetProgress
        val expandedCoverFraction = coverProgress * (1f - lyricAnimFraction)
        val expandedCoverLift = if (isCompactHeight || isLandscape) 0f else {
            with(density) { ExpandedArtworkTitleSpacing.toPx() }
        }
        val expandedSize = lerp(miniSize, normalSize, coverProgress)
        val expandedTop = lerp(miniTop, normalTop, coverProgress) -
            expandedCoverLift * coverProgress
        val expandedStart = lerp(miniStart, normalStart, coverProgress)
        val expandedScale = 1f + (playingCoverScale - 1f) * coverProgress
        val expandedVisualSize = expandedSize * expandedScale
        val expandedVisualInset = (expandedVisualSize - expandedSize) / 2f

        // The graphics layer scales around its center. Interpolate the visible artwork bounds,
        // then convert back to the layer's layout bounds; otherwise the shrinking scale adds a
        // second vertical motion near the lyric header (and on the way back).
        val lyricCoverProgress = coverProgress * lyricAnimFraction
        val visualSize = lerp(expandedVisualSize, headerSize, lyricCoverProgress)
        val visualTop = lerp(expandedTop - expandedVisualInset, headerTop, lyricCoverProgress)
        val visualStart = lerp(expandedStart - expandedVisualInset, headerStart, lyricCoverProgress)
        val coverScale = 1f + (playingCoverScale - 1f) * expandedCoverFraction
        val finalSize = visualSize / coverScale
        val finalInset = (visualSize - finalSize) / 2f
        val finalTop = visualTop + finalInset
        val finalStart = visualStart + finalInset
        val targetRadius = with(density) { lerp(12.dp.toPx(), headerRadius, lyricAnimFraction) }
        val finalRadius = lerp(miniRadius, targetRadius, coverProgress)
        val backgroundExpansion =
            PlayerMotionSpec.PlayerBackgroundExpansion.transform(sheetProgress)
        val effectiveBottomMargin =
            lerp(collapsedBottomOffset, 0.dp, sheetProgress) * state.revealProgress
        // The mini player's own surface now occupies the gesture inset on secondary screens.
        val collapsedBottomClearance = (
            state.collapsedBound - MiniPlayerBarHeight - miniPlayerBottomInset +
                collapsedBottomOffset
            ).coerceAtLeast(0.dp)
        val playerBackgroundHeight = (
            state.value + effectiveBottomMargin -
                collapsedBottomClearance * (1f - backgroundExpansion)
            ).coerceAtLeast(MiniPlayerBarHeight) + PlayerBackgroundSeamOverlap

        val shadowAlpha = if (sheetProgress > PlayerMotionSpec.CoverShadowStartProgress) {
            1f - lyricAnimFraction
        } else {
            0f
        }
        val mShadowElevation = 16.dp * shadowAlpha * (1f - edgeExtensionAlpha)

        val controlsBackdrop = rememberLayerBackdrop()
        val captureControlsBackdrop = LocalMeiGlassEnabled.current && expandedUiAlpha > 0f

        // --- 3. UI Structure ---
        BottomSheet(
            state = state,
            modifier = Modifier.fillMaxSize(),
            backgroundColor = Color.Transparent,
            morphSpec = BottomSheetMorphSpec(
                collapsedHorizontalMargin = 0.dp,
                collapsedCornerRadius = MiniPlayerCornerRadius,
                roundBottomCorners = false,
                expandedHorizontalMargin = 0.dp,
                expandedCornerRadius = 0.dp,
                collapsedHeight = MiniPlayerBarHeight + miniPlayerBottomInset,
                collapsedBottomMargin = collapsedBottomOffset,
                expandedBottomMargin = 0.dp,
            ),
            containerMotion = BottomSheetContainerMotion.Sheet,
            keepExpandedContentComposed = true,
            // The bar is a full-width Material 3 surface; the player cover is drawn by the
            // sheet-local overlay so it shares the sheet's coordinate space.
            transparentCollapsedContainer = true,
            onDismiss = {
                stateContainer.playerConnection.player.stop()
                stateContainer.playerConnection.player.clearMediaItems()
            },
            onHorizontalSwipe = { direction ->
                if (!state.isExpanded) {
                    when (direction) {
                        HorizontalSwipeDirection.Left -> stateContainer.playerConnection.seekToNext()
                        HorizontalSwipeDirection.Right -> stateContainer.playerConnection.seekToPrevious()
                    }
                }
            },
            collapsedContent = {
                MiniPlayerBarContent(
                    title = mediaMetadata?.title,
                    artist = mediaMetadata?.artists?.joinToString { it.name },
                    coverUrl = mediaMetadata?.coverUrl,
                    isPlaying = isPlaying,
                    canSkipNext = stateContainer.canSkipNext.value,
                    onClick = state::expandSoft,
                    onPlayPause = {
                        val player = stateContainer.playerConnection.player
                        if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                            player.seekTo(0, 0)
                            player.playWhenReady = true
                        } else {
                            if (isPlaying) player.pause() else player.play()
                        }
                    },
                    onNext = stateContainer.playerConnection::seekToNext,
                    drawCover = true,
                    bottomInset = miniPlayerBottomInset,
                )
            },
            overlayContent = {
                mediaMetadata?.let { currentMedia ->
                    AnimatedContent(
                        targetState = currentMedia,
                        transitionSpec = {
                            val enter = if (state.revealProgress <= 0f) {
                                fadeIn(
                                    animationSpec = PlayerMotionSpec.tween(
                                        PlayerMotionSpec.InitialCoverEnterDurationMillis,
                                    ),
                                )
                            } else {
                                fadeIn(
                                    animationSpec = PlayerMotionSpec.tween(
                                        PlayerMotionSpec.CoverSwapDurationMillis,
                                    ),
                                ) +
                                    scaleIn(
                                        initialScale = PlayerMotionSpec.CoverSwapInitialScale,
                                        animationSpec = PlayerMotionSpec.tween(
                                            PlayerMotionSpec.CoverSwapDurationMillis,
                                        ),
                                    )
                            }
                            enter.togetherWith(
                                fadeOut(
                                    animationSpec = PlayerMotionSpec.tween(
                                        PlayerMotionSpec.CoverSwapDurationMillis,
                                    ),
                                ),
                            )
                        },
                        label = "CoverTransition",
                        modifier = Modifier
                            .graphicsLayer {
                                // The collapsed bar already has the cached thumbnail. Keep it
                                // visible until the full-size overlay for this song is ready.
                                alpha = state.revealProgress * (1f - portraitAlpha) *
                                    if (state.isExpanded || miniOverlayReady) 1f else 0f
                                translationX = finalStart
                                translationY = finalTop
                                scaleX = coverScale
                                scaleY = coverScale
                                shadowElevation = mShadowElevation.toPx()
                                shape = RoundedCornerShape(finalRadius * (1f - edgeExtensionAlpha))
                                clip = true
                            }
                            .size(
                                width = with(density) { finalSize.toDp() },
                                height = with(density) { finalSize.toDp() },
                            )
                            .combinedClickable(
                                onClick = {
                                    if (!state.isExpanded) {
                                        state.expandSoft()
                                    } else {
                                        toggleLyrics()
                                    }
                                },
                                onLongClick = {
                                    if (state.isExpanded) {
                                        showFullImage = true
                                    }
                                },
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) { currentMetadata ->
                        val activeCover = dynamicCover?.takeIf {
                            it.songId == currentMetadata.id && portraitCover == null &&
                                state.isExpanded && !detailsVisible
                        }
                        if (activeCover != null) {
                            DynamicCoverView(
                                imageUrl = currentMetadata.coverUrl,
                                cover = activeCover,
                                playing = isPlaying,
                                onPlaybackError = { stateContainer.playerViewModel.fallbackDynamicCover(currentMetadata) },
                                onArtworkReady = {
                                    if (currentMetadata.id == mediaMetadata?.id) {
                                        readyCoverId = currentMetadata.id
                                        readyFullCover = currentMetadata.id to currentMetadata.coverUrl
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(currentMetadata.coverUrl)
                                    .size(Size.ORIGINAL)
                                    .precision(Precision.EXACT)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                onSuccess = {
                                    if (currentMetadata.id == mediaMetadata?.id) {
                                        readyCoverId = currentMetadata.id
                                        readyFullCover = currentMetadata.id to currentMetadata.coverUrl
                                    }
                                },
                                onError = {
                                    if (currentMetadata.id == mediaMetadata?.id) readyCoverId = currentMetadata.id
                                },
                                modifier = Modifier.fillMaxSize().coverEdgeFeather(
                                    coverEdgeArtwork.takeIf { currentMetadata.coverUrl == coverUrl },
                                    edgeExtensionAlpha,
                                ),
                            )
                        }
                    }
                }
            },
        ) {
            CompositionLocalProvider(
                LocalPlayerControlsBackdrop provides controlsBackdrop.takeIf { captureControlsBackdrop },
            ) {
                Box(Modifier.fillMaxSize().then(
                    if (captureControlsBackdrop) Modifier.layerBackdrop(controlsBackdrop) else Modifier,
                )) {
                    // This OpenGL surface now keeps its full-screen size for the entire transition. The
                    // sheet moves as one fixed page, avoiding the SurfaceView resize seam seen on devices.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(playerBackgroundHeight)
                            .background(if (state.isExpanded && staticArtworkReady && portraitCover == null &&
                                portraitAlpha < 0.001f && coverEdgeColor != null && edgeExtensionAlpha > 0.001f
                            ) coverEdgeColor else lerp(
                                portraitBackdropState.edgeColor ?: portraitFallbackColor,
                                backgroundColor,
                                fluidReveal,
                            )),
                    )
                    // Keep the GL surface and its last album frame alive across cover changes.
                    FluidBackground(
                        imageUrl = coverUrl,
                        isPlaying = isPlaying,
                        renderAlpha = meshBackgroundAlpha * fluidReveal * (1f - edgeExtensionAlpha),
                        onArtworkReady = { readyFluidUrl = it },
                    )
                    coverEdgeArtwork?.let { artwork ->
                        CoverEdgeExtensionBackdrop(
                            artwork = artwork,
                            alpha = edgeExtensionAlpha,
                            coverLeft = visualStart,
                            coverTop = visualTop,
                            coverSize = visualSize,
                        )
                    }
                    if (displayedPortrait != null && sheetProgress > 0f && mediaMetadata != null) {
                        val backgroundTap = if (portraitAlpha > 0.5f) {
                            Modifier.clickable { showLyrics = true; overlayHandler.showLyrics() }
                        } else Modifier
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = portraitAlpha }
                                .background(backgroundColor),
                        ) {
                            DynamicCoverView(
                                imageUrl = displayedPortrait.imageUrl,
                                cover = displayedPortrait.cover,
                                playing = isPlaying && displayedPortrait.cover.songId == mediaMetadata!!.id &&
                                    !detailsVisible && sheetProgress > 0.5f,
                                onPlaybackError = {
                                    mediaMetadata?.takeIf { it.id == displayedPortrait.cover.songId }
                                        ?.let(stateContainer.playerViewModel::fallbackDynamicCover)
                                },
                                onArtworkReady = {
                                    if (displayedPortrait.cover.songId == mediaMetadata?.id) {
                                        readyPortraitKey = "${displayedPortrait.cover.cacheKey}:${displayedPortrait.cover.url}"
                                    }
                                },
                                onFrameSample = portraitBackdropState::accept,
                                onPreviewSample = portraitBackdropState::acceptPreview,
                                fitVideoWidth = true,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .height(portraitArtworkHeight),
                            )
                            AndroidView(
                                factory = { PortraitMotionBackdropView(it).apply {
                                    controlColor = (portraitBackdropState.edgeColor ?: portraitFallbackColor).toArgb()
                                } },
                                update = { view ->
                                    portraitBackdropState.bind(view)
                                    view.videoBottomPx = with(density) { portraitArtworkHeight.toPx() }
                                    view.controlColor = (portraitBackdropState.edgeColor ?: portraitFallbackColor).toArgb()
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                            Box(modifier = Modifier.fillMaxSize().then(backgroundTap))
                        }
                    }

                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = expandedUiAlpha },
                ) {


                    // Mode B: Lyric Player
                    AnimatedVisibility(
                        visible = detailsVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Spacer(modifier = Modifier.height(with(density) { (headerTop + headerSize).toDp() + 16.dp }))

                            if (showComments) {
                                PlayerCommentsContent(
                                    songId = mediaMetadata?.id?.toString().orEmpty(),
                                    onControlsVisibilityChange = { stateContainer.controlsVisible = it },
                                    onViewAllComments = {
                                        mediaMetadata?.let { song ->
                                            state.collapseThen {
                                                Screen.Comment.navigate(navController) {
                                                    addPath(song.id.toString())
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                        .padding(horizontal = PlayerHorizontalPadding),
                                )
                            } else LyricScreen(
                                lyricData = lyricLine,
                                playerConnection = stateContainer.playerConnection,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = PlayerHorizontalPadding),
                                onClick = {
                                    mediaMetadata?.let {
                                        if (overlayHandler.currentOverlayValue is OverlayState.None) {
                                            overlayHandler.showLyricSourceSelection(
                                                mediaMetadata = it
                                            )
                                        }
                                    }
                                },
                                onLongClick = { source ->
                                    if (source == LyricSource.QQMusic && mediaMetadata != null) {
                                        stateContainer.playerViewModel.deleteSongById(id = mediaMetadata!!.id.toString())
                                        android.widget.Toast.makeText(context, "已删除QQ音乐歌词", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                controlsVisible = stateContainer.controlsVisible,
                                onToggleControls = { stateContainer.controlsVisible = it },
                            )
                            val spacerHeight by animateDpAsState(
                                targetValue = if (stateContainer.controlsVisible) bottomControlsHeightDp else 16.dp,
                                label = "spacer"
                            )
                            Spacer(modifier = Modifier.height(spacerHeight))
                        }
                    }


                    // Mode C: Header Info
                    if (mediaMetadata != null) {
                        val fraction = lyricAnimFraction
                        val headerTextAlpha =
                            PlayerMotionSpec.LyricHeaderReveal.transform(fraction) * sheetProgress

                        if (headerTextAlpha > 0.01f) {
                            val headerTextWidth =
                                screenWidth - with(density) { (headerStart + headerSize).toDp() } - 24.dp
                            val slideUpOffset =
                                with(density) { (20.dp.toPx() * (1f - fraction)).toInt() }

                            val headerTextStart = lerp(normalStart, headerStart, fraction)
                            val currentX = (headerTextStart + headerSize + 12.dp.toPx(context)).toInt()
                            val fixedY = headerTop.toInt() + slideUpOffset

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        alpha = headerTextAlpha
                                        val scale = 0.9f + (0.1f * fraction)
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .offset {
                                        IntOffset(
                                            x = currentX,
                                            y = fixedY
                                        )
                                    }
                                    .height(with(density) { headerSize.toDp() })
                                    .width(headerTextWidth),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Title(
                                    title = mediaMetadata!!.title,
                                    subTitle = mediaMetadata!!.artists.joinToString { it.name },
                                    isLiked = isLiked,
                                    onLikeClick = { mediaMetadata?.let { stateContainer.playerViewModel.like(it.id.toString()) } },
                                    onMoreClick = { overlayHandler.showMoreAction() },
                                    onTitleClick = {
                                        mediaMetadata?.let {
                                            overlayHandler.showAlbumArtist(
                                                album = it.album,
                                                artists = it.artists,
                                                cover = it.coverUrl
                                            )
                                        }
                                    },
                                    titleStyle = MaterialTheme.typography.titleMedium,
                                    subTitleStyle = MaterialTheme.typography.bodySmall,
                                    needShadow = false,
                                    titleColor = playerForegroundColor,
                                    subTitleColor = playerSecondaryColor,
                                    iconColor = playerForegroundColor,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // --- 统一的底部控制区域 ---
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = ExpandedControlsBottomLift)
                            .background(Color.Transparent)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                    ) {
                        val isBottomBarVisible = !detailsVisible || stateContainer.controlsVisible

                        AnimatedVisibility(
                            visible = isBottomBarVisible,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut()
                        ) {
                            Column(Modifier.fillMaxWidth()) {

                                val isTitleVisible = !detailsVisible && (!isCompactHeight && !isLandscape)

                                if (isTitleVisible) {
                                    mediaMetadata?.let {
                                        Title(
                                            title = it.title,
                                            subTitle = it.artists.joinToString { artist -> artist.name },
                                            isLiked = isLiked,
                                            onLikeClick = { stateContainer.playerViewModel.like(it.id.toString()) },
                                            onMoreClick = { overlayHandler.showMoreAction() },
                                            onTitleClick = {
                                                overlayHandler.showAlbumArtist(
                                                    album = it.album,
                                                    artists = it.artists,
                                                    cover = it.coverUrl
                                                )
                                            },
                                            titleColor = playerForegroundColor,
                                            subTitleColor = playerSecondaryColor,
                                            iconColor = playerForegroundColor,
                                            needShadow = false,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = PlayerHorizontalPadding)
                                                .padding(bottom = 16.dp)
                                        )
                                    }
                                }

                                CompositionLocalProvider(
                                    LocalPlayerForegroundColor provides playerForegroundColor,
                                ) {
                                    PlayerControlsSection(
                                        sliderPosition = sliderPosition,
                                        duration = duration,
                                        isPlaying = isPlaying,
                                        playbackState = playbackState,
                                        playerConnection = stateContainer.playerConnection,
                                        onLyricClick = { toggleLyrics() },
                                        onPlaylistClick = { overlayHandler.showPlaylist() },
                                        onSleepTimerClick = { overlayHandler.showSleepTimer() },
                                        onAddToPlaylistClick = {
                                            mediaMetadata?.let {
                                                overlayHandler.showAddToPlaylist(it.id)
                                            }
                                        },
                                        onMoreClick = { overlayHandler.showMoreAction() },
                                        onPositionPreview = stateContainer::previewSeek,
                                        onPositionChange = stateContainer::commitSeek,
                                        onMusicQualityClick = overlayHandler::showMusicQualitySelection,
                                        portraitMotionStyle = portraitAlpha > 0.5f,
                                        isCompact = isCompactHeight || isLandscape
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFullImage && mediaMetadata?.coverUrl != null) {
            FullScreenImageViewer(
                imageUrl = mediaMetadata!!.coverUrl,
                onDismiss = { showFullImage = false }
            )
        }
    }
}
