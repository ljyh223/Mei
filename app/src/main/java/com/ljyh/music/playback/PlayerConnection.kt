package com.ljyh.music.playback

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import com.ljyh.music.constants.PlayModeKey
import com.ljyh.music.data.model.metadata
import com.ljyh.music.di.AppDatabase
import com.ljyh.music.extensions.currentMetadata
import com.ljyh.music.extensions.getCurrentQueueIndex
import com.ljyh.music.extensions.getQueueWindows
import com.ljyh.music.playback.queue.Queue
import com.ljyh.music.utils.dataStore
import com.ljyh.music.utils.get
import com.ljyh.music.utils.rememberEnumPreference
import com.ljyh.music.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn


@UnstableApi
class PlayerConnection
    (
    context: Context,
    binder: MusicService.MusicBinder,
    val database: AppDatabase,
    scope: CoroutineScope,
) : Player.Listener {

    init {
        Log.d("PlayerConnection", "PlayerConnection initialized")
    }

    val service = binder.service
    val player = service.player

    val playbackState = MutableStateFlow(player.playbackState)
    private val playWhenReady = MutableStateFlow(player.playWhenReady)
    val isPlaying = combine(playbackState, playWhenReady) { playbackState, playWhenReady ->
        playWhenReady && playbackState != STATE_ENDED
    }.stateIn(
        scope,
        SharingStarted.Lazily,
        player.playWhenReady && player.playbackState != STATE_ENDED
    )
    val mediaMetadata = MutableStateFlow(player.currentMetadata)

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    val currentSong = mediaMetadata.flatMapLatest {
        database.songDao().getSong((it?.id ?: 0).toString())
    }

    //
//    val currentFormat = mediaMetadata.flatMapLatest { mediaMetadata ->
//        database.format(mediaMetadata?.id)
//    }
    val queueTitle = MutableStateFlow<String?>(null)
    val queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
    val currentMediaItemIndex = MutableStateFlow(-1)
    val currentWindowIndex = MutableStateFlow(-1)

    val shuffleModeEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(REPEAT_MODE_OFF)

    val canSkipPrevious = MutableStateFlow(true)
    val canSkipNext = MutableStateFlow(true)

    val error = MutableStateFlow<PlaybackException?>(null)

    init {
        player.addListener(this)

        playbackState.value = player.playbackState
        playWhenReady.value = player.playWhenReady
        mediaMetadata.value = player.currentMetadata
        queueTitle.value = service.queueTitle
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        currentMediaItemIndex.value = player.currentMediaItemIndex
        shuffleModeEnabled.value = player.shuffleModeEnabled
        repeatMode.value = player.repeatMode
    }


    fun playQueue(queue: Queue) {
        service.playQueue(queue)
    }

    fun playNext(item: MediaItem) = playNext(listOf(item))


    fun playNext(items: List<MediaItem>) {
        service.playNext(items)
    }

    fun addToQueue(item: MediaItem) = addToQueue(listOf(item))


    fun addToQueue(items: List<MediaItem>) {
        service.addToQueue(items)
    }

    fun seekToNext() {
        player.seekToNext()
        player.prepare()
        player.playWhenReady = true
    }

    fun seekToPrevious() {
        player.seekToPrevious()
        player.prepare()
        player.playWhenReady = true
    }

    fun switchPlayMode(mode: PlayMode): Int {
        return when (mode) {
            // OFF -> ALL -> SHUFFLE -> ONE
            PlayMode.REPEAT_MODE_OFF -> {
                player.repeatMode = REPEAT_MODE_ALL
                player.repeatMode
            }
            PlayMode.REPEAT_MODE_ALL -> {
                player.shuffleModeEnabled = true
                3
            }
            PlayMode.SHUFFLE_MODE_ALL -> {
                player.repeatMode = REPEAT_MODE_ONE
                player.repeatMode
            }
            PlayMode.REPEAT_MODE_ONE -> {
                player.repeatMode = REPEAT_MODE_OFF
                player.repeatMode
            }
        }

    }




    override fun onPlaybackStateChanged(state: Int) {
        playbackState.value = state
        error.value = player.playerError
    }

    override fun onPlayWhenReadyChanged(newPlayWhenReady: Boolean, reason: Int) {
        playWhenReady.value = newPlayWhenReady
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        mediaMetadata.value = mediaItem?.metadata
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        queueWindows.value = player.getQueueWindows()
        queueTitle.value = service.queueTitle
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onShuffleModeEnabledChanged(enabled: Boolean) {
        shuffleModeEnabled.value = enabled
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onRepeatModeChanged(mode: Int) {
        repeatMode.value = mode
        shuffleModeEnabled.value = false
        updateCanSkipPreviousAndNext()
    }


    override fun onPlayerErrorChanged(playbackError: PlaybackException?) {
        if (playbackError != null) {
            reportException(playbackError)
        }
        error.value = playbackError
    }

    private fun updateCanSkipPreviousAndNext() {
        if (!player.currentTimeline.isEmpty) {
            val window =
                player.currentTimeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
            canSkipPrevious.value = player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    || !window.isLive
                    || player.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            canSkipNext.value = window.isLive && window.isDynamic
                    || player.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        } else {
            canSkipPrevious.value = false
            canSkipNext.value = false
        }
    }

    fun dispose() {
        player.removeListener(this)
    }
}

enum class PlayMode(val mode: Int) {
    // 整个播放列表循环播放。
    REPEAT_MODE_ALL(2),

    // 播放列表按顺序播放，播放完最后一首后停止。
    REPEAT_MODE_OFF(0),

    // 单曲循环播放。
    REPEAT_MODE_ONE(1),

    // 播放列表随机播放。
    SHUFFLE_MODE_ALL(3);

    companion object {
        fun fromInt(value: Int): PlayMode? {
            return entries.find { it.mode == value }
        }
    }
}
