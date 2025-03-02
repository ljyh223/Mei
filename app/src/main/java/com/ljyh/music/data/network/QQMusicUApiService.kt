package com.ljyh.music.data.network

import com.ljyh.music.data.model.qq.u.GetLyricData
import com.ljyh.music.data.model.qq.u.GetSearchData
import com.ljyh.music.data.model.qq.u.LyricResult
import com.ljyh.music.data.model.qq.u.MusicU
import com.ljyh.music.data.model.qq.u.Search
import com.ljyh.music.data.model.qq.u.SearchResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface QQMusicUApiService {
    @POST("/cgi-bin/musicu.fcg")
    suspend fun search(@Body body: MusicU):Search


    @POST("/cgi-bin/musicu.fcg")
    suspend fun search(@Body body: GetSearchData): SearchResult


    @POST("/cgi-bin/musicu.fcg")
    suspend fun getLyric(@Body body: GetLyricData): LyricResult
}