package com.ljyh.mei.ui.screen.main.library

import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.data.model.response.MiniPlaylistDetail
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.playback.queue.QueueSource

/** Preserve the complete order even when detail only includes metadata for some tracks. */
internal fun buildLibraryPlaylistQueue(detail: MiniPlaylistDetail): ListQueue? {
    val ids = detail.trackIds.ifEmpty { detail.tracks.map { it.id } }.distinct()
    if (detail.id <= 0 || ids.isEmpty()) return null
    val metadata = detail.tracks.associateBy { it.id }
    return ListQueue(
        id = "playlist_${detail.id}", title = detail.name,
        source = QueueSource(QueueSource.Type.PLAYLIST, detail.id.toString(), detail.name),
        items = ids.map { id -> id.toString() to metadata[id]?.toMediaItem() },
    )
}
