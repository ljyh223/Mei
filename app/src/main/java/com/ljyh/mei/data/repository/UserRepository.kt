package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.AlbumPhoto
import com.ljyh.mei.data.model.UserAccountSummary
import com.ljyh.mei.data.model.UserDetail
import com.ljyh.mei.data.model.UserVipInfo
import com.ljyh.mei.data.model.UserAlbumList
import com.ljyh.mei.data.model.UserPlaylistPage
import com.ljyh.mei.data.model.toDomain
import com.ljyh.mei.data.model.ListenDataRealtimeResponse
import com.ljyh.mei.data.model.ListenDataReportResponse
import com.ljyh.mei.data.model.api.GetAlbumList
import com.ljyh.mei.data.model.api.GetUserPhotoAlbum
import com.ljyh.mei.data.model.api.GetUserPlaylist
import com.ljyh.mei.data.model.auth.QrLoginKeyResponse
import com.ljyh.mei.data.model.weapi.UserSubcount
import com.ljyh.mei.data.network.QrLoginClient
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class UserRepository(
    private val apiService: ApiService,
    private val eApiService: EApiService,
    private val weApiService: WeApiService,
    private val qrLoginClient: QrLoginClient,
) {
    suspend fun createQrLoginKey(): Resource<QrLoginKeyResponse> = withContext(Dispatchers.IO) {
        safeApiCall { qrLoginClient.createQrLoginKey() }
    }

    suspend fun checkQrLogin(unikey: String) = withContext(Dispatchers.IO) {
        safeApiCall { qrLoginClient.checkQrLogin(unikey) }
    }

    suspend fun getUserAccount(): Resource<UserAccountSummary> {
        return withContext(Dispatchers.IO) {
            safeApiCall { apiService.getAccountDetail().toDomain() }
        }
    }

    suspend fun getUserDetail(uid: String): Resource<UserDetail> = withContext(Dispatchers.IO) {
        safeApiCall { apiService.getUserDetail(uid) }
    }

    suspend fun getUserVipInfo(uid: String): Resource<UserVipInfo> = withContext(Dispatchers.IO) {
        safeApiCall { weApiService.getUserVipInfo(mapOf("userId" to uid)) }
    }

    suspend fun getUserPlaylist(uid: String, limit: Int): Resource<UserPlaylistPage> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getUserPlaylist(
                    GetUserPlaylist(
                        uid = uid,
                        limit = limit.toString()
                    )
                ).toDomain()
            }
        }
    }
    suspend fun getPhotoAlbum(id: String): Resource<AlbumPhoto> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getUserPhotoAlbum(
                    GetUserPhotoAlbum(
                        userId = id
                    )
                )
            }
        }
    }

    suspend fun getAlbumList(): Resource<UserAlbumList> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getCollectAlbumList(
                    GetAlbumList()
                )
            }
        }
    }

    suspend fun getUsrSubcount(): Resource<UserSubcount> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                weApiService.getUserSubcount()
            }
        }
    }

    suspend fun getListenDataRealtimeReport(type: String): Resource<ListenDataRealtimeResponse> =
        withContext(Dispatchers.IO) {
            safeApiCall {
                eApiService.getListenDataRealtimeReport(mapOf("type" to type))
            }
        }

    suspend fun getListenDataReport(
        type: String,
        endTime: Long? = null,
    ): Resource<ListenDataReportResponse> = withContext(Dispatchers.IO) {
        safeApiCall {
            eApiService.getListenDataReport(
                buildMap {
                    put("type", type)
                    endTime?.let { put("endTime", it.toString()) }
                },
            )
        }
    }
}
