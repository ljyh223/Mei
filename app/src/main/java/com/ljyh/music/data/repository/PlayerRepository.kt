package com.ljyh.music.data.repository

import com.ljyh.music.data.model.Lyric
import com.ljyh.music.data.model.api.GetLyric
import com.ljyh.music.data.model.api.GetLyricV1
import com.ljyh.music.data.model.qq.u.GetLyricData
import com.ljyh.music.data.model.qq.u.GetSearchData
import com.ljyh.music.data.model.qq.u.GetSearchData.Comm1
import com.ljyh.music.data.model.qq.u.LyricResult
import com.ljyh.music.data.model.qq.u.SearchResult
import com.ljyh.music.data.network.QQMusicUApiService
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.network.api.ApiService
import com.ljyh.music.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayerRepository(
    private val qqMusicUApiService: QQMusicUApiService,
    private val apiService: ApiService
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
                        GetLyricData.GetPlayLyricInfo(
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


}
