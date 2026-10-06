package com.ljyh.mei.data.network.api

import com.ljyh.mei.data.model.AlbumPhoto
import com.ljyh.mei.data.model.ListenDataRealtimeResponse
import com.ljyh.mei.data.model.ListenDataReportResponse
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.eapi.HomePageResourceShow
import com.ljyh.mei.data.model.api.GetUserPhotoAlbum
import com.ljyh.mei.data.model.api.SubscribePlaylist
import com.ljyh.mei.data.model.weapi.GetHomePageResourceShow
import com.ljyh.mei.data.network.NeteaseHttp


class EApiService(private val http: NeteaseHttp) {
    private val base = "https://interface.music.163.com"
    suspend fun getHomePageResourceShow(body: GetHomePageResourceShow): HomePageResourceShow =
        http.post(base, "/eapi/link/page/rcmd/resource/show", body)

    suspend fun search(body: GetUserPhotoAlbum): AlbumPhoto =
        http.post(base, "/eapi/search/pc/complex/page/v3", body)

    suspend fun subscribePlaylist(body: SubscribePlaylist): BaseResponse =
        http.post(base, "/api/playlist/subscribe", body)

    suspend fun unSubscribePlaylist(body: SubscribePlaylist): BaseResponse =
        http.post(base, "/api/playlist/unsubscribe", body)

    suspend fun getListenDataRealtimeReport(
        body: Map<String, String>,
    ): ListenDataRealtimeResponse =
        http.post(base, "/eapi/content/activity/listen/data/realtime/report", body)

    suspend fun getListenDataReport(
        body: Map<String, String>,
    ): ListenDataReportResponse =
        http.post(base, "/eapi/content/activity/listen/data/report", body)
}
