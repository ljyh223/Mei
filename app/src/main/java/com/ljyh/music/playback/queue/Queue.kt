package com.ljyh.music.playback.queue

import androidx.media3.common.MediaItem
import com.ljyh.music.data.model.MediaMetadata
import com.ljyh.music.data.model.SongEntity
import com.ljyh.music.data.model.metadata

interface Queue {
    val preloadItem: String?
    suspend fun getInitialStatus(): Status
    fun hasNextPage(): Boolean
    suspend fun nextPage(): List<String>

    data class Status(
        val title: String?,
        val ids: List<String>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    ) {

    }
}

fun List<MediaItem>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filterNot {
            it.metadata?.explicit == true
        }
    } else this

