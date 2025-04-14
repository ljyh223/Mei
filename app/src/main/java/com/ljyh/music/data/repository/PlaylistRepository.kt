package com.ljyh.music.data.repository

import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.data.model.SongUrl
import com.ljyh.music.data.model.api.GetPlaylistDetail
import com.ljyh.music.data.model.api.GetSongUrl
import com.ljyh.music.data.model.api.GetSongUrlV1
import com.ljyh.music.data.model.api.ManipulateTrack
import com.ljyh.music.data.model.api.ManipulateTrackResult
import com.ljyh.music.data.model.weapi.EveryDaySongs
import com.ljyh.music.data.network.api.ApiService
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.network.api.WeApiService
import com.ljyh.music.data.network.safeApiCall
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


}