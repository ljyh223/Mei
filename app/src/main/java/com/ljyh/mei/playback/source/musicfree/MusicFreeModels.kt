package com.ljyh.mei.playback.source.musicfree

import com.ljyh.mei.data.model.domain.MediaMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.nio.ByteBuffer
import java.security.MessageDigest

internal val musicFreeJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
internal fun JsonObject.text(key: String): String = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()

@Serializable
data class MusicFreeVariable(val key: String, val name: String = key)

@Serializable
data class MusicFreePlugin(
    val id: String,
    val name: String,
    val version: String = "",
    val author: String = "",
    val enabled: Boolean = true,
    val canSearch: Boolean = false,
    val canResolve: Boolean = false,
    val userVariables: List<MusicFreeVariable> = emptyList(),
    val variables: Map<String, String> = emptyMap(),
)

@Serializable
data class MusicFreeTrack(val pluginId: String, val data: JsonObject) {
    val originalId: String get() = data.text("id")
    val mediaId: Long get() {
        val hash = MessageDigest.getInstance("SHA-256").digest("$pluginId|$originalId".toByteArray())
        return -((ByteBuffer.wrap(hash).long and Long.MAX_VALUE).coerceAtLeast(1))
    }
    fun metadata(): MediaMetadata = MediaMetadata(
        id = mediaId, title = data.text("title"), coverUrl = data.text("artwork"),
        artists = listOf(MediaMetadata.Artist(0, data.text("artist"))),
        duration = ((data["duration"] as? JsonPrimitive)?.doubleOrNull ?: 0.0)
            .takeIf { it.isFinite() && it > 0 }?.times(1000)?.toLong() ?: 0,
        album = MediaMetadata.Album(0, data.text("album")), neteaseId = 0,
    )
}

data class MusicFreeSearchPage(val tracks: List<MusicFreeTrack>, val isEnd: Boolean)
data class MusicFreeAudio(val url: String, val headers: Map<String, String>, val quality: String)

internal fun musicFreePage(pluginId: String, result: JsonElement): MusicFreeSearchPage {
    val objectValue = result as? JsonObject ?: error("插件未返回搜索结果")
    val list = objectValue["data"] as? JsonArray ?: error("插件未返回歌曲列表")
    require(list.size <= 1000) { "单页歌曲数量过多" }
    val tracks = list.mapNotNull { item ->
        val raw = item as? JsonObject ?: return@mapNotNull null
        if (raw.text("id").isBlank() || raw.text("title").isBlank()) null else MusicFreeTrack(pluginId, raw)
    }.distinctBy { it.originalId }
    return MusicFreeSearchPage(tracks, (objectValue["isEnd"] as? JsonPrimitive)?.booleanOrNull == true || list.isEmpty())
}

internal fun musicFreeQuality(quality: String): String = when (quality.lowercase()) {
    "standard", "128k", "low" -> "low"
    "higher", "exhigh", "320k" -> "standard"
    "lossless", "flac", "high" -> "high"
    else -> "super"
}

/** MusicFree's "standard" is its 320k tier, unlike the player catalogue's "standard". */
internal fun musicFreePlaybackQuality(quality: String): String? = when (quality.lowercase()) {
    "low" -> "standard"
    "standard" -> "exhigh"
    "high" -> "lossless"
    "super" -> "hires"
    else -> null
}
