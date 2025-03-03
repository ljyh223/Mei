package com.ljyh.music.ui.component.player


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.ljyh.music.data.model.MediaMetadata
import com.ljyh.music.data.model.emptyLyric
import com.ljyh.music.data.model.parseYrc
import com.ljyh.music.data.model.qq.c.LyricCmd
import com.ljyh.music.data.model.qq.c.emptyLyricCmd
import com.ljyh.music.data.model.qq.u.LyricResult.MusicMusichallSongPlayLyricInfoGetPlayLyricInfo
import com.ljyh.music.data.model.qq.u.emptyData
import com.ljyh.music.data.network.Resource
import com.ljyh.music.ui.ShareViewModel
import com.ljyh.music.ui.component.BottomSheet
import com.ljyh.music.ui.component.BottomSheetState
import com.ljyh.music.ui.component.rememberBottomSheetState
import com.ljyh.music.ui.local.LocalPlayerConnection
import com.ljyh.music.utils.encrypt.QRCUtils
import com.ljyh.music.utils.extractContent
import com.ljyh.music.utils.makeTimeString
import com.ljyh.music.utils.rememberEnumPreference
import com.ljyh.music.utils.rememberPreference
import com.smarttoolfactory.slider.ColorfulSlider
import com.smarttoolfactory.slider.MaterialSliderDefaults
import com.smarttoolfactory.slider.SliderBrushColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewmodel: ShareViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val useQQMusicLyric by rememberPreference(UseQQMusicLyricKey,defaultValue = true)
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
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    var cover by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }

    var netLyric by remember { mutableStateOf(emptyLyric) }
    var qqLyric by remember {
        mutableStateOf(
            emptyData
        )
    }

    val netLyricResult by viewmodel.lyric.collectAsState()
    val searchNew by viewmodel.searchNew.collectAsState()
    val lyricNew by viewmodel.lyricNew.collectAsState()


    val lyricLine = remember {
        mutableStateOf(
            LyricData(
                isVerbatim = false,
                lyricLine = listOf(
                    LyricLine(
                        lyric = "歌词加载错误",
                        startTimeMs = 0,
                        durationMs = 0,
                        words = emptyList()
                    )
                )
            )
        )
    }




    LaunchedEffect(qqLyric, netLyric) {
        val qrc = QRCUtils.decodeLyric(qqLyric.lyric)
        val qrcT = QRCUtils.decodeLyric(qqLyric.trans)


        if (netLyric.yrc != null && netLyric.tlyric != null) {
            Log.d("Lyric parse", "netLyric and netLyric T")
            lyricLine.value = LyricData(
                isVerbatim = true,
                source = LyricSource.NetEaseCloudMusic,
                lyricLine = netLyric.parseYrc()
            )
            return@LaunchedEffect
        }


        if (qrc != "" && qrcT != "") {
            Log.d("Lyric parse", "qrc and qrcT")
            lyricLine.value = LyricData(
                isVerbatim = true,
                source = LyricSource.QQMusic,
                lyricLine = QRCUtils.parse(qrc, qrcT)
            )
            return@LaunchedEffect
        }

        if (qrc != "" && netLyric.tlyric != null) {
            Log.d("Lyric parse", "qrc and netLyric T")
            lyricLine.value = LyricData(
                isVerbatim = true,
                source = LyricSource.QQMusic,
                lyricLine = QRCUtils.parse(qrc, netLyric.tlyric?.lyric ?: "")
            )
            return@LaunchedEffect
        }


        if (qrc != "") {
            Log.d("Lyric parse", "qrc")
            lyricLine.value = LyricData(
                isVerbatim = true,
                source = LyricSource.QQMusic,
                lyricLine = QRCUtils.parse(qrc, "")
            )
            return@LaunchedEffect
        }


        Log.d("Lyric parse", "netLyric")
        lyricLine.value = LyricData(
            isVerbatim = false,
            source = LyricSource.NetEaseCloudMusic,
            lyricLine = netLyric.parseYrc()
        )
        return@LaunchedEffect


    }
    // 网易云官方的歌词
    LaunchedEffect(netLyricResult) {
        if (!lyricLine.value.isVerbatim) {
            when (val result = netLyricResult) {
                is Resource.Success -> {
                    netLyric = result.data
                }

                is Resource.Error -> {
                    Log.d("searchLyric", result.toString())
                    lyricLine.value = LyricData(
                        isVerbatim = false,
                        lyricLine = listOf(
                            LyricLine(
                                lyric = "歌词加载错误",
                                startTimeMs = 0,
                                durationMs = 0,
                                words = emptyList()
                            )
                        )
                    )
                }

                Resource.Loading -> {}
            }
        }

    }

    // 检索qq音乐的歌曲
    LaunchedEffect(searchNew) {
        when (val result = searchNew) {
            is Resource.Success -> {
                Log.d("searchLyric", result.toString())
                val s =
                    result.data.req0.data.body.song.list.find { it.title == title && it.singer.any { si -> si.name == artist } }
                if (s != null) {
                    Log.d("searchLyric", "id ==>${s.id}")
                    viewmodel.getLyricNew(
                        title = title,
                        album = album,
                        artist = artist,
                        duration = s.interval,
                        id = s.id
                    )
                } else {
                    Log.d("searchLyric", "qqSearch id is null")
                    qqLyric = emptyData
                }
            }


            is Resource.Error -> {
                qqLyric = emptyData
                Log.d("searchLyric", result.message)
            }

            Resource.Loading -> {
            }
        }
    }

    //获取qq音乐的歌词
    LaunchedEffect(lyricNew) {
        when (val result = lyricNew) {
            is Resource.Success -> {
                qqLyric = result.data.musicMusichallSongPlayLyricInfoGetPlayLyricInfo.data
            }

            is Resource.Error -> {
                qqLyric = emptyData
                Log.d("qLyric", result.message)
            }

            Resource.Loading -> {
            }
        }
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    LaunchedEffect(position, duration) {
        if (position == 0L || duration == C.TIME_UNSET) {
            sliderPosition = 0f // 重置滑块
        }
    }
    // 在这里处理 mediaMetadata 的变化
    LaunchedEffect(mediaMetadata) {
        qqLyric = emptyData
        netLyric = emptyLyric
        lyricLine.value = LyricData(
            isVerbatim = false,
            lyricLine = listOf(
                LyricLine(
                    lyric = "歌词加载中",
                    startTimeMs = 0,
                    durationMs = 0,
                    words = emptyList()
                )
            )
        )
        mediaMetadata?.let {
            cover = it.coverUrl
            artist = it.artists[0].name
            title = it.title
            album = it.album.title

            viewmodel.getLyricV1(it.id.toString())
            if(useQQMusicLyric) viewmodel.searchNew(it.title)


        }
    }


    val queueSheetState = rememberBottomSheetState(
        dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues()
            .calculateBottomPadding(),
        expandedBound = state.expandedBound,
    )

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
                position = position,
                duration = duration,
            )
        }
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = {
            ColorfulSlider(
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                value = position.toFloat(),
                thumbRadius = 0.dp,
                trackHeight = 2.dp,
                onValueChange = { value ->
                    sliderPosition = value // 先更新滑块 UI
                },
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChangeFinished = {
                    position = sliderPosition.toLong() // 确保 `position` 也是最新值
                    playerConnection.player.seekTo(position) // ExoPlayer 跳转到新位置
                },
                colors = MaterialSliderDefaults.materialColors(
                    activeTrackColor = SliderBrushColor(
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    thumbColor = SliderBrushColor(
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp)
            ) {
                Text(
                    text = makeTimeString(position),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    lineHeight = 10.sp,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    lineHeight = 10.sp,
                    overflow = TextOverflow.Ellipsis,
                )
            }

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
        OptimizedBlurredImage(cover, isPlaying, 100.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(bottom = queueSheetState.collapsedBound)
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
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
                                position = position
                            )

                        }
                    }
                }
            }

            mediaMetadata?.let {
                controlsContent(it)
            }
            Spacer(Modifier.height(24.dp))
        }


    }


}


enum class DarkMode {
    ON, OFF, AUTO
}