package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.api.GetLyric
import com.ljyh.mei.data.model.api.GetLyricV1
import com.ljyh.mei.data.model.qq.u.GetLyricData
import com.ljyh.mei.data.model.qq.u.GetSearchData
import com.ljyh.mei.data.model.qq.u.GetSearchData.Comm1
import com.ljyh.mei.data.model.qq.u.LyricResult
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.data.model.weapi.Like
import com.ljyh.mei.data.network.QQMusicUApiService
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

class PlayerRepository(
    private val qqMusicUApiService: QQMusicUApiService,
    private val apiService: ApiService,
    private val weApiService: WeApiService
) {

    suspend fun searchNew(keyword: String): Resource<SearchResult> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                qqMusicUApiService.search(
                    GetSearchData(
                        comm = Comm1(),
                        req = GetSearchData.Req(param = GetSearchData.Req.Param(query = keyword))
                    )
                )
            }
        }
    }

    suspend fun getLyricNew(
        title: String,
        album: String,
        artist: String,
        duration: Int,
        id: Int
    ): Resource<LyricResult> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                qqMusicUApiService.getLyric(
                    GetLyricData(
                        comm = GetLyricData.Comm(),
                        getPlayLyricInfo =GetLyricData.GetPlayLyricInfo(
                            param = GetLyricData.GetPlayLyricInfo.GetLyric(
                                singerName = artist,
                                songName = title,
                                albumName = album,
                                interval = duration,
                                songID = id

                            )
                        )
                    )
                )
            }
        }
    }

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

    suspend fun getAMLLyric(id: String): Resource<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://amlldb.bikonoo.com/ncm-lyrics/$id.ttml"
                val request = Request.Builder()
                    .url(url)
                    .build()
                // 使用 execute() 进行同步请求，因为我们已在 IO 协程中
                val response = OkHttpClient().newCall(request).execute()

                if (response.isSuccessful) {
                    val lyricContent = response.body?.string()
                    // 检查响应体是否有效且不是"歌词不存在"的特定字符串
                    if (!lyricContent.isNullOrEmpty() && lyricContent != "歌词不存在") {
                        Resource.Success(lyricContent)
                    } else {
                        // 服务器成功响应，但内容表明歌词不存在
                        Resource.Error("歌词不存在")
                    }
                } else {
                    // 处理 HTTP 错误，例如 404 Not Found 也意味着歌词不存在
                    if (response.code == 404) {
                        Resource.Error("歌词不存在")
                    } else {
                        Resource.Error("请求失败，错误码: ${response.code}")
                    }
                }
            } catch (e: IOException) {
                // 处理网络连接等 IO 异常
                Resource.Error("网络异常，请检查你的网络连接")
            }
        }
    }

}
