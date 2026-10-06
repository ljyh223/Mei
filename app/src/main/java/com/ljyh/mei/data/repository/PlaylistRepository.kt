package com.ljyh.mei.data.repository

import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.constants.checkToken
import com.ljyh.mei.data.model.AlbumContent
import com.ljyh.mei.data.model.FeaturedPlaylistPage
import com.ljyh.mei.data.model.MiniPlaylistDetail
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.SongUrl
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.model.toDomain
import com.ljyh.mei.data.model.api.BaseMessageResponse
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.api.CreatePlaylist
import com.ljyh.mei.data.model.api.CreatePlaylistResult
import com.ljyh.mei.data.model.api.DeletePlaylist
import com.ljyh.mei.data.model.api.GetPlaylistDetail
import com.ljyh.mei.data.model.api.GetSongDetails
import com.ljyh.mei.data.model.api.GetSongUrl
import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.data.model.api.ManipulateTrack
import com.ljyh.mei.data.model.api.ManipulateTrackResult
import com.ljyh.mei.data.model.api.SubscribePlaylist
import com.ljyh.mei.data.model.weapi.HighQualityPlaylist
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistRepository(
    private val apiService: ApiService,
    private val weApiService: WeApiService,
    private val eApiService: EApiService
) {
    suspend fun getPlaylistDetail(id: String): Resource<MiniPlaylistDetail> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getPlaylistDetail(
                    GetPlaylistDetail(
                        id = id
                    )
                ).toDomain()
            }
        }
    }

    suspend fun getSongDetails(ids: List<String>): List<MediaMetadata> =
        apiService.getSongDetail(
            GetSongDetails(ids.joinToString(","))
        ).songs.map { it.toMediaMetadata() }

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

    suspend fun getSongUrlV1(ids: List<String>, quality: MusicQuality): Resource<SongUrl> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getSongUrlV1(
                    GetSongUrlV1(
                        ids = "[${ids.joinToString(",")}]",
                        level = quality.text
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


    suspend fun getEveryDayRecommendSongs(): Resource<List<MediaMetadata>> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                weApiService.getEveryDayRecommendSongs()
                    .data.dailySongs
                    .map { it.toMediaMetadata() }
            }
        }
    }


    suspend fun createPlaylist(
        name: String,
        privacy: Boolean, // 0 普通歌单, 10 隐私歌单
        type: String = "NORMAL" // 默认 NORMAL, VIDEO 视频歌单, SHARED 共享歌单
    ): Resource<CreatePlaylistResult> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.createPlaylist(
                    CreatePlaylist(
                        name = name,
                        privacy = if (privacy) "10" else "0",
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
                eApiService.subscribePlaylist(
                    SubscribePlaylist(
                        id = id,
                        checkToken = checkToken
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
                eApiService.unSubscribePlaylist(
                    SubscribePlaylist(
                        id = id,
                        checkToken = checkToken
                    )
                )
            }
        }
    }


    suspend fun subscribeAlbum(id: String): Resource<BaseResponse> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.subscribeAlbum(
                    SubscribePlaylist(
                        id = id,
                    )
                )
            }
        }
    }


    suspend fun unsubscribeAlbum(id: String): Resource<BaseResponse> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.unsubscribeAlbum(
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


    suspend fun getAlbumDetail(id: String): Resource<AlbumContent> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getAlbumDetail(
                    id = id
                ).toDomain()
            }
        }
    }

    suspend fun getHighQualityPlaylist(cat:String, limit:Int): Resource<FeaturedPlaylistPage>{
        return withContext(Dispatchers.IO){
            safeApiCall {
                weApiService.getHighQualityPlaylist(
                    HighQualityPlaylist(
                        category = cat,
                        limit = limit
                    )
                ).toDomain()
            }
        }
    }


}
