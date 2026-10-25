package com.ljyh.mei.playback.queue

import com.ljyh.mei.data.model.domain.MediaMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** The current song includes its metadata so the mini player is usable before network requests. */
@Serializable
internal data class PlaybackQueueSnapshot(
    val ids: List<String>,
    val currentIndex: Int = 0,
    val positionMs: Long = 0,
    val currentSong: MediaMetadata? = null,
    val isFmMode: Boolean = false,
    /** Null in snapshots written before per-song sources were introduced. */
    val entries: List<QueueEntrySnapshot>? = null,
)

@Serializable
internal data class QueueEntrySnapshot(val entryId: String, val source: QueueSource? = null)

internal object PlaybackQueueSnapshotCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(snapshot: PlaybackQueueSnapshot): String = json.encodeToString(snapshot)

    fun decode(value: String?): PlaybackQueueSnapshot? = runCatching {
        value?.let { json.decodeFromString<PlaybackQueueSnapshot>(it) }
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
            ?.let { snapshot ->
                // Older local queue snapshots have only an internal numeric song ID.
                // Without an explicitly saved catalogue ID, do not turn it into a public link.
                val local = snapshot.entries?.getOrNull(snapshot.currentIndex)?.source?.type == QueueSource.Type.LOCAL
                val song = snapshot.currentSong
                if (local && song != null &&
                    (json.parseToJsonElement(requireNotNull(value)).jsonObject["currentSong"] as? JsonObject)
                        ?.containsKey("neteaseId") != true
                ) snapshot.copy(currentSong = song.copy(neteaseId = 0L)) else snapshot
            }

    }.getOrNull()
}
