package com.ljyh.mei.ui.screen.index.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.AlbumPhoto
import com.ljyh.mei.data.model.UserAccount
import com.ljyh.mei.data.model.UserPlaylist
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.UserRepository
import com.ljyh.mei.di.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: UserRepository,
    private val playlistRepository: PlaylistRepository
):ViewModel() {
    private val _account = MutableStateFlow<Resource<UserAccount>>(Resource.Loading)
    val account: StateFlow<Resource<UserAccount>> = _account


    fun getUserAccount() {
        if (account.value is Resource.Success) return
        viewModelScope.launch {
            _account.value = Resource.Loading
            _account.value = repository.getUserAccount()
        }
    }


    private val _playlists=MutableStateFlow<Resource<UserPlaylist>>(Resource.Loading)
    val playlists:StateFlow<Resource<UserPlaylist>> = _playlists
    fun getUserPlaylist(uid:String){
        viewModelScope.launch {
            _playlists.value = Resource.Loading
            _playlists.value = repository.getUserPlaylist(uid)
        }
    }

    private val _photoAlbum=MutableStateFlow<Resource<AlbumPhoto>>(Resource.Loading)
    val photoAlbum:StateFlow<Resource<AlbumPhoto>> = _photoAlbum

    fun getPhotoAlbum(id:String){
        viewModelScope.launch {
            _photoAlbum.value = Resource.Loading
            _photoAlbum.value = repository.getPhotoAlbum(id)
        }
    }


    fun insertPlaylist(playlist: List<Playlist>) {
        viewModelScope.launch {
            playlistRepository.insertPlaylists(playlist)
        }
    }
    val allPlaylist = playlistRepository.getAllPlaylist()
}