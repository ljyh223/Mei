package com.ljyh.mei.data.network.api


import com.ljyh.mei.data.model.api.CreatePlaylistResult
import com.ljyh.mei.data.model.weapi.EveryDaySongs
import com.ljyh.mei.data.model.weapi.HighQualityPlaylist
import com.ljyh.mei.data.model.weapi.HighQualityPlaylistResult
import com.ljyh.mei.data.model.weapi.UserSubcount
import retrofit2.http.Body
import retrofit2.http.POST

interface WeApiService {

    // weapi 其实也是api开头的，但是为了拦截器区分，所以使用weapi开头
    @POST("/weapi/v3/discovery/recommend/songs")
    suspend fun getEveryDayRecommendSongs(@Body body:Map<String,String> = mapOf()): EveryDaySongs

    @POST("/weapi/subcount")
    suspend fun getUserSubcount(@Body body: Map<String,String> = mapOf()) : UserSubcount

    @POST("/api/playlist/highquality/list")
    suspend fun getHighQualityPlaylist(@Body body: HighQualityPlaylist): HighQualityPlaylistResult

}