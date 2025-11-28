package com.ljyh.mei.playback

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.playback.queue.Queue
import com.ljyh.mei.playback.queue.QueueFactory
import com.ljyh.mei.playback.queue.QueueListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 播放队列管理器
 *
 */
class PlaybackQueueManager(
    private val context: Context,
    private val player: ExoPlayer,
    private val apiService: ApiService,
    private val scope: CoroutineScope
) : QueueListener {

    private val TAG = "PlaybackQueueManager"

    // 当前队列状态
    private val _queueState = MutableStateFlow<QueueState>(QueueState.Idle)
    val queueState: StateFlow<QueueState> = _queueState

    // 当前活动队列
    private var currentQueue: Queue = QueueFactory.createEmptyQueue()
    private var queueTitle: String? = null

    // 预加载相关
    private var preloadJob: Job? = null
    private var preloadWindowSize = 10
    private var preloadThreshold = 3

    // 错误恢复管理器
    private lateinit var errorRecoveryManager: ErrorRecoveryManager

    // 智能预加载策略管理器
    lateinit var preloadStrategyManager: PreloadStrategyManager

    // 只需要保存一份原始数据用于业务逻辑（如分页），播放逻辑交给 Player
    // 注意：这里的 originalMediaItems 仅用于记录，不应作为播放器当前状态的唯一真理
    private var originalMediaItems: List<MediaItem> = emptyList()

    private val _isShuffleModeEnabled = MutableStateFlow(false)
    val isShuffleModeEnabled: StateFlow<Boolean> = _isShuffleModeEnabled.asStateFlow()

    /**
     * 播放一个新队列
     */
    suspend fun playQueue(
        queue: Queue,
        startInShuffleMode: Boolean = false,
        playWhenReady: Boolean = true,
    ) {
        _queueState.value = QueueState.Loading(queue.title ?: "播放列表")

        try {
            preloadJob?.cancel()
            currentQueue = queue
            queueTitle = queue.title

            val status = queue.getInitialStatus()
            if (status.ids.isEmpty()) {
                _queueState.value = QueueState.Empty
                return
            }

            val mediaItems = loadSongDetails(status.ids)
            if (mediaItems.isEmpty()) {
                _queueState.value = QueueState.Error("无法加载歌曲")
                return
            }

            originalMediaItems = mediaItems

            // 更新 UI 状态
            _isShuffleModeEnabled.value = startInShuffleMode

            // --- 核心修改开始 ---

            // 1. 设置播放器随机模式 (不会触发重置，只是改变下一首的计算逻辑)
            player.shuffleModeEnabled = startInShuffleMode

            // 2. 一次性设置所有 MediaItems (原始顺序)
            val startIndex = status.mediaItemIndex
            val startPosition = status.position.toLong()

            player.setMediaItems(mediaItems, startIndex, startPosition)

            // --- 核心修改结束 ---

            player.prepare()
            player.playWhenReady = playWhenReady

            _queueState.value = QueueState.Playing(queue.title ?: "播放列表", player.mediaItemCount)
            startPreloadMonitoring()
            preloadStrategyManager.startMonitoring()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to play queue", e)
            _queueState.value = QueueState.Error("播放失败: ${e.message}")
        }
    }

    /**
     * 添加到队列末尾
     */
    fun addToQueue(items: List<MediaItem>) {
        // ExoPlayer 会自动处理：如果是随机模式，这些新歌会被随机插入到后续的随机序列中
        // 如果是顺序模式，会加到末尾
        player.addMediaItems(items)

        // 更新本地记录
        originalMediaItems = originalMediaItems + items

        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
    }

    /**
     * 在下一首播放 (插队)
     */
    fun playNext(items: List<MediaItem>) {
        val insertIndex = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
        player.addMediaItems(insertIndex, items)

        // 更新本地记录 (注意：这里简化了处理，如果非常严格的一致性要求，建议监听 Player 的 Timeline 变化)
        val mutableList = originalMediaItems.toMutableList()
        if (insertIndex <= mutableList.size) {
            mutableList.addAll(insertIndex, items)
            originalMediaItems = mutableList
        }

        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
    }

    /**
     * 切换随机播放模式
     *
     * 核心修复：直接设置 player.shuffleModeEnabled，不重新 setMediaItems
     */
    fun setShuffleModeEnabled(enabled: Boolean) {
        if (_isShuffleModeEnabled.value == enabled) return

        _isShuffleModeEnabled.value = enabled

        // ExoPlayer 原生支持无缝切换
        // enabled = true: 保持当前歌曲继续播放，后续歌曲顺序打乱
        // enabled = false: 保持当前歌曲继续播放，后续歌曲恢复列表原始顺序
        player.shuffleModeEnabled = enabled
    }

    // --- 移除了 updatePlayerQueue 方法，因为不再需要手动重建列表 ---

    /**
     * 开始预加载监控
     */
    private fun startPreloadMonitoring() {
        preloadJob?.cancel()
        preloadJob = scope.launch {
            while (true) {
                delay(1000)

                if (!player.isPlaying) continue

                val currentIndex = player.currentMediaItemIndex
                val totalItems = player.mediaItemCount
                val queueSize = currentQueue.loadedCount

                // ... 保持原有逻辑 ...
                val advice = preloadStrategyManager.getPreloadAdvice(
                    currentIndex,
                    totalItems,
                    queueSize
                )

                if (advice.shouldPreload && currentQueue.hasNextPage() && queueSize < currentQueue.totalCount) {
                    loadNextPage()
                }

                preloadAroundPosition(currentIndex)

                if (player.isPlaying) {
                    preloadStrategyManager.recordPlay()
                }
            }
        }
    }

    /**
     * 加载下一页歌曲
     */
    private suspend fun loadNextPage() {
        try {
            val result = currentQueue.nextPage()
            when (result) {
                is Queue.Result.Success -> {
                    if (result.data.isNotEmpty()) {
                        val mediaItems = loadSongDetails(result.data)
                        if (mediaItems.isNotEmpty()) {
                            // 直接添加到 Player，Player 会根据当前的 Shuffle 模式自动处理顺序
                            player.addMediaItems(mediaItems)
                            originalMediaItems = originalMediaItems + mediaItems
                            _queueState.value = QueueState.ItemsAdded(mediaItems.size)
                        }
                    }
                    errorRecoveryManager.resetRetryCount()
                }
                is Queue.Result.Error -> {
                    // handleLoadError 需要你自己实现或保留原有逻辑，这里暂未提供 ErrorRecoveryManager 代码
                    Log.e(TAG, "Load page error: ${result.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Load page exception", e)
        }
    }

    // ... 其余辅助方法保持不变 (preloadAroundPosition, loadSongDetails, clearQueue, release) ...

    private suspend fun preloadAroundPosition(position: Int) {
        if (currentQueue is com.ljyh.mei.playback.queue.PlaylistQueue) {
            val playlistQueue = currentQueue as com.ljyh.mei.playback.queue.PlaylistQueue
            playlistQueue.preloadAround(position, preloadWindowSize)
        }
    }

    private suspend fun loadSongDetails(ids: List<String>): List<MediaItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSongDetail(
                    com.ljyh.mei.data.model.api.GetSongDetails(c = ids.joinToString(","))
                )
                response.songs.map { it.toMediaItem() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load song details", e)
                emptyList()
            }
        }
    }

    fun clearQueue() {
        preloadJob?.cancel()
        preloadStrategyManager.stopMonitoring()
        currentQueue.clear()
        queueTitle = null
        _queueState.value = QueueState.Idle
        // 清理 player
        player.clearMediaItems()
        originalMediaItems = emptyList()
    }

    fun release() {
        preloadJob?.cancel()
        preloadStrategyManager.stopMonitoring()
        currentQueue.release()
    }

    // ... Listener 实现 ...
    override fun onQueueStateChanged(state: Queue.QueueState) {}
    override fun onQueueItemsAdded(items: List<String>, position: Int) {}
    override fun onQueueError(error: String) { _queueState.value = QueueState.Error(error) }
    override fun onQueueCompleted() { _queueState.value = QueueState.Completed }

    sealed class QueueState {
        object Idle : QueueState()
        data class Loading(val queueName: String) : QueueState()
        data class Playing(val queueName: String, val itemCount: Int) : QueueState()
        data class ItemsAdded(val count: Int) : QueueState()
        data class Error(val message: String) : QueueState()
        object Completed : QueueState()
        object Empty : QueueState()
    }
}