package com.ljyh.mei.ui.component.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.component.player.PlayerViewModel
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.utils.lyric.createDefaultLyricData
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@UnstableApi
class PlayerStateContainer(
    val playerViewModel: PlayerViewModel,
    val playerConnection: PlayerConnection
) {
    var sliderPosition by mutableFloatStateOf(0f)
        internal set

    var duration by mutableLongStateOf(0L)
        internal set

    var isDragging by mutableStateOf(false)
        internal set

    var lyricLine by mutableStateOf(createDefaultLyricData("歌词加载中"))
        internal set

    var currentSongId: String? = null
        internal set

    lateinit var playbackState: State<Int>
        internal set

    lateinit var isPlaying: State<Boolean>
        internal set

    lateinit var mediaMetadata: State<MediaMetadata?>
        internal set

    lateinit var lyricResult: State<LyricData>
        internal set

    lateinit var qqLyricSearch: State<Resource<SearchResult>>
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

    var controlsVisible by mutableStateOf(true)
        internal set

    fun reset() {
        lyricLine = createDefaultLyricData("歌词加载中", source = LyricSource.Loading)
        sliderPosition = 0f
        duration = 0L
    }
}

@Composable
@UnstableApi
fun rememberPlayerStateContainer(
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playerConnection: PlayerConnection
): PlayerStateContainer {
    val debug by rememberPreference(DebugKey, defaultValue = false)

    val container = remember(playerConnection) {
        PlayerStateContainer(
            playerViewModel = playerViewModel,
            playerConnection = playerConnection
        )
    }

    container.playbackState = playerConnection.playbackState.collectAsState()
    container.isPlaying = playerConnection.isPlaying.collectAsState()
    container.mediaMetadata = playerConnection.mediaMetadata.collectAsState()
    container.canSkipPrevious = playerConnection.canSkipPrevious.collectAsState()
    container.canSkipNext = playerConnection.canSkipNext.collectAsState()
    container.isFMMode = playerConnection.isFMMode.collectAsState()

    container.lyricResult = playerViewModel.lyric.collectAsState()
    container.qqLyricSearch = playerViewModel.searchResult.collectAsState()
    container.isLiked = playerViewModel.like.collectAsState(initial = null)
    container.allPlaylist = playerViewModel.localPlaylists.collectAsState()

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
            container.sliderPosition = playerConnection.player.currentPosition.toFloat()
            container.duration = playerConnection.player.duration.coerceAtLeast(1L)
        }
    }

    LaunchedEffect(container.mediaMetadata.value?.id) {
        container.mediaMetadata.value?.let { meta ->
            container.currentSongId = meta.id.toString()
            container.reset()
            playerViewModel.lyricManager.loadLyrics(meta)
        }
    }

    LaunchedEffect(container.lyricResult.value) {
        container.lyricLine = container.lyricResult.value
    }

    return container
}
