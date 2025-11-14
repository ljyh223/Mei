package com.ljyh.mei.ui.component.player


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import com.ljyh.mei.constants.DarkModeKey
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.constants.DynamicStreamerKey
import com.ljyh.mei.constants.DynamicStreamerType
import com.ljyh.mei.constants.DynamicStreamerTypeKey
import com.ljyh.mei.constants.PlayModeKey
import com.ljyh.mei.constants.PlayerHorizontalPadding
import com.ljyh.mei.constants.PureBlackKey
import com.ljyh.mei.constants.QueuePeekHeight
import com.ljyh.mei.constants.UseQQMusicLyricKey
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.PlayMode
import com.ljyh.mei.ui.component.sheet.BottomSheet
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.sheet.HorizontalSwipeDirection
import com.ljyh.mei.ui.component.player.component.Controls
import com.ljyh.mei.ui.component.player.component.Cover
import com.ljyh.mei.ui.component.player.component.Debug
import com.ljyh.mei.ui.component.player.component.LyricScreen
import com.ljyh.mei.ui.component.player.component.LyricSourceData
import com.ljyh.mei.ui.component.player.component.OptimizedBlurredImage
import com.ljyh.mei.ui.component.player.component.PlayerProgressSlider
import com.ljyh.mei.ui.component.sheet.rememberBottomSheetState
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.utils.TimeUtils.formatMilliseconds
import com.ljyh.mei.utils.encrypt.QRCUtils
import com.ljyh.mei.utils.lyric.createDefaultLyricData
import com.ljyh.mei.utils.lyric.mergeLyrics
import com.ljyh.mei.utils.rememberEnumPreference
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current

    // Theme preferences
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useQQMusicLyric by rememberPreference(UseQQMusicLyricKey, defaultValue = true)
    val dynamicStreamerType by rememberEnumPreference(
        DynamicStreamerTypeKey,
        defaultValue = DynamicStreamerType.Image
    )
    val dynamicStreamer by rememberPreference(DynamicStreamerKey, defaultValue = true)
    val debug by rememberPreference(DebugKey, defaultValue = false)
    val playMode by rememberPreference(PlayModeKey, defaultValue = 3)

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
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

//    val playMode = remember { mutableStateOf(PlayMode.REPEAT_MODE_ALL) }


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
    val netLyricResult by playerViewModel.lyric.collectAsState()
    val searchResult by playerViewModel.searchResult.collectAsState()
    val qqLyricResult by playerViewModel.lyricResult.collectAsState()
    val amLyrcResult by playerViewModel.amLyric.collectAsState()

    val qqSong by playerViewModel.qqSong.collectAsState()

    if (PlayMode.fromInt(playMode) == PlayMode.SHUFFLE_MODE_ALL) {
        playerConnection.player.shuffleModeEnabled = true
    } else {
        playerConnection.player.shuffleModeEnabled = false
        playerConnection.player.repeatMode = playMode
    }
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
            position = 0L
        }
    }

    LaunchedEffect(netLyricResult, qqLyricResult, amLyrcResult) {
        val sources = mutableListOf<LyricSourceData>()
        (amLyrcResult as? Resource.Success)?.let {
            sources.add(LyricSourceData.AM(it.data))
        }

        (netLyricResult as? Resource.Success)?.data?.let {
            sources.add(LyricSourceData.NetEase(it))
        }

        (qqLyricResult as? Resource.Success)?.data?.musicMusichallSongPlayLyricInfoGetPlayLyricInfo?.data?.let {
            // 此时的歌词还没有解密
            val qrc=it.copy(
                lyric = QRCUtils.decodeLyric(it.lyric),
                trans = QRCUtils.decodeLyric(it.trans, true),
                roma  = QRCUtils.decodeLyric(it.roma)
            )

            sources.add(LyricSourceData.QQMusic(qrc))
        }

        lyricLine.value = mergeLyrics(sources)
        Log.d("lyricLine", lyricLine.value.toString())
    }

    // Handle media metadata changes
    LaunchedEffect(mediaMetadata) {
        playerViewModel.clear()
        lyricLine.value = createDefaultLyricData("歌词加载中")
        playerViewModel.mediaMetadata = mediaMetadata
        mediaMetadata?.let {
            mediaInfo = MediaInfo(
                id = it.id.toString(),
                cover = it.coverUrl,
                artist = it.artists[0].name,
                title = it.title,
                album = it.album.title
            )
            playerViewModel.getLyricV1(it.id.toString())
            playerViewModel.getAMLLyric(it.id.toString())
            if (useQQMusicLyric) {
                playerViewModel.fetchQQSong(it.id.toString())
                playerViewModel.searchNew(it.title)
            }
        }
    }

    // 观察 来自数据库中的数据
    LaunchedEffect(qqSong) {
        if (qqSong != null) {
            // 如果有信息，那么自己获取并加载歌词了
            Log.d("qqSong", qqSong.toString())
            playerViewModel.getLyricNew(
                title = qqSong!!.title,
                album = qqSong!!.album,
                artist = qqSong!!.artist,
                duration = qqSong!!.duration,
                id = qqSong!!.qid.toInt()
            )
        }
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
        onHorizontalSwipe = { direction ->
            when (direction) {
                HorizontalSwipeDirection.Left -> playerConnection.seekToNext()
                HorizontalSwipeDirection.Right -> playerConnection.seekToPrevious()
            }
        },
        collapsedContent = {
            MiniPlayer(
                position = position,
                duration = duration,
            )
        }
    ) {
        // 背景渲染
        if (dynamicStreamerType == DynamicStreamerType.Image)
            OptimizedBlurredImage(mediaInfo.cover, isPlaying, 100.dp)

        if (debug) {
            Debug(
                title = mediaInfo.title,
                artist = mediaInfo.artist,
                album = mediaInfo.album,
                duration = formatMilliseconds(duration).toString(),
                id = mediaInfo.id,
                qid = qqSong?.qid ?: "null",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = PlayerHorizontalPadding, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部标题栏
            mediaMetadata?.let { metadata ->
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = metadata.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.basicMarquee()
                    )

                    Row {
                        if (metadata.artists.isNotEmpty()) {
                            Text(
                                text = metadata.artists.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                        }

                        if(metadata.album.title.isNotEmpty()){
                            Text(" - ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            Text(
                                text = metadata.album.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }

                }
            }



            // 中间封面 + 歌词（可滑动）
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        mediaMetadata?.let {
                            Cover(
                                viewModel = playerViewModel,
                                playerConnection = playerConnection,
                                mediaMetadata = it,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                        }
                    }

                    1 -> {
                        LyricScreen(
                            lyricData = lyricLine.value,
                            playerConnection = playerConnection,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 进度条
            PlayerProgressSlider(
                position = position,
                duration = duration,
                onPositionChange = { newPosition ->
                    position = newPosition
                    playerConnection.player.seekTo(newPosition)
                },
                trackId = mediaInfo.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(12.dp))

            Controls(
                playerConnection = playerConnection,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                isPlaying = isPlaying,
                playbackState = playbackState,
            )

            Spacer(Modifier.height(12.dp))

            Queue(
                state = queueSheetState,
                playerBottomSheetState = state,
                backgroundColor = backgroundColor,
                navController = navController,
                playerViewModel = playerViewModel,
                mediaMetadata = mediaMetadata
            )

            Spacer(Modifier.height(8.dp))
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