package com.ljyh.mei.playback.queue

import androidx.media3.common.MediaItem
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.metadata
import kotlinx.coroutines.flow.StateFlow

/**
 * 播放队列接口 - 重新设计版本
 * 支持完整的歌单管理、分页加载、错误处理和状态管理
 */
interface Queue {
    /** 队列唯一标识 */
    val id: String
    
    /** 队列标题 */
    val title: String?
    
    /** 队列总歌曲数量 */
    val totalCount: Int
    
    /** 当前已加载的歌曲数量 */
    val loadedCount: Int
    
    /** 队列状态 */
    val state: StateFlow<QueueState>
    
    /** 预加载项（如果有） */
    val preloadItem: String?
    
    /** 获取初始状态 */
    suspend fun getInitialStatus(): Status
    
    /** 是否有下一页 */
    fun hasNextPage(): Boolean
    
    /** 加载下一页 */
    suspend fun nextPage(): Result<List<String>>
    
    /** 重新加载当前页（用于错误恢复） */
    suspend fun reloadCurrentPage(): Result<List<String>>
    
    /** 获取指定位置的歌曲ID */
    suspend fun getItemAt(position: Int): Result<String>
    
    /** 清空队列 */
    fun clear()
    
    /** 释放资源 */
    fun release()

    data class Status(
        val title: String?,
        val ids: List<String>,
        val mediaItemIndex: Int,
        val position: Int = 0,
    )

    /**
     * 队列状态
     */
    sealed class QueueState {
        object Idle : QueueState()
        object Loading : QueueState()
        data class Loaded(val items: List<String>, val hasMore: Boolean) : QueueState()
        data class Error(val message: String) : QueueState()
        object Completed : QueueState()
    }

    /**
     * 队列操作结果
     */
    sealed class Result<out T> {
        data class Success<out T>(val data: T) : Result<T>()
        data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
    }
}

/**
 * 队列工厂，用于创建不同类型的队列
 */
object QueueFactory {
    
    /**
     * 创建列表队列
     */
    fun createListQueue(
        id: String,
        title: String? = null,
        items: List<String>,
        startIndex: Int = 0,
        position: Int = 0
    ): Queue = ListQueue(id, title, items, startIndex, position)
    
    /**
     * 创建空队列
     */
    fun createEmptyQueue(): Queue = EmptyQueue

}

/**
 * 队列监听器接口
 */
interface QueueListener {
    fun onQueueStateChanged(state: Queue.QueueState)
    fun onQueueItemsAdded(items: List<String>, position: Int)
    fun onQueueError(error: String)
    fun onQueueCompleted()
}
