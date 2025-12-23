package com.ljyh.mei.playback

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ljyh.mei.data.model.PLACEHOLDER_URI
import com.ljyh.mei.data.model.createPlaceholder
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.playback.queue.Queue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class PlaybackQueueManager(
    private val player: ExoPlayer,
    private val apiService: ApiService,
    private val scope: CoroutineScope
) : Player.Listener {

    private val TAG = "QueueManager"
    private val _queueState = MutableStateFlow<QueueState>(QueueState.Idle)
    private val loadingIds = ConcurrentHashMap.newKeySet<String>()

    private val _isShuffleModeEnabled = MutableStateFlow(false)
    var isFmMode = false

    init {
        player.addListener(this)
    }

    suspend fun startFmMode(seedSongId: String?) {
        isFmMode = true
        player.clearMediaItems()
        // 获取第一批推荐
        fetchAndAppendFmRecommendations(seedSongId)
    }

    suspend fun fetchAndAppendFmRecommendations(seedId: String? = null) {
        try {
            // 1. 获取当前正在播放的 ID 作为种子，或者使用传入的 seed
            val currentId = if (player.mediaItemCount > 0) {
                player.currentMediaItem?.mediaId
            } else seedId

            // 2. 调用 API 获取推荐 (你需要实现这个 API)
            // val newSongs = apiService.getRecommendations(currentId)

            // 3. 转换为 MediaItem
            // val items = newSongs.map { it.toMediaItem() }

            // 4. 添加到播放列表末尾
            // withContext(Dispatchers.Main) {
            //     player.addMediaItems(items)
            //     if (player.playbackState == Player.STATE_IDLE) {
            //         player.prepare()
            //         player.play()
            //     }
            // }
        } catch (e: Exception) {
            Log.e("QueueManager", "Failed to fetch FM songs", e)
        }
    }

    @OptIn(UnstableApi::class)
    fun playQueue(
        queue: Queue,
        startInShuffleMode: Boolean = false,
        playWhenReady: Boolean = true,
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                _queueState.value = QueueState.Loading(queue.title ?: "加载中")

                val status = queue.getInitialStatus()
                val allIds = status.ids

                if (allIds.isEmpty()) {
                    _queueState.value = QueueState.Empty
                    return@launch
                }

                val mediaItems = allIds.map { item -> item.second ?: createPlaceholder(item.first) }

                // 2. 停止并重置
                player.stop()
                player.clearMediaItems()

                // 强制先关闭随机模式 必须先关掉，才能保证 setMediaItems 里的 index 是线性的、准确的
                player.shuffleModeEnabled = false
                _isShuffleModeEnabled.value = false

                // 设置列表并直接跳转 此时 shuffle 是 false，所以 status.mediaItemIndex 绝对对应 list 里的第 N 个
                player.setMediaItems(
                    mediaItems,
                    status.mediaItemIndex,
                    status.position.toLong()
                )

                player.repeatMode = Player.REPEAT_MODE_ALL
                player.prepare()

                // 如果需要，再开启随机 此时当前播放的歌曲已经定下来了，ExoPlayer 只会打乱"后面"的歌
                if (startInShuffleMode) {
                    player.shuffleModeEnabled = true
                    _isShuffleModeEnabled.value = true
                }

                player.playWhenReady = playWhenReady

                _queueState.value = QueueState.Playing(queue.title ?: "播放列表", allIds.size)

                // 6. 触发懒加载
                // 因为当前歌曲已经是 RealItem 了，这个方法会自动跳过当前歌曲，去加载下一首
                checkAndLoadMetadata()

            } catch (e: Exception) {
                Log.e(TAG, "playQueue Error", e)
                _queueState.value = QueueState.Error(e.message ?: "播放错误")
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_IDLE && player.playerError != null) {
            _queueState.value = QueueState.Error(player.playerError?.message ?: "播放错误")
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // 只有当自动切歌、手动切歌、列表变化时才触发
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK ||
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
        ) {
            checkAndLoadMetadata()
        }
    }

    private fun checkAndLoadMetadata(windowSize: Int = 3) {
        scope.launch(Dispatchers.Main) {
            // 基础状态检查 确保线程存活且列表不为空
            if (!player.applicationLooper.thread.isAlive || player.mediaItemCount == 0) return@launch

            val timeline = player.currentTimeline
            if (timeline.isEmpty) return@launch

            val currentIndex = player.currentMediaItemIndex
            val indicesToCheck = mutableSetOf<Int>()
            indicesToCheck.add(currentIndex)

            // 计算窗口索引 必须在获取到 INDEX_UNSET 时立即 break，不能把 -1 传给下一次调用

            // 向后找 (Next)
            var next = currentIndex
            for (i in 0 until windowSize) {
                // 如果当前已经是最后一个，next 会变成 INDEX_UNSET (-1)
                if (next == C.INDEX_UNSET) break

                next = timeline.getNextWindowIndex(next, player.repeatMode, player.shuffleModeEnabled)
                if (next != C.INDEX_UNSET) {
                    indicesToCheck.add(next)
                }
            }

            // 向前找 (Previous)
            var prev = currentIndex
            for (i in 0 until windowSize) {
                // 如果当前已经是第一个，prev 会变成 INDEX_UNSET (-1)
                if (prev == C.INDEX_UNSET) break

                prev = timeline.getPreviousWindowIndex(prev, player.repeatMode, player.shuffleModeEnabled)
                if (prev != C.INDEX_UNSET) {
                    indicesToCheck.add(prev)
                }
            }

            // 3. 筛选需要加载的 ID
            val itemsNeedLoading = indicesToCheck.mapNotNull { index ->
                if (index < 0 || index >= player.mediaItemCount) return@mapNotNull null

                val item = player.getMediaItemAt(index)
                val isPlaceholder = item.localConfiguration?.uri.toString() == PLACEHOLDER_URI
                val id = item.mediaId

                // 只有是占位符且没在加载中才请求
                if (isPlaceholder && !loadingIds.contains(id)) {
                    id
                } else null
            }.distinct()

            if (itemsNeedLoading.isEmpty()) return@launch

            loadingIds.addAll(itemsNeedLoading)

            // IO 线程加载数据
            val loadedItems = withContext(Dispatchers.IO) {
                try {
                    loadSongDetails(itemsNeedLoading)
                } catch (e: Exception) {
                    Log.e(TAG, "Fetch Metadata Failed", e)
                    // 失败移除标记
                    loadingIds.removeAll(itemsNeedLoading.toSet())
                    emptyList()
                }
            }

            // 回到主线程更新
            if (loadedItems.isNotEmpty()) {
                loadingIds.removeAll(loadedItems.map { it.mediaId }.toSet())

                // 再次检查状态，防止异步期间 Player 被释放
                if (player.mediaItemCount == 0) return@launch
                // 为了性能，我们只遍历我们关心的那几个位置（indicesToCheck），但是必须做 Double Check
                indicesToCheck.forEach { index ->
                    // 索引防越界
                    if (index >= 0 && index < player.mediaItemCount) {
                        val currentItem = player.getMediaItemAt(index)

                        // 查找这个位置的 ID 是否有对应的新数据
                        val newItem = loadedItems.find { it.mediaId == currentItem.mediaId }

                        // 只有当 ID 匹配，且当前确实是占位符时才替换
                        // 这样即使 index 错位了（比如变成了别的歌），因为 ID 不匹配，也不会错误替换
                        if (newItem != null) {
                            val isStillPlaceholder = currentItem.localConfiguration?.uri.toString() == PLACEHOLDER_URI

                            // 只有当它是占位符，或者我们要强制刷新时才替换
                            if (isStillPlaceholder) {
                                player.replaceMediaItem(index, newItem)
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * 添加到队列末尾
     */
    fun addToQueue(items: List<MediaItem>) {
        scope.launch(Dispatchers.Main) {
            player.addMediaItems(items)

            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
        }
    }

    /**
     * 在下一首播放 (插队)
     */
    fun playNext(items: List<MediaItem>) {
        scope.launch(Dispatchers.Main) {
            val insertIndex =
                if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
            player.addMediaItems(insertIndex, items)

            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
        }
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.Main) {
            if (_isShuffleModeEnabled.value == enabled) return@launch
            _isShuffleModeEnabled.value = enabled
            player.shuffleModeEnabled = enabled
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

    fun release() {
        player.removeListener(this)
        loadingIds.clear()
        // 不要 cancel scope，因为它是由外部 Service 传进来的
    }

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