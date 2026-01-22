package com.ljyh.mei.ui.screen.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.AlbumDetail
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: PlaylistRepository
): ViewModel() {
    private val _albumDetail = MutableStateFlow<Resource<AlbumDetail>>(Resource.Loading)
    val albumDetail: StateFlow<Resource<AlbumDetail>> = _albumDetail

    private val _subscribeAlbum = MutableStateFlow<Resource<BaseResponse>>(Resource.Loading)
    val subscribeAlbum: StateFlow<Resource<BaseResponse>> = _subscribeAlbum

    private val _unSubscribeAlbum = MutableStateFlow<Resource<BaseResponse>>(Resource.Loading)
    val unSubscribeAlbum: StateFlow<Resource<BaseResponse>> = _unSubscribeAlbum


    fun getAlbumDetail(id: String) {
        viewModelScope.launch {
            _albumDetail.value = Resource.Loading
            _albumDetail.value = repository.getAlbumDetail(id)
        }
    }

    fun subscribeAlbum(id: String) {
        viewModelScope.launch {
            _subscribeAlbum.value = Resource.Loading
            _subscribeAlbum.value = repository.subscribeAlbum(id)
        }
    }

    fun unSubscribeAlbum(id: String) {
        viewModelScope.launch {
            _unSubscribeAlbum.value = Resource.Loading
            _unSubscribeAlbum.value = repository.unsubscribeAlbum(id)
        }
    }

}