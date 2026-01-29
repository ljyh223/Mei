package com.ljyh.mei.ui.component.player

import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Size
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.constants.PlayerHorizontalPadding
import com.ljyh.mei.constants.ThumbnailCornerRadius
import com.ljyh.mei.constants.UseQQMusicLyricKey
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.player.component.AppleMusicFluidBackground
import com.ljyh.mei.ui.component.player.component.LyricScreen
import com.ljyh.mei.ui.component.player.component.PlayerControlsSection
import com.ljyh.mei.ui.component.player.component.Title
import com.ljyh.mei.ui.component.player.component.sheet.AlbumArtistBottomSheet
import com.ljyh.mei.ui.component.player.component.sheet.MoreActionsSheet
import com.ljyh.mei.ui.component.player.component.sheet.PlayerActionSettingsSheet
import com.ljyh.mei.ui.component.player.component.sheet.PlaylistBottomSheet
import com.ljyh.mei.ui.component.player.component.sheet.QQMusicSelectSheet
import com.ljyh.mei.ui.component.player.component.sheet.SleepTimerSheet
import com.ljyh.mei.ui.component.playlist.AddToPlaylistSheet
import com.ljyh.mei.ui.component.playlist.CreatePlaylistSheet
import com.ljyh.mei.ui.component.sheet.BottomSheet
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.sheet.HorizontalSwipeDirection
import com.ljyh.mei.ui.component.utils.lerp
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.ui.model.MoreAction
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.utils.UnitUtils.toPx
import com.ljyh.mei.utils.encrypt.QRCUtils
import com.ljyh.mei.utils.lyric.createDefaultLyricData
import com.ljyh.mei.utils.lyric.mergeLyrics
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.min

@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val density = LocalDensity.current
    val context = LocalContext.current
    val navController = LocalNavController.current
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val configuration = LocalConfiguration.current
    var currentOverlay by remember { mutableStateOf<OverlayState>(OverlayState.None) }

    // --- State Management ---
    val useQQMusicLyric by rememberPreference(UseQQMusicLyricKey, defaultValue = true)
    val debug by rememberPreference(DebugKey, defaultValue = false)

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }

    // 控制歌词模式的开关
    var showLyrics by remember { mutableStateOf(false) }
