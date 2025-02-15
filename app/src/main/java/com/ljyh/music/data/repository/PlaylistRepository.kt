package com.ljyh.music.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.data.model.SongUrl
import com.ljyh.music.data.model.TrackAll
import com.ljyh.music.data.network.ApiService
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.network.safeApiCall
import com.ljyh.music.ui.screen.playlist.PlaylistTrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PlaylistRepository(private val apiService: ApiService) {
    suspend fun getPlaylistDetail(id:String): Resource<PlaylistDetail> {
        return withContext(Dispatchers.IO) {
            safeApiCall { apiService.getPlaylistDetail(id) }
        }
    }

    suspend fun getSongUrl(id:String): Resource<SongUrl> {
        return withContext(Dispatchers.IO) {
            safeApiCall { apiService.getSongUrl(id) }
        }
    }

    suspend fun getSongUrlV1(id:String): Resource<SongUrl> {
        return withContext(Dispatchers.IO) {
            safeApiCall { apiService.getSongUrlV1(id) }
        }
    }


}