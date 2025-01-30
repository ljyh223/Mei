package com.ljyh.music.playback.queue

import androidx.media3.common.MediaItem
import com.ljyh.music.data.model.MediaMetadata
import com.ljyh.music.data.model.SongEntity


class ListQueue(
    val title: String? = null,
    val items: List<String>,
    val startIndex: Int = 0,
    val position: Long = 0L,
) : Queue {
    override val preloadItem: String? = null
    override suspend fun getInitialStatus() = Queue.Status(title, items, startIndex, position)
    override fun hasNextPage(): Boolean = false
    override suspend fun nextPage() = throw UnsupportedOperationException()
}