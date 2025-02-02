package com.ljyh.music.data.network

import com.ljyh.music.data.model.AlbumPhoto
import com.ljyh.music.data.model.HomePage
import com.ljyh.music.data.model.HomePageResourceShow
import com.ljyh.music.data.model.Lyric
import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.data.model.Recommend
import com.ljyh.music.data.model.SongUrl
import com.ljyh.music.data.model.TrackAll
import com.ljyh.music.data.model.UserAccount
import com.ljyh.music.data.model.UserPlaylist
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("/homepage/block/page") // 示例 endpoint
    suspend fun getRadar(): HomePage

    @GET("/recommend/songs")
    suspend fun getRecommend(): Recommend

    @GET("/playlist/detail")
    suspend fun getPlaylistDetail(@Query("id") id: String): PlaylistDetail

    @GET("/song/url")
    suspend fun getSongUrl(@Query("id") id: String): SongUrl

    @GET("/song/url/v1")
    suspend fun getSongUrlV1(
        @Query("id") id: String,
        @Query("level") level: String = "level"
    ): SongUrl


    @GET("/user/account")
    suspend fun getUserAccount(): UserAccount

    @GET("/user/playlist")
    suspend fun getUserPlaylist(@Query("uid") uid: String): UserPlaylist

    @GET("/playlist/track/all")
    suspend fun getPlaylistTracks(
        @Query("id") id: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int = 0
    ): TrackAll


    @GET("/lyric")
    suspend fun getLyric(@Query("id") id: String): Lyric

    @GET("/song/detail")
    suspend fun getSongDetail(@Query("ids") ids: String): TrackAll


    @GET("/homepage/resource/show")
    suspend fun getHomePageResourceShow(): HomePageResourceShow


    @GET("/like")
    suspend fun like(@Query("id") id:String, @Query("like") like:Boolean=true)


    @GET("/photo/album/get")
    suspend fun getPhotoAlbum(@Query("id") id:String): AlbumPhoto
}
