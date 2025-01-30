package com.ljyh.music.ui.screen.index.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.music.constants.UserIdKey
import com.ljyh.music.data.model.HomePage
import com.ljyh.music.data.model.UserAccount
import com.ljyh.music.data.model.UserPlaylist
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.repository.HomeRepository
import com.ljyh.music.data.repository.UserRepository
import com.ljyh.music.utils.dataStore
import com.ljyh.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: UserRepository,
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
}