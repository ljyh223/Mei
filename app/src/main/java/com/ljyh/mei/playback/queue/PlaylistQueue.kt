package com.ljyh.mei.playback.queue

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 播放列表队列实现
 * 支持分页加载、错误恢复和智能预加载
 */
class PlaylistQueue(
    override val id: String,
    override val title: String? = null,
    private val trackIds: List<Long>,
    private val batchSize: Int = 20
) : Queue {

    override val totalCount: Int = trackIds.size
    override val loadedCount: Int get() = loadedItems.size
    override val preloadItem: String? = null

    private val _state = MutableStateFlow<Queue.QueueState>(Queue.QueueState.Idle)
    override val state: StateFlow<Queue.QueueState> = _state

    private var currentPage = 0
    private var loadedItems = mutableListOf<String>()
    private var hasMore = true

    init {
        if (trackIds.isEmpty()) {
            _state.value = Queue.QueueState.Completed
        } else {
            _state.value = Queue.QueueState.Idle
        }
    }

    override suspend fun getInitialStatus(): Queue.Status {
        // 加载第一页数据
        val firstPageResult = loadPage(0)
        return when (firstPageResult) {
            is Queue.Result.Success -> {
                loadedItems.addAll(firstPageResult.data)
                Queue.Status(title, loadedItems, 0)
            }
            is Queue.Result.Error -> {
                _state.value = Queue.QueueState.Error(
                    "Failed to load initial page: ${firstPageResult.message}",

                )
                Queue.Status(title, emptyList(), -1)
            }
        }
    }

    override fun hasNextPage(): Boolean = hasMore

    override suspend fun nextPage(): Queue.Result<List<String>> {
        if (!hasMore) {
            return Queue.Result.Success(emptyList())
        }

        val nextPage = currentPage + 1
        val result = loadPage(nextPage)

        if (result is Queue.Result.Success) {
            currentPage = nextPage
            loadedItems.addAll(result.data)
            hasMore = loadedItems.size < totalCount
            _state.value = Queue.QueueState.Loaded(loadedItems, hasMore)
        }

        return result
    }

    override suspend fun reloadCurrentPage(): Queue.Result<List<String>> {
        val result = loadPage(currentPage)
        if (result is Queue.Result.Success) {
            // 替换当前页的数据
            val startIndex = currentPage * batchSize
            val endIndex = minOf(startIndex + batchSize, totalCount)
            
            // 清除当前页的数据
            if (startIndex < loadedItems.size) {
                val itemsToRemove = minOf(batchSize, loadedItems.size - startIndex)
                repeat(itemsToRemove) {
                    if (startIndex < loadedItems.size) {
                        loadedItems.removeAt(startIndex)
                    }
                }
            }
            
            // 添加重新加载的数据
            loadedItems.addAll(startIndex, result.data)
            _state.value = Queue.QueueState.Loaded(loadedItems, hasMore)
        }
        return result
    }

    override suspend fun getItemAt(position: Int): Queue.Result<String> {
        return if (position in loadedItems.indices) {
            Queue.Result.Success(loadedItems[position])
        } else if (position < totalCount) {
            // 需要加载更多数据
            val page = position / batchSize
            val result = loadPage(page)
            if (result is Queue.Result.Success) {
                // 更新加载的项目
                val startIndex = page * batchSize
                if (startIndex + result.data.size > loadedItems.size) {
                    loadedItems.addAll(result.data)
                } else {
                    for (i in result.data.indices) {
                        val index = startIndex + i
                        if (index < loadedItems.size) {
                            loadedItems[index] = result.data[i]
                        } else {
                            loadedItems.add(result.data[i])
                        }
                    }
                }
                Queue.Result.Success(loadedItems[position])
            } else {
                Queue.Result.Error("Failed to load item at position $position")
            }
        } else {
            Queue.Result.Error("Position $position out of bounds")
        }
    }

    override fun clear() {
        loadedItems.clear()
        currentPage = 0
        hasMore = true
        _state.value = Queue.QueueState.Idle
    }

    override fun release() {
        loadedItems.clear()
        _state.value = Queue.QueueState.Completed
    }

    /**
     * 加载指定页的数据
     */
    private suspend fun loadPage(page: Int): Queue.Result<List<String>> {
        _state.value = Queue.QueueState.Loading

        val startIndex = page * batchSize
        val endIndex = minOf(startIndex + batchSize, totalCount)

        if (startIndex >= totalCount) {
            hasMore = false
            _state.value = Queue.QueueState.Completed
            return Queue.Result.Success(emptyList())
        }

        return try {
            // 从trackIds中获取当前页的ID
            val pageIds = trackIds.subList(startIndex, endIndex)
            val stringIds = pageIds.map { it.toString() }
            
            _state.value = Queue.QueueState.Loaded(loadedItems, hasMore = endIndex < totalCount)
            Queue.Result.Success(stringIds)
        } catch (e: Exception) {
            _state.value = Queue.QueueState.Error(
                "Failed to load page $page: ${e.message}",
            )
            Queue.Result.Error("Load page failed: ${e.message}", e)
        }
    }

    /**
     * 预加载指定位置附近的数据
     */
    suspend fun preloadAround(position: Int, windowSize: Int = 10): Queue.Result<Unit> {
        val startPos = maxOf(0, position - windowSize / 2)
        val endPos = minOf(totalCount - 1, position + windowSize / 2)
        
        return try {
            // 确保所需范围内的数据都已加载
            for (pos in startPos..endPos) {
                if (pos >= loadedItems.size) {
                    val page = pos / batchSize
                    loadPage(page)
                }
            }
            Queue.Result.Success(Unit)
        } catch (e: Exception) {
            Queue.Result.Error("Preload failed: ${e.message}", e)
        }
    }
}