package com.ljyh.mei.data.network.api

import com.ljyh.mei.data.model.api.GetFloorComment
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

    // weapi 其实也是api开头的，但是为了拦截器区分，所以使用weapi开头
    suspend fun getEveryDayRecommendSongs(body: Map<String, String> = mapOf()): EveryDaySongs =
        http.post(base, "/weapi/v3/discovery/recommend/songs", body)

    suspend fun getUserSubcount(body: Map<String, String> = mapOf()): UserSubcount =
        http.post(base, "/weapi/subcount", body)

    suspend fun getUserVipInfo(body: Map<String, String>): UserVipInfo =
        http.post(base, "/api/music-vip-membership/front/vip/info", body, cryptoMode = "weapi")

    suspend fun getHighQualityPlaylist(body: HighQualityPlaylist): HighQualityPlaylistResult =
        http.post(base, "/api/playlist/highquality/list", body)

    suspend fun getRadio(body: Map<String, String> = mapOf()): Radio =
        http.post(base, "/weapi/v1/radio/get", body)

    suspend fun getFloorComment(body: GetFloorComment): FloorComment =
        http.post(base, "/weapi/resource/comment/floor/get", body)
}
