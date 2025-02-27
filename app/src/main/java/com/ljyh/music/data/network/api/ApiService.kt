package com.ljyh.music.data.network.api

import com.ljyh.music.data.model.AlbumPhoto
import com.ljyh.music.data.model.Lyric
import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.data.model.SongUrl
import com.ljyh.music.data.model.TrackAll
import com.ljyh.music.data.model.UserAccount
import com.ljyh.music.data.model.UserPlaylist
import com.ljyh.music.data.model.api.GetLyric
import com.ljyh.music.data.model.api.GetLyricV1
import com.ljyh.music.data.model.api.GetPlaylistDetail
import com.ljyh.music.data.model.api.GetSongDetails
import com.ljyh.music.data.model.api.GetSongUrl
import com.ljyh.music.data.model.api.GetSongUrlV1
import com.ljyh.music.data.model.api.GetUserPhotoAlbum
import com.ljyh.music.data.model.api.GetUserPlaylist
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
//    @GET("/homepage/block/page") // 示例 endpoint
//    suspend fun getRadar(): HomePage
//
//    @GET("/recommend/songs")
//    suspend fun getRecommend(): Recommend
//
//    @GET("/playlist/detail")
//    suspend fun getPlaylistDetail1(@Query("id") id: String): PlaylistDetail
//
//    @GET("/song/url")
//    suspend fun getSongUrl(@Query("id") id: String): SongUrl
//
//    @GET("/song/url/v1")
//    suspend fun getSongUrlV1(
//        @Query("id") id: String,
//        @Query("level") level: String = "standard"
//    ): SongUrl
//
//
//    @GET("/user/account")
//    suspend fun getUserAccount(): UserAccount
//
//    @GET("/user/playlist")
//    suspend fun getUserPlaylist(@Query("uid") uid: String): UserPlaylist
//
//    @GET("/playlist/track/all")
//    suspend fun getPlaylistTracks(
//        @Query("id") id: String,
//        @Query("limit") limit: Int,
//        @Query("offset") offset: Int = 0
//    ): TrackAll
//
//
//    @GET("/lyric/new")
//    suspend fun getLyric(@Query("id") id: String): Lyric
//
//    @GET("/song/detail")
//    suspend fun getSongDetail(@Query("ids") ids: String): TrackAll
//
//
//    @GET("/homepage/resource/show")
//    suspend fun getHomePageResourceShow(): HomePageResourceShow
//
//
//    @GET("/like")
//    suspend fun like(@Query("id") id:String, @Query("like") like:Boolean=true)
//
//
//    @GET("/photo/album/get")
//    suspend fun getPhotoAlbum(@Query("id") id:String): AlbumPhoto

    /*
    * 获取歌单详情
    * */
    @POST("/api/v6/playlist/detail")
    suspend fun getPlaylistDetail(@Body body: GetPlaylistDetail):PlaylistDetail

    /*
    * 获取歌单详情
    * */
    @POST("/api/v3/song/detail")
    suspend fun getSongDetail(@Body body: GetSongDetails):TrackAll


    /*
    * 获取用户信息
    * */
    @GET("/api/nuser/account/get")
    suspend fun getAccountDetail(): UserAccount

    /*
    * 获取歌词
    * */
    @POST("/api/song/lyric")
    suspend fun getLyric(
        @Body body: GetLyric
    ): Lyric


    /*
    * 获取歌词 新接口
    * */

    @POST("/api/song/lyric/v1")
    suspend fun getLyricV1(
        @Body body: GetLyricV1
    ): Lyric

    /*
    * 获取用户歌单
    * */
    @POST("/api/user/playlist")
    suspend fun getUserPlaylist(
        @Body body: GetUserPlaylist
    ): UserPlaylist



    @POST("/api/song/enhance/player/url/v1")
    suspend fun getSongUrlV1(@Body body: GetSongUrlV1): SongUrl


    @POST("/api/song/enhance/player/url")
    suspend fun getSongUrl(@Body body: GetSongUrl): SongUrl


    @POST("/api/user/photo/album/get")
    suspend fun getUserPhotoAlbum(@Body body: GetUserPhotoAlbum): AlbumPhoto

    /**
     * 每日推荐歌曲
     */
//    @GET("/api/v3/discovery/recommend/songs")
//    suspend fun getDailyRecommendSongList(): DailyRecommendSongs
}
