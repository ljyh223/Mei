package com.ljyh.mei.data.network.qqandroid

import java.util.Base64
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** `musics.fcg` 两个方法的参数模板，客户端与单测共用。 */
object QqAndroidParams {

    fun search(query: String, pageSize: Int = 15): JsonObject = buildJsonObject {
        put("ver", 0)
        put("searchid", "")
        put("search_type", 100)
        put("query", query)
        put("page_id", 1)
        put("page_num", pageSize)
        put("remoteplace", "search.android.keyboard")
        put("highlight", 1)
        put("nqc_flag", 0)
        put("grp", 1)
        put("multi_zhida", 1)
        put("auto_page", 1)
        put("related_data", "")
        put("ambi_data", "")
        put("from", "8,")
        put("seqno", 1)
        putJsonObject("custom_flags") {
            put("ai_resp_br", 1)
            put("change_skin", 0)
            put("agent_param", 1)
        }
    }

    fun lyric(song: QqSong, preferWordSynced: Boolean = true): JsonObject = buildJsonObject {
        put("needSingingAnnotations", false)
        put("singingAnnotationsTs", 0)
        put("ct", 11)
        put("cv", QQ_ANDROID_VERSION_CODE)
        put("gt", 1)
        put("cid", "111")
        put("cmd", 0)
        put("num", 1)
        put("songID", song.id)
        put("type", 1)
        put("songName", encodeUtf8Base64(song.name))
        put("singerName", encodeUtf8Base64(song.singer))
        put("albumName", encodeUtf8Base64(song.album))
        put("qrc", if (preferWordSynced) 1 else 0)
        put("trans", if (preferWordSynced) 1 else 0)
        put("roma", if (preferWordSynced) 1 else 0)
        put("lrc_t", 0)
        put("qrc_t", 0)
        put("trans_t", 0)
        put("roma_t", 0)
        put("needLTLyric", false)
        put("lt_lyric_t", 0)
        put("crypt", 0)
        put("searchID", "")
        put("modify", 0)
    }

    private fun encodeUtf8Base64(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
}
