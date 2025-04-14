package com.ljyh.music.data.network.api


import com.ljyh.music.data.model.weapi.EveryDaySongs
import retrofit2.http.Body
import retrofit2.http.POST

interface WeApiService {


    @POST("/weapi/v3/discovery/recommend/songs")
    suspend fun getEveryDayRecommendSongs(@Body body:Map<String,String> = mapOf()): EveryDaySongs

}