package com.ljyh.mei.playback.queue

import androidx.media3.common.MediaItem

/** An input list used to replace the active Media3 playlist, not a second live queue. */
interface Queue {
    val id: String
    val title: String?
    val source: QueueSource? get() = null
    suspend fun getInitialStatus(): Status

    data class Status(
        val title: String?,
        val ids: List<Pair<String, MediaItem?>>,
        val mediaItemIndex: Int,
        val position: Int = 0,
    )
}
