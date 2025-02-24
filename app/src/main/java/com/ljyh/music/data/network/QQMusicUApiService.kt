package com.ljyh.music.data.network

import com.ljyh.music.data.model.qq.u.MusicU
import com.ljyh.music.data.model.qq.u.Search
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface QQMusicUApiService {

    @Headers("Content-Type: application/json")
    @POST("/cgi-bin/musicu.fcg")
    suspend fun search(@Body body: MusicU):Search
}