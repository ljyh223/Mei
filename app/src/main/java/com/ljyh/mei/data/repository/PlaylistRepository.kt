package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.PlaylistDetail
import com.ljyh.mei.data.model.SongUrl
import com.ljyh.mei.data.model.api.BaseMessageResponse
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.api.CreatePlaylist
import com.ljyh.mei.data.model.api.CreatePlaylistResult
import com.ljyh.mei.data.model.api.DeletePlaylist
import com.ljyh.mei.data.model.api.GetPlaylistDetail
import com.ljyh.mei.data.model.api.GetSongUrl
import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.data.model.api.ManipulateTrack
import com.ljyh.mei.data.model.api.ManipulateTrackResult
import com.ljyh.mei.data.model.api.SubscribePlaylist
import com.ljyh.mei.data.model.api.SubscribePlaylistResult
import com.ljyh.mei.data.model.weapi.EveryDaySongs
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistRepository(
    private val apiService: ApiService,
    private val weApiService: WeApiService
) {
    suspend fun getPlaylistDetail(id: String): Resource<PlaylistDetail> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getPlaylistDetail(
                    GetPlaylistDetail(
                        id = id
                    )
                )
            }
        }
    }

    suspend fun getSongUrl(id: String): Resource<SongUrl> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getSongUrl(
                    GetSongUrl(
                        ids = "[$id]"
                    )
                )
            }
        }
    }

    suspend fun getSongUrlV1(id: String): Resource<SongUrl> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getSongUrlV1(
                    GetSongUrlV1(
                        ids = "[$id]"
                    )
                )
            }
        }
    }

    suspend fun manipulateTrack(
        op: String,
        pid: String,
        trackIds: String,
        imme: Boolean = true
    ): Resource<ManipulateTrackResult> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.manipulateTracks(
                    ManipulateTrack(
                        op = op,
                        pid = pid,
                        trackIds = trackIds,
                        imme = imme
                    )
                )
            }
        }
    }


    suspend fun getEveryDayRecommendSongs(): Resource<EveryDaySongs> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                weApiService.getEveryDayRecommendSongs()
            }
        }
    }


    suspend fun createPlaylist(
        name: String,
        privacy: String = "0", // 0 普通歌单, 10 隐私歌单
        type: String = "NORMAL" // 默认 NORMAL, VIDEO 视频歌单, SHARED 共享歌单
    ): Resource<CreatePlaylistResult> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.createPlaylist(
                    CreatePlaylist(
                        name = name,
                        privacy = privacy,
                        type = type
                    )
                )
            }
        }
    }

    suspend fun subscribePlaylist(
        id: String
    ): Resource<BaseResponse> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.subscribePlaylist(
                    SubscribePlaylist(
                        id = id,
                    )
                )
            }
        }
    }

    suspend fun unSubscribePlaylist(
        id: String
    ): Resource<BaseResponse> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.unSubscribePlaylist(
                    SubscribePlaylist(
                        id = id,
                    )
                )
            }
        }
    }
    suspend fun deletePlaylist(
        id: String
    ): Resource<BaseMessageResponse> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.deletePlaylist(
                    DeletePlaylist(
                        ids = "[$id]"
                    )
                )
            }
        }
    }

}