package com.ljyh.mei.data.network.api

import com.ljyh.mei.data.model.AlbumPhoto
import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.PlaylistDetail
import com.ljyh.mei.data.model.SongUrl
import com.ljyh.mei.data.model.TrackAll
import com.ljyh.mei.data.model.UserAccount
import com.ljyh.mei.data.model.UserPlaylist
import com.ljyh.mei.data.model.api.BaseMessageResponse
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.api.CreatePlaylist
import com.ljyh.mei.data.model.api.CreatePlaylistResult
import com.ljyh.mei.data.model.api.DeletePlaylist
import com.ljyh.mei.data.model.api.GetLyric
import com.ljyh.mei.data.model.api.GetLyricV1
import com.ljyh.mei.data.model.api.GetPlaylistDetail
import com.ljyh.mei.data.model.api.GetSongDetails
import com.ljyh.mei.data.model.api.GetSongUrl
import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.data.model.api.GetUserPhotoAlbum
import com.ljyh.mei.data.model.api.GetUserPlaylist
import com.ljyh.mei.data.model.api.ManipulateTrack
import com.ljyh.mei.data.model.api.ManipulateTrackResult
import com.ljyh.mei.data.model.api.SubscribePlaylist
import com.ljyh.mei.data.model.api.SubscribePlaylistResult
import com.ljyh.mei.data.model.weapi.Like
import com.ljyh.mei.data.model.weapi.LikeResult
import retrofit2.http.Body
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
    @POST("/api/nuser/account/get")
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

    @POST("/api/radio/like")
    suspend fun like(@Body body: Like): LikeResult

    @POST("/api/playlist/manipulate/tracks")
    suspend fun manipulateTracks(@Body body: ManipulateTrack): ManipulateTrackResult

    @POST("/api/playlist/create")
    suspend fun createPlaylist(@Body body: CreatePlaylist): CreatePlaylistResult

    @POST("/api/playlist/subscribe")
    suspend fun subscribePlaylist(@Body body: SubscribePlaylist): BaseResponse


    @POST("/api/playlist/unsubscribe")
    suspend fun unSubscribePlaylist(@Body body: SubscribePlaylist): BaseResponse

    @POST("/api/playlist/remove")
    suspend fun deletePlaylist(@Body body: DeletePlaylist): BaseMessageResponse

}
