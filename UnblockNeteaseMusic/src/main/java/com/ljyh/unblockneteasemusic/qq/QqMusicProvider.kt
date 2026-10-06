package com.ljyh.unblockneteasemusic.qq

import com.ljyh.unblockneteasemusic.model.LyricRequest
import com.ljyh.unblockneteasemusic.model.MusicAlbum
import com.ljyh.unblockneteasemusic.model.MusicArtist
import com.ljyh.unblockneteasemusic.model.MusicLyrics
import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.unblockneteasemusic.model.PlayableAudio
import com.ljyh.unblockneteasemusic.model.TrackId
import com.ljyh.unblockneteasemusic.provider.MusicCatalogProvider
import com.ljyh.unblockneteasemusic.provider.MusicLyricProvider
import com.ljyh.unblockneteasemusic.provider.PlayableAudioProvider
import com.ljyh.unblockneteasemusic.provider.canReadAudio
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlin.random.Random

class QqMusicProvider internal constructor(
    private val client: HttpClient,
    private val cookieProvider: suspend () -> String? = { null },
) :
    MusicCatalogProvider, MusicLyricProvider, PlayableAudioProvider, AutoCloseable {
    constructor(cookieProvider: suspend () -> String? = { null }) : this(defaultClient(), cookieProvider)
    override val sourceId: String = SOURCE_ID

    override suspend fun search(query: String, limit: Int): List<MusicTrack> {
        val cookie = cookieProvider()
        val liteSongs = try {
            client.post(ENDPOINT) {
                cookie?.takeIf(String::isNotBlank)?.let { header(HttpHeaders.Cookie, it) }
                contentType(ContentType.Application.Json)
                setBody(SearchRequest(request = SearchCall(param = SearchParams(query, pageSize = limit))))
            }.body<SearchResponse>().request?.data?.body?.songs.orEmpty()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
        val songs = if (liteSongs.isNotEmpty()) liteSongs else client.get(ENDPOINT) {
            cookie?.takeIf(String::isNotBlank)?.let { header(HttpHeaders.Cookie, it) }
            parameter("data", wireJson.encodeToString(
                DesktopSearchRequest(DesktopSearchCall(param = DesktopSearchParams(
                    pageSize = limit, query = query,
                )))
            ))
        }.body<DesktopSearchResponse>().search?.data?.body?.song?.list.orEmpty()
        return songs.map { song ->
            MusicTrack(
                id = TrackId(SOURCE_ID, song.id.toString()),
                title = song.title.ifBlank { song.name },
                artists = song.singer.map { MusicArtist(it.name, it.id.toString()) },
                album = song.album?.let { album ->
                    MusicAlbum(
                        name = album.title.ifBlank { album.name },
                        id = album.id.toString(),
                        coverUrl = album.pmid.takeIf(String::isNotBlank)
                            ?.let { "https://y.qq.com/music/photo_new/T002R300x300M000$it.jpg" },
                        displayName = album.name.ifBlank { album.title },
                    )
                },
                durationMs = song.interval * 1_000,
                displayTitle = song.name.ifBlank { song.title },
                playbackId = song.mid.takeIf(String::isNotBlank),
            )
        }
    }

    override suspend fun resolve(track: MusicTrack): PlayableAudio? {
        require(track.id.source == SOURCE_ID) { "Track is not from QQ Music" }
        val mid = track.playbackId?.takeIf(String::isNotBlank) ?: return null
        val cookie = cookieProvider().orEmpty()
        val uin = Regex("(?:^|;\\s*)uin=o?(\\d+)").find(cookie)?.groupValues?.get(1) ?: "0"
        val formats = if (cookie.isBlank()) listOf("M500$mid.mp3", null)
            else listOf("M800$mid.mp3", "M500$mid.mp3", null)
        for (filename in formats) {
            try {
                val response = client.post(ENDPOINT) {
                    cookie.takeIf(String::isNotBlank)?.let { header(HttpHeaders.Cookie, it) }
                    contentType(ContentType.Application.Json)
                    setBody(VkeyRequest(VkeyCall(param = VkeyParams(
                        guid = Random.nextInt(10_000_000).toString(),
                        filename = filename?.let(::listOf),
                        songmid = listOf(mid),
                        uin = uin,
                    ))))
                }.body<VkeyResponse>()
                val data = response.payload?.data ?: continue
                val path = data.midurlinfo.firstOrNull()?.purl?.takeIf(String::isNotBlank) ?: continue
                val url = if (path.startsWith("http://") || path.startsWith("https://")) path
                    else (data.sip.firstOrNull() ?: continue) + path
                if (client.canReadAudio(url)) return PlayableAudio(url, track, mimeType = "audio/mpeg")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Try the next format.
            }
        }
        return null
    }

    override suspend fun lyrics(request: LyricRequest): MusicLyrics? {
        require(request.track.id.source == SOURCE_ID) { "Track is not from QQ Music" }
        val track = request.track
        val response = client.post(ENDPOINT) {
            cookieProvider()?.takeIf(String::isNotBlank)?.let { header(HttpHeaders.Cookie, it) }
            contentType(ContentType.Application.Json)
            setBody(
                LyricRequestDto(
                    call = LyricCall(
                        param = LyricParams(
                            albumName = encode(track.album?.name.orEmpty()),
                            interval = track.durationMs / 1_000,
                            qrc = if (request.preferWordSynced) 1 else 0,
                            singerName = encode(track.artists.joinToString(",") { it.name }),
                            songID = track.id.value.toLong(),
                            songName = encode(track.title),
                        ),
                    ),
                ),
            )
        }.body<LyricResponse>()
        val data = response.payload?.data ?: return null
        return MusicLyrics(data.lyric, data.trans, data.roma, data.qrcTime != 0)
    }

    override fun close() = client.close()

    private fun encode(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    companion object {
        const val SOURCE_ID = "qq"
        private const val ENDPOINT = "https://u.y.qq.com/cgi-bin/musicu.fcg"
        private val wireJson = Json { encodeDefaults = true }

        private fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
            defaultRequest {
                header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/91.0.4472.164 Safari/537.36",
                )
                header("Referer", "https://y.qq.com/")
            }
        }
    }
}
