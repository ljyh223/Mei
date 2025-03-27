package com.ljyh.music.data.network.api

import com.ljyh.music.data.model.HomePageResourceShow
import com.ljyh.music.data.model.weapi.GetHomePageResourceShow
import retrofit2.http.Body
import retrofit2.http.POST

interface WeApiService {

    @POST("/weapi/link/page/rcmd/resource/show")
    suspend fun getHomePageResourceShow(@Body body:GetHomePageResourceShow): HomePageResourceShow



}