package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.SongUrl
import com.ljyh.mei.data.model.api.GetLyric
import com.ljyh.mei.data.model.api.GetLyricV1
import com.ljyh.mei.data.model.api.GetSongUrl
import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.data.model.qq.u.GetLyricData
import com.ljyh.mei.data.model.qq.u.GetSearchData
import com.ljyh.mei.data.model.qq.u.GetSearchData.Comm1
import com.ljyh.mei.data.model.qq.u.LyricResult
import com.ljyh.mei.data.model.qq.u.MusicU
import com.ljyh.mei.data.model.qq.u.Search
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.QQMusicCApiService
import com.ljyh.mei.data.network.QQMusicUApiService
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShareRepository(
    val apiService: ApiService,
    val qqMusicCApiService: QQMusicCApiService,
    val qqMusicUApiService: QQMusicUApiService
) {
    suspend fun getSongUrl(id: String): Resource<SongUrl> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getSongUrl(
                    GetSongUrl(
                        ids = "[$id]"
                    )
                )
            }
        }
    }

    suspend fun getSongUrlV1(id: String): Resource<SongUrl> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getSongUrlV1(
                    GetSongUrlV1(
                        ids = "[$id]"
                    )
                )
            }
        }
    }




    suspend fun getQQMusicLyric(id: String): Resource<String> {
        return withContext(Dispatchers.IO) {
            safeApiCall { qqMusicCApiService.getQQMusicLyric(id) }
        }
    }


}