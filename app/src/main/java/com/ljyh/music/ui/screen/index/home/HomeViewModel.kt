package com.ljyh.music.ui.screen.index.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.music.AppContext
import com.ljyh.music.data.model.HomePageResourceShow
import com.ljyh.music.data.model.room.Color
import com.ljyh.music.data.model.weapi.GetHomePageResourceShow
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.repository.HomeRepository
import com.ljyh.music.di.ColorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val colorRepository: ColorRepository
) : ViewModel() {
    val context= AppContext.instance

    private val _homePageResourceShow =
        MutableStateFlow<Resource<List<HomePageResourceShow.Data.Block>>>(Resource.Loading)
    val homePageResourceShow: StateFlow<Resource<List<HomePageResourceShow.Data.Block>>> = _homePageResourceShow



    init {
        homePageResourceShow()
    }

    fun homePageResourceShow(refresh:Boolean=false) {
        viewModelScope.launch {
            _homePageResourceShow.value = Resource.Loading
            _homePageResourceShow.value =
                repository.getHomePageResourceShow(refresh)
        }
    }

    fun getColors(url: String): Color? {
        return colorRepository.getColor(url)
    }

    fun addColor(color: Color) {
        viewModelScope.launch {
            colorRepository.insertColor(color)
        }
    }





}
