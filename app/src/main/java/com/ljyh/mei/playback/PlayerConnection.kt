package com.ljyh.mei.playback

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
import com.ljyh.mei.constants.LoopPlaybackKey
import com.ljyh.mei.constants.PreviousPlaybackKey
import com.ljyh.mei.data.model.metadata
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.extensions.currentMetadata
import com.ljyh.mei.extensions.getCurrentQueueIndex
import com.ljyh.mei.extensions.getQueueWindows
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.get
import com.ljyh.mei.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber


@UnstableApi
class PlayerConnection(
    context: Context,
    binder: MusicService.MusicBinder,
    val database: AppDatabase,
    scope: CoroutineScope,
) : Player.Listener {

    init {
        Timber.tag("PlayerConnection").d("PlayerConnection initialized")
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
    val queueTitle = MutableStateFlow<String?>(null)
    val queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
    val currentMediaItemIndex = MutableStateFlow(-1)
    val currentWindowIndex = MutableStateFlow(-1)

    // 这里 repeatMode 我们存储对应的 PlayMode 枚举值(int)，用于 UI 显示
    val repeatMode = MutableStateFlow(PlayMode.SHUFFLE_MODE_ALL.mode)
    // 同时也暴露原始的 shuffle 状态
    val shuffleModeEnabled = MutableStateFlow(false)

    val canSkipPrevious = MutableStateFlow(true)
    val canSkipNext = MutableStateFlow(true)

    val error = MutableStateFlow<PlaybackException?>(null)

    init {
        player.addListener(this)
        updateAllStates()
    }

    private fun updateAllStates() {
        playbackState.value = player.playbackState
        playWhenReady.value = player.playWhenReady
        mediaMetadata.value = player.currentMetadata
        queueTitle.value = service.queueTitle
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        currentMediaItemIndex.value = player.currentMediaItemIndex

        updatePlayModeState() // 核心：更新播放模式
        updateCanSkipPreviousAndNext()
    }

    fun isPlaying(id: String): Boolean {
        return mediaMetadata.value?.id.toString() == id &&
                player.playbackState == Player.STATE_READY &&
                player.playWhenReady
    }

    fun playQueue(queue: ListQueue) {
        // 判断当前 UI 上的模式是否是随机模式
        val startInShuffle = repeatMode.value == PlayMode.SHUFFLE_MODE_ALL.mode
        // 调用新的 playQueue 方法，传入随机意图
        service.scope.launch {
            service.queueManager.playQueue(queue, startInShuffleMode = startInShuffle)
        }
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
        // 原生 Shuffle 模式下，seekToNext 会自动跳到随机的下一首，不需要额外逻辑
        if (player.hasNextMediaItem()) {
            player.seekToNext()
            // 通常不需要重新 prepare，除非出错
            if (!player.playWhenReady) player.playWhenReady = true
        }
    }

    fun seekToPrevious() {
        if(service.dataStore[PreviousPlaybackKey] ?: true){
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else {
                player.seekTo(0)
            }
        }else{
            player.seekToPrevious()
        }

        if (!player.playWhenReady) player.playWhenReady = true
    }

    /**
     * 切换播放模式
     * 逻辑：列表循环 (REPEAT_ALL) -> 随机播放 (SHUFFLE) -> 单曲循环 (REPEAT_ONE) -> ...
     */
    fun switchPlayMode() {
        // 基于当前的 UI 状态决定下一步
        // 注意：这里不需要返回 Int，因为操作完 Player 后，ExoPlayer 会回调 Listener，
        // Listener 会调用 updatePlayModeState() 更新 repeatMode Flow，
        // UI 观察 Flow 自动变化。这样才可靠。

        val currentUiMode = PlayMode.fromInt(repeatMode.value) ?: PlayMode.REPEAT_MODE_ALL

        // 判断用户是否开启了“列表循环”设置
        // 如果开启：列表模式 = REPEAT_MODE_ALL
        // 如果关闭：列表模式 = REPEAT_MODE_OFF (播完停止)
        val shouldLoopAll = service.dataStore[LoopPlaybackKey] == true

        when (currentUiMode) {
            PlayMode.REPEAT_MODE_ALL -> {
                // 当前是列表 -> 切换到 随机
                // 行为：开启随机，且通常随机模式下也是列表循环的
                service.setShuffleModeEnabled(true)
                player.repeatMode = if (shouldLoopAll) REPEAT_MODE_ALL else REPEAT_MODE_OFF
            }
            PlayMode.SHUFFLE_MODE_ALL -> {
                // 当前是随机 -> 切换到 单曲
                // 行为：关闭随机，开启单曲循环
                service.setShuffleModeEnabled(false)
                player.repeatMode = REPEAT_MODE_ONE
            }
            PlayMode.REPEAT_MODE_ONE -> {
                // 当前是单曲 -> 切换到 列表
                // 行为：关闭随机，开启列表循环(或不循环)
                service.setShuffleModeEnabled(false)
                player.repeatMode = if (shouldLoopAll) REPEAT_MODE_ALL else REPEAT_MODE_OFF
            }
        }
    }


    // --- Listeners ---

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

    // 监听 Shuffle 变化
    override fun onShuffleModeEnabledChanged(enabled: Boolean) {
        updatePlayModeState() // 统一更新状态
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    // 监听 Repeat 变化
    override fun onRepeatModeChanged(mode: Int) {
        updatePlayModeState() // 统一更新状态
        updateCanSkipPreviousAndNext()
    }

    override fun onPlayerErrorChanged(playbackError: PlaybackException?) {
        if (playbackError != null) {
            reportException(playbackError)
        }
        error.value = playbackError
    }

    /**
     * 统一计算当前的 PlayMode 并更新 Flow
     * 解决了之前两个 Listener 互相冲突或者覆盖状态的问题
     */
    private fun updatePlayModeState() {
        val isShuffle = player.shuffleModeEnabled
        val pRepeat = player.repeatMode

        shuffleModeEnabled.value = isShuffle

        // UI 状态映射逻辑：
        if (isShuffle) {
            // 只要开了随机，UI 就显示随机图标
            repeatMode.value = PlayMode.SHUFFLE_MODE_ALL.mode
        } else {
            // 没开随机，检查循环模式
            if (pRepeat == REPEAT_MODE_ONE) {
                repeatMode.value = PlayMode.REPEAT_MODE_ONE.mode
            } else {
                // 无论是 OFF 还是 ALL，UI 通常都显示为“列表循环”图标
                // (除非你有专门的图标表示“播完停止”)
                repeatMode.value = PlayMode.REPEAT_MODE_ALL.mode
            }
        }
    }
    private fun updateCanSkipPreviousAndNext() {
        if (!player.currentTimeline.isEmpty) {
            canSkipPrevious.value = player.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    || player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            canSkipNext.value = player.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        } else {
            canSkipPrevious.value = false
            canSkipNext.value = false
        }
    }

    fun dispose() {
        player.removeListener(this)
    }
}

// 保持你的 Enum 不变
enum class PlayMode(val mode: Int) {
    REPEAT_MODE_ALL(2),
    REPEAT_MODE_ONE(1),
    SHUFFLE_MODE_ALL(3);

    companion object {
        fun fromInt(value: Int): PlayMode? {
            return entries.find { it.mode == value }
        }
    }
}