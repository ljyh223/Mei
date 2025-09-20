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

    /**
     * 播放指定队列
     */
    suspend fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
        startPosition: Int = 0
    ) {
        _queueState.value = QueueState.Loading(queue.title ?: "播放列表")

        try {
            // 停止之前的预加载
            preloadJob?.cancel()

            // 设置当前队列
            currentQueue = queue
            queueTitle = queue.title

            // 获取初始状态
            val status = queue.getInitialStatus()
            if (status.ids.isEmpty()) {
                _queueState.value = QueueState.Empty
                return
            }

            // 加载第一批歌曲
            val firstBatch = loadSongDetails(status.ids)
            if (firstBatch.isEmpty()) {
                _queueState.value = QueueState.Error("无法加载歌曲")
                return
            }

            // 设置播放器
            val actualStartPosition = if (startPosition in firstBatch.indices) startPosition else 0
            player.setMediaItems(firstBatch, actualStartPosition, status.position.toLong())
            player.prepare()
            player.playWhenReady = playWhenReady

            _queueState.value = QueueState.Playing(queue.title ?: "播放列表", firstBatch.size)

            // 开始预加载监听
            startPreloadMonitoring()
            
            // 开始智能预加载策略监控
            preloadStrategyManager.startMonitoring()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to play queue", e)
            _queueState.value = QueueState.Error("播放失败: ${e.message}")
        }
    }

    /**
     * 添加到队列末尾
     */
    suspend fun addToQueue(items: List<MediaItem>) {
        player.addMediaItems(items)
        if (player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            player.prepare()
        }
    }

    /**
     * 在下一首播放
     */
    suspend fun playNext(items: List<MediaItem>) {
        val insertIndex = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
        player.addMediaItems(insertIndex, items)
        if (player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            player.prepare()
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