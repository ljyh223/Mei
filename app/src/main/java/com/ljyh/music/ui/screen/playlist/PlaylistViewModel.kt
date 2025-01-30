package com.ljyh.music.ui.screen.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.data.model.SongUrl
import com.ljyh.music.data.network.ApiService
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    val apiService: ApiService
) : ViewModel() {

    private val _playlistDetail = MutableStateFlow<Resource<PlaylistDetail>>(Resource.Loading)
    val playlistDetail: StateFlow<Resource<PlaylistDetail>> = _playlistDetail


    private val _songUrl= MutableStateFlow<Resource<SongUrl>>(Resource.Loading)
    val songUrl: StateFlow<Resource<SongUrl>> = _songUrl

    fun getSongUrl(id: String) {
        viewModelScope.launch {
            _songUrl.value = Resource.Loading
            _songUrl.value = repository.getSongUrl(id)
        }
    }

    fun getPlaylistDetail(id: String) {
        viewModelScope.launch {
            _playlistDetail.value = Resource.Loading
            _playlistDetail.value = repository.getPlaylistDetail(id)
        }
    }

    fun getPlaylistTracks(id: String): Flow<PagingData<PlaylistDetail.Playlist.Track>> =
        repository.getPlaylistTracks(id).cachedIn(viewModelScope)





}

