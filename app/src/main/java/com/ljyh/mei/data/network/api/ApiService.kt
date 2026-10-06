package com.ljyh.mei.data.network.api

import com.ljyh.mei.data.model.response.AlbumDetail
import com.ljyh.mei.data.model.response.AlbumPhoto
import com.ljyh.mei.data.model.response.Lyric
import com.ljyh.mei.data.model.response.PlaylistDetail
import com.ljyh.mei.data.model.response.SongUrl
import com.ljyh.mei.data.model.response.Tracks
import com.ljyh.mei.data.model.response.UserAccount
import com.ljyh.mei.data.model.response.UserDetail
import com.ljyh.mei.data.model.response.UserAlbumList
import com.ljyh.mei.data.model.response.UserPlaylist
import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.model.api.BaseMessageResponse
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.api.CheckSongLike
import com.ljyh.mei.data.model.api.CheckSongLikeResult
import com.ljyh.mei.data.model.api.CreatePlaylist
import com.ljyh.mei.data.model.api.CreatePlaylistResult
import com.ljyh.mei.data.model.api.DeletePlaylist
import com.ljyh.mei.data.model.api.GetAlbumList
import com.ljyh.mei.data.model.api.GetArtistAlbum
import com.ljyh.mei.data.model.api.GetArtistDetail
import com.ljyh.mei.data.model.api.GetArtistSong
import com.ljyh.mei.data.model.api.GetComment
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
import com.ljyh.mei.data.model.api.UpdatePlaylistTrackOrder
import com.ljyh.mei.data.model.api.ManipulateTrackResult
import com.ljyh.mei.data.model.api.SearchResult
import com.ljyh.mei.data.model.api.SearchSuggest
import com.ljyh.mei.data.model.api.SubscribePlaylist
import com.ljyh.mei.data.model.weapi.Comment
import com.ljyh.mei.data.model.weapi.Like
import com.ljyh.mei.data.model.weapi.LikeResult
import com.ljyh.mei.data.network.NeteaseHttp

class ApiService(private val http: NeteaseHttp) {
    private val base = "https://interface.music.163.com"

    suspend fun getDynamicCover(body: Map<String, Long>): DynamicCoverResponse =
        http.post(base, "/api/songplay/dynamic-cover", body)
    suspend fun getPlaylistDetail(body: GetPlaylistDetail): PlaylistDetail =
        http.post(base, "/api/v6/playlist/detail", body)
    suspend fun getSongDetail(body: GetSongDetails): Tracks =
        http.post(base, "/api/v3/song/detail", body)
    suspend fun getAccountDetail(): UserAccount =
        http.post(base, "/api/nuser/account/get", emptyMap<String, String>())
    suspend fun getUserDetail(id: String, body: Map<String, String> = emptyMap()): UserDetail =
        http.post(base, "/api/v1/user/detail/$id", body)
    suspend fun getLyric(body: GetLyric): Lyric =
        http.post(base, "/api/song/lyric", body)
    suspend fun getLyricV1(body: GetLyricV1): Lyric =
        http.post(base, "/api/song/lyric/v1", body)
    suspend fun getUserPlaylist(body: GetUserPlaylist): UserPlaylist =
        http.post(base, "/api/user/playlist", body)
    suspend fun checkSongLike(body: CheckSongLike): CheckSongLikeResult =
        http.post(base, "/api/song/like/check", body)
    suspend fun getCollectAlbumList(body: GetAlbumList): UserAlbumList =
        http.post(base, "/api/album/sublist", body)
    suspend fun getAlbumDetail(body: Map<String, String> = emptyMap(), id: String): AlbumDetail =
        http.post(base, "/api/v1/album/$id", body)
    suspend fun search(body: GetSearch): SearchResult =
        http.post(base, "/api/search/get/", body)
    suspend fun searchSuggest(body: GetSearchSuggest): SearchSuggest =
        http.post(base, "/api/search/suggest/web/", body)
    suspend fun getSongUrlV1(body: GetSongUrlV1): SongUrl =
        http.post(base, "/api/song/enhance/player/url/v1", body)
    suspend fun getSongUrl(body: GetSongUrl): SongUrl =
        http.post(base, "/api/song/enhance/player/url", body)
    suspend fun getUserPhotoAlbum(body: GetUserPhotoAlbum): AlbumPhoto =
        http.post(base, "/api/user/photo/album/get", body)
    suspend fun like(body: Like): LikeResult =
        http.post(base, "/api/radio/like", body)
    suspend fun manipulateTracks(body: ManipulateTrack): ManipulateTrackResult =
        http.post(base, "/api/playlist/manipulate/tracks", body)
    suspend fun updatePlaylistTrackOrder(body: UpdatePlaylistTrackOrder): BaseResponse =
        http.post(base, "/api/playlist/manipulate/tracks", body)
    suspend fun createPlaylist(body: CreatePlaylist): CreatePlaylistResult =
        http.post(base, "/api/playlist/create", body)
    suspend fun subscribeAlbum(body: SubscribePlaylist): BaseResponse =
        http.post(base, "/api/album/sub", body)
    suspend fun unsubscribeAlbum(body: SubscribePlaylist): BaseResponse =
        http.post(base, "/api/album/unsub", body)
    suspend fun deletePlaylist(body: DeletePlaylist): BaseMessageResponse =
        http.post(base, "/api/playlist/remove", body)
    suspend fun getArtistDetail(body: GetArtistDetail): ArtistDetail =
        http.post(base, "/api/artist/head/info/get", body)
    suspend fun getArtistAlbums(body: GetArtistAlbum, id: String): ArtistAlbum =
        http.post(base, "/api/artist/albums/$id", body)
    suspend fun getArtistSongs(body: GetArtistSong, id: String): ArtistSong =
        http.post(base, "/api/v1/artist/$id", body)
    suspend fun getIntelligenceList(body: GetIntelligence): Intelligence =
        http.post(base, "/api/playmode/intelligence/list", body)
    suspend fun getComment(body: GetComment): Comment =
        http.post(base, "/api/v2/resource/comments", body)
}
