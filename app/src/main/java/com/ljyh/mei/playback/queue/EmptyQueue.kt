package com.ljyh.mei.playback.queue

import androidx.media3.common.MediaItem
import com.ljyh.mei.data.model.MediaMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 空队列实现
 */
object EmptyQueue : Queue {
    override val id: String = "empty_queue"
    override val title: String? = null
    override val totalCount: Int = 0
    override val loadedCount: Int = 0
    override val preloadItem: String? = null

    private val _state = MutableStateFlow<Queue.QueueState>(Queue.QueueState.Completed)
    override val state: StateFlow<Queue.QueueState> = _state

    override suspend fun getInitialStatus(): Queue.Status {
        return Queue.Status(null, emptyList(), -1)
    }

    override fun hasNextPage(): Boolean = false

    override suspend fun nextPage(): Queue.Result<List<String>> {
        return Queue.Result.Success(emptyList())
    }

    override suspend fun reloadCurrentPage(): Queue.Result<List<String>> {
        return Queue.Result.Success(emptyList())
    }

    override suspend fun getItemAt(position: Int): Queue.Result<String> {
        return Queue.Result.Error("Empty queue has no items")
    }

    override fun clear() {
        // 空队列不需要清理操作
    }

    override fun release() {
        // 空队列不需要释放操作
    }
}