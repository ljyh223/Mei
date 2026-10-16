package com.ljyh.mei.playback.queue

import com.google.gson.Gson
import com.ljyh.mei.data.model.domain.MediaMetadata

/** The current song includes its metadata so the mini player is usable before network requests. */
internal data class PlaybackQueueSnapshot(
    val ids: List<String>,
    val currentIndex: Int,
    val positionMs: Long,
    val currentSong: MediaMetadata?,
    val isFmMode: Boolean = false,
    /** Null in snapshots written before per-song sources were introduced. */
    val entries: List<QueueEntrySnapshot>? = null,
)

internal data class QueueEntrySnapshot(val entryId: String, val source: QueueSource?)

internal object PlaybackQueueSnapshotCodec {
    private val gson = Gson()

    fun encode(snapshot: PlaybackQueueSnapshot): String = gson.toJson(snapshot)

    fun decode(value: String?): PlaybackQueueSnapshot? = runCatching {
        value?.let { gson.fromJson(it, PlaybackQueueSnapshot::class.java) }
            ?.takeIf { snapshot ->
                snapshot.ids.isNotEmpty() &&
                    snapshot.ids.all { it.isNotBlank() } &&
                    snapshot.currentIndex in snapshot.ids.indices &&
                    snapshot.positionMs >= 0L &&
                    (snapshot.entries == null || (snapshot.entries.size == snapshot.ids.size &&
                        snapshot.entries.all { it.entryId.isNotBlank() })) &&
                    (snapshot.currentSong == null ||
                        snapshot.currentSong.id.toString() == snapshot.ids[snapshot.currentIndex])
            }
    }.getOrNull()
}
