package com.ljyh.unblockneteasemusic.kuwo

import com.ljyh.unblockneteasemusic.model.MusicAlbum
import com.ljyh.unblockneteasemusic.model.MusicArtist
import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.unblockneteasemusic.model.PlayableAudio
import com.ljyh.unblockneteasemusic.model.TrackId
import com.ljyh.unblockneteasemusic.provider.MusicCatalogProvider
import com.ljyh.unblockneteasemusic.provider.PlayableAudioProvider
import com.ljyh.unblockneteasemusic.provider.canReadAudio
import com.ljyh.unblockneteasemusic.provider.providerClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

/** Search and direct URL protocol adapted from the local UnblockNeteaseMusic server. */
class KuwoMusicProvider internal constructor(private val client: HttpClient) :
    MusicCatalogProvider, PlayableAudioProvider, AutoCloseable {
    constructor() : this(providerClient())
    override val sourceId = SOURCE_ID

    override suspend fun search(query: String, limit: Int): List<MusicTrack> {
        val response = client.get("http://search.kuwo.cn/r.s") {
            parameter("correct", 1)
            parameter("vipver", 1)
            parameter("stype", "comprehensive")
            parameter("encoding", "utf8")
            parameter("rformat", "json")
            parameter("mobi", 1)
            parameter("show_copyright_off", 1)
            parameter("searchapi", 6)
            parameter("all", query.replace(" - ", " "))
        }
        val root = json.decodeFromString<KuwoSearchResponse>(response.body())
        return root.content.getOrNull(1)?.musicpage?.abslist.orEmpty()
            .take(limit.coerceAtLeast(0))
            .mapNotNull { song ->
                val id = song.MUSICRID.substringAfterLast('_').takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
                MusicTrack(
                    id = TrackId(SOURCE_ID, id),
                    title = song.SONGNAME,
                    artists = song.ARTIST.split('&').mapNotNull { name ->
                        name.trim().takeIf(String::isNotBlank)?.let(::MusicArtist)
                    },
                    album = song.ALBUM.takeIf(String::isNotBlank)?.let {
                        MusicAlbum(it, song.ALBUMID)
                    },
                    durationMs = (song.DURATION.content.toLongOrNull() ?: 0L) * 1_000,
                )
            }
    }

    override suspend fun resolve(track: MusicTrack): PlayableAudio? {
        require(track.id.source == SOURCE_ID) { "Track is not from Kuwo Music" }
        val response = client.get("http://antiserver.kuwo.cn/anti.s") {
            parameter("type", "convert_url")
            parameter("format", "mp3")
            parameter("response", "url")
            parameter("rid", "MUSIC_${track.id.value}")
            header("User-Agent", "okhttp/3.10.0")
        }.body<String>()
        val url = URL_PATTERN.find(response)?.value ?: return null
        return if (client.canReadAudio(url, track.durationMs)) {
            PlayableAudio(url, track, mimeType = "audio/mpeg")
        } else null
    }

    override fun close() = client.close()

    companion object {
        const val SOURCE_ID = "kuwo"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
        private val URL_PATTERN = Regex("https?://[^\\s\\\"']+")
    }
}

@Serializable
internal data class KuwoSearchResponse(val content: List<KuwoContent> = emptyList())
@Serializable
internal data class KuwoContent(val musicpage: KuwoMusicPage? = null)
@Serializable
internal data class KuwoMusicPage(val abslist: List<KuwoSong> = emptyList())
@Serializable
internal data class KuwoSong(
    val MUSICRID: String = "",
    val SONGNAME: String = "",
    val ARTIST: String = "",
    val ALBUMID: String = "",
    val ALBUM: String = "",
    val DURATION: JsonPrimitive = JsonPrimitive(0),
)
