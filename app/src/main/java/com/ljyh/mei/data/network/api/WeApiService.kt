package com.ljyh.mei.data.network.api

import com.ljyh.mei.data.model.api.GetFloorComment
import com.ljyh.mei.data.model.response.AlbumDynamic
import com.ljyh.mei.data.model.response.UserVipInfo
import com.ljyh.mei.data.model.weapi.EveryDaySongs
import com.ljyh.mei.data.model.weapi.FloorComment
import com.ljyh.mei.data.model.weapi.HighQualityPlaylist
import com.ljyh.mei.data.model.weapi.HighQualityPlaylistResult
import com.ljyh.mei.data.model.weapi.Radio
import com.ljyh.mei.data.model.weapi.UserSubcount
import com.ljyh.mei.data.network.NeteaseHttp

class WeApiService(private val http: NeteaseHttp) {
    private val base = "https://music.163.com"

    suspend fun getSubscribedArtists(offset: Int, limit: Int = 100): kotlinx.serialization.json.JsonObject =
        http.post(base, "/weapi/artist/sublist", mapOf("offset" to offset.toString(), "limit" to limit.toString(), "total" to "true"))

    suspend fun setArtistSubscribed(id: String, subscribed: Boolean): com.ljyh.mei.data.model.api.BaseResponse =
        http.post(base, if (subscribed) "/weapi/artist/sub" else "/weapi/artist/unsub",
            mapOf("artistId" to id, "artistIds" to "[$id]"))

    suspend fun getListeningRank(uid: String, allTime: Boolean): com.ljyh.mei.data.model.response.ListeningRankResponse =
        http.post(base, "/weapi/v1/play/record", mapOf("uid" to uid, "type" to if (allTime) "0" else "1"))

    suspend fun getRecentListening(): com.ljyh.mei.data.model.response.RecentListeningResponse =
        http.post(base, "/weapi/play-record/song/list", mapOf("limit" to "100"))

    // weapi 其实也是api开头的，但是为了拦截器区分，所以使用weapi开头
    suspend fun getEveryDayRecommendSongs(body: Map<String, String> = mapOf()): EveryDaySongs =
        http.post(base, "/weapi/v3/discovery/recommend/songs", body)

    suspend fun getUserSubcount(body: Map<String, String> = mapOf()): UserSubcount =
        http.post(base, "/weapi/subcount", body)

    // API Enhanced's /api/ path is rewritten to /weapi/ when encrypted with weapi.
    suspend fun getAlbumDynamic(id: Long): AlbumDynamic =
        http.post(base, "/weapi/album/detail/dynamic", mapOf("id" to id.toString()))

    suspend fun getUserVipInfo(body: Map<String, String>): UserVipInfo =
        http.post(base, "/api/music-vip-membership/front/vip/info", body, cryptoMode = "weapi")

    suspend fun getHighQualityPlaylist(body: HighQualityPlaylist): HighQualityPlaylistResult =
        http.post(base, "/api/playlist/highquality/list", body)

    suspend fun getRadio(body: Map<String, String> = mapOf()): Radio =
        http.post(base, "/weapi/v1/radio/get", body)

    suspend fun getFloorComment(body: GetFloorComment): FloorComment =
        http.post(base, "/weapi/resource/comment/floor/get", body)
}
