package com.ljyh.mei.playback.queue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 列表队列实现
 * 用于处理静态的歌曲ID列表
 */
class ListQueue(
    override val id: String,
    override val title: String? = null,
    private val items: List<String>, // 改为 private val，外部不应修改
    val startIndex: Int = 0,
    val position: Int = 0,
) : Queue {

    override val totalCount: Int = items.size
    override val loadedCount: Int = items.size
    override val preloadItem: String? = null

    private val _state = MutableStateFlow<Queue.QueueState>(Queue.QueueState.Idle)
    override val state: StateFlow<Queue.QueueState> = _state

    // 不再需要 originalItems，因为 items 本身就是原始列表

    init {
        _state.value = if (items.isNotEmpty()) {
            Queue.QueueState.Loaded(items, hasMore = false)
        } else {
            Queue.QueueState.Completed
        }
    }

    override suspend fun getInitialStatus(): Queue.Status {
        // 直接返回原始 items
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
    }

    override fun release() {
    }
}