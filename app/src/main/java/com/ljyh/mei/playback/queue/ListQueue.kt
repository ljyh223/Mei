package com.ljyh.mei.playback.queue

import androidx.media3.common.MediaItem
import com.ljyh.mei.data.model.MediaMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 列表队列实现
 * 用于处理静态的歌曲ID列表
 */
class ListQueue(
    override val id: String,
    override val title: String? = null,
    private val items: List<String>,
    private val startIndex: Int = 0,
    private val position: Int = 0,
) : Queue {

    override val totalCount: Int = items.size
    override val loadedCount: Int = items.size
    override val preloadItem: String? = null

    private val _state = MutableStateFlow<Queue.QueueState>(Queue.QueueState.Idle)
    override val state: StateFlow<Queue.QueueState> = _state

    init {
        _state.value = if (items.isNotEmpty()) {
            Queue.QueueState.Loaded(items, hasMore = false)
        } else {
            Queue.QueueState.Completed
        }
    }

    override suspend fun getInitialStatus(): Queue.Status {
        return Queue.Status(title, items, startIndex, position)
    }

    override fun hasNextPage(): Boolean = false

    override suspend fun nextPage(): Queue.Result<List<String>> {
        return Queue.Result.Error("ListQueue does not support pagination")
    }

    override suspend fun reloadCurrentPage(): Queue.Result<List<String>> {
        return Queue.Result.Success(items)
    }

    override suspend fun getItemAt(position: Int): Queue.Result<String> {
        return if (position in items.indices) {
            Queue.Result.Success(items[position])
        } else {
            Queue.Result.Error("Position $position out of bounds")
        }
    }

    override fun clear() {
        // 列表队列不需要清理操作
    }

    override fun release() {
        // 列表队列不需要释放操作
    }
}