package com.ljyh.music.data.repository

import com.ljyh.music.data.model.Lyric
import com.ljyh.music.data.model.SongUrl
import com.ljyh.music.data.network.ApiService
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class  ShareRepository(val apiService: ApiService) {
    suspend fun getSongUrl(id:String): Resource<SongUrl> {
        return withContext(Dispatchers.IO) {
            safeApiCall { apiService.getSongUrl(id) }
        }
    }

    suspend fun getSongUrlV1(id:String): Resource<SongUrl> {
        return withContext(Dispatchers.IO) {
            safeApiCall { apiService.getSongUrlV1(id) }
        }
    }


    suspend fun getLyric(id:String): Resource<Lyric> {
        return withContext(Dispatchers.IO) {
            safeApiCall { apiService.getLyric(id) }
        }
    }

}