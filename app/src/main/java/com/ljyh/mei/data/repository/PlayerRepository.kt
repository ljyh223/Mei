package com.ljyh.mei.data.repository

import android.content.Context
import com.ljyh.mei.constants.DefaultTtmlLyricsBaseUrl
import com.ljyh.mei.constants.QqLyricApi
import com.ljyh.mei.constants.QqLyricApiKey
import com.ljyh.mei.constants.TtmlLyricsBaseUrlKey
import com.ljyh.mei.data.network.qqandroid.QqAndroidClient
import com.ljyh.mei.data.network.qqandroid.QqSong
import com.ljyh.mei.data.network.qqandroid.toMusicTrack
import com.ljyh.mei.data.model.response.Lyric
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaMetadata
import com.ljyh.mei.data.model.api.GetIntelligence
import com.ljyh.mei.data.model.api.GetSongDetails
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
import com.ljyh.mei.utils.preferences.toEnum
import com.ljyh.mei.utils.lyric.LRCParser
import com.ljyh.mei.utils.lyric.QRCParser
import com.ljyh.unblockneteasemusic.qq.QRCUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.IOException
import java.util.concurrent.TimeUnit
import timber.log.Timber

class PlayerRepository(
    private val qqMusicProvider: QqMusicProvider,
    private val qqAndroidClient: QqAndroidClient,
    private val apiService: ApiService,
    private val weApiService: WeApiService,
    private val context: Context,
) {

    suspend fun searchNew(keyword: String): Resource<List<MusicTrack>> {
        return withContext(Dispatchers.IO) {
            val api = context.dataStore.data.first()[QqLyricApiKey].toEnum(QqLyricApi.Auto)
            safeApiCall {
                when (api) {
                    QqLyricApi.Legacy -> qqMusicProvider.search(keyword)
                    QqLyricApi.Android -> qqAndroidClient.search(keyword).map { it.toMusicTrack() }
                    QqLyricApi.Auto -> try {
                        qqAndroidClient.search(keyword).map { it.toMusicTrack() }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Timber.w(error, "QQ Android search failed, falling back to legacy")
                        qqMusicProvider.search(keyword)
                    }
                }
            }
        }
    }

    suspend fun getLyricNew(
        title: String,
        album: String,
        artist: String,
        duration: Long,
        id: Long
    ): Resource<MusicLyrics> = qqLyric(title, album, artist, duration, id, preferWordSynced = true)

    suspend fun getLyricLrc(
        title: String,
        album: String,
        artist: String,
        duration: Long,
        id: Long
    ): Resource<MusicLyrics> = qqLyric(title, album, artist, duration, id, preferWordSynced = false)

    /**
     * 按设置里选的「QQ 搜索与歌词接口」在两条 QQ 实现之间切换：
     *  - [QqLyricApi.Android] —— 新版 Android 客户端协议（musics.fcg + sign/mask）
     *  - [QqLyricApi.Legacy]  —— 旧版 musicu.fcg
     *  - [QqLyricApi.Auto]    —— 先 Android，失败回退旧版
     *
     * 返回的 [MusicLyrics] 保持「服务端原始字段」语义，由 LyricManager 统一解码。
     */
    private suspend fun qqLyric(
        title: String,
        album: String,
        artist: String,
        duration: Long,
        id: Long,
        preferWordSynced: Boolean,
    ): Resource<MusicLyrics> = withContext(Dispatchers.IO) {
        val api = context.dataStore.data.first()[QqLyricApiKey].toEnum(QqLyricApi.Auto)
        safeApiCall {
            when (api) {
                QqLyricApi.Legacy -> legacyQqLyric(title, album, artist, duration, id, preferWordSynced)
                QqLyricApi.Android -> androidQqLyric(title, album, artist, duration, id, preferWordSynced)
                QqLyricApi.Auto -> try {
                    androidQqLyric(title, album, artist, duration, id, preferWordSynced)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    Timber.w(e, "QQ Android lyric failed, falling back to legacy")
                    legacyQqLyric(title, album, artist, duration, id, preferWordSynced)
                }
            }
        }
    }

    private suspend fun legacyQqLyric(
        title: String,
        album: String,
        artist: String,
        duration: Long,
        id: Long,
        preferWordSynced: Boolean,
    ): MusicLyrics = qqMusicProvider.lyrics(
        LyricRequest(
            qqTrack(title, album, artist, duration, id),
            preferWordSynced = preferWordSynced,
        )
    ) ?: error("QQ lyric response has no data")

    private suspend fun androidQqLyric(
        title: String,
        album: String,
        artist: String,
        duration: Long,
        id: Long,
        preferWordSynced: Boolean,
    ): MusicLyrics {
        val lyrics = qqAndroidClient.lyricRaw(
            song = QqSong(
                id = id,
                mid = "",
                name = title,
                singer = artist,
                album = album,
                durationSec = duration.toInt(),
            ),
            preferWordSynced = preferWordSynced,
        )
        val decoded = QRCUtils.decodeLyric(lyrics.original)
        val hasTimedLines = QRCParser.parse(decoded, null).lines.isNotEmpty() ||
            LRCParser.parse(decoded, null).lines.isNotEmpty()
        require(hasTimedLines) { "QQ Android lyric has no decodable timed lines" }
        return lyrics
    }

    private fun qqTrack(title: String, album: String, artist: String, durationSeconds: Long, id: Long) =
        MusicTrack(
            id = TrackId(QqMusicProvider.SOURCE_ID, id.toString()),
            title = title,
            artists = artist.split(',').map { MusicArtist(it.trim()) },
            album = MusicAlbum(album),
            durationMs = durationSeconds * 1_000,
        )

    suspend fun getLyricV1(id: String): Resource<Lyric> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getLyricV1(id)
            }
        }
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


    suspend fun getIntelligenceTracks(id: String, playlistId: String): Resource<List<MediaMetadata>> {
        if (id.toLongOrNull()?.let { it > 0 } != true || playlistId.toLongOrNull()?.let { it > 0 } != true) {
            return Resource.Error("心动模式入口已失效，请刷新首页后重试")
        }
        return withContext(Dispatchers.IO) {
            val response = safeApiCall {
                apiService.getIntelligenceList(
                    GetIntelligence(songId = id, playlistId = playlistId, startMusicId = id),
                )
            }
            when (response) {
                is Resource.Success -> {
                    // Validate recommendations before fetching the seed; never replace the queue on failure.
                    val recommendations = intelligencePlaybackTracks(response.data, seed = null)
                    if (recommendations is Resource.Error) recommendations else {
                        val seed = (getSongDetail(id) as? Resource.Success)?.data
                            ?.firstOrNull { it.id.toString() == id }
                        intelligencePlaybackTracks(response.data, seed)
                    }
                }
                is Resource.Error -> Resource.Error("心动模式加载失败，请检查网络后重试")
                Resource.Loading -> Resource.Loading
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

}
