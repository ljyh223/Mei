package com.ljyh.mei.data.network.api


import com.ljyh.mei.data.model.api.CreatePlaylistResult
import com.ljyh.mei.data.model.weapi.EveryDaySongs
import retrofit2.http.Body
import retrofit2.http.POST

interface WeApiService {


    @POST("/weapi/v3/discovery/recommend/songs")
    suspend fun getEveryDayRecommendSongs(@Body body:Map<String,String> = mapOf()): EveryDaySongs

}