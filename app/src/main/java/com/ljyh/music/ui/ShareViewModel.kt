package com.ljyh.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.music.data.model.HomePageResourceShow
import com.ljyh.music.data.model.Lyric
import com.ljyh.music.data.model.UserAccount
import com.ljyh.music.data.model.qq.u.LyricResult
import com.ljyh.music.data.model.qq.u.Search
import com.ljyh.music.data.model.qq.u.SearchResult
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.repository.ShareRepository
import com.ljyh.music.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.Thread.State
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val repository: ShareRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _lyric = MutableStateFlow<Resource<Lyric>>(Resource.Loading)
    val lyric: StateFlow<Resource<Lyric>> = _lyric


    val _qLyric = MutableStateFlow<Resource<String>>(Resource.Loading)
    val qLyric: StateFlow<Resource<String>> = _qLyric


    private val _searchU = MutableStateFlow<Resource<Search>>(Resource.Loading)
    val searchU: StateFlow<Resource<Search>> = _searchU

    private val _searchNew = MutableStateFlow<Resource<SearchResult>>(Resource.Loading)
    val searchNew: StateFlow<Resource<SearchResult>> = _searchNew

    private val _lyricNew=MutableStateFlow<Resource<LyricResult>>(Resource.Loading)
    val lyricNew: StateFlow<Resource<LyricResult>> = _lyricNew


    private val _userAccount = MutableStateFlow<Resource<UserAccount>>(Resource.Loading)
    val userAccount: StateFlow<Resource<UserAccount>> = _userAccount
    fun getLyric(id: String) {
        viewModelScope.launch {
            _lyric.value = Resource.Loading
            _lyric.value = repository.getLyric(id)
        }
    }


    fun getLyricV1(id: String) {
        viewModelScope.launch {
            _lyric.value = Resource.Loading
            _lyric.value = repository.getLyricV1(id)
        }
    }


    fun getQQMusicLyric(id: String) {
        viewModelScope.launch {
            _qLyric.value = Resource.Loading
            _qLyric.value = repository.getQQMusicLyric(id)
        }

    }


    fun searchU(keyword:String){
        viewModelScope.launch {
            _searchU.value = Resource.Loading
            _searchU.value = repository.searchU(keyword)
        }
    }

    fun searchNew(keyword:String){
        viewModelScope.launch {
            _searchNew.value = Resource.Loading
            _searchNew.value = repository.searchNew(keyword)
        }
    }


    fun getLyricNew(title:String, album:String, artist:String, duration:Int,id:Int){
        viewModelScope.launch {
            _lyricNew.value = Resource.Loading
            _lyricNew.value = repository.getLyricNew(title, album, artist, duration, id)
        }
    }

    fun getUserAccount(){
        viewModelScope.launch {
            _userAccount.value = Resource.Loading
            _userAccount.value = userRepository.getUserAccount()
        }
    }

}