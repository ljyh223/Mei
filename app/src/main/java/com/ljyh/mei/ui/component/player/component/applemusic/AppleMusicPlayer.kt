package com.ljyh.mei.ui.component.player.component.applemusic

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Size
import com.ljyh.mei.constants.MiniPlayerBarHeight
import com.ljyh.mei.constants.MiniPlayerCoverSize
import com.ljyh.mei.constants.PlayerHorizontalPadding
import com.ljyh.mei.constants.ThumbnailCornerRadius
import com.ljyh.mei.data.repository.DynamicCover
import com.ljyh.mei.ui.component.MiniPlayerBarContent
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.component.player.component.FluidBackground
import com.ljyh.mei.ui.component.player.component.DynamicCoverView
import com.ljyh.mei.ui.component.player.component.LocalPlayerForegroundColor
import com.ljyh.mei.ui.component.player.component.classic.component.FullScreenImageViewer
import com.ljyh.mei.ui.component.player.component.LyricScreen
import com.ljyh.mei.ui.component.player.component.PlayerControlsSection
import com.ljyh.mei.ui.component.player.overlay.PlayerOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.component.sheet.BottomSheet
import com.ljyh.mei.ui.component.sheet.BottomSheetContainerMotion
import com.ljyh.mei.ui.component.sheet.BottomSheetMorphSpec
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.sheet.HorizontalSwipeDirection
import com.ljyh.mei.ui.component.utils.lerp
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.motion.PlayerMotionSpec
import com.ljyh.mei.utils.UnitUtils.toPx
import kotlin.math.min
import com.kyant.backdrop.Backdrop


private val PlayerBackgroundSeamOverlap = 1.dp

