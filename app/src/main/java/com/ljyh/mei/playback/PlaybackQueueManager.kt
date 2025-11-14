package com.ljyh.mei.playback

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.api.GetSongDetails
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
 * 负责管理播放队列的加载、预加载、错误恢复等
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
    private var preloadWindowSize = 10 // 预加载窗口大小
    private var preloadThreshold = 3   // 距离末尾多少首开始预加载

    // 错误恢复管理器
    private lateinit var errorRecoveryManager: ErrorRecoveryManager

    // 智能预加载策略管理器
    lateinit var preloadStrategyManager: PreloadStrategyManager

    private var originalMediaItems: List<MediaItem> = emptyList()
    private var shuffledMediaItems: List<MediaItem> = emptyList()

    private val _isShuffleModeEnabled = MutableStateFlow(false)
    val isShuffleModeEnabled: StateFlow<Boolean> = _isShuffleModeEnabled.asStateFlow()


// PlaybackQueueManager.kt

    /**
     * 播放一个新队列
     * @param queue 要播放的队列
     * @param startInShuffleMode 是否以随机模式开始播放，默认为 false
     * @param playWhenReady 是否准备好后立即播放
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

            // 1. 无论如何，都先保存原始顺序的列表
            originalMediaItems = mediaItems

            // 2. 设置初始的播放模式状态
            _isShuffleModeEnabled.value = startInShuffleMode

            // 3. 调用我们已经写好的核心函数来更新播放器
            // 这个函数会根据 _isShuffleModeEnabled 的值来决定是使用原始列表还是生成并使用随机列表
            updatePlayerQueue(
                startMediaId = status.ids.getOrNull(status.mediaItemIndex),
                startPositionMs = status.position.toLong()
            )

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
        player.addMediaItems(items)
        if (player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            player.prepare()
        }
    }

    /**
     * 在下一首播放
     */
    fun playNext(items: List<MediaItem>) {
        val insertIndex = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
        player.addMediaItems(insertIndex, items)
        if (player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            player.prepare()
        }
    }
    /**
     * 切换随机播放模式
     * @param enabled 是否开启随机播放
     */
    fun setShuffleModeEnabled(enabled: Boolean) {
        if (_isShuffleModeEnabled.value == enabled) return

        _isShuffleModeEnabled.value = enabled

        // 获取当前播放状态，用于无缝切换
        val currentMediaId = player.currentMediaItem?.mediaId
        val currentPosition = player.currentPosition.coerceAtLeast(0)

        updatePlayerQueue(currentMediaId, currentPosition)
        player.prepare()
    }

    private fun updatePlayerQueue(startMediaId: String?, startPositionMs: Long) {
        val mediaItemsToSet: List<MediaItem>
        var newIndex = 0

        if (_isShuffleModeEnabled.value) {
            // 生成随机列表，并将当前歌曲放在第一位，其余随机
            val currentItem = originalMediaItems.find { it.mediaId == startMediaId }
            shuffledMediaItems = if (currentItem != null) {
                val otherItems = originalMediaItems.filter { it.mediaId != startMediaId }
                listOf(currentItem) + otherItems.shuffled()
            } else {
                originalMediaItems.shuffled()
            }
            mediaItemsToSet = shuffledMediaItems
            // 新的索引永远是0，因为我们把当前歌曲放到了第一位
            newIndex = mediaItemsToSet.indexOfFirst { it.mediaId == startMediaId }.coerceAtLeast(0)
        } else {
            // 使用原始顺序列表
            mediaItemsToSet = originalMediaItems
            newIndex = originalMediaItems.indexOfFirst { it.mediaId == startMediaId }.coerceAtLeast(0)
        }

        if (mediaItemsToSet.isNotEmpty()) {
            player.setMediaItems(mediaItemsToSet, newIndex, startPositionMs)
        }
    }

    /**
     * 开始预加载监控
     */
    private fun startPreloadMonitoring() {
        preloadJob?.cancel()
        preloadJob = scope.launch {
            while (true) {
                delay(1000) // 每秒检查一次

                if (!player.isPlaying) continue

                val currentIndex = player.currentMediaItemIndex
                val totalItems = player.mediaItemCount
                val queueSize = currentQueue.loadedCount

                // 使用智能预加载策略决定是否需要预加载
                val advice = preloadStrategyManager.getPreloadAdvice(
                    currentIndex,
                    totalItems,
                    queueSize
                )
                
                if (advice.shouldPreload && currentQueue.hasNextPage() && queueSize < currentQueue.totalCount) {
                    loadNextPage()
                }

                // 预加载当前播放位置附近的歌曲
                preloadAroundPosition(currentIndex)
                
                // 记录播放行为
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
                            player.addMediaItems(mediaItems)
                            _queueState.value = QueueState.ItemsAdded(mediaItems.size)
                        }
                    }
                    errorRecoveryManager.resetRetryCount() // 重置重试计数
                }
                is Queue.Result.Error -> {
                    handleLoadError(result.message, ::loadNextPage)
                }
            }
        } catch (e: Exception) {
            handleLoadError("加载下一页失败: ${e.message}", ::loadNextPage)
        }
    }

    /**
     * 预加载指定位置附近的歌曲
     */
    private suspend fun preloadAroundPosition(position: Int) {
        if (currentQueue is com.ljyh.mei.playback.queue.PlaylistQueue) {
            val playlistQueue = currentQueue as com.ljyh.mei.playback.queue.PlaylistQueue
            playlistQueue.preloadAround(position, preloadWindowSize)
        }
    }

    /**
     * 加载歌曲详情
     */
    private suspend fun loadSongDetails(ids: List<String>): List<MediaItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSongDetail(
                    GetSongDetails(c = ids.joinToString(","))
                )
                response.songs.map { it.toMediaItem() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load song details", e)
                emptyList()
            }
        }
    }

    /**
     * 处理加载错误
     */
    private suspend fun handleLoadError(message: String, retryAction: suspend () -> Unit) {
        Log.e(TAG, message)
        
        val success = errorRecoveryManager.handleNetworkError("加载歌曲", retryAction)
        if (!success) {
            _queueState.value = QueueState.Error("加载失败，请重试")
        }
    }

    /**
     * 清除队列
     */
    fun clearQueue() {
        preloadJob?.cancel()
        preloadStrategyManager.stopMonitoring()
        currentQueue.clear()
        queueTitle = null
        _queueState.value = QueueState.Idle
    }

    /**
     * 释放资源
     */
    fun release() {
        preloadJob?.cancel()
        preloadStrategyManager.stopMonitoring()
        currentQueue.release()
    }

    // QueueListener 实现
    override fun onQueueStateChanged(state: Queue.QueueState) {
        // 处理队列状态变化
    }

    override fun onQueueItemsAdded(items: List<String>, position: Int) {
        // 处理队列项目添加
    }

    override fun onQueueError(error: String) {
        _queueState.value = QueueState.Error(error)
    }

    override fun onQueueCompleted() {
        _queueState.value = QueueState.Completed
    }

    /**
     * 队列状态
     */
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