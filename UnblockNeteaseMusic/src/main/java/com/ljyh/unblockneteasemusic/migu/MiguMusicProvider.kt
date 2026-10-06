package com.ljyh.unblockneteasemusic.migu

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
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

/** The current Migu H5 search and listen protocol. No Android APIs are required. */
class MiguMusicProvider internal constructor(private val client: HttpClient) :
    MusicCatalogProvider, PlayableAudioProvider, AutoCloseable {
    constructor() : this(providerClient())
    override val sourceId = SOURCE_ID

    override suspend fun search(query: String, limit: Int): List<MusicTrack> {
        val response = client.get("https://c.musicapp.migu.cn/v1.0/content/search_all.do") {
            parameter("text", query)
            parameter("pageNo", 1)
            parameter("pageSize", limit.coerceIn(1, 20))
            parameter("isCopyright", 1)
            parameter("sort", 1)
            parameter("searchSwitch", "{\"song\":1,\"album\":0,\"singer\":0,\"tagSong\":1,\"mvSong\":0,\"bestShow\":1}")
            miguHeaders()
        }
        return json.decodeFromString<MiguSearchResponse>(response.body()).songResultData?.result.orEmpty()
            .take(limit.coerceAtLeast(0))
            .mapNotNull { song ->
                val contentId = song.contentId.content.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val copyrightId = song.copyrightId.content.takeIf(String::isNotBlank) ?: return@mapNotNull null
                MusicTrack(
                    id = TrackId(SOURCE_ID, contentId),
                    title = song.name,
                    artists = song.singers.mapNotNull { singer ->
                        singer.name.takeIf(String::isNotBlank)?.let { MusicArtist(it, singer.id.content) }
                    },
                    album = song.albums.firstOrNull()?.let { MusicAlbum(it.name, it.id.content) },
                    durationMs = 0,
                    playbackId = copyrightId,
                )
            }
    }

    override suspend fun resolve(track: MusicTrack): PlayableAudio? {
        require(track.id.source == SOURCE_ID) { "Track is not from Migu Music" }
        val copyrightId = track.playbackId ?: return null
        for (tone in listOf("HQ", "PQ")) {
            try {
                val response = client.get("https://c.musicapp.migu.cn/strategy/listen-url/h5/v2.4") {
                    parameter("contentId", track.id.value)
                    parameter("copyrightId", copyrightId)
                    parameter("resourceType", 2)
                    parameter("netType", "01")
                    parameter("toneFlag", tone)
                    parameter("scene", "")
                    parameter("lowerQualityContentId", track.id.value)
                    miguHeaders()
                    header("Content-Type", "application/json;charset=UTF-8")
                    header("birth", "h5page")
                    header("signature", "1")
                }
                val data = json.decodeFromString<MiguListenResponse>(decodeMiguPayload(response.body())).data
                    ?: continue
                val url = data.url.takeIf(String::isNotBlank) ?: continue
                if (data.audioFormatType != tone) continue
                val normalized = if (url.startsWith("//")) "https:$url" else url
                if (client.canReadAudio(normalized)) {
                    val durationMs = (data.song?.duration?.content?.toLongOrNull() ?: 0L) * 1_000
                    return PlayableAudio(normalized, track.copy(durationMs = durationMs))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Retry at a lower quality before moving to another source.
            }
        }
        return null
    }

    override fun close() = client.close()

    private fun io.ktor.client.request.HttpRequestBuilder.miguHeaders() {
        header("User-Agent", "Mozilla/5.0")
        header("Accept", "application/json, text/plain, */*")
        header("Origin", "https://h5.nf.migu.cn")
        header("Referer", "https://h5.nf.migu.cn/")
        header("ua", "Android_migu")
        header("version", "6.8.8")
        header("channel", "014021I")
        header("subchannel", "014021I")
    }

    companion object {
        const val SOURCE_ID = "migu"
        private val json = Json { ignoreUnknownKeys = true }
    }
}

/** The H5 listen response may be encoded with a short prefix and a rotating byte offset. */
internal fun decodeMiguPayload(raw: ByteArray): String {
    if (raw.size < 4 || raw[0] != 0xab.toByte() || raw[1] != 0xcd.toByte() || raw[2] != 0x01.toByte()) {
        return raw.toString(Charsets.UTF_8)
    }
    val key = "Jk8qzuePiJ1qE3mDYhLQ3T73DtDoAhLP".toByteArray(Charsets.US_ASCII)
    val seed = raw[3].toInt() and 0xff
    val decoded = ByteArray(raw.size - 4) { index ->
        ((raw[index + 4].toInt() and 0xff) + seed - (key[index % key.size].toInt() and 0xff)).toByte()
    }
    return decoded.toString(Charsets.UTF_8)
}

@Serializable
internal data class MiguSearchResponse(val songResultData: MiguSongResults? = null)
@Serializable
internal data class MiguSongResults(val result: List<MiguSong> = emptyList())
@Serializable
internal data class MiguSong(
    val contentId: JsonPrimitive = JsonPrimitive(""),
    val copyrightId: JsonPrimitive = JsonPrimitive(""),
    val name: String = "",
    val singers: List<MiguNamedItem> = emptyList(),
    val albums: List<MiguNamedItem> = emptyList(),
)
@Serializable
internal data class MiguNamedItem(val id: JsonPrimitive = JsonPrimitive(""), val name: String = "")
@Serializable
internal data class MiguListenResponse(val data: MiguListenData? = null)
@Serializable
internal data class MiguListenData(
    val audioFormatType: String = "",
    val url: String = "",
    val song: MiguListenSong? = null,
)
@Serializable
internal data class MiguListenSong(val duration: JsonPrimitive = JsonPrimitive(0))
