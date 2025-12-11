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
import com.ljyh.mei.data.model.metadata
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.extensions.currentMetadata
import com.ljyh.mei.extensions.getCurrentQueueIndex
import com.ljyh.mei.extensions.getQueueWindows
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@UnstableApi
class PlayerConnection(
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
    val queueTitle = MutableStateFlow<String?>(null)
    val queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
    val currentMediaItemIndex = MutableStateFlow(-1)
    val currentWindowIndex = MutableStateFlow(-1)

    // 这里 repeatMode 我们存储对应的 PlayMode 枚举值(int)，用于 UI 显示
    val repeatMode = MutableStateFlow(PlayMode.REPEAT_MODE_ALL.mode)
    // 同时也暴露原始的 shuffle 状态
    val shuffleModeEnabled = MutableStateFlow(false)

    val canSkipPrevious = MutableStateFlow(true)
    val canSkipNext = MutableStateFlow(true)

    val error = MutableStateFlow<PlaybackException?>(null)

    init {
        player.addListener(this)

        // 初始化状态
        playbackState.value = player.playbackState
        playWhenReady.value = player.playWhenReady
        mediaMetadata.value = player.currentMetadata
        queueTitle.value = service.queueTitle
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        currentMediaItemIndex.value = player.currentMediaItemIndex

        // 初始化播放模式状态
        updatePlayModeState()
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
        player.seekToPrevious()
        if (!player.playWhenReady) player.playWhenReady = true
    }

    /**
     * 切换播放模式
     * 逻辑：列表循环 (REPEAT_ALL) -> 随机播放 (SHUFFLE) -> 单曲循环 (REPEAT_ONE) -> ...
     */
    fun switchPlayMode(currentModeInt: Int): Int {
        val currentMode = PlayMode.fromInt(currentModeInt) ?: PlayMode.REPEAT_MODE_ALL

        when (currentMode) {
            PlayMode.REPEAT_MODE_ALL -> {
                // 当前是【列表循环】，切换到 -> 【随机播放】
                // 逻辑：开启随机开关，同时保持 REPEAT_MODE_ALL，这样随机播完一轮会自动重新洗牌继续播
                service.player.repeatMode = REPEAT_MODE_ALL
                service.setShuffleModeEnabled(true)
            }
            PlayMode.SHUFFLE_MODE_ALL -> {
                // 当前是【随机播放】，切换到 -> 【单曲循环】
                // 逻辑：关闭随机，设置单曲循环
                service.setShuffleModeEnabled(false)
                service.player.repeatMode = REPEAT_MODE_ONE
            }
            PlayMode.REPEAT_MODE_ONE -> {
                // 当前是【单曲循环】，切换到 -> 【列表循环】
                // 逻辑：关闭随机，设置列表循环
                service.setShuffleModeEnabled(false)
                service.player.repeatMode = REPEAT_MODE_ALL
            }
        }

        // 返回预测的下一个状态用于 UI 快速响应
        return when (currentMode) {
            PlayMode.REPEAT_MODE_ALL -> PlayMode.SHUFFLE_MODE_ALL.mode
            PlayMode.SHUFFLE_MODE_ALL -> PlayMode.REPEAT_MODE_ONE.mode
            PlayMode.REPEAT_MODE_ONE -> PlayMode.REPEAT_MODE_ALL.mode
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

        // 优先级逻辑：如果是随机模式，UI显示随机；否则看 Repeat 模式
        if (isShuffle) {
            repeatMode.value = PlayMode.SHUFFLE_MODE_ALL.mode
        } else {
            if (pRepeat == REPEAT_MODE_ONE) {
                repeatMode.value = PlayMode.REPEAT_MODE_ONE.mode
            } else {
                // REPEAT_MODE_OFF 也视为 REPEAT_MODE_ALL (通常音乐APP逻辑)
                repeatMode.value = PlayMode.REPEAT_MODE_ALL.mode
            }
        }
    }

    private fun updateCanSkipPreviousAndNext() {
        if (!player.currentTimeline.isEmpty) {
            // 在原生 Shuffle 模式下，ExoPlayer 会正确处理 getWindow
            // 并且 isCommandAvailable 会正确反映是否可以随机跳到下一首
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