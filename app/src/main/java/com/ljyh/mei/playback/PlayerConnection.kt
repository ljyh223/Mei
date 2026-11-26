package com.ljyh.mei.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.data.model.domain.createPlaceholder
import com.ljyh.mei.data.model.domain.metadata
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.extensions.currentMetadata
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.utils.log.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val queueState = service.queueManager.state
    val isFMMode = service.queueManager.fmMode

    // 这里 repeatMode 我们存储对应的 PlayMode 枚举值(int)，用于 UI 显示
    val repeatMode = MutableStateFlow(PlayMode.REPEAT_MODE_ALL.mode)
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

        updatePlayModeState() // 核心：更新播放模式
        updateCanSkipPreviousAndNext()
    }

    fun isPlaying(id: String): Boolean {
        return mediaMetadata.value?.id.toString() == id && isPlaying.value
    }
    fun onTrackClicked(trackId: String, buildQueue: () -> ListQueue?, fallbackSource: QueueSource? = null) {
        // Rebuild the list selected by the user. The existing queue may contain the same song
        // from another source or have a different shuffle order.
        val selectedQueue = buildQueue()?.startingAt(trackId)
        if (selectedQueue != null) {
            playQueue(selectedQueue, startInShuffleMode = false)
            return
        }
        // A partial or loading list can still play the exact tapped song. It must not fall back
        // to a different queue whose order and source do not match this tap.
        playQueue(
            ListQueue(
                id = "track_$trackId",
                items = listOf(trackId to createPlaceholder(trackId)),
                source = fallbackSource ?: QueueSource(QueueSource.Type.OTHER, title = "单曲播放"),
            ),
            startInShuffleMode = false,
        )
    }

    fun fmStart(firstSongId: String? = null) = service.queueManager.startFmModeById(firstSongId)

    fun playQueue(queue: ListQueue, startInShuffleMode: Boolean = player.shuffleModeEnabled) {
        service.queueManager.playQueue(queue, startInShuffleMode = startInShuffleMode)
    }
    fun playNext(item: MediaItem, source: QueueSource? = null) = playNext(listOf(item), source)

    fun playNext(items: List<MediaItem>, source: QueueSource? = null) {
        service.playNext(items, source)
    }

    fun addToQueue(item: MediaItem, source: QueueSource? = null) = addToQueue(listOf(item), source)

    fun addToQueue(items: List<MediaItem>, source: QueueSource? = null) {
        service.addToQueue(items, source)
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
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0)
        }

        if (!player.playWhenReady) player.playWhenReady = true
    }

    /**
     * 切换播放模式
     * 逻辑：列表循环 (REPEAT_ALL) -> 随机播放 (SHUFFLE) -> 单曲循环 (REPEAT_ONE) -> ...
     */
    fun switchPlayMode() = service.queueManager.switchPlayMode()

    fun seekToQueueEntry(entryId: String) = service.queueManager.seekToEntry(entryId)
    fun removeQueueEntry(entryId: String): Boolean = service.queueManager.removeEntry(entryId)
    fun moveQueueEntry(entryId: String, targetEntryId: String): Boolean =
        service.queueManager.moveEntry(entryId, targetEntryId)
    fun loadQueueMetadata(entryIds: List<String>) = service.queueManager.loadMetadataForEntries(entryIds)

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
        updateCanSkipPreviousAndNext()
    }

    // 监听 Shuffle 变化
    override fun onShuffleModeEnabledChanged(enabled: Boolean) {
        updatePlayModeState() // 统一更新状态
        updateCanSkipPreviousAndNext()
    }

    // 监听 Repeat 变化
    override fun onRepeatModeChanged(mode: Int) {
        updatePlayModeState() // 统一更新状态
        updateCanSkipPreviousAndNext()
    }

    override fun onEvents(player: Player, events: Player.Events) {
        // Metadata replacement need not transition to a different song.
        if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_METADATA_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_POSITION_DISCONTINUITY)) {
            mediaMetadata.value = player.currentMetadata
        }
        // Commands can change after individual timeline/repeat callbacks have run.
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
