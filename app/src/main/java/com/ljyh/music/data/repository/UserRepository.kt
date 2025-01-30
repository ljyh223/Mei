package com.ljyh.music.data.repository

import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.data.model.UserAccount
import com.ljyh.music.data.model.UserPlaylist
import com.ljyh.music.data.network.ApiService
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class UserRepository(private val apiService: ApiService) {
    suspend fun getUserAccount(): Resource<UserAccount> {
        return withContext(Dispatchers.IO) {
            safeApiCall { apiService.getUserAccount() }
        }
    }

    suspend fun getUserPlaylist(uid:String): Resource<UserPlaylist> {
        return withContext(Dispatchers.IO) {
            safeApiCall { apiService.getUserPlaylist(uid) }
        }
    }
}