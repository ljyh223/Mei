package com.ljyh.mei.ui.component.player

import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.constants.PlayerHorizontalPadding
import com.ljyh.mei.constants.ThumbnailCornerRadius
import com.ljyh.mei.constants.UseQQMusicLyricKey
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.component.player.component.AppleMusicFluidBackground
import com.ljyh.mei.ui.component.player.component.Controls
import com.ljyh.mei.ui.component.player.component.LyricScreen
import com.ljyh.mei.ui.component.player.component.PlayerActionToolbar
import com.ljyh.mei.ui.component.player.component.PlayerProgressSlider
import com.ljyh.mei.ui.component.player.component.QQMusicSelectSheet
import com.ljyh.mei.ui.component.sheet.BottomSheet
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.sheet.HorizontalSwipeDirection
import com.ljyh.mei.ui.component.utils.lerp
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.utils.encrypt.QRCUtils
import com.ljyh.mei.utils.lyric.createDefaultLyricData
import com.ljyh.mei.utils.lyric.mergeLyrics
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
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
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val configuration = LocalConfiguration.current

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
    var showQQMusicSelect by remember { mutableStateOf(false) }

    // Lyric Logic
    val netLyricResult by playerViewModel.lyric.collectAsState()
    val qqLyricResult by playerViewModel.lyricResult.collectAsState()
    val amLyricResult by playerViewModel.amLyric.collectAsState()
    val qqSong by playerViewModel.qqSong.collectAsState()

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
            showQQMusicSelect = false
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
            (netLyricResult as? Resource.Success)?.data?.let { sources.add(LyricSourceData.NetEase(it)) }
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
        var shadowElevation = 16.dp * shadowAlpha


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

                // Mode A: Normal Player Content
                AnimatedVisibility(
                    visible = showLyrics,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Sheet logic wrapper
                    Box(modifier = Modifier.fillMaxSize()) {
                        mediaMetadata?.let {
                            if (showQQMusicSelect && playerViewModel.searchResult.value is Resource.Success) {
                                QQMusicSelectSheet(
                                    showSheet = showQQMusicSelect,
                                    searchNew = playerViewModel.searchResult.value as Resource.Success,
                                    viewmodel = playerViewModel,
                                    mediaMetadata = it,
                                    onDismiss = { showQQMusicSelect = false }
                                )
                            }
                        }
                    }
                }

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
                            onClick = { showQQMusicSelect = true },
                            onLongClick = { /* delete logic */ }
                        )
                        // 底部留白，这里的 Spacer 高度也应该动态化
                        // 但由于我们在 controls 里使用了 Align Bottom，这里 Spacer 主要是为了不让歌词被遮挡
                        Spacer(modifier = Modifier.height(bottomControlsHeightDp))
                    }
                }
                val shadowStyle = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = Offset(2f, 2f),
                    blurRadius = 8f
                )

                val subTitleStyle = MaterialTheme.typography.titleMedium.copy(
                    shadow = shadowStyle,
                    color = Color.White.copy(alpha = 0.7f)
                )

                // Mode C: Header Info
                if (mediaMetadata != null) {
                    val headerTextAlpha = lyricAnimFraction * sheetProgress
                    if (headerTextAlpha > 0.1f) {
                        Column(
                            modifier = Modifier
                                .graphicsLayer { alpha = headerTextAlpha }
                                .offset {
                                    IntOffset(
                                        x = (targetStart + headerSize + 12.dp.toPx()).toInt(),
                                        y = targetTop.toInt()
                                    )
                                }
                                .width(screenWidth - with(density) { (headerStart + headerSize).toDp() } - 24.dp)
                                .height(with(density) { headerSize.toDp() }),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = mediaMetadata!!.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    shadow = shadowStyle,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                            Text(
                                text = mediaMetadata!!.artists.joinToString { it.name },
                                style = subTitleStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
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

                    // 1. 标题区域 (小窗/横屏模式下，为了节省空间，可以考虑隐藏标题或缩小间距)
                    if (!isCompactHeight || !isLandscape) {
                        AnimatedVisibility(
                            visible = !showLyrics,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            mediaMetadata?.let {

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = PlayerHorizontalPadding)
                                        .padding(bottom = 12.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = it.title,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            shadow = shadowStyle,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = it.artists.joinToString { artist -> artist.name },
                                        style = subTitleStyle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    } else if (!showLyrics) {
                        // 紧凑模式下的简单标题（可选）
                        // 可以在这里放一个更小的标题
                    }

                    // 2. 进度条 + 控制按钮 + 工具栏
                    PlayerControlsSection(
                        sliderPosition = sliderPosition,
                        duration = duration,
                        isPlaying = isPlaying,
                        playbackState = playbackState,
                        playerConnection = playerConnection,
                        playerViewModel = playerViewModel,
                        playlistViewModel = playlistViewModel,
                        mediaMetadata = mediaMetadata,
                        onLyricClick = { showLyrics = !showLyrics },
                        isLyricActive = showLyrics,
                        isCompact = isCompactHeight || isLandscape // 传进去控制间距
                    )
                }
            }
        }

        // --- Shared Element (Cover) ---
        if (mediaMetadata != null) {
            // 修改 BottomSheetPlayer 里的 AsyncImage 部分
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mediaMetadata?.coverUrl)
                    .size(coil3.size.Size.ORIGINAL)
                    .precision(Precision.EXACT) // 强制使用精确大小
                    .crossfade(true)            // 开启淡入动画，切换更平滑
                    .build(),

                contentDescription = "Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = finalStart
                        translationY = finalTop
                        shadowElevation = shadowElevation
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
            )
        }
    }
}

// 稍微修改 Controls Section，让它支持紧凑模式
@OptIn(UnstableApi::class)
@Composable
fun PlayerControlsSection(
    sliderPosition: Float,
    duration: Long,
    isPlaying: Boolean,
    playbackState: Int,
    playerConnection: PlayerConnection,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    mediaMetadata: MediaMetadata? = null,
    onLyricClick: () -> Unit,
    isLyricActive: Boolean = false,
    isCompact: Boolean = false // 新增参数
) {
    // 紧凑模式下间距减半
    val spacerHeight = if (isCompact) 8.dp else 32.dp

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        PlayerProgressSlider(
            position = sliderPosition.toLong(),
            duration = duration,
            isPlaying = isPlaying,
            onPositionChange = { newPosition ->
                playerConnection.player.seekTo(newPosition)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding + 8.dp)
        )

        Spacer(Modifier.height(spacerHeight))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding)
        ) {
            Controls(
                playerConnection = playerConnection,
                canSkipPrevious = true,
                canSkipNext = true,
                isPlaying = isPlaying,
                playbackState = playbackState,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 紧凑模式下可能需要隐藏底部 Toolbar 或者减小间距
        if (!isCompact) {
            Spacer(Modifier.height(spacerHeight))
            PlayerActionToolbar(
                playerViewModel = playerViewModel,
                mediaMetadata = mediaMetadata,
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                playlistViewModel = playlistViewModel,
                isLyricActive = isLyricActive,
                onLyricClick = onLyricClick
            )
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            Spacer(Modifier.height(16.dp))
        } else {
            // 紧凑模式底部留白少一点
            Spacer(Modifier.height(16.dp))
        }
    }
}