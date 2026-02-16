package com.ljyh.mei.ui.component.player.state

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.AutoMatchQQMusicLyricKey
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.constants.MatchSuccessToastKey
import com.ljyh.mei.constants.UseQQMusicLyricKey
import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.qq.u.LyricResult
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.data.model.qq.u.SearchResult.Req0.Data.Body.Song.S
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.component.player.PlayerViewModel
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.utils.encrypt.QRCUtils
import com.ljyh.mei.utils.lyric.LyricMatchAlgorithm
import com.ljyh.mei.utils.lyric.createDefaultLyricData
import com.ljyh.mei.utils.lyric.mergeLyrics
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 自动匹配结果
 */
private sealed class MatchResult {
    data class Success(val song: S?, val bestMatch: LyricMatchAlgorithm.MatchResult) : MatchResult()
    data object NoMatch : MatchResult()
    data object EmptyList : MatchResult()
}

/**
 * 播放器状态容器
 * 封装所有重复的状态管理和歌词加载逻辑
 * 注意：状态收集（collectAsState）必须在 Composable 函数中进行
 */
@UnstableApi
class PlayerStateContainer(
    val playerViewModel: PlayerViewModel,
    val playerConnection: PlayerConnection
) {
    // ========== 可变状态 ==========

    var sliderPosition by mutableFloatStateOf(0f)
        internal set

    var duration by mutableLongStateOf(0L)
        internal set

    var isDragging by mutableStateOf(false)
        internal set

    var lyricLine by mutableStateOf(createDefaultLyricData("歌词加载中"))
        internal set


    // ========== 状态槽（将在 Composable 中填充） ==========

    lateinit var playbackState: State<Int>
        internal set

    lateinit var isPlaying: State<Boolean>
        internal set

    lateinit var mediaMetadata: State<MediaMetadata?>
        internal set

    lateinit var netLyricResult: State<Resource<Lyric>>
        internal set

    lateinit var qqLyricResult: State<Resource<LyricResult>>
        internal set

    lateinit var qqLyricSearch: State<Resource<SearchResult>>
        internal set

    lateinit var amLyricResult: State<Resource<String>>
        internal set

    lateinit var qqSong: State<QQSong?>
        internal set

    lateinit var isLiked: State<Like?>
        internal set

    lateinit var allPlaylist: State<List<Playlist>>
        internal set

    lateinit var canSkipPrevious: State<Boolean>
        internal set

    lateinit var canSkipNext: State<Boolean>
        internal set

    lateinit var isFMMode: State<Boolean>
        internal set


    var autoMatchResult by mutableStateOf<String?>(null)
        internal set


    // ========== 公共方法 ==========

    /**
     * 重置状态（当歌曲切换时调用）
     */
    fun reset() {
        lyricLine = createDefaultLyricData("歌词加载中", source = LyricSource.Loading)
        sliderPosition = 0f
        duration = 0L
    }
}

/**
 * 记忆并创建播放器状态容器
 */
