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
import androidx.compose.runtime.mutableLongStateOf
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
import com.ljyh.music.data.model.parseYrc
import com.ljyh.music.data.model.qq.u.LyricResult
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


    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    // Media info state
    var mediaInfo by rememberSaveable(stateSaver = MediaInfoSaver) {
        mutableStateOf(
            MediaInfo()
        )
    }

    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }


    var showDialog by remember { mutableStateOf(false) }
    val lyricLine = remember { mutableStateOf(createDefaultLyricData("歌词加载中")) }

    // ViewModel state
    val netLyricResult by viewmodel.lyric.collectAsState()
    val searchResult by viewmodel.searchResult.collectAsState()
    val qqLyricResult by viewmodel.lyricResult.collectAsState()

    val qqSong by viewmodel.qqSong.collectAsState()
    // UI state
    val pagerState = rememberPagerState(pageCount = { 2 })

    // Update position and duration
    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    // Reset position when duration is invalid
    LaunchedEffect(position, duration) {
        if (position == 0L || duration == C.TIME_UNSET) {
            position=0L
        }
    }

    LaunchedEffect(netLyricResult, qqLyricResult) {
        var netLyric: Lyric? = null
        var qqLyric: LyricResult.MusicMusichallSongPlayLyricInfoGetPlayLyricInfo.Data? = null

        when (val result = netLyricResult) {
            is Resource.Error -> {}
            Resource.Loading -> {}
            is Resource.Success -> {
                netLyric = result.data
            }
        }

        when (val result = qqLyricResult) {
            is Resource.Error -> {}
            Resource.Loading -> {}
            is Resource.Success -> {
                qqLyric = result.data.musicMusichallSongPlayLyricInfoGetPlayLyricInfo.data
            }
        }

        lyricLine.value = processLyrics(netLyric, qqLyric)


    }

    // Handle media metadata changes
    LaunchedEffect(mediaMetadata) {
        viewmodel.clear()
        lyricLine.value = createDefaultLyricData("歌词加载中")
        mediaMetadata?.let {
            mediaInfo = MediaInfo(
                id = it.id.toString(),
                cover = it.coverUrl,
                artist = it.artists[0].name,
                title = it.title,
                album = it.album.title,

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
                id = qqSong!!.qid.toInt()
            )
        }
    }

    DialogSelect(id = mediaInfo.id, showDialog, searchResult, viewmodel, duration) {
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
                position = position,
                duration = duration,
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
                                position = position
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
                    position = position,
                    duration = duration,
                    onPositionChange = { newPosition ->
                        position=newPosition
                        playerConnection.player.seekTo(newPosition)
                    },
                    modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
                )

                PlayerTimeDisplay(
                    position = position,
                    duration = duration
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
    netLyric: Lyric?,
    qqLyric: LyricResult.MusicMusichallSongPlayLyricInfoGetPlayLyricInfo.Data?
): LyricData {
    Log.d("lyric load", "start")
    Log.d("lyric load netLyric ->", netLyric.toString())
    Log.d("lyric load qqLyric  ->", qqLyric.toString())
    when {
        netLyric?.yrc != null && netLyric.tlyric != null -> {
            Log.d("lyric load", "YRC found")
            return LyricData(
                isVerbatim = true,
                source = LyricSource.NetEaseCloudMusic,
                lyricLine = netLyric.parseYrc()
            )
        }

        qqLyric?.lyric != null && qqLyric.trans != "" -> {
            Log.d("lyric load", "QRC found")
            return LyricData(
                isVerbatim = true,
                source = LyricSource.QQMusic,
                lyricLine = QRCUtils.parse(
                    QRCUtils.decodeLyric(qqLyric.lyric), QRCUtils.decodeLyric(qqLyric.trans,true)
                )
            )
        }

        netLyric?.lrc != null -> {
            Log.d("lyric load", "LRC found")
            return LyricData(
                isVerbatim = false,
                source = LyricSource.NetEaseCloudMusic,
                lyricLine = netLyric.parseYrc()
            )
        }


        qqLyric?.lyric != null -> {
            Log.d("lyric load", "QRC-1 found")
            return LyricData(
                isVerbatim = true,
                source = LyricSource.QQMusic,
                lyricLine = QRCUtils.parse(QRCUtils.decodeLyric(qqLyric.lyric), "")
            )
        }


        else -> {
            Log.d("lyric load", "No lyric found")
            return createDefaultLyricData("暂无歌词")
        }


    }

}


data class MediaInfo(
    val id: String = "",
    val cover: String = "", // Assuming cover is a URL or file path
    val album: String = "",
    val artist: String = "",
    val title: String = "",
)

val MediaInfoSaver: Saver<MediaInfo, *> = Saver(
    save = { mediaInfo ->
        // Save MediaInfo as a Bundle-compatible Map
        mapOf(
            "cover" to mediaInfo.cover,
            "album" to mediaInfo.album,
            "artist" to mediaInfo.artist,
            "title" to mediaInfo.title,
        )
    },
    restore = { restored ->
        // Restore MediaInfo from the saved Map
        MediaInfo(
            cover = restored["cover"].toString(),
            album = restored["album"].toString(),
            artist = restored["artist"].toString(),
            title = restored["title"].toString(),
        )
    }
)

enum class DarkMode {
    ON, OFF, AUTO
}