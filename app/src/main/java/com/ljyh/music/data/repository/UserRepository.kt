package com.ljyh.music.data.repository

import com.ljyh.music.data.model.AlbumPhoto
import com.ljyh.music.data.model.UserAccount
import com.ljyh.music.data.model.UserPlaylist
import com.ljyh.music.data.model.api.GetUserPhotoAlbum
import com.ljyh.music.data.model.api.GetUserPlaylist
import com.ljyh.music.data.network.api.ApiService
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.network.api.EApiService
import com.ljyh.music.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class UserRepository(private val apiService: ApiService,private val eApiService: EApiService) {
    suspend fun getUserAccount(): Resource<UserAccount> {
        return withContext(Dispatchers.IO) {
            safeApiCall { apiService.getAccountDetail() }
        }
    }

    suspend fun getUserPlaylist(uid: String): Resource<UserPlaylist> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getUserPlaylist(
                    GetUserPlaylist(
                        uid = uid
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
}