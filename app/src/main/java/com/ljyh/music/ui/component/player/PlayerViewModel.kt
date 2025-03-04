package com.ljyh.music.ui.component.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.music.data.model.Lyric
import com.ljyh.music.data.model.qq.u.LyricResult
import com.ljyh.music.data.model.qq.u.SearchResult
import com.ljyh.music.data.model.room.QQSong
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.repository.PlayerRepository
import com.ljyh.music.di.QQSongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerRepository,
    private val qqSongRepository: QQSongRepository
): ViewModel(){
    private val _searchNew = MutableStateFlow<Resource<SearchResult>>(Resource.Loading)
    val searchNew: StateFlow<Resource<SearchResult>> = _searchNew

    private val _lyricNew=MutableStateFlow<Resource<LyricResult>>(Resource.Loading)
    val lyricNew: StateFlow<Resource<LyricResult>> = _lyricNew

    private val _lyric = MutableStateFlow<Resource<Lyric>>(Resource.Loading)
    val lyric: StateFlow<Resource<Lyric>> = _lyric

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

    fun getQQSong(id: String) {
        viewModelScope.launch {
            qqSongRepository.getQQSong(id)
        }
    }

    fun insertSong(song: QQSong) {
        viewModelScope.launch {
            qqSongRepository.insertSong(song)
        }
    }

}