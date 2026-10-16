package com.ljyh.mei.ui.screen.album

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.data.model.domain.AlbumContent
import com.ljyh.mei.data.model.domain.AlbumOverview
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.room.AlbumEntity
import com.ljyh.mei.data.model.room.ArtistEntity
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.di.repository.AlbumsRepository
import com.ljyh.mei.di.repository.ColorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val albumsRepository: AlbumsRepository,
    private val colorRepository: ColorRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val _albumDetail = MutableStateFlow<Resource<AlbumContent>>(Resource.Loading)
    val albumDetail: StateFlow<Resource<AlbumContent>> = _albumDetail

    private val _subscribeAlbum = MutableStateFlow<Resource<BaseResponse>>(Resource.Loading)
    val subscribeAlbum: StateFlow<Resource<BaseResponse>> = _subscribeAlbum

    private val _unSubscribeAlbum = MutableStateFlow<Resource<BaseResponse>>(Resource.Loading)
    val unSubscribeAlbum: StateFlow<Resource<BaseResponse>> = _unSubscribeAlbum

    private val _isSubscribe = MutableStateFlow(false)
    val isSubscribe: StateFlow<Boolean> = _isSubscribe


    fun getAlbumDetail(id: String) {
        viewModelScope.launch {
            _albumDetail.value = Resource.Loading
            _albumDetail.value = repository.getAlbumDetail(id)
        }
    }

    fun subscribeAlbum(album: AlbumOverview) {
        viewModelScope.launch {
            _subscribeAlbum.value = Resource.Loading
            val result = repository.subscribeAlbum(album.id.toString())
            if (result is Resource.Success && result.data.code == 200) {
                albumsRepository.insertAlbum(
                    AlbumEntity(album.id, album.name, album.picUrl, album.publishTime, album.size),
                    album.artistIds.zip(album.artistNames).mapNotNull { (id, name) ->
                        id.takeIf { it > 0 }?.let { ArtistEntity(it, name, null) }
                    },
                )
                _isSubscribe.value = true
            }
            _subscribeAlbum.value = result
        }
    }

    fun unSubscribeAlbum(id: String) {
        viewModelScope.launch {
            _unSubscribeAlbum.value = Resource.Loading
            val result = repository.unsubscribeAlbum(id)
            if (result is Resource.Success && result.data.code == 200) {
                albumsRepository.deleteAlbum(id.toLong())
                _isSubscribe.value = false
            }
            _unSubscribeAlbum.value = result
        }
    }


    fun isSubscribe(id: Long) {
        viewModelScope.launch {
            _isSubscribe.value = albumsRepository.existsAlbum(id)
        }
    }

    fun cachedCoverColor(url: String): Color? = colorRepository.getFromMemory(url)

    suspend fun coverColor(url: String): Color? =
        url.takeIf { it.isNotBlank() }?.let { colorRepository.getColorOrExtract(context, it) }

    suspend fun resolveSongUrls(ids: List<String>, quality: MusicQuality) =
        repository.getSongUrlV1(ids, quality)

}
