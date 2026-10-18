package com.ljyh.mei.data.network.qqandroid

import com.ljyh.unblockneteasemusic.model.MusicLyrics
import com.ljyh.unblockneteasemusic.qq.QRCUtils
import java.util.Base64
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** 纯解析：把 `musics.fcg` 的响应 JSON 变成模型，不依赖 Android，方便单测。 */
object QqAndroidParser {

    const val MOD_SEARCH = "music.adaptor.SearchAdaptorQMMobile"
    const val MET_SEARCH = "do_search_v2"
    const val MOD_LYRIC = "music.musichallSong.PlayLyricInfo"
    const val MET_LYRIC = "GetPlayLyricInfo"

    val KEY_SEARCH = "$MOD_SEARCH.$MET_SEARCH"
    val KEY_LYRIC = "$MOD_LYRIC.$MET_LYRIC"

    fun parseSearch(root: JsonObject): List<QqSong> {
        val body = root[KEY_SEARCH]?.jsonObject
            ?.get("data")?.jsonObject
            ?.get("body")?.jsonObject
            ?: error("QQ Android search response missing body")
        val items = body["item_song"]?.jsonObject?.get("items")?.jsonArray
            ?: error("QQ Android search response missing item_song.items")
        return items.mapNotNull { item -> item.jsonObject.toSong() }
    }

    /** 原样返回服务端字段（hex 的 QRC 或 base64 的 LRC），供 LyricManager 统一解码。 */
    fun parseLyricRaw(root: JsonObject): MusicLyrics {
        val data = root[KEY_LYRIC]?.jsonObject?.get("data")?.jsonObject
            ?: error("QQ Android lyric response missing data")
        val qrcT = data["qrc_t"]?.jsonPrimitive?.longOrNull ?: 0L
        return MusicLyrics(
            original = data.str("lyric"),
            translation = data.str("trans"),
            romanization = data.str("roma"),
            wordSynced = qrcT != 0L,
        )
    }

    fun parseLyric(root: JsonObject): MusicLyrics {
        val data = root[KEY_LYRIC]?.jsonObject?.get("data")?.jsonObject
            ?: error("QQ Android lyric response missing data")
        val qrc = decodeField(data.str("lyric"))
        return MusicLyrics(
            original = qrc,
            translation = decodeField(data.str("trans")),
            romanization = decodeField(data.str("roma")),
            wordSynced = qrc.contains(WORD_SYNCED),
        )
    }

    private val WORD_SYNCED = Regex("""\[\d+,\d+\]""")

    private fun JsonObject.toSong(): QqSong? {
        val id = this["id"]?.jsonPrimitive?.longOrNull ?: return null
        val artistNames = this["singer"]?.jsonArray
            ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }
            .orEmpty()
        val albumData = this["album"]?.jsonObject
        return QqSong(
            id = id,
            mid = str("mid"),
            name = str("name").ifBlank { str("title") },
            singer = artistNames.joinToString("、"),
            album = albumData?.let { it.str("title").ifBlank { it.str("name") } }.orEmpty(),
            durationSec = this["interval"]?.jsonPrimitive?.intOrNull ?: 0,
            artistNames = artistNames,
            albumId = albumData?.str("id"),
            albumPmid = albumData?.str("pmid"),
        )
    }

    private fun JsonObject.str(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: ""

    /**
     * `lyric`/`roma` 是 hex 的 QRC 密文；`trans` 常见是 base64 明文。
     * 先按 QRC 解，失败再按 base64 解。
     */
    fun decodeField(raw: String): String {
        if (raw.isBlank()) return ""
        val qrc = runCatching { QRCUtils.decodeLyric(raw) }.getOrDefault("")
        if (qrc.isNotBlank()) return qrc
        return runCatching { String(Base64.getDecoder().decode(raw), Charsets.UTF_8) }
            .getOrDefault("")
    }
}