//    var showQQMusicSelect by remember { mutableStateOf(false) }
    var areControlsVisible by remember { mutableStateOf(true) }

    // Lyric Logic
    val netLyricResult by playerViewModel.lyric.collectAsState()
    val qqLyricResult by playerViewModel.lyricResult.collectAsState()
    val amLyricResult by playerViewModel.amLyric.collectAsState()
    val qqSong by playerViewModel.qqSong.collectAsState()
    val isLiked by playerViewModel.like.collectAsState(initial = null)
    val allPlaylist by playerViewModel.localPlaylists.collectAsState()

    var lyricLine by remember { mutableStateOf(createDefaultLyricData("歌词加载中")) }


    // --- Effects (Slider & Lyrics) ---
    LaunchedEffect(playbackState, isPlaying, isDragging) {
        if (playbackState == STATE_READY && isPlaying && !isDragging) {
            while (isActive) {
                sliderPosition = playerConnection.player.currentPosition.toFloat()
                duration = playerConnection.player.duration.coerceAtLeast(1L)
                delay(50)
            }
        } else if (!isPlaying && !isDragging) {
            sliderPosition = playerConnection.player.currentPosition.toFloat()
            duration = playerConnection.player.duration.coerceAtLeast(1L)
        }
    }
    LaunchedEffect(mediaMetadata) {
        mediaMetadata?.let { meta ->
            lyricLine = createDefaultLyricData("歌词加载中")
            playerViewModel.clear()
            playerViewModel.mediaMetadata = meta
            playerViewModel.getLyricV1(meta.id.toString())
            playerViewModel.getAMLLyric(meta.id.toString())
            if (useQQMusicLyric) {
                playerViewModel.fetchQQSong(meta.id.toString())
                playerViewModel.searchNew(meta.title)
            }
            currentOverlay = OverlayState.None
        }
    }
    LaunchedEffect(qqSong) {
        qqSong?.let { song ->
            playerViewModel.getLyricNew(
                song.title, song.album, song.artist, song.duration.toLong(), song.qid.toLong()
            )
        }
    }
    LaunchedEffect(netLyricResult, qqLyricResult, amLyricResult) {
        withContext(Dispatchers.Default) {
            val sources = mutableListOf<LyricSourceData>()
            (amLyricResult as? Resource.Success)?.let { sources.add(LyricSourceData.AM(it.data)) }
            (netLyricResult as? Resource.Success)?.data?.let {
                sources.add(
                    LyricSourceData.NetEase(
                        it
                    )
                )
            }
            (qqLyricResult as? Resource.Success)?.data?.musicMusichallSongPlayLyricInfoGetPlayLyricInfo?.data?.let {
                try {
                    val qrc = it.copy(
                        lyric = QRCUtils.decodeLyric(it.lyric),
                        trans = QRCUtils.decodeLyric(it.trans, true),
                        roma = QRCUtils.decodeLyric(it.roma)
                    )
                    sources.add(LyricSourceData.QQMusic(qrc))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val merged = mergeLyrics(sources)
            withContext(Dispatchers.Main) { lyricLine = merged }
        }
    }

    LaunchedEffect(state.isCollapsed) {
        if (state.isCollapsed) {
            showLyrics = false
        }
    }
    LaunchedEffect(showLyrics) {
        if (!showLyrics) {
            areControlsVisible = true
        }
    }

    BackHandler(enabled = state.isExpanded && showLyrics) {
        showLyrics = false
    }


    // --- Animation & Geometry Calculation ---
    val lyricTransition = updateTransition(targetState = showLyrics, label = "LyricMode")
    val lyricAnimFraction by lyricTransition.animateFloat(
        label = "Fraction",
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) }
    ) { if (it) 1f else 0f }

    val sheetProgress = state.progress

    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = remember(isSystemInDarkTheme, state.value, state.collapsedBound) {
        if (isSystemInDarkTheme && state.value > state.collapsedBound) {
            lerp(colorScheme.surfaceContainer, Color.Black, state.progress)
        } else {
            colorScheme.surfaceContainer
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        // --- 响应式布局判断 ---
        // 判断是否是横屏或者高度很小 (小窗模式)
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isCompactHeight = maxHeightPx < with(density) { 600.dp.toPx() } // 阈值可以调节

        // --- 1. 定义关键尺寸参数 ---

        // A. Mini Player (Bottom)
        val miniSize = with(density) { 48.dp.toPx() }
        val miniStart = with(density) { 12.dp.toPx() }
        val miniRadius = with(density) { ThumbnailCornerRadius.toPx() }
        val collapsedBoundPx = with(density) { state.collapsedBound.toPx() }
        val miniAbsTop = maxHeightPx - collapsedBoundPx + with(density) { 6.dp.toPx() }

        // B. Normal Expanded
        val topSafeArea = with(density) { WindowInsets.statusBars.getTop(this).toFloat() }

        // --- 动态计算底部预留高度 ---
        // 正常竖屏留 300dp，横屏或小窗时大大减小预留高度，
        // 在小窗模式下，底部可能只需要进度条和控制按钮，Toolbar 可能需要隐藏或缩减间距
        val bottomControlsHeightDp = if (isCompactHeight || isLandscape) 220.dp else 300.dp
        val bottomControlsHeight = with(density) { bottomControlsHeightDp.toPx() }

        // --- 动态计算封面大小 ---
        val normalPaddingH = with(density) { (PlayerHorizontalPadding + 24.dp).toPx() }
        val maxAvailableWidth = maxWidthPx - (normalPaddingH * 2)

        // 计算垂直方向剩余给图片的空间
        val minTopMargin = with(density) { 16.dp.toPx() } // 顶部最小留白
        val availableVerticalSpace = maxHeightPx - bottomControlsHeight - topSafeArea - minTopMargin

        // 封面大小取：(可用宽度) 和 (可用高度) 中的较小值
        // 这样在高度不足时，图片会自动缩小，而不会被切掉
        val normalSize = min(maxAvailableWidth, availableVerticalSpace.coerceAtLeast(0f))

        // 计算图片垂直居中位置
        // 如果高度非常小，availableVerticalSpace 可能为负或很小，此时尽可能往下一点点
        val realAvailableHeight = (maxHeightPx - bottomControlsHeight - topSafeArea)
        val verticalBias = (realAvailableHeight - normalSize) / 2
        val normalTop = topSafeArea + verticalBias.coerceAtLeast(with(density) { 12.dp.toPx() })

        // 居中显示
        val normalStart = (maxWidthPx - normalSize) / 2

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

        val finalSize = lerp(miniSize, targetSize, sheetProgress)
        val finalTop = lerp(miniAbsTop, targetTop, sheetProgress)
        val finalStart = lerp(miniStart, targetStart, sheetProgress)
        val finalRadius = lerp(miniRadius, targetRadius, sheetProgress)

        val shadowAlpha = if (sheetProgress > 0.8f) (1f - lyricAnimFraction) else 0f
        var mShadowElevation = 16.dp * shadowAlpha


        // --- 3. UI Structure ---
        BottomSheet(
            state = state,
            modifier = Modifier.fillMaxSize(),
            backgroundColor = backgroundColor,
            onDismiss = {
                playerConnection.player.stop()
                playerConnection.player.clearMediaItems()
            },
            onHorizontalSwipe = { direction ->
                if (!state.isExpanded) {
                    when (direction) {
                        HorizontalSwipeDirection.Left -> playerConnection.seekToNext()
                        HorizontalSwipeDirection.Right -> playerConnection.seekToPrevious()
                    }
                }
            },
            collapsedContent = {
                MiniPlayer(
                    position = sliderPosition.toLong(),
                    duration = duration,
                )
            }
        ) {
            val coverUrl = mediaMetadata?.coverUrl
            AppleMusicFluidBackground(imageUrl = coverUrl)

            Box(modifier = Modifier.fillMaxSize()) {


                // Mode B: Lyric Player
                AnimatedVisibility(
                    visible = showLyrics,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(Modifier.fillMaxSize()) {
                        // 顶部留白给 Header
                        Spacer(modifier = Modifier.height(with(density) { (headerTop + headerSize).toDp() + 16.dp }))

                        LyricScreen(
                            lyricData = lyricLine,
                            playerConnection = playerConnection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = PlayerHorizontalPadding),
                            onClick = {
                                mediaMetadata?.let {
                                    if (currentOverlay is OverlayState.None && playerViewModel.searchResult.value is Resource.Success) {
                                        currentOverlay = OverlayState.QQMusicSelection(
                                            searchResult = playerViewModel.searchResult.value as Resource.Success,
                                            mediaMetadata = it
                                        )
                                    }
                                }
                            },
                            onLongClick = {},
                            onToggleControls = { show ->
                                areControlsVisible = show
                            }
                        )
                        val spacerHeight by animateDpAsState(
                            targetValue = if (areControlsVisible) bottomControlsHeightDp else 16.dp, // 16dp 为隐藏时的底部安全边距
                            label = "spacer"
                        )
                        Spacer(modifier = Modifier.height(spacerHeight))
                    }
                }


                // Mode C: Header Info
                if (mediaMetadata != null) {
                    // 不要 0f 就开始显示，而是等到动画进行到 40% 以后才开始淡入
                    // 这样可以避免和底部正在淡出的标题视觉打架
                    val fraction = lyricAnimFraction
                    val enterThreshold = 0.4f

                    // 重新映射 alpha：在 0.4 -> 1.0 的区间内，从 0f 变到 1f
                    val headerTextAlpha = if (fraction > enterThreshold) {
                        ((fraction - enterThreshold) / (1f - enterThreshold)).coerceIn(
                            0f,
                            1f
                        ) * sheetProgress
                    } else {
                        0f
                    }

                    if (headerTextAlpha > 0.01f) {
                        // 计算文字区域的宽度
                        val headerTextWidth =
                            screenWidth - with(density) { (headerStart + headerSize).toDp() } - 24.dp
                        // 不再使用 targetTop (它是动态变化的)，而是固定使用 headerTop
                        // 并添加一个微小的向上滑入效果 (Slide Up)
                        // 当 fraction = 1 (结束) 时，offset = 0
                        // 当 fraction = 0.4 (开始出现) 时，offset = 20dp
                        val slideUpOffset =
                            with(density) { (20.dp.toPx() * (1f - fraction)).toInt() }

                        // X轴依然需要跟随封面 (targetStart + targetSize)，保证不重叠
                        val currentX = (targetStart + headerSize + 12.dp.toPx(context)).toInt()
                        val fixedY = headerTop.toInt() + slideUpOffset

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = headerTextAlpha
                                    // 还可以加一点缩放效果，显得更灵动
                                    val scale = 0.9f + (0.1f * fraction) // 0.9 -> 1.0
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
                                isLiked = isLiked != null, // 传入状态
                                onLikeClick = { mediaMetadata?.let { playerViewModel.like(it.id.toString()) } },
                                onMoreClick = { currentOverlay = OverlayState.MoreAction },
                                onTitleClick = {
                                    mediaMetadata?.let {
                                        currentOverlay = OverlayState.AlbumArtist(
                                            album = it.album,
                                            artists = it.artists,
                                            cover = it.coverUrl
                                        )
                                    }

                                },
                                titleStyle = MaterialTheme.typography.titleMedium,
                                subTitleStyle = MaterialTheme.typography.bodySmall,
                                needShadow = false,
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
                    // 逻辑梳理：
                    // 1. 整个底部区域是否可见？
                    //    - 如果是普通模式 (!showLyrics): 永远可见 (true)
                    //    - 如果是歌词模式 (showLyrics): 由滑动状态 (areControlsVisible) 决定
                    val isBottomBarVisible = !showLyrics || areControlsVisible

                    // 使用 AnimatedVisibility 包裹【整个】底部内容（标题 + 进度条 + 按钮）
                    AnimatedVisibility(
                        visible = isBottomBarVisible,
                        // 进出场动画：向下滑出/向上滑入 + 淡入淡出
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        // AnimatedVisibility 内部需要一个容器来垂直排列 Title 和 Controls
                        Column(Modifier.fillMaxWidth()) {

                            // A. 标题区域
                            // 逻辑：只有在【非歌词模式】且【非紧凑模式】下才显示底部的标题
                            // (歌词模式下标题在顶部，紧凑模式下空间不够不显示标题)
                            val isTitleVisible = !showLyrics && (!isCompactHeight && !isLandscape)

                            if (isTitleVisible) {
                                mediaMetadata?.let {
                                    Title(
                                        title = it.title,
                                        subTitle = it.artists.joinToString { artist -> artist.name },
                                        isLiked = isLiked != null,
                                        onLikeClick = { playerViewModel.like(it.id.toString()) },
                                        onMoreClick = { currentOverlay = OverlayState.MoreAction },
                                        onTitleClick = {
                                            currentOverlay = OverlayState.AlbumArtist(
                                                album = it.album,
                                                artists = it.artists,
                                                cover = it.coverUrl
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = PlayerHorizontalPadding)
                                            .padding(bottom = 12.dp)
                                    )
                                }
                            }

                            // B. 进度条 + 控制按钮 + 工具栏
                            // 这一部分跟随父级 AnimatedVisibility 一起隐藏/显示
                            PlayerControlsSection(
                                sliderPosition = sliderPosition,
                                duration = duration,
                                isPlaying = isPlaying,
                                playbackState = playbackState,
                                playerConnection = playerConnection,
                                onLyricClick = { showLyrics = !showLyrics },
                                onPlaylistClick = { currentOverlay = OverlayState.Playlist },
                                onSleepTimerClick = { currentOverlay = OverlayState.SleepTimer },
                                onAddToPlaylistClick = {
                                    mediaMetadata?.let {
                                        currentOverlay = OverlayState.AddToPlaylist(it.id)
                                    }
                                },
                                onMoreClick = { currentOverlay = OverlayState.MoreAction },
                                isCompact = isCompactHeight || isLandscape
                            )
                        }
                    }
                }
            }
        }

        AnimatedContent(
            targetState = mediaMetadata, // 监听歌曲元数据变化
            transitionSpec = {
                // 定义切换动画：
                // 进入的图片：淡入 + 从 0.9 倍大小放大到 1.0 (这也是 Apple Music 的专辑切换效果之一)
                (fadeIn(animationSpec = tween(durationMillis = 400)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 400)))
                    .togetherWith(
                        // 离开的图片：淡出
                        fadeOut(animationSpec = tween(durationMillis = 400))
                    )
            },
            label = "CoverTransition",
            modifier = Modifier
                .graphicsLayer {
                    translationX = finalStart
                    translationY = finalTop
                    shadowElevation = mShadowElevation.toPx()
                    shape = RoundedCornerShape(finalRadius)
                    clip = true
                }
                .size(
                    width = with(density) { finalSize.toDp() },
                    height = with(density) { finalSize.toDp() }
                )
                .clickable {
                    if (!state.isExpanded) {
                        state.expandSoft()
                    } else {
                        showLyrics = !showLyrics
                    }
                }
                .background(MaterialTheme.colorScheme.surfaceVariant) // 占位背景色
        ) { currentMetadata ->
            // 这里是动画的内容部分
            if (currentMetadata != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentMetadata.coverUrl)
                        .size(Size.ORIGINAL)
                        .precision(Precision.EXACT)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
            }
        }


        when (val overlay = currentOverlay) {
            OverlayState.None -> {}
            OverlayState.Playlist -> {
                PlaylistBottomSheet(
                    onDismiss = { currentOverlay = OverlayState.None }
                )
            }

            is OverlayState.AlbumArtist -> {
                AlbumArtistBottomSheet(
                    coverUrl = overlay.cover,
                    albumInfo = overlay.album,
                    artistList = overlay.artists,
                    onAlbumClick = { id ->
                        Screen.Album.navigate(navController) {
                            addPath(id.toString())
                        }
                        state.collapse(spring(stiffness = Spring.StiffnessVeryLow))
                    },
                    onArtistClick = { id ->

                        Screen.Artist.navigate(navController) {
                            addPath(id.toString())
                        }
                        state.collapse(spring(stiffness = Spring.StiffnessVeryLow))
                    },
                    onDismissRequest = { currentOverlay = OverlayState.None },
                )
            }

            is OverlayState.QQMusicSelection -> {
                QQMusicSelectSheet(
                    searchNew = overlay.searchResult,
                    viewmodel = playerViewModel,
                    mediaMetadata = overlay.mediaMetadata,
                    onDismiss = { currentOverlay = OverlayState.None }
                )
            }

            OverlayState.SleepTimer -> {
                SleepTimerSheet(
                    playerConnection = playerConnection,
                    onDismiss = { currentOverlay = OverlayState.None }
                )
            }

            is OverlayState.AddToPlaylist -> {
                AddToPlaylistSheet(
                    playlists = allPlaylist,
                    onDismiss = { currentOverlay = OverlayState.None },
                    onSelectPlaylist = { selectedPlaylist ->
                        playlistViewModel.addSongToPlaylist(
                            pid = selectedPlaylist.id,
                            trackIds = overlay.mediaId.toString()
                        )
                        Toast.makeText(
                            context,
                            "已添加到 ${selectedPlaylist.title}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Timber.tag("Playlist")
                            .d("Added ${selectedPlaylist.title} to ${selectedPlaylist.title}")
                        currentOverlay = OverlayState.None


                    },
                    onCreateNewPlaylist = {
                        currentOverlay = OverlayState.CreatePlaylist
                    }
                )
            }

            OverlayState.CreatePlaylist -> {
                CreatePlaylistSheet(
                    onDismiss = { currentOverlay = OverlayState.None },
                    onConfirm = { name, privacy -> playerViewModel.createPlaylist(name, privacy) }
                )
            }

            OverlayState.BottomAction -> {
                PlayerActionSettingsSheet(onDismiss = { currentOverlay = OverlayState.None })
            }

            OverlayState.MoreAction -> {
                MoreActionsSheet(
                    onDismissRequest = {
                        currentOverlay = OverlayState.None
                    },
                    onActionClick = { action ->
                        when (action) {
                            MoreAction.ADD_TO_PLAYLIST -> {
                                mediaMetadata?.let {
                                    currentOverlay = OverlayState.AddToPlaylist(it.id)
                                }
                            }

                            MoreAction.SHARE -> {
                                currentOverlay = OverlayState.None
                                Toast.makeText(context, "暂未实现", Toast.LENGTH_SHORT).show()
                            }

                            MoreAction.DOWNLOAD -> {
                                currentOverlay = OverlayState.None
                                Toast.makeText(context, "暂未实现", Toast.LENGTH_SHORT).show()
                            }

                            MoreAction.DELETE -> {
                                mediaMetadata?.let {
                                    playerViewModel.deleteSongById(it.id.toString())
                                }
                            }

                            MoreAction.VIEW_PLAYLIST -> {
                                currentOverlay = OverlayState.Playlist
                            }

                            MoreAction.SLEEP_TIMER -> {
                                currentOverlay = OverlayState.SleepTimer
                            }

                            MoreAction.BOTTOM_ACTION -> {
                                currentOverlay = OverlayState.BottomAction
                            }
                        }

                    },
                    viewModel = playerViewModel
                )
            }

            else -> {}
        }
    }
}