private fun String?.asPaletteColor(): Color? = this?.takeIf(String::isNotBlank)?.let { hex ->
    runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrNull()
}

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
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val configuration = LocalConfiguration.current

    // --- Apple Music 特定状态 ---
    var showLyrics by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }

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

    // --- Apple Music 特定的 LaunchedEffect ---
    LaunchedEffect(state.isCollapsed) {
        if (state.isCollapsed) {
            showLyrics = false
        }
    }
    BackHandler(enabled = state.isExpanded && showLyrics) {
        showLyrics = false
    }


    // --- Animation & Geometry Calculation ---
    val lyricTransition = updateTransition(targetState = showLyrics, label = "LyricMode")
    val lyricAnimFraction by lyricTransition.animateFloat(
        label = "Fraction",
        transitionSpec = { PlayerMotionSpec.LyricModeSpring }
    ) { if (it) 1f else 0f }

    val sheetProgress = state.progress
    val expandedUiAlpha = PlayerMotionSpec.ExpandedUiReveal.transform(sheetProgress)
    val meshBackgroundAlpha = PlayerMotionSpec.BackgroundReveal.transform(sheetProgress)
    val portraitReveal by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (portraitCover != null) 1f else 0f,
        animationSpec = PlayerMotionSpec.tween(PlayerMotionSpec.CoverSwapDurationMillis),
        label = "PortraitArtworkReveal",
    )
    val portraitAlpha = if (portraitCover != null) {
        meshBackgroundAlpha * portraitReveal * (1f - lyricAnimFraction)
    } else 0f
    val coverUrl = mediaMetadata?.coverUrl
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = remember(isSystemInDarkTheme, state.value, state.collapsedBound) {
        if (isSystemInDarkTheme && state.value > state.collapsedBound) {
            lerp(colorScheme.surfaceContainer, Color.Black, state.progress)
        } else {
            colorScheme.surfaceContainer
        }
    }
    val portraitBackgroundColor = portraitCover?.palette?.bgColor.asPaletteColor() ?: backgroundColor
    val portraitBackdropState = remember(portraitCover?.cacheKey, portraitCover?.url) {
        PortraitMotionBackdropState()
    }
    val motionBackgroundColor = motionControlsColor(portraitBackgroundColor, portraitBackdropState.edgeColor)
    val playerForegroundColor = Color.White
    val playerSecondaryColor = Color.White.copy(alpha = 0.7f + 0.12f * portraitAlpha)

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
                .isAppearanceLightStatusBars = portraitAlpha <= 0.5f && !isSystemInDarkTheme
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

        // A. Mini Player (Bottom) — align with the full-width Material 3 player bar.
        val miniSize = with(density) { MiniPlayerCoverSize.toPx() }
        val miniStart = with(density) { 16.dp.toPx() }
        val miniRadius = with(density) { ThumbnailCornerRadius.toPx() }
        val miniTop = with(density) { 12.dp.toPx() }

        // B. Normal Expanded
        val topSafeArea = with(density) { WindowInsets.statusBars.getTop(this).toFloat() }

        val bottomControlsHeightDp = if (isCompactHeight || isLandscape) 220.dp else 300.dp
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
        val portraitArtworkHeight = minOf(screenWidth * (4f / 3f), maxHeight * 0.66f)

        // C. Header (Top Left Small)
        val headerSize = with(density) { 46.dp.toPx() }
        val headerTop = topSafeArea + with(density) { 12.dp.toPx() }
        val headerStart = with(density) { PlayerHorizontalPadding.toPx() }
        val headerRadius = with(density) { 4.dp.toPx() }

        // --- 2. 坐标插值 ---
        val targetSize = lerp(normalSize, headerSize, lyricAnimFraction)
        val targetTop = lerp(normalTop, headerTop, lyricAnimFraction)
        val targetStart = lerp(normalStart, headerStart, lyricAnimFraction)
        val targetRadius = with(density) { lerp(12.dp.toPx(), headerRadius, lyricAnimFraction) }

        // Keep the cover on the same continuous progress as the sheet. Forcing this to zero when
        // the collapsed anchor settles creates a visible second shrink on the final frame.
        val coverProgress = sheetProgress
        val finalSize = lerp(miniSize, targetSize, coverProgress)
        val finalTop = lerp(miniTop, targetTop, coverProgress)
        val finalStart = lerp(miniStart, targetStart, coverProgress)
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
        var mShadowElevation = 16.dp * shadowAlpha

        // --- 3. UI Structure ---
        BottomSheet(
            state = state,
            modifier = Modifier.fillMaxSize(),
            backgroundColor = Color.Transparent,
            morphSpec = BottomSheetMorphSpec(
                collapsedHorizontalMargin = 0.dp,
                collapsedCornerRadius = 0.dp,
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
                    drawCover = false,
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
                                alpha = state.revealProgress * (1f - portraitAlpha)
                                translationX = finalStart
                                translationY = finalTop
                                shadowElevation = mShadowElevation.toPx()
                                shape = RoundedCornerShape(finalRadius)
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
                                        showLyrics = !showLyrics
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
                                state.isExpanded && !showLyrics
                        }
                        if (activeCover != null) {
                            DynamicCoverView(
                                imageUrl = currentMetadata.coverUrl,
                                cover = activeCover,
                                playing = isPlaying,
                                onPlaybackError = { stateContainer.playerViewModel.fallbackDynamicCover(currentMetadata) },
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
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            },
        ) {
            // This OpenGL surface now keeps its full-screen size for the entire transition. The
            // sheet moves as one fixed page, avoiding the SurfaceView resize seam seen on devices.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(playerBackgroundHeight)
                    .background(backgroundColor),
            )
            if (portraitCover == null || showLyrics || portraitAlpha < 0.999f) {
                FluidBackground(
                    imageUrl = coverUrl,
                    isPlaying = isPlaying,
                    renderAlpha = meshBackgroundAlpha,
                )
            }
            if (portraitCover != null && portraitAlpha > 0.001f && mediaMetadata != null) {
                val backgroundTap = if (portraitAlpha > 0.5f) {
                    Modifier.clickable { showLyrics = true }
                } else Modifier
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = portraitAlpha }
                        .background(motionBackgroundColor),
                ) {
                    DynamicCoverView(
                        imageUrl = mediaMetadata!!.coverUrl,
                        cover = portraitCover,
                        playing = isPlaying && !showLyrics && sheetProgress > 0.5f,
                        onPlaybackError = {
                            mediaMetadata?.let(stateContainer.playerViewModel::fallbackDynamicCover)
                        },
                        onFrameSample = portraitBackdropState::accept,
                        bottomFadeColor = motionBackgroundColor,
                        fitVideoWidth = true,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(portraitArtworkHeight),
                    )
                    portraitBackdropState.edgeColor?.let { color ->
                        PortraitMotionColorWash(
                            color = color,
                            artworkHeight = portraitArtworkHeight,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(portraitArtworkHeight),
                    ) {
                        PortraitMotionTopScrim()
                    }
                    Box(modifier = Modifier.fillMaxSize().then(backgroundTap))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = expandedUiAlpha },
            ) {


                // Mode B: Lyric Player
                AnimatedVisibility(
                    visible = showLyrics,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(with(density) { (headerTop + headerSize).toDp() + 16.dp }))

                        LyricScreen(
                            lyricData = lyricLine,
                            playerConnection = stateContainer.playerConnection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = PlayerHorizontalPadding),
                            onClick = {
                                mediaMetadata?.let {
                                    if (overlayHandler.currentOverlayValue is OverlayState.None) {
                                        stateContainer.playerViewModel.searchQQSong(it.title)
                                        overlayHandler.showQQMusicSelection(
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

                        val currentX = (targetStart + headerSize + 12.dp.toPx(context)).toInt()
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
                        .background(Color.Transparent)
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                ) {
                    val isBottomBarVisible = !showLyrics || stateContainer.controlsVisible

                    AnimatedVisibility(
                        visible = isBottomBarVisible,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Column(Modifier.fillMaxWidth()) {

                            val isTitleVisible = !showLyrics && (!isCompactHeight && !isLandscape)

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
                                        subTitleStyle = if (portraitAlpha > 0.5f) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                                        iconColor = playerForegroundColor,
                                        needShadow = false,
                                        titleFontWeight = if (portraitAlpha > 0.5f) androidx.compose.ui.text.font.FontWeight.Normal else androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = PlayerHorizontalPadding + if (portraitAlpha > 0.5f) 12.dp else 0.dp,
                                                end = PlayerHorizontalPadding + if (portraitAlpha > 0.5f) 7.dp else 0.dp,
                                            )
                                            .padding(bottom = if (portraitAlpha > 0.5f) 28.dp else 12.dp)
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
                                    onLyricClick = { showLyrics = !showLyrics },
                                    onPlaylistClick = { overlayHandler.showPlaylist() },
                                    onSleepTimerClick = { overlayHandler.showSleepTimer() },
                                    onAddToPlaylistClick = {
                                        mediaMetadata?.let {
                                            overlayHandler.showAddToPlaylist(it.id)
                                        }
                                    },
                                    onMoreClick = { overlayHandler.showMoreAction() },
                                    portraitMotionStyle = portraitAlpha > 0.5f,
                                    isCompact = isCompactHeight || isLandscape
                                )
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