@Composable
@UnstableApi
fun rememberPlayerStateContainer(
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playerConnection: PlayerConnection
): PlayerStateContainer {
    val context = LocalContext.current
    val useQQMusicLyric by rememberPreference(UseQQMusicLyricKey, defaultValue = true)
    val debug by rememberPreference(DebugKey, defaultValue = false)

    val autoMatchQQMusicLyric = rememberPreference(AutoMatchQQMusicLyricKey, defaultValue = false)
    val matchSuccessToast = rememberPreference(MatchSuccessToastKey, defaultValue = true)

    val container = remember(playerConnection) {
        PlayerStateContainer(
            playerViewModel = playerViewModel,
            playerConnection = playerConnection
        )
    }

    // ========== 收集所有状态（必须在 Composable 中进行） ==========
    container.playbackState = playerConnection.playbackState.collectAsState()
    container.isPlaying = playerConnection.isPlaying.collectAsState()
    container.mediaMetadata = playerConnection.mediaMetadata.collectAsState()
    container.canSkipPrevious = playerConnection.canSkipPrevious.collectAsState()
    container.canSkipNext = playerConnection.canSkipNext.collectAsState()
    container.isFMMode = playerConnection.isFMMode.collectAsState()

    container.netLyricResult = playerViewModel.lyric.collectAsState()
    container.qqLyricResult = playerViewModel.lyricResult.collectAsState()
    container.qqLyricSearch = playerViewModel.searchResult.collectAsState()
    container.amLyricResult = playerViewModel.amLyric.collectAsState()
    container.qqSong = playerViewModel.qqSong.collectAsState()
    container.isLiked = playerViewModel.like.collectAsState(initial = null)
    container.allPlaylist = playerViewModel.localPlaylists.collectAsState()

    // ========== 进度条更新 LaunchedEffect ==========
    LaunchedEffect(container.playbackState.value, container.isPlaying.value, container.isDragging) {
        val playbackState = container.playbackState.value
        val isPlaying = container.isPlaying.value
        val isDragging = container.isDragging

        if (playbackState == STATE_READY && isPlaying && !isDragging) {
            while (isActive) {
                container.sliderPosition = playerConnection.player.currentPosition.toFloat()
                container.duration = playerConnection.player.duration.coerceAtLeast(1L)
                delay(50)
            }
        } else if (!isPlaying && !isDragging) {
            // 暂停时同步一次，防止状态不一致
            container.sliderPosition = playerConnection.player.currentPosition.toFloat()
            container.duration = playerConnection.player.duration.coerceAtLeast(1L)
        }
    }

    // ========== 歌词加载 LaunchedEffect - MediaMetadata 变化 ==========
    LaunchedEffect(container.mediaMetadata.value) {
        container.mediaMetadata.value?.let { meta ->
            // 重置状态
            container.reset()

            // 设置当前媒体元数据
            playerViewModel.mediaMetadata = meta

            // 获取歌词
            playerViewModel.getLyricV1(meta.id.toString())
            playerViewModel.getAMLLyric(meta.id.toString())

            // 如果启用了 QQ 音乐歌词
            if (useQQMusicLyric) {
                playerViewModel.fetchQQSong(meta.id.toString())
                playerViewModel.searchNew(meta.title)
            }

            if (debug) {
                Timber.tag("Player").d("MediaMetadata: $meta")
                Timber.tag("Player").d("MediaMetadata: cover ${meta.coverUrl}")
            }
        }
    }

    // ========== 歌词加载 LaunchedEffect - QQSong 变化 ==========
    LaunchedEffect(container.qqSong.value) {
        if (debug) {
            Timber.tag("Player").d("QQSong: ${container.qqSong.value}")
        }
        container.qqSong.value?.let { song ->
            if (debug) {
                Timber.tag("Player").d("QQSong111: $song")
            }
            playerViewModel.getLyricNew(
                title = song.title,
                album = song.album,
                artist = song.artist,
                duration = song.duration.toLong(),
                id = song.qid.toLong()
            )
        }
    }

    LaunchedEffect(container.qqLyricSearch.value, container.mediaMetadata.value) {
        if(!autoMatchQQMusicLyric.value) return@LaunchedEffect
        val searchResult = container.qqLyricSearch.value
        val metadata = container.mediaMetadata.value

        if (searchResult is Resource.Success && metadata != null) {
            // Move heavy computation to background thread
            val result = withContext(Dispatchers.Default) {
                val songList = searchResult.data.req0.data.body.song.list

                if (songList.isNotEmpty()) {
                    if (debug) {
                        Timber.tag("Player").d("Found ${songList.size} songs, starting auto match")
                    }

                    // 构建目标歌曲信息
                    val targetInfo = LyricMatchAlgorithm.SongMatchInfo(
                        id = metadata.id.toString(),
                        title = metadata.title,
                        artist = metadata.artists.joinToString(",") { it.name },
                        album = metadata.album.title,
                        duration = container.duration
                    )

                    // 转换搜索结果为匹配算法格式
                    val matchCandidates = songList.map { song ->
                        LyricMatchAlgorithm.SongMatchInfo(
                            id = song.id.toString(),
                            title = song.title,
                            artist = song.singer.joinToString(",") { it.name },
                            album = song.album.title,
                            duration = (song.interval) * 1000  // 转换为毫秒
                        )
                    }

                    // 执行自动匹配
                    val bestMatch = LyricMatchAlgorithm.findBestMatch(
                        targetInfo = targetInfo,
                        searchResults = matchCandidates,
                        minScore = 50f  // 最低50分才认为匹配
                    )

                    if (bestMatch != null) {
                        // 找到最佳匹配，返回结果用于主线程处理
                        val matchedSong = songList.find {
                            it.id.toString() == bestMatch.song.id
                        }
                        MatchResult.Success(matchedSong, bestMatch)
                    } else {
                        if (debug) {
                            Timber.tag("Player").d("No suitable match found")
                        }
                        MatchResult.NoMatch
                    }
                } else {
                    MatchResult.EmptyList
                }
            }

            // Handle UI updates on main thread
            when (result) {
                is MatchResult.Success -> {
                    val song = result.song
                    val bestMatch = result.bestMatch

                    song?.let {
                        if (debug) {
                            Timber.tag("Player").d(
                                """Auto matched song:
                                    |Title: ${song.title}
                                    |Score: ${bestMatch.totalScore}
                                    |Title Score: ${bestMatch.titleScore}
                                    |Artist Score: ${bestMatch.artistScore}
                                """.trimMargin()
                            )
                        }

                        container.autoMatchResult = """
                            自动匹配成功:
                            标题: ${song.title}
                            歌手: ${song.singer.joinToString(",") { it.name }}
                            匹配分数: ${"%.2f".format(bestMatch.totalScore)}
                            标题分数: ${"%.2f".format(bestMatch.titleScore)}
                            歌手分数: ${"%.2f".format(bestMatch.artistScore)}
                        """.trimIndent()

                        // 自动获取歌词
                        playerViewModel.getLyricNew(
                            title = song.title,
                            album = song.album.title,
                            artist = song.singer.joinToString(",") { it.name },
                            duration = (song.interval) * 1000L,
                            id = song.id
                        )

                        // 显示提示（可选）
                        if(matchSuccessToast.value){
                            Toast.makeText(
                                context,
                                "已自动匹配: ${song.title}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                is MatchResult.NoMatch -> {
                    container.autoMatchResult = "未找到合适的匹配，请手动选择"
                }
                is MatchResult.EmptyList -> {
                    // Do nothing
                }
            }
        }
    }

    // ========== 歌词合并 LaunchedEffect ==========
    LaunchedEffect(container.netLyricResult.value, container.qqLyricResult.value, container.amLyricResult.value) {
        withContext(Dispatchers.Default) {
            val isPureMusic = (container.netLyricResult.value as? Resource.Success)?.data?.pureMusic == true
            val sources = mutableListOf<LyricSourceData>()
            // 添加 AML 歌词
            (container.amLyricResult.value as? Resource.Success)?.let {
                sources.add(LyricSourceData.AM(it.data))
            }

            // 添加网易云歌词
            (container.netLyricResult.value as? Resource.Success)?.data?.let {
                sources.add(LyricSourceData.NetEase(it))
            }

            // 添加 QQ 音乐歌词
            (container.qqLyricResult.value as? Resource.Success)?.data?.musicMusichallSongPlayLyricInfoGetPlayLyricInfo?.data?.let {
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

            // 合并歌词
            val merged = mergeLyrics(sources, isPureMusic)

            // 切换回主线程更新 UI
            withContext(Dispatchers.Main) {
                container.lyricLine = merged
            }
        }
    }

    return container
}
