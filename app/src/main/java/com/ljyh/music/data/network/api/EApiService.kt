package com.ljyh.music.data.network.api

import com.ljyh.music.data.model.AlbumPhoto
import com.ljyh.music.data.model.HomePageResourceShow
import com.ljyh.music.data.model.api.GetUserPhotoAlbum
import com.ljyh.music.data.model.weapi.GetHomePageResourceShow
import retrofit2.http.Body
import retrofit2.http.POST


interface EApiService {
    @POST("/eapi/link/page/rcmd/resource/show")
    suspend fun getHomePageResourceShow(@Body body: GetHomePageResourceShow): HomePageResourceShow
}