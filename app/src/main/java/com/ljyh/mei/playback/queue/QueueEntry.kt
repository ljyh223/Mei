package com.ljyh.mei.playback.queue

import android.os.Bundle
import androidx.media3.common.MediaItem
import java.util.UUID

/** The page or collection from which one occurrence of a song entered the queue. */
data class QueueSource(
    val type: Type,
    val id: String? = null,
    val title: String? = null,
) {
    enum class Type { PLAYLIST, ALBUM, DAILY, LOCAL, SEARCH, ARTIST, HISTORY, RADIO, RECOMMENDATION, RECOGNITION, MANUAL, OTHER }

    val displayText: String
        get() {
            val name = when (type) {
                Type.PLAYLIST -> "歌单"
                Type.ALBUM -> "专辑"
                Type.DAILY -> "每日推荐"
                Type.LOCAL -> "本地音乐"
                Type.SEARCH -> "搜索"
                Type.ARTIST -> "歌手"
                Type.HISTORY -> "最近播放"
                Type.RADIO -> "私人 FM"
                Type.RECOMMENDATION -> "推荐"
                Type.RECOGNITION -> "听歌识曲"
                Type.MANUAL -> "手动添加"
                Type.OTHER -> "播放列表"
            }
            val detail = title?.takeIf { it.isNotBlank() && it != name }
            return if (detail == null) "来自$name" else "来自$name · $detail"
        }

    companion object {
        fun fromQueue(id: String, title: String?): QueueSource = when {
            id.startsWith("playlist_") -> QueueSource(Type.PLAYLIST, id.removePrefix("playlist_"), title)
            id.startsWith("album_") -> QueueSource(Type.ALBUM, id.removePrefix("album_"), title)
            id == "dailySongs" -> QueueSource(Type.DAILY, id, title)
            id.startsWith("local_") -> QueueSource(Type.LOCAL, id, title)
            id.startsWith("SearchQueue-") -> QueueSource(Type.SEARCH, id, title)
            id == "history" -> QueueSource(Type.HISTORY, id, title)
            id.startsWith("intelligence-") -> QueueSource(Type.RECOMMENDATION, id, title)
            id.startsWith("AudioMatch-") -> QueueSource(Type.RECOGNITION, id, title)
            else -> QueueSource(Type.OTHER, id, title)
        }
    }
}

/** Keep identity and source on the MediaItem so Media3 moves them with each queue occurrence. */
private const val ENTRY_ID_KEY = "mei.queue.entry_id"
private const val SOURCE_TYPE_KEY = "mei.queue.source_type"
private const val SOURCE_ID_KEY = "mei.queue.source_id"
private const val SOURCE_TITLE_KEY = "mei.queue.source_title"

val MediaItem.queueEntryId: String?
    get() = mediaMetadata.extras?.getString(ENTRY_ID_KEY)

val MediaItem.queueSource: QueueSource?
    get() {
        val extras = mediaMetadata.extras ?: return null
        val type = extras.getString(SOURCE_TYPE_KEY)?.let { value ->
            QueueSource.Type.entries.firstOrNull { it.name == value }
        } ?: return null
        return QueueSource(type, extras.getString(SOURCE_ID_KEY), extras.getString(SOURCE_TITLE_KEY))
    }

fun MediaItem.withQueueEntry(
    source: QueueSource?,
    entryId: String = UUID.randomUUID().toString(),
): MediaItem {
    val extras = Bundle(mediaMetadata.extras ?: Bundle())
    extras.putString(ENTRY_ID_KEY, entryId)
    if (source == null) {
        extras.remove(SOURCE_TYPE_KEY)
        extras.remove(SOURCE_ID_KEY)
        extras.remove(SOURCE_TITLE_KEY)
    } else {
        extras.putString(SOURCE_TYPE_KEY, source.type.name)
        extras.putString(SOURCE_ID_KEY, source.id)
        extras.putString(SOURCE_TITLE_KEY, source.title)
    }
    return buildUpon().setMediaMetadata(mediaMetadata.buildUpon().setExtras(extras).build()).build()
}
