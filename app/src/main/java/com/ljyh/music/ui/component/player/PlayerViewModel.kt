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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerRepository,
    private val qqSongRepository: QQSongRepository
): ViewModel(){
    private val _searchResult = MutableStateFlow<Resource<SearchResult>>(Resource.Loading)
    val searchResult: StateFlow<Resource<SearchResult>> = _searchResult

    private val _lyricResult=MutableStateFlow<Resource<LyricResult>>(Resource.Loading)
    val lyricResult: StateFlow<Resource<LyricResult>> = _lyricResult

    private val _lyric = MutableStateFlow<Resource<Lyric>>(Resource.Loading)
    val lyric: StateFlow<Resource<Lyric>> = _lyric

    fun clear(){
        _searchResult.value = Resource.Loading
        _lyricResult.value = Resource.Loading
        _lyric.value = Resource.Loading
    }
    private val _qqSong = MutableStateFlow<QQSong?>(null)
    val qqSong: StateFlow<QQSong?> = _qqSong
    fun searchNew(keyword:String){
        viewModelScope.launch {
            _searchResult.value = Resource.Loading
            _searchResult.value = repository.searchNew(keyword)
        }
    }


    fun getLyricNew(title:String, album:String, artist:String, duration:Int,id:Int){
        viewModelScope.launch {
            _lyricResult.value = Resource.Loading
            _lyricResult.value = repository.getLyricNew(title, album, artist, duration, id)
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

    fun fetchQQSong(id: String) {
        viewModelScope.launch {
            qqSongRepository.getQQSong(id)
                .catch { e ->
                    // 处理错误
                    println("Error fetching song: ${e.message}")
                }
                .collect { song ->
                    _qqSong.value = song
                }
        }
    }

    fun insertSong(song: QQSong) {
        viewModelScope.launch {
            qqSongRepository.insertSong(song)
        }
    }

}