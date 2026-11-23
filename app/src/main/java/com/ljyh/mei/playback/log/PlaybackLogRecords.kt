package com.ljyh.mei.playback.log

import com.ljyh.mei.playback.queue.QueueSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

@Serializable
internal data class PendingPlaybackLog(
    val id: String = UUID.randomUUID().toString(),
    val owner: String,
    val createdAtMs: Long,
    val ua: String,
    val bi: String,
    val entropy: ByteArray,
    val accepted: Set<String> = emptySet(),
    val attempts: Int = 0,
) {
    fun fileName(channel: String): String {
        val nonce = UUID.nameUUIDFromBytes(id.toByteArray()).leastSignificantBits ushr 1
        return "flush_${channel}_${10000 + nonce % 90000}_${nonce ushr 32}_$nonce"
    }
}

internal fun PlaybackLogEvent.records(pid: Int, serializedAtMs: Long = atMs): Pair<String, String> {
    fun record(action: String, channel: String): String {
        val fields = buildJsonObject {
            put("id", track.id)
            put("type", "song")
            put("source", track.source)
            put("sourceId", track.sourceId)
            put("startlogtime", startedAtMs / 1000)
            if (channel == "ua") {
                put("logtime", atMs)
                put("_eventcode", action)
                put("_sessid", appSessionId)
            }
            put("_logTime", serializedAtMs)
            put("_log_thoroughfare", channel)
            put("time", playedTimeMs / 1000)
            put("realtime", realTimeMs / 1000)
            put("resource_time", track.durationMs.coerceAtLeast(0) / 1000)
            if (track.title.isNotBlank()) put("songName", track.title.take(1000))
            if (track.artist.isNotBlank()) put("artistName", track.artist.take(1000))
            put("currentProcessName", "com.ljyh.mei")
            put("pid", pid)
            put("lca", 1)
            put("buildType", "release")
            end?.let { put("end", it) }
        }
        return "${serializedAtMs / 1000}\u0001$action\u0001$fields\n"
    }
    return record(if (end == null) "_plv" else "_pld", "ua") to
        record(if (end == null) "startplay" else "play", "bi")
}

/** Unknown collection identifiers never masquerade as playlist or album ids. */
internal fun playbackLogSource(source: QueueSource?, songId: Long): Pair<String, String> {
    val collection = source?.id?.toLongOrNull()?.takeIf { it > 0 }?.toString()
    return when {
        collection != null && source?.type == QueueSource.Type.PLAYLIST -> "list" to collection
        collection != null && source?.type == QueueSource.Type.ALBUM -> "album" to collection
        collection != null && source?.type == QueueSource.Type.ARTIST -> "artist" to collection
        else -> "track" to songId.toString()
    }
}
