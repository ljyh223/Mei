package com.ljyh.mei.playback.source.lx

import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.unblockneteasemusic.model.MusicArtist
import com.ljyh.unblockneteasemusic.model.MusicAlbum
import com.ljyh.unblockneteasemusic.model.TrackId
import com.ljyh.unblockneteasemusic.qq.QqMusicProvider
import com.ljyh.unblockneteasemusic.kuwo.KuwoMusicProvider
import com.ljyh.unblockneteasemusic.migu.MiguMusicProvider
import com.ljyh.unblockneteasemusic.unblock.DefaultTrackMatcher
import kotlinx.serialization.json.*
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LxCatalog @Inject constructor(
    private val qq: QqMusicProvider,
    private val kuwo: KuwoMusicProvider,
    private val migu: MiguMusicProvider,
) {
    suspend fun candidates(target: MusicTrack, source: String): List<LxCandidate> {
        if (source == "wy") return listOf(candidate(target, "wy"))
        val query = "${target.title} ${target.artists.take(2).joinToString(" ") { it.name }}".trim()
        if (source == "kg") return kugou(query).filter { matches(target, it.first) }.take(3).map { it.second }
        val tracks = when (source) {
            "tx" -> qq.search(query, 5)
            "kw" -> kuwo.search(query, 5)
            "mg" -> migu.search(query, 5)
            else -> return emptyList()
        }
        return tracks.filter { matches(target, it) }.sortedByDescending { DefaultTrackMatcher.score(target, it) }
            .take(3).map { track ->
                val base = candidate(track, source)
                val mid = track.playbackId
                if (source == "tx" && mid != null) {
                    try { enrichQq(base, mid) }
                    catch (c: CancellationException) { throw c }
                    catch (_: Exception) { base }
                } else base
            }
    }

    private fun matches(target: MusicTrack, candidate: MusicTrack): Boolean =
        DefaultTrackMatcher.score(target, candidate) >= DefaultTrackMatcher.minimumScore

    internal fun candidate(track: MusicTrack, source: String): LxCandidate = LxCandidate(source, buildJsonObject {
        put("source", source); put("name", track.title)
        put("singer", track.artists.joinToString("、") { it.name })
        put("albumName", track.album?.name.orEmpty()); put("albumId", track.album?.id.orEmpty())
        put("songmid", if (source == "tx") track.playbackId ?: track.id.value else track.id.value)
        put("songId", track.id.value.toLongOrNull()?.let(::JsonPrimitive) ?: JsonPrimitive(track.id.value))
        put("interval", "%02d:%02d".format(track.durationMs / 60_000, track.durationMs / 1000 % 60))
        put("_interval", track.durationMs / 1000); put("img", track.album?.coverUrl.orEmpty())
        if (source == "mg") { put("copyrightId", track.playbackId.orEmpty()); put("contentId", track.id.value) }
        put("types", buildJsonArray { listOf("128k", "320k", "flac", "flac24bit").forEach { add(buildJsonObject { put("type", it) }) } })
        put("_types", buildJsonObject { listOf("128k", "320k", "flac", "flac24bit").forEach { put(it, buildJsonObject {}) } })
        put("typeUrl", buildJsonObject {})
    }, track.durationMs)

    private suspend fun enrichQq(base: LxCandidate, mid: String): LxCandidate {
        val payload = buildJsonObject {
            put("comm", buildJsonObject { put("ct", 19); put("cv", 1859); put("uin", "0") })
            put("req", buildJsonObject {
                put("module", "music.pf_song_detail_svr"); put("method", "get_song_detail_yqq")
                put("param", buildJsonObject { put("song_type", 0); put("song_mid", mid) })
            })
        }
        val body = fetch("https://u.y.qq.com/cgi-bin/musicu.fcg", payload)
        val track = body["req"]?.jsonObject?.get("data")?.jsonObject?.get("track_info")?.jsonObject ?: return base
        return base.copy(musicInfo = JsonObject(base.musicInfo + buildJsonObject {
            track["file"]?.jsonObject?.string("media_mid")?.takeIf { it.isNotBlank() }?.let { put("strMediaMid", it) }
            track["album"]?.jsonObject?.string("mid")?.let { put("albumMid", it); put("albumId", it) }
        }))
    }

    private suspend fun kugou(query: String): List<Pair<MusicTrack, LxCandidate>> {
        val url = "https://songsearch.kugou.com/song_search_v2".toHttpUrl().newBuilder()
            .addQueryParameter("keyword", query).addQueryParameter("page", "1").addQueryParameter("pagesize", "5")
            .addQueryParameter("userid", "0").addQueryParameter("clientver", "").addQueryParameter("platform", "WebFilter")
            .addQueryParameter("filter", "2").addQueryParameter("iscorrection", "1")
            .addQueryParameter("privilege_filter", "0").addQueryParameter("area_code", "1").build()
        val response = fetch(url.toString())
        val list = response["data"]?.jsonObject?.get("lists") as? JsonArray ?: return emptyList()
        return list.mapNotNull { value ->
            val item = value.jsonObject
            val hash = item.string("FileHash").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val id = item.string("Audioid")
            val track = MusicTrack(TrackId("kugou", id), item.string("OriSongName") + item.string("Suffix").let { if (it.isBlank()) "" else " $it" },
                (item["Singers"] as? JsonArray)?.map { MusicArtist(it.jsonObject.string("name")) }
                    ?: item.string("SingerName").split('、').map(::MusicArtist),
                MusicAlbum(item.string("AlbumName"), item.string("AlbumID")), item.long("Duration") * 1000)
            val types = listOf("128k" to "FileHash", "320k" to "HQFileHash", "flac" to "SQFileHash", "flac24bit" to "ResFileHash")
                .mapNotNull { (quality, field) -> item.string(field).takeIf { it.isNotBlank() && it.any { c -> c != '0' } }?.let { quality to it } }
            val info = JsonObject(candidate(track, "kg").musicInfo + buildJsonObject {
                put("hash", hash)
                put("types", buildJsonArray { types.forEach { (type, h) -> add(buildJsonObject { put("type", type); put("hash", h) }) } })
                put("_types", buildJsonObject { types.forEach { (type, h) -> put(type, buildJsonObject { put("hash", h) }) } })
            })
            track to LxCandidate("kg", info, track.durationMs)
        }
    }

    private suspend fun fetch(url: String, body: JsonObject? = null): JsonObject {
        val transport = LxHttpTransport()
        try {
            val raw = transport.request(buildJsonObject {
                put("id", "catalog"); put("url", url)
                put("options", buildJsonObject {
                    if (body != null) { put("method", "POST"); put("body", body) }
                    put("timeout", 5_000)
                })
            }.toString())
            val response = lxJson.parseToJsonElement(raw).jsonObject
            require(response.long("statusCode") in 200..299)
            val result = response.getValue("body")
            return if (result is JsonObject) result else lxJson.parseToJsonElement(result.jsonPrimitive.content).jsonObject
        } finally { transport.close() }
    }
}
