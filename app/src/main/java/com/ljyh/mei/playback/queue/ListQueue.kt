package com.ljyh.mei.playback.queue

import androidx.media3.common.MediaItem

/**
 * 列表队列实现
 * 用于处理静态的歌曲ID列表
 */
class ListQueue(
    override val id: String,
    override val title: String? = null,
    private val items: List<Pair<String, MediaItem?>>,
    val startIndex: Int = 0,
    val position: Int = 0,
    override val source: QueueSource = QueueSource.fromQueue(id, title),
) : Queue {

    /** Use the song tapped in the visible list, even if the caller supplied a stale index. */
    fun startingAt(trackId: String): ListQueue? {
        val index = items.indexOfFirst { it.first == trackId }
        if (index < 0) return null
        return ListQueue(id, title, items, startIndex = index, source = source)
    }

    override suspend fun getInitialStatus(): Queue.Status =
        Queue.Status(title, items, startIndex, position)
}
