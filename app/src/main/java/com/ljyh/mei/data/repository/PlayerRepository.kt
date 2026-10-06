package com.ljyh.mei.data.repository

import android.content.Context
import com.ljyh.mei.constants.DefaultTtmlLyricsBaseUrl
import com.ljyh.mei.constants.TtmlLyricsBaseUrlKey
import com.ljyh.mei.data.model.response.Lyric
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaMetadata
import com.ljyh.mei.data.model.api.GetIntelligence
import com.ljyh.mei.data.model.api.GetLyric
import com.ljyh.mei.data.model.api.GetLyricV1
import com.ljyh.mei.data.model.api.GetSongDetails
import com.ljyh.mei.data.model.api.Intelligence
import com.ljyh.mei.data.model.weapi.Like
import com.ljyh.mei.data.model.weapi.Radio
import com.ljyh.unblockneteasemusic.model.LyricRequest
import com.ljyh.unblockneteasemusic.model.MusicAlbum
import com.ljyh.unblockneteasemusic.model.MusicArtist
import com.ljyh.unblockneteasemusic.model.MusicLyrics
import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.unblockneteasemusic.model.TrackId
import com.ljyh.unblockneteasemusic.qq.QqMusicProvider
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.network.safeApiCall
import com.ljyh.mei.utils.preferences.dataStore
import com.ljyh.mei.data.model.api.CheckSongLike
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.IOException
import timber.log.Timber
import java.util.concurrent.TimeUnit

class PlayerRepository(
    private val qqMusicProvider: QqMusicProvider,
    private val apiService: ApiService,
    private val weApiService: WeApiService,
    private val context: Context,
) {

    suspend fun searchNew(keyword: String): Resource<List<MusicTrack>> {
        return withContext(Dispatchers.IO) {
            safeApiCall { qqMusicProvider.search(keyword) }
        }
    }

    suspend fun getLyricNew(
        title: String,
        album: String,
        artist: String,
        duration: Long,
        id: Long
    ): Resource<MusicLyrics> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                qqMusicProvider.lyrics(LyricRequest(qqTrack(title, album, artist, duration, id)))
                    ?: error("QQ lyric response has no data")
            }
        }
    }

    suspend fun getLyricLrc(
        title: String,
        album: String,
        artist: String,
        duration: Long,
        id: Long
    ): Resource<MusicLyrics> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                qqMusicProvider.lyrics(
                    LyricRequest(qqTrack(title, album, artist, duration, id), preferWordSynced = false)
                ) ?: error("QQ lyric response has no data")
            }
        }
    }

    private fun qqTrack(title: String, album: String, artist: String, durationSeconds: Long, id: Long) =
        MusicTrack(
            id = TrackId(QqMusicProvider.SOURCE_ID, id.toString()),
            title = title,
            artists = artist.split(',').map { MusicArtist(it.trim()) },
            album = MusicAlbum(album),
            durationMs = durationSeconds * 1_000,
        )

    suspend fun getLyric(id: String): Resource<Lyric> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getLyric(
                    GetLyric(
                        id = id
                    )
                )
            }
        }
    }


    suspend fun getLyricV1(id: String): Resource<Lyric> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getLyricV1(
                    GetLyricV1(
                        id = id
                    )
                )
            }
        }
    }


    suspend fun like(id: String, like: Boolean) {
        apiService.like(
            Like(
                trackId = id,
                like = like
            )
        )
    }

    private val amllClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getAMLLyric(id: String): Resource<String> {
        return withContext(Dispatchers.IO) {
            try {
                val configuredBaseUrl = context.dataStore.data.first()[TtmlLyricsBaseUrlKey]
                    ?.trim()
                    ?.trimEnd('/')
                    ?.takeIf(String::isNotBlank)
                    ?: DefaultTtmlLyricsBaseUrl
                val url = "$configuredBaseUrl/ncm-lyrics/$id.ttml".toHttpUrlOrNull()
                    ?: return@withContext Resource.Error("TTML 歌词服务地址无效")
                val request = Request.Builder().url(url).build()

                val result = amllClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val lyricContent = response.body.string()
                        if (!lyricContent.isNullOrEmpty() && lyricContent != "歌词不存在") {
                            Resource.Success(lyricContent)
                        } else {
                            Resource.Error("歌词不存在")
                        }
                    } else {
                        if (response.code == 404) {
                            Resource.Error("歌词不存在")
                        } else {
                            Resource.Error("请求失败，错误码: ${response.code}")
                        }
                    }
                }
                result
            } catch (e: IOException) {
                Resource.Error("网络异常，请检查你的网络连接")
            }
        }
    }

    suspend fun getRadio(): Resource<Radio>{
        return withContext(Dispatchers.IO){
            safeApiCall {
                weApiService.getRadio()
            }
        }
    }


    suspend fun getIntelligenceList(id: String, playlistId: String, startSongId:String): Resource<Intelligence>{
        return withContext(Dispatchers.IO){
            safeApiCall {
                apiService.getIntelligenceList(
                    GetIntelligence(
                        songId = id,
                        playlistId = playlistId,
                        startMusicId = startSongId
                    )
                )
            }
        }
    }

    suspend fun getSongDetail(id: String): Resource<List<MediaMetadata>>{
        return withContext(Dispatchers.IO){
            safeApiCall {
                apiService.getSongDetail(
                    GetSongDetails(id)
                ).songs.map { it.toMediaMetadata() }
            }
        }
    }

    suspend fun checkSongLike(id: Long): Resource<Boolean>{
        return withContext(Dispatchers.IO){
            safeApiCall {
                val result = apiService.checkSongLike(CheckSongLike("[${id}]"))
                Timber.tag("Player Repo").d(result.toString())
                result.ids.contains(id)
            }
        }
    }

}
