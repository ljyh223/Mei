package com.ljyh.mei.audio.match

import androidx.media3.common.MediaItem
import com.ljyh.mei.data.model.domain.createPlaceholder
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.data.model.api.GetSongDetails
import com.ljyh.mei.data.network.api.ApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class MatchedSong(
    val id: Long,
    val title: String,
    val album: String,
    val artist: String,
    val coverUrl: String?,
    val startTimeMs: Long,
    val mediaItem: MediaItem,
)

private data class RawMatch(
    val id: Long,
    val title: String,
    val album: String,
    val artist: String,
    val startTimeMs: Long,
)

class AudioMatchRepository @Inject constructor(
    private val apiService: ApiService,
) {
    // The match endpoint accepts unencrypted requests; the regular app client rewrites API calls.
    private val matchClient = OkHttpClient.Builder()
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun match(fingerprint: String): List<MatchedSong> {
        val url = "https://interface.music.163.com/api/music/audio/match".toHttpUrl()
            .newBuilder()
            .addQueryParameter("sessionId", "0123456789abcdef")
            .addQueryParameter("algorithmCode", "shazam_v2")
            .addQueryParameter("duration", MatchDurationSeconds.toString())
            .addQueryParameter("rawdata", fingerprint)
            .addQueryParameter("times", "1")
            .addQueryParameter("decrypt", "1")
            .build()
        val body = request(Request.Builder().url(url).get().build())
        val rawMatches = parseMatches(body)
        if (rawMatches.isEmpty()) return emptyList()

        val details = try {
            apiService.getSongDetail(GetSongDetails(rawMatches.joinToString(",") { it.id.toString() }))
                .songs.associateBy { it.id }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyMap()
        }
        return rawMatches.map { raw ->
            val detail = details[raw.id]
            MatchedSong(
                id = raw.id,
                title = detail?.name ?: raw.title,
                album = detail?.al?.name ?: raw.album,
                artist = detail?.ar?.joinToString("、") { it.name.orEmpty() } ?: raw.artist,
                coverUrl = detail?.al?.picUrl,
                startTimeMs = raw.startTimeMs,
                mediaItem = detail?.toMediaItem() ?: createPlaceholder(raw.id.toString()),
            )
        }
    }

    private suspend fun request(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = matchClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        if (!it.isSuccessful) throw IOException("识曲服务请求失败（HTTP ${it.code}）")
                        val body = it.body.string()
                        if (body.isBlank()) throw IOException("识曲服务未返回数据")
                        if (continuation.isActive) continuation.resume(body)
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            }
        })
    }

    private fun parseMatches(body: String): List<RawMatch> {
        val response = JSONObject(body)
        val code = response.optInt("code", -1)
        if (code != 200) throw IOException("识曲服务返回错误（$code）")
        val results = response.optJSONObject("data")?.optJSONArray("result") ?: return emptyList()
        return buildList {
            for (index in 0 until results.length()) {
                val result = results.optJSONObject(index) ?: continue
                val song = result.optJSONObject("song") ?: continue
                val id = song.optLong("id", 0)
                if (id <= 0) continue
                val artists = song.optJSONArray("artists") ?: song.optJSONArray("ar")
                val artistNames = artists?.let { entries ->
                    (0 until entries.length()).mapNotNull { artistIndex ->
                        entries.optJSONObject(artistIndex)?.optString("name")?.takeIf(String::isNotBlank)
                    }.joinToString("、")
                }.orEmpty()
                add(
                    RawMatch(
                        id = id,
                        title = song.optString("name").ifBlank { "未知歌曲" },
                        album = song.optJSONObject("album")?.optString("name").orEmpty(),
                        artist = artistNames,
                        startTimeMs = result.optLong("startTime", 0),
                    ),
                )
            }
        }.distinctBy { it.id }
    }
}
