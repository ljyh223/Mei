package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.model.api.GetArtistAlbum
import com.ljyh.mei.data.model.api.GetArtistDetail
import com.ljyh.mei.data.model.api.GetArtistSong
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArtistRepository(private val apiService: ApiService) {
    
    suspend fun getArtistDetail(id: String): Resource<ArtistDetail> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getArtistDetail(GetArtistDetail(id = id))
            }
        }
    }

    suspend fun getArtistAlbums(id: String): Resource<ArtistAlbum> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getArtistAlbums(GetArtistAlbum(), id)
            }
        }
    }

    suspend fun getArtistSongs(id: String): Resource<ArtistSong> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getArtistSongs(GetArtistSong(), id)
            }
        }
    }
}
