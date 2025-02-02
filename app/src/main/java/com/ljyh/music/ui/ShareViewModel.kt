package com.ljyh.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.music.data.model.HomePageResourceShow
import com.ljyh.music.data.model.Lyric
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.repository.ShareRepository
import com.ljyh.music.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val repository: ShareRepository,
): ViewModel(){

    private val _lyric = MutableStateFlow<Resource<Lyric>>(Resource.Loading)
    val lyric: StateFlow<Resource<Lyric>> = _lyric

    fun getLyric(id:String){

        viewModelScope.launch {
            _lyric.value = Resource.Loading
            _lyric.value = repository.getLyric(id)
        }

    }
}