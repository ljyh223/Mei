package com.ljyh.mei.data.network.api

import com.ljyh.mei.data.model.AlbumDetail
import com.ljyh.mei.data.model.AlbumPhoto
import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.PlaylistDetail
import com.ljyh.mei.data.model.SongUrl
import com.ljyh.mei.data.model.Tracks
import com.ljyh.mei.data.model.UserAccount
import com.ljyh.mei.data.model.UserAlbumList
import com.ljyh.mei.data.model.UserPlaylist
import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.model.api.BaseMessageResponse
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.api.CreatePlaylist
import com.ljyh.mei.data.model.api.CreatePlaylistResult
import com.ljyh.mei.data.model.api.DeletePlaylist
import com.ljyh.mei.data.model.api.GetAlbumList
import com.ljyh.mei.data.model.api.GetArtistAlbum
import com.ljyh.mei.data.model.api.GetArtistDetail
import com.ljyh.mei.data.model.api.GetArtistSong
import com.ljyh.mei.data.model.api.GetIntelligence
import com.ljyh.mei.data.model.api.GetLyric
import com.ljyh.mei.data.model.api.GetLyricV1
import com.ljyh.mei.data.model.api.GetPlaylistDetail
import com.ljyh.mei.data.model.api.GetSearch
import com.ljyh.mei.data.model.api.GetSearchSuggest
import com.ljyh.mei.data.model.api.GetSongDetails
import com.ljyh.mei.data.model.api.GetSongUrl
import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.data.model.api.GetUserPhotoAlbum
import com.ljyh.mei.data.model.api.GetUserPlaylist
import com.ljyh.mei.data.model.api.Intelligence
import com.ljyh.mei.data.model.api.ManipulateTrack
import com.ljyh.mei.data.model.api.ManipulateTrackResult
import com.ljyh.mei.data.model.api.SearchResult
import com.ljyh.mei.data.model.api.SearchSuggest
import com.ljyh.mei.data.model.api.SubscribePlaylist
import com.ljyh.mei.data.model.weapi.Like
import com.ljyh.mei.data.model.weapi.LikeResult
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    /*
    * 获取歌单详情
    * */
    @POST("/api/v6/playlist/detail")
    suspend fun getPlaylistDetail(@Body body: GetPlaylistDetail): PlaylistDetail

    /*
    * 获取歌曲详情
    * */
    @POST("/api/v3/song/detail")
    suspend fun getSongDetail(@Body body: GetSongDetails): Tracks


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

    @POST("/api/album/sublist")
    suspend fun getCollectAlbumList(@Body body: GetAlbumList): UserAlbumList

    @POST("/api/v1/album/{id}")
    suspend fun getAlbumDetail(
        @Body body: Map<String, String> = emptyMap(),
        @Path("id") id: String
    ): AlbumDetail

    @POST("/api/search/get/")
    suspend fun search(
        @Body body: GetSearch
    ): SearchResult


    @POST("/api/search/suggest/web/")
    suspend fun searchSuggest(
        @Body body: GetSearchSuggest
    ): SearchSuggest


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



    @POST("/api/album/sub")
    suspend fun subscribeAlbum(@Body body: SubscribePlaylist): BaseResponse

    @POST("/api/album/unsub")
    suspend fun unsubscribeAlbum(@Body body: SubscribePlaylist): BaseResponse

    @POST("/api/playlist/remove")
    suspend fun deletePlaylist(@Body body: DeletePlaylist): BaseMessageResponse


    @POST("/api/artist/head/info/get")
    suspend fun getArtistDetail(@Body body: GetArtistDetail): ArtistDetail

    @POST("/api/artist/albums/{id}")
    suspend fun getArtistAlbums(@Body body: GetArtistAlbum, @Path("id") id: String): ArtistAlbum

    @POST("/api/v1/artist/{id}")
    suspend fun getArtistSongs(@Body body: GetArtistSong, @Path("id") id: String): ArtistSong

    @POST("/api/playmode/intelligence/list")
    suspend fun getIntelligenceList(@Body body: GetIntelligence): Intelligence

}
