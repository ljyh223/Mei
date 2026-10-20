package com.ljyh.mei.download

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Keep the JSON string-array format used by already-enqueued WorkManager requests. */
internal object DownloadSongIdsCodec {
    fun encode(ids: List<String>): String = Json.encodeToString(ids)

    fun decode(value: String): List<String> = Json.decodeFromString(value)
}
