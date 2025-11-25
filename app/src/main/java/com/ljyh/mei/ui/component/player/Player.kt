package com.ljyh.mei.ui.component.player


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player.STATE_READY
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.constants.PlayerHorizontalPadding
import com.ljyh.mei.constants.UseQQMusicLyricKey
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.player.component.Controls
import com.ljyh.mei.ui.component.player.component.Cover
import com.ljyh.mei.ui.component.player.component.Debug
import com.ljyh.mei.ui.component.player.component.Header
import com.ljyh.mei.ui.component.player.component.LyricScreen
import com.ljyh.mei.ui.component.player.component.PlayerProgressSlider
import com.ljyh.mei.ui.component.player.component.SmoothCoverBackground
import com.ljyh.mei.ui.component.sheet.BottomSheet
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.sheet.HorizontalSwipeDirection
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.utils.TimeUtils.formatMilliseconds
import com.ljyh.mei.utils.encrypt.QRCUtils
import com.ljyh.mei.utils.lyric.createDefaultLyricData
import com.ljyh.mei.utils.lyric.mergeLyrics
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current

    // --- 1. Preferences & Theme ---
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useQQMusicLyric by rememberPreference(UseQQMusicLyricKey, defaultValue = true)
    val debug by rememberPreference(DebugKey, defaultValue = false)


    // --- 2. Player State Collection ---
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    // --- 3. UI Local State ---
    // 进度条状态：直接使用 Float 避免频繁 Long 转换，且分离 UI 状态与播放器真实状态
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(0L) }
    // 标记用户是否正在拖动进度条，拖动时不自动更新
    var isDragging by remember { mutableStateOf(false) }

    // 歌词数据源
    val netLyricResult by playerViewModel.lyric.collectAsState()
    val qqLyricResult by playerViewModel.lyricResult.collectAsState()
    val amLyricResult by playerViewModel.amLyric.collectAsState()
    val qqSong by playerViewModel.qqSong.collectAsState()

    // 歌词最终状态 (默认为加载中)
    var lyricLine by remember { mutableStateOf(createDefaultLyricData("歌词加载中")) }
    var rawSeedColor by remember {
        mutableStateOf(
            Color.White
        )
    }

    // 更新进度条与时长
    LaunchedEffect(playbackState, isPlaying, isDragging) {
        if (playbackState == STATE_READY && isPlaying && !isDragging) {
            while (isActive) {
                sliderPosition = playerConnection.player.currentPosition.toFloat()
                duration = playerConnection.player.duration.coerceAtLeast(1L)
                delay(50)
            }
        } else if (!isPlaying && !isDragging) {
            // 暂停时同步一次，防止状态不一致
            sliderPosition = playerConnection.player.currentPosition.toFloat()
            duration = playerConnection.player.duration.coerceAtLeast(1L)
        }
    }

    // 处理 Metadata 变化：加载歌词、重置状态
    LaunchedEffect(mediaMetadata) {
        mediaMetadata?.let { meta ->
            // 重置歌词状态
            lyricLine = createDefaultLyricData("歌词加载中")

            // 触发 ViewModel 获取数据
            playerViewModel.clear()
            playerViewModel.mediaMetadata = meta

            playerViewModel.getLyricV1(meta.id.toString())
            playerViewModel.getAMLLyric(meta.id.toString())

            if (useQQMusicLyric) {
                playerViewModel.fetchQQSong(meta.id.toString())
                playerViewModel.searchNew(meta.title)
            }
            Log.d("Player", "MediaMetadata: $meta")
            Log.d("Player", "MediaMetadata: cover ${meta.coverUrl}")
            val color= playerViewModel.getColor(meta.coverUrl)
            if(color == null){
                Log.d("Player", "Raw seed color is null")
                rawSeedColor = Color.White
            }else{
                rawSeedColor = color
                Log.d("Player", "Raw seed color: $rawSeedColor")
            }

        }
    }
    // 监听 QQSong 变化，获取新歌词
    LaunchedEffect(qqSong) {
        qqSong?.let { song ->
            playerViewModel.getLyricNew(
                title = song.title,
                album = song.album,
                artist = song.artist,
                duration = song.duration,
                id = song.qid.toInt()
            )
        }
    }

    LaunchedEffect(netLyricResult, qqLyricResult, amLyricResult) {
        withContext(Dispatchers.Default) {
            val sources = mutableListOf<LyricSourceData>()

            (amLyricResult as? Resource.Success)?.let {
                sources.add(LyricSourceData.AM(it.data))
            }
            (netLyricResult as? Resource.Success)?.data?.let {
                sources.add(LyricSourceData.NetEase(it))
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
            // 切换回主线程更新 UI
            withContext(Dispatchers.Main) {
                lyricLine = merged
            }
        }
    }

    // --- 5. UI Rendering ---

    // 背景颜色计算
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = remember(isSystemInDarkTheme, state.value, state.collapsedBound) {
        if (isSystemInDarkTheme && state.value > state.collapsedBound) {
            lerp(colorScheme.surfaceContainer, Color.Black, state.progress)
        } else {
            colorScheme.surfaceContainer
        }
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        backgroundColor = backgroundColor,
        onDismiss = {
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        onHorizontalSwipe = { direction ->
            when (direction) {
                HorizontalSwipeDirection.Left -> playerConnection.seekToNext()
                HorizontalSwipeDirection.Right -> playerConnection.seekToPrevious()
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
        SmoothCoverBackground(
            coverUrl = coverUrl,
            seedColor = rawSeedColor
        )

        // 2. Debug 信息层
        if (debug && mediaMetadata != null) {
            Debug(
                title = mediaMetadata!!.title,
                artist = mediaMetadata!!.artists.firstOrNull()?.name ?: "",
                album = mediaMetadata!!.album.title,
                duration = formatMilliseconds(duration).toString(),
                id = mediaMetadata!!.id.toString(),
                qid = qqSong?.qid ?: "null",
                color = rawSeedColor.value.toString(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
            )
        }

        // 3. 主内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(bottom = 16.dp, top = 24.dp), // 底部留白
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            mediaMetadata?.let {
                Header(
                    mediaMetadata = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
                    onNavigateToAlbum = {},
                    onNavigateToArtist = {},
                )
            }

            // Pager (Cover / Lyrics)
            val pagerState = rememberPagerState(pageCount = { 2 })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = PlayerHorizontalPadding),
                //pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            mediaMetadata?.let {
                                Cover(
                                    playerConnection = playerConnection,
                                    mediaMetadata = it,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .shadow(
                                            elevation = 12.dp,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                )
                            }
                        }
                    }

                    1 -> {
                        LyricScreen(
                            lyricData = lyricLine,
                            playerConnection = playerConnection,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = PlayerHorizontalPadding)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp)) // 增加间距，让布局更舒展

            // Progress Bar
            PlayerProgressSlider(
                position = sliderPosition.toLong(),
                duration = duration,
                isPlaying = isPlaying,
                onPositionChange = { newPosition ->
                    // 只有手指抬起或确认拖拽时才 seek
                    playerConnection.player.seekTo(newPosition)
                    sliderPosition = newPosition.toFloat()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Controls
            Controls(
                playerConnection = playerConnection,
                canSkipPrevious = playerConnection.canSkipPrevious.collectAsState().value,
                canSkipNext = playerConnection.canSkipNext.collectAsState().value,
                isPlaying = isPlaying,
                playbackState = playbackState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            )

            Spacer(Modifier.height(16.dp))

            // Action Toolbar (Queue, Like, Sleep, etc.)
            PlayerActionToolbar(
                playerViewModel = playerViewModel,
                mediaMetadata = mediaMetadata,
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
            )

            // 底部安全区，防止被手势条遮挡
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}