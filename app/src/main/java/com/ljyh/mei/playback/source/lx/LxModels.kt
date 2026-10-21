package com.ljyh.mei.playback.source.lx

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class LxScript(
    val id: String,
    val name: String,
    val version: String = "",
    val author: String = "",
    val description: String = "",
    val homepage: String = "",
    val enabled: Boolean = true,
    val sources: Map<String, LxCapability> = emptyMap(),
)

@Serializable
data class LxCapability(
    val name: String = "",
    val type: String = "music",
    val actions: List<String> = emptyList(),
    val qualitys: List<String> = emptyList(),
)

data class LxCandidate(val source: String, val musicInfo: JsonObject, val durationMs: Long)
data class LxAudio(val url: String, val scriptId: String, val source: String, val candidateId: String, val quality: String) {
    val identity: String get() = "$scriptId|$source|$candidateId|$quality"
}

internal val lxJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
internal fun JsonObject.string(key: String, default: String = ""): String =
    (get(key) as? JsonPrimitive)?.contentOrNull ?: default
internal fun JsonObject.long(key: String, default: Long = 0): Long =
    (get(key) as? JsonPrimitive)?.longOrNull ?: default

/** Never infer the duration of returned audio from a catalog song's duration. */
object PreviewPolicy {
    fun isPreview(durationMs: Long?): Boolean = durationMs != null && durationMs in 0..30_000
    fun qualities(requested: String, available: List<String>): List<String> {
        val ordered = listOf("128k", "320k", "flac", "flac24bit")
        val target = when (requested.lowercase()) {
            "standard", "128k" -> 0
            "higher", "exhigh", "320k" -> 1
            "lossless", "flac" -> 2
            else -> 3
        }
        return ordered.take(target + 1).asReversed().filter { it in available }
    }
}

internal fun java.io.InputStream.readLimited(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream(minOf(limit, 8192))
    val buffer = ByteArray(minOf(limit, 8192))
    while (output.size() < limit) {
        val count = read(buffer, 0, minOf(buffer.size, limit - output.size()))
        if (count < 0) break
        if (count > 0) output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

internal fun sourceDisplayName(id: String): String = when (id) {
    "wy", "netease" -> "网易云"
    "tx", "qq" -> "QQ 音乐"
    "kw", "kuwo" -> "酷我音乐"
    "kg", "kugou" -> "酷狗音乐"
    "mg", "migu" -> "咪咕音乐"
    "local" -> "本地音乐"
    else -> id
}
