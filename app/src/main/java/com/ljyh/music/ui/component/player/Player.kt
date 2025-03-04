package com.ljyh.music.ui.component.player


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import com.ljyh.music.constants.DarkModeKey
import com.ljyh.music.constants.PlayerHorizontalPadding
import com.ljyh.music.constants.PureBlackKey
import com.ljyh.music.constants.QueuePeekHeight
import com.ljyh.music.constants.UseQQMusicLyricKey
import com.ljyh.music.data.model.Lyric
import com.ljyh.music.data.model.emptyLyric
import com.ljyh.music.data.model.parseYrc
import com.ljyh.music.data.model.qq.u.LyricResult
import com.ljyh.music.data.model.qq.u.emptyData
import com.ljyh.music.data.network.Resource
import com.ljyh.music.ui.component.BottomSheet
import com.ljyh.music.ui.component.BottomSheetState
import com.ljyh.music.ui.component.rememberBottomSheetState
import com.ljyh.music.ui.local.LocalPlayerConnection
import com.ljyh.music.utils.encrypt.QRCUtils
import com.ljyh.music.utils.rememberEnumPreference
import com.ljyh.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

//
//@RequiresApi(Build.VERSION_CODES.S)
//@Composable
//fun BottomSheetPlayer(
//    state: BottomSheetState,
//    navController: NavController,
//    modifier: Modifier = Modifier,
//    viewmodel: PlayerViewModel = hiltViewModel(),
//) {
//    val playerConnection = LocalPlayerConnection.current ?: return
//    val context = LocalContext.current
//    val isSystemInDarkTheme = isSystemInDarkTheme()
//    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
//    val useQQMusicLyric by rememberPreference(UseQQMusicLyricKey, defaultValue = true)
//    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
//    val useBlackBackground = remember(isSystemInDarkTheme, darkTheme, pureBlack) {
//        val useDarkTheme =
//            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
//        useDarkTheme && pureBlack
//    }
//    val backgroundColor = if (useBlackBackground && state.value > state.collapsedBound) {
//        lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
//    } else {
//        MaterialTheme.colorScheme.surfaceContainer
//    }
//    var sliderPosition by remember { mutableFloatStateOf(0f) }
//    val playbackState by playerConnection.playbackState.collectAsState()
//    val isPlaying by playerConnection.isPlaying.collectAsState()
//    val repeatMode by playerConnection.repeatMode.collectAsState()
//    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
//    val pagerState = rememberPagerState(pageCount = { 2 })
//    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
//    val canSkipNext by playerConnection.canSkipNext.collectAsState()
//    var cover by remember { mutableStateOf("") }
//    var album by remember { mutableStateOf("") }
//    var artist by remember { mutableStateOf("") }
//    var title by remember { mutableStateOf("") }
//    var position by rememberSaveable(playbackState) { mutableLongStateOf(playerConnection.player.currentPosition)
//    }
//    var duration by rememberSaveable(playbackState) {
//        mutableLongStateOf(playerConnection.player.duration)
//    }
//
//    var netLyric by remember { mutableStateOf(emptyLyric) }
//    var qqLyric by remember { mutableStateOf(emptyData) }
//    var showDialog by remember { mutableStateOf(false) }
//
//    val netLyricResult by viewmodel.lyric.collectAsState()
//    val searchNew by viewmodel.searchNew.collectAsState()
//    val lyricNew by viewmodel.lyricNew.collectAsState()
//
//
//    val lyricLine = remember {
//        mutableStateOf(
//            LyricData(
//                isVerbatim = false,
//                lyricLine = listOf(
//                    LyricLine(
//                        lyric = "歌词加载错误",
//                        startTimeMs = 0,
//                        durationMs = 0,
//                        words = emptyList()
//                    )
//                )
//            )
//        )
//    }
//
//
//
//
//    LaunchedEffect(qqLyric, netLyric) {
//        val qrc = QRCUtils.decodeLyric(qqLyric.lyric)
//        val qrcT = QRCUtils.decodeLyric(qqLyric.trans)
//
//
//        if (netLyric.yrc != null && netLyric.tlyric != null) {
//            Log.d("Lyric parse", "netLyric and netLyric T")
//            lyricLine.value = LyricData(
//                isVerbatim = true,
//                source = LyricSource.NetEaseCloudMusic,
//                lyricLine = netLyric.parseYrc()
//            )
//            return@LaunchedEffect
//        }
//
//
//        if (qrc != "" && qrcT != "") {
//            Log.d("Lyric parse", "qrc and qrcT")
//            lyricLine.value = LyricData(
//                isVerbatim = true,
//                source = LyricSource.QQMusic,
//                lyricLine = QRCUtils.parse(qrc, qrcT)
//            )
//            return@LaunchedEffect
//        }
//
//        if (qrc != "" && netLyric.tlyric != null) {
//            Log.d("Lyric parse", "qrc and netLyric T")
//            lyricLine.value = LyricData(
//                isVerbatim = true,
//                source = LyricSource.QQMusic,
//                lyricLine = QRCUtils.parse(qrc, netLyric.tlyric?.lyric ?: "")
//            )
//            return@LaunchedEffect
//        }
//
//
//        if (qrc != "") {
//            Log.d("Lyric parse", "qrc")
//            lyricLine.value = LyricData(
//                isVerbatim = true,
//                source = LyricSource.QQMusic,
//                lyricLine = QRCUtils.parse(qrc, "")
//            )
//            return@LaunchedEffect
//        }
//
//
//        Log.d("Lyric parse", "netLyric")
//        lyricLine.value = LyricData(
//            isVerbatim = false,
//            source = LyricSource.NetEaseCloudMusic,
//            lyricLine = netLyric.parseYrc()
//        )
//        return@LaunchedEffect
//
//
//    }
//    // 网易云官方的歌词
//    LaunchedEffect(netLyricResult) {
//        if (!lyricLine.value.isVerbatim) {
//            when (val result = netLyricResult) {
//                is Resource.Success -> {
//                    netLyric = result.data
//                }
//
//                is Resource.Error -> {
//                    Log.d("searchLyric", result.toString())
//                    lyricLine.value = LyricData(
//                        isVerbatim = false,
//                        lyricLine = listOf(
//                            LyricLine(
//                                lyric = "歌词加载错误",
//                                startTimeMs = 0,
//                                durationMs = 0,
//                                words = emptyList()
//                            )
//                        )
//                    )
//                }
//
//                Resource.Loading -> {}
//            }
//        }
//
//    }
//
//    // 检索qq音乐的歌曲
//    LaunchedEffect(searchNew) {
//        when (val result = searchNew) {
//            is Resource.Success -> {
//                Log.d("searchLyric", result.toString())
//                val s =
//                    result.data.req0.data.body.song.list.find { it.title == title && it.singer.any { si -> si.name == artist } }
//                if (s != null) {
//                    Log.d("searchLyric", "id ==>${s.id}")
//                    viewmodel.getLyricNew(
//                        title = title,
//                        album = album,
//                        artist = artist,
//                        duration = s.interval,
//                        id = s.id
//                    )
//                } else {
//                    Log.d("searchLyric", "qqSearch id is null")
//                    qqLyric = emptyData
//                }
//            }
//
//
//            is Resource.Error -> {
//                qqLyric = emptyData
//                Log.d("searchLyric", result.message)
//            }
//
//            Resource.Loading -> {
//            }
//        }
//    }
//
//    //获取qq音乐的歌词
//    LaunchedEffect(lyricNew) {
//        when (val result = lyricNew) {
//            is Resource.Success -> {
//                qqLyric = result.data.musicMusichallSongPlayLyricInfoGetPlayLyricInfo.data
//            }
//
//            is Resource.Error -> {
//                qqLyric = emptyData
//                Log.d("qLyric", result.message)
//            }
//
//            Resource.Loading -> {
//            }
//        }
//    }
//
//    LaunchedEffect(playbackState) {
//        if (playbackState == STATE_READY) {
//            while (isActive) {
//                delay(100)
//                position = playerConnection.player.currentPosition
//                duration = playerConnection.player.duration
//            }
//        }
//    }
//
//    LaunchedEffect(position, duration) {
//        if (position == 0L || duration == C.TIME_UNSET) {
//            sliderPosition = 0f // 重置滑块
//        }
//    }
//    // 在这里处理 mediaMetadata 的变化
//    LaunchedEffect(mediaMetadata) {
//        qqLyric = emptyData
//        netLyric = emptyLyric
//        lyricLine.value = LyricData(
//            isVerbatim = false,
//            lyricLine = listOf(
//                LyricLine(
//                    lyric = "歌词加载中",
//                    startTimeMs = 0,
//                    durationMs = 0,
//                    words = emptyList()
//                )
//            )
//        )
//        mediaMetadata?.let {
//            cover = it.coverUrl
//            artist = it.artists[0].name
//            title = it.title
//            album = it.album.title
//
//            viewmodel.getLyricV1(it.id.toString())
//            if (useQQMusicLyric) viewmodel.searchNew(it.title)
//        }
//    }
//
//    if(searchNew is Resource.Success){
//        DialogSelect(showDialog, searchNew,viewmodel,duration) {
//            showDialog=false
//        }
//    }
//
//    val queueSheetState = rememberBottomSheetState(
//        dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues()
//            .calculateBottomPadding(),
//        expandedBound = state.expandedBound,
//    )
//
//    BottomSheet(
//        state = state,
//        modifier = modifier,
//        backgroundColor = backgroundColor,
//        onDismiss = {
//            playerConnection.player.stop()
//            playerConnection.player.clearMediaItems()
//        },
//        collapsedContent = {
//            MiniPlayer(
//                position = position,
//                duration = duration,
//            )
//        }
//    ) {
//        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = {
//            ColorfulSlider(
//                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
//                value = position.toFloat(),
//                thumbRadius = 0.dp,
//                trackHeight = 2.dp,
//                onValueChange = { value ->
//                    sliderPosition = value // 先更新滑块 UI
//                },
//                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
//                onValueChangeFinished = {
//                    position = sliderPosition.toLong() // 确保 `position` 也是最新值
//                    playerConnection.player.seekTo(position) // ExoPlayer 跳转到新位置
//                },
//                colors = MaterialSliderDefaults.materialColors(
//                    activeTrackColor = SliderBrushColor(
//                        color = MaterialTheme.colorScheme.onPrimaryContainer
//                    ),
//                    thumbColor = SliderBrushColor(
//                        color = MaterialTheme.colorScheme.tertiaryContainer
//                    )
//                )
//            )
//            Row(
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = PlayerHorizontalPadding + 4.dp)
//            ) {
//                Text(
//                    text = makeTimeString(position),
//                    style = MaterialTheme.typography.labelMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    maxLines = 1,
//                    lineHeight = 10.sp,
//                    overflow = TextOverflow.Ellipsis,
//                )
//
//                Text(
//                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
//                    style = MaterialTheme.typography.labelMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    maxLines = 1,
//                    lineHeight = 10.sp,
//                    overflow = TextOverflow.Ellipsis,
//                )
//            }
//
//            Spacer(Modifier.height(12.dp))
//
//            Controls(
//                playerConnection = playerConnection,
//                canSkipPrevious = canSkipPrevious,
//                canSkipNext = canSkipNext,
//                isPlaying = isPlaying,
//                playbackState = playbackState,
//                repeatMode = repeatMode,
//            )
//        }
//        OptimizedBlurredImage(cover, isPlaying, 100.dp)
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            modifier = Modifier
//                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
//                .padding(bottom = queueSheetState.collapsedBound)
//        ) {
//            Box(
//                modifier = Modifier.weight(1f)
//            ) {
//                HorizontalPager(
//                    state = pagerState,
//                    modifier = Modifier
//                        .padding(horizontal = PlayerHorizontalPadding)
//                        .fillMaxSize()
//
//                ) { page ->
//                    when (page) {
//                        0 -> {
//                            mediaMetadata?.let {
//
//                                Column(
//                                    modifier = Modifier.fillMaxSize(),
//                                    verticalArrangement = Arrangement.Bottom
//                                ) {
//                                    ShowMain(
//                                        playerConnection = playerConnection,
//                                        mediaMetadata = it,
//                                        modifier = Modifier.padding(bottom = 8.dp)
//                                    )
//                                }
//                            }
//                        }
//
//                        1 -> {
//                            LyricScreen(
//                                lyricData = lyricLine.value,
//                                playerConnection = playerConnection,
//                                position = position
//                            ) {
//                                showDialog = true
//                            }
//
//                        }
//                    }
//                }
//            }
//
//            mediaMetadata?.let {
//                controlsContent(it)
//            }
//            Spacer(Modifier.height(24.dp))
//        }
//
//
//    }
//
//
//}


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewmodel: PlayerViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current

    // Theme preferences
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useQQMusicLyric by rememberPreference(UseQQMusicLyricKey, defaultValue = true)

    // Derived state for background color
    val useBlackBackground = remember(isSystemInDarkTheme, darkTheme, pureBlack) {
        val useDarkTheme =
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        useDarkTheme && pureBlack
    }

    val backgroundColor = if (useBlackBackground && state.value > state.collapsedBound) {
        lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    // Player state
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    // Media info state
    var mediaInfo by rememberSaveable(stateSaver = MediaInfoSaver) {
        mutableStateOf(
            MediaInfo()
        )
    }


    // Lyric state
    var netLyric by remember { mutableStateOf(emptyLyric) }
    var qqLyric by remember { mutableStateOf(emptyData) }
    var showDialog by remember { mutableStateOf(false) }
    val lyricLine = remember { mutableStateOf(createDefaultLyricData("歌词加载中")) }

    // ViewModel state
    val netLyricResult by viewmodel.lyric.collectAsState()
    val searchNew by viewmodel.searchNew.collectAsState()
    val lyricNew by viewmodel.lyricNew.collectAsState()

    val qqSong by viewmodel.qqSong.collectAsState()
    // UI state
    val pagerState = rememberPagerState(pageCount = { 2 })

    // Update position and duration
    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(150)
                mediaInfo = mediaInfo.copy(
                    position = playerConnection.player.currentPosition,
                    duration = playerConnection.player.duration
                )
            }
        }
    }

    // Reset position when duration is invalid
    LaunchedEffect(mediaInfo.position, mediaInfo.duration) {
        if (mediaInfo.position == 0L || mediaInfo.duration == C.TIME_UNSET) {
            mediaInfo = mediaInfo.copy(position = 0L)
        }
    }

    // Process lyrics when data changes
    LaunchedEffect(qqLyric, netLyric) {
        lyricLine.value = processLyrics(netLyric, qqLyric)
    }

    // Handle network lyric results
    LaunchedEffect(netLyricResult) {
        if (!lyricLine.value.isVerbatim) {
            when (val result = netLyricResult) {
                is Resource.Success -> netLyric = result.data
                is Resource.Error -> {
                    Log.d("searchLyric", result.toString())
                    lyricLine.value = createDefaultLyricData("歌词加载错误")
                }

                Resource.Loading -> {}
            }
        }
    }

    // Handle QQ Music lyric results
    LaunchedEffect(lyricNew) {
        when (val result = lyricNew) {
            is Resource.Success -> {
                qqLyric = result.data.musicMusichallSongPlayLyricInfoGetPlayLyricInfo.data
            }

            is Resource.Error -> {
                Log.d("qLyric", result.message)
            }

            Resource.Loading -> {}
        }
    }

    // Handle media metadata changes
    LaunchedEffect(mediaMetadata) {
        lyricLine.value = createDefaultLyricData("歌词加载中")

        mediaMetadata?.let {
            mediaInfo = MediaInfo(
                id = it.id.toString(),
                cover = it.coverUrl,
                artist = it.artists[0].name,
                title = it.title,
                album = it.album.title
            )
            // 获取网易云的歌词
            viewmodel.getLyricV1(it.id.toString())
            // 如果启用QQ音乐歌词
            if (useQQMusicLyric) {
                // 获取数据库中是否有这首歌的QQ音乐信息
                viewmodel.fetchQQSong(it.id.toString())
                // 搜索歌曲
                viewmodel.searchNew(it.title)
            }
        }
    }

    // 观察 来自数据库中的数据
    LaunchedEffect(qqSong) {
        if (qqSong != null) {
            // 如果有信息，那么自己获取并加载歌词了
            Log.d("qqSong", qqSong.toString())
            viewmodel.getLyricNew(
                title = qqSong!!.title,
                album = qqSong!!.album,
                artist = qqSong!!.artist,
                duration = qqSong!!.duration,
                id = qqSong!!.id.toInt()
            )
        }
    }

    DialogSelect(id = mediaInfo.id, showDialog, searchNew, viewmodel, mediaInfo.duration) {
        showDialog = false
    }
    // Queue sheet state
    val queueSheetState = rememberBottomSheetState(
        dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues()
            .calculateBottomPadding(),
        expandedBound = state.expandedBound,
    )

    // Main UI
    BottomSheet(
        state = state,
        modifier = modifier,
        backgroundColor = backgroundColor,
        onDismiss = {
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            MiniPlayer(
                position = mediaInfo.position,
                duration = mediaInfo.duration,
            )
        }
    ) {
        OptimizedBlurredImage(mediaInfo.cover, isPlaying, 100.dp)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(bottom = queueSheetState.collapsedBound)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .padding(horizontal = PlayerHorizontalPadding)
                        .fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            mediaMetadata?.let {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    ShowMain(
                                        playerConnection = playerConnection,
                                        mediaMetadata = it,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }

                        1 -> {
                            LyricScreen(
                                lyricData = lyricLine.value,
                                playerConnection = playerConnection,
                                position = mediaInfo.position
                            ) {
                                showDialog = true
                            }
                        }
                    }
                }
            }

            // Player controls
            mediaMetadata?.let {
                PlayerProgressSlider(
                    position = mediaInfo.position,
                    duration = mediaInfo.duration,
                    onPositionChange = { newPosition ->
                        mediaInfo = mediaInfo.copy(position = newPosition)
                        playerConnection.player.seekTo(newPosition)
                    },
                    modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
                )

                PlayerTimeDisplay(
                    position = mediaInfo.position,
                    duration = mediaInfo.duration
                )

                Spacer(Modifier.height(12.dp))

                Controls(
                    playerConnection = playerConnection,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    isPlaying = isPlaying,
                    playbackState = playbackState,
                    repeatMode = repeatMode,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// Helper functions
private fun createDefaultLyricData(message: String): LyricData {
    return LyricData(
        isVerbatim = false,
        lyricLine = listOf(
            LyricLine(
                lyric = message,
                startTimeMs = 0,
                durationMs = 0,
                words = emptyList()
            )
        )
    )
}

private fun processLyrics(
    netLyric: Lyric,
    qqLyric: LyricResult.MusicMusichallSongPlayLyricInfoGetPlayLyricInfo.Data
): LyricData {
    val qrc = QRCUtils.decodeLyric(qqLyric.lyric)
    val qrcT = QRCUtils.decodeLyric(qqLyric.trans)

    Log.d("Lyric Log",qrc)
    Log.d("Lyric Log",qrcT)

    return when {
        // Case 1: NetEase yrc and tlyric available
        netLyric.yrc != null && netLyric.tlyric != null -> {
            Log.d("Lyric parse", "使用网易云的逐字歌词以及翻译")
            LyricData(
                isVerbatim = true,
                source = LyricSource.NetEaseCloudMusic,
                lyricLine = netLyric.parseYrc()
            )
        }
        // Case 2: QQ Music lyric and translation available
        qrc != "" && qrcT != "" -> {
            Log.d("Lyric parse", "使用QQ音乐的逐字歌词以及翻译")
            LyricData(
                isVerbatim = true,
                source = LyricSource.QQMusic,
                lyricLine = QRCUtils.parse(qrc, qrcT)
            )
        }
        // Case 3: QQ Music lyric and NetEase translation available
        qrc != "" && netLyric.tlyric != null -> {
            Log.d("Lyric parse", "使用QQ音乐的逐字歌词兵合并网易云的翻译歌词")
            LyricData(
                isVerbatim = true,
                source = LyricSource.QQMusic,
                lyricLine = QRCUtils.parse(qrc, netLyric.tlyric.lyric)
            )
        }
        // Case 4: Only QQ Music lyric available
        qrc != "" -> {
            Log.d("Lyric parse", "使用")
            LyricData(
                isVerbatim = true,
                source = LyricSource.QQMusic,
                lyricLine = QRCUtils.parse(qrc, "")
            )
        }
        // Default: Use NetEase lyric
        else -> {
            Log.d("Lyric parse", "netLyric")
            LyricData(
                isVerbatim = false,
                source = LyricSource.NetEaseCloudMusic,
                lyricLine = netLyric.parseYrc()
            )
        }
    }
}


data class MediaInfo(
    val id: String = "",
    val cover: String = "", // Assuming cover is a URL or file path
    val album: String = "",
    val artist: String = "",
    val title: String = "",
    val position: Long = 0L,
    val duration: Long = 0L
)

val MediaInfoSaver: Saver<MediaInfo, *> = Saver(
    save = { mediaInfo ->
        // Save MediaInfo as a Bundle-compatible Map
        mapOf(
            "cover" to mediaInfo.cover,
            "album" to mediaInfo.album,
            "artist" to mediaInfo.artist,
            "title" to mediaInfo.title,
            "position" to mediaInfo.position,
            "duration" to mediaInfo.duration
        )
    },
    restore = { restored ->
        // Restore MediaInfo from the saved Map
        MediaInfo(
            cover = restored["cover"].toString(),
            album = restored["album"].toString(),
            artist = restored["artist"].toString(),
            title = restored["title"].toString(),
            position = restored["position"] as? Long ?: 0L,
            duration = restored["duration"] as? Long ?: 0L
        )
    }
)

enum class DarkMode {
    ON, OFF, AUTO
}