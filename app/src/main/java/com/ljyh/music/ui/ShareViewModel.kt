package com.ljyh.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.music.data.model.HomePageResourceShow
import com.ljyh.music.data.model.Lyric
import com.ljyh.music.data.model.qq.u.Search
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
) : ViewModel() {

    private val _lyric = MutableStateFlow<Resource<Lyric>>(Resource.Loading)
    val lyric: StateFlow<Resource<Lyric>> = _lyric


    private val _qLyric = MutableStateFlow<Resource<String>>(Resource.Loading)
    val qLyric: StateFlow<Resource<String>> = _qLyric


    private val _searchLyric = MutableStateFlow<Resource<String>>(Resource.Loading)
    val searchLyric: StateFlow<Resource<String>> = _searchLyric


    private val _searchU = MutableStateFlow<Resource<Search>>(Resource.Loading)
    val searchU: StateFlow<Resource<Search>> = _searchU

    private val _searchC = MutableStateFlow<Resource<com.ljyh.music.data.model.qq.c.Search>>(Resource.Loading)
    val searchC: StateFlow<Resource<com.ljyh.music.data.model.qq.c.Search>> = _searchC

    fun getLyric(id: String) {

        viewModelScope.launch {
            _lyric.value = Resource.Loading
            _lyric.value = repository.getLyric(id)
        }
    }


    fun getQQMusicLyric(id: String) {
        viewModelScope.launch {
            _qLyric.value = Resource.Loading
            _qLyric.value = repository.getQQMusicLyric(id)
        }

    }


    fun searchLyric(songName: String, singerName: String) {
        viewModelScope.launch {
            _searchLyric.value = Resource.Loading
            _searchLyric.value = repository.searchLyric(songName, singerName)
        }
    }


    fun searchU(keyword:String){
        viewModelScope.launch {
            _searchU.value = Resource.Loading
            _searchU.value = repository.searchU(keyword)
        }
    }

    fun searchC(keyword: String){
        viewModelScope.launch {
            _searchC.value = Resource.Loading
            _searchC.value = repository.searchC(keyword)
        }
    }
}