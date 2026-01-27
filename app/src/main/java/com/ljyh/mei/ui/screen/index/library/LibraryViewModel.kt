package com.ljyh.mei.ui.screen.index.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.AlbumPhoto
import com.ljyh.mei.data.model.UserAccount
import com.ljyh.mei.data.model.UserAlbumList
import com.ljyh.mei.data.model.UserPlaylist
import com.ljyh.mei.data.model.room.AlbumEntity
import com.ljyh.mei.data.model.room.ArtistEntity
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.weapi.UserSubcount
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.UserRepository
import com.ljyh.mei.di.AlbumsRepository
import com.ljyh.mei.di.PlaylistRepository
import com.ljyh.mei.ui.model.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: UserRepository,
    private val playlistRepository: PlaylistRepository,
    private val albumsRepository: AlbumsRepository
):ViewModel() {
    private val _account = MutableStateFlow<Resource<UserAccount>>(Resource.Loading)
    val account: StateFlow<Resource<UserAccount>> = _account

    private val _photoAlbum=MutableStateFlow<Resource<AlbumPhoto>>(Resource.Loading)
    val photoAlbum:StateFlow<Resource<AlbumPhoto>> = _photoAlbum

    private val _networkPlaylistsState = MutableStateFlow<Resource<UserPlaylist>>(Resource.Loading)
    val networkPlaylistsState: StateFlow<Resource<UserPlaylist>> = _networkPlaylistsState

    private val _albumList = MutableStateFlow<Resource<UserAlbumList>>(Resource.Loading)
    val albumList: StateFlow<Resource<UserAlbumList>> = _albumList

    private val _userSubcount = MutableStateFlow<Resource<UserSubcount>>(Resource.Loading)
    val userSubcount: StateFlow<Resource<UserSubcount>> = _userSubcount

    val localPlaylists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylist()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // 5秒内无订阅者则停止
            initialValue = emptyList() // 初始值为空列表
        )
    fun getUserAccount() {
        if (account.value is Resource.Success) return
        viewModelScope.launch {
            _account.value = Resource.Loading
            _account.value = repository.getUserAccount()
        }
    }

    fun getPhotoAlbum(id:String){
        viewModelScope.launch {
            _photoAlbum.value = Resource.Loading
            _photoAlbum.value = repository.getPhotoAlbum(id)
        }
    }



    fun syncUserPlaylists(uid: String, limit: Int = 100) {
        viewModelScope.launch {
            _networkPlaylistsState.value = Resource.Loading
            when (val networkResult = repository.getUserPlaylist(uid, limit)) {
                is Resource.Success -> {
                    val playlistsToInsert = networkResult.data.playlist.map {
                        Playlist(
                            id = it.id.toString(),
                            title = it.name,
                            cover = it.coverImgUrl,
                            author = it.creator.userId.toString(),
                            authorName = it.creator.nickname,
                            authorAvatar = it.creator.avatarUrl,
                            count = it.trackCount
                        )
                    }
                    playlistRepository.insertPlaylists(playlistsToInsert)
                    _networkPlaylistsState.value = networkResult
                }
                is Resource.Error -> {
                    _networkPlaylistsState.value = networkResult
                }
                Resource.Loading -> { }
            }
        }
    }

    fun getAlbumList(){
        viewModelScope.launch {
            _albumList.value= Resource.Loading
            _albumList.value=repository.getAlbumList()
        }
    }

    fun getUserSubcount(){
        viewModelScope.launch {
            _userSubcount.value= Resource.Loading
            _userSubcount.value= repository.getUsrSubcount()
        }
    }

    fun insertAlbum(album: AlbumEntity, artists: List<ArtistEntity>){
        viewModelScope.launch {
            albumsRepository.insertAlbum(album, artists)
        }
    }


}