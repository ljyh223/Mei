package com.ljyh.mei.playback.queue

import androidx.media3.common.MediaItem
import com.ljyh.mei.data.model.MediaMetadata


object EmptyQueue : Queue {
    override val preloadItem: String? = null
    override suspend fun getInitialStatus() = Queue.Status(null, emptyList(), -1)
    override fun hasNextPage() = false
    override suspend fun nextPage() = emptyList<String>()
}