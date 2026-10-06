package com.ljyh.unblockneteasemusic.qq

import com.ljyh.unblockneteasemusic.model.LyricRequest
import com.ljyh.unblockneteasemusic.model.MusicAlbum
import com.ljyh.unblockneteasemusic.model.MusicArtist
import com.ljyh.unblockneteasemusic.model.MusicLyrics
import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.unblockneteasemusic.model.TrackId
import com.ljyh.unblockneteasemusic.provider.MusicCatalogProvider
import com.ljyh.unblockneteasemusic.provider.MusicLyricProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.Base64

class QqMusicProvider internal constructor(private val client: HttpClient) :
    MusicCatalogProvider, MusicLyricProvider, AutoCloseable {
    constructor() : this(defaultClient())
    override val sourceId: String = SOURCE_ID

    override suspend fun search(query: String, limit: Int): List<MusicTrack> {
        val response = client.post(ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(SearchRequest(request = SearchCall(param = SearchParams(query, pageSize = limit))))
        }.body<SearchResponse>()
        return response.request?.data?.body?.songs.orEmpty().map { song ->
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
            )
        }
    }

    override suspend fun lyrics(request: LyricRequest): MusicLyrics? {
        require(request.track.id.source == SOURCE_ID) { "Track is not from QQ Music" }
        val track = request.track
        val response = client.post(ENDPOINT) {
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
