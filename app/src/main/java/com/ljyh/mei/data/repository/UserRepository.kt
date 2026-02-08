package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.AlbumPhoto
import com.ljyh.mei.data.model.UserAccount
import com.ljyh.mei.data.model.UserAlbumList
import com.ljyh.mei.data.model.UserPlaylist
import com.ljyh.mei.data.model.api.GetAlbumList
import com.ljyh.mei.data.model.api.GetUserPhotoAlbum
import com.ljyh.mei.data.model.api.GetUserPlaylist
import com.ljyh.mei.data.model.weapi.UserSubcount
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class UserRepository(private val apiService: ApiService,private val eApiService: EApiService, private val weApiService: WeApiService) {
    suspend fun getUserAccount(): Resource<UserAccount> {
        return withContext(Dispatchers.IO) {
            safeApiCall { apiService.getAccountDetail() }
        }
    }

    suspend fun getUserPlaylist(uid: String, limit: Int): Resource<UserPlaylist> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getUserPlaylist(
                    GetUserPlaylist(
                        uid = uid,
                        limit = limit.toString()
                    )
                )
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
}