package com.ljyh.mei.data.repository

import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.constants.checkToken
import com.ljyh.mei.data.model.domain.AlbumContent
import com.ljyh.mei.data.model.domain.FeaturedPlaylistPage
import com.ljyh.mei.data.model.response.MiniPlaylistDetail
import com.ljyh.mei.data.model.response.AlbumDynamic
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.response.SongUrl
import com.ljyh.mei.data.model.domain.toMediaMetadata
import com.ljyh.mei.data.model.domain.toDomain
import com.ljyh.mei.data.model.response.toDomain
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
import com.ljyh.mei.data.model.api.UpdatePlaylistTrackOrder
import com.ljyh.mei.data.model.api.SubscribePlaylist
import com.ljyh.mei.data.model.weapi.HighQualityPlaylist
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.network.safeApiCall
import com.ljyh.mei.di.repository.LocalPlaylistRepository
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistRepository(
    private val apiService: ApiService,
    private val weApiService: WeApiService,
    private val eApiService: EApiService,
    private val localPlaylists: LocalPlaylistRepository,
) {
    suspend fun getAlbumDynamic(id: Long): Resource<AlbumDynamic> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = weApiService.getAlbumDynamic(id)
            check(response.code == 200 && response.isSubscribed != null) {
                "专辑收藏状态请求失败 (${response.code})"
            }
            response
        }
    }

    suspend fun getPlaylistName(id: String): Resource<String> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = apiService.getPlaylistName(id)
            val playlist = response.playlist
            check(response.code == 200 && playlist != null && playlist.id.toString() == id && playlist.name.isNotBlank())
            requireNotNull(playlist).name
        }
    }

    suspend fun getPlaylistDetail(id: String): Resource<MiniPlaylistDetail> {
        return withContext(Dispatchers.IO) {
            val revision = localPlaylists.playlistSyncRevision()
            val result = safeApiCall {
                val response = apiService.getPlaylistDetail(GetPlaylistDetail(id = id))
                check(response.code == 200 && response.playlist.Id.toString() == id)
                response.toDomain()
            }
            if (result is Resource.Success) updateCachedPlaylistSummary {
                localPlaylists.updatePlaylistSummary(id, result.data.count, result.data.coverUrl, revision)
            }
            result
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
            val result = safeApiCall {
                apiService.manipulateTracks(ManipulateTrack(op, pid, trackIds, imme))
            }
            if (result is Resource.Success && result.data.code == 200) updateCachedPlaylistSummary {
                val revision = localPlaylists.recordTrackMutation(pid, result.data.count)
                // An absent row will be populated by the normal library sync.
                if (localPlaylists.getPlaylist(pid) != null) {
                    val response = apiService.getPlaylistSummary(pid)
                    val summary = response.playlist
                    if (response.code == 200 && summary?.id?.toString() == pid &&
                        (result.data.count == null || summary.trackCount == result.data.count)) {
                        localPlaylists.updatePlaylistSummary(pid, summary.trackCount, summary.coverImgUrl, revision)
                    }
                }
            }
            result
        }
    }

    private suspend fun updateCachedPlaylistSummary(update: suspend () -> Unit) {
        try {
            update()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // A readback or cache failure must not turn a completed server mutation into a failure.
            Timber.tag("PlaylistSync").w(error, "Could not refresh cached playlist summary")
        }
    }

    suspend fun updatePlaylistTrackOrder(pid: String, ids: List<Long>): Resource<BaseResponse> =
        withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.updatePlaylistTrackOrder(UpdatePlaylistTrackOrder(pid, ids))
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
                apiService.subscribePlaylist(id)
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
