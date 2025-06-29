package com.ljyh.mei.playback.queue

import androidx.media3.common.MediaItem
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.SongEntity
import com.ljyh.mei.data.model.metadata

interface Queue {
    val preloadItem: String?
    suspend fun getInitialStatus(): Status
    fun hasNextPage(): Boolean
    suspend fun nextPage(): List<String>

    data class Status(
        val title: String?,
        val ids: List<String>,
        val mediaItemIndex: Int,
        val position: Int = 0,
    ) {

    }
}

fun List<MediaItem>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filterNot {
            it.metadata?.explicit == true
        }
    } else this

