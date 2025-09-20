package com.ljyh.mei.ui.screen.index.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.AppContext
import com.ljyh.mei.data.model.HomePageResourceShow
import com.ljyh.mei.data.model.room.Color
import com.ljyh.mei.data.model.weapi.GetHomePageResourceShow
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.HomeRepository
import com.ljyh.mei.di.ColorRepository
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


    fun homePageResourceShow(refresh: Boolean = false) {
        viewModelScope.launch {
            // 如果不是刷新且有成功数据，则不加载
            val currentData = _homePageResourceShow.value
            if (!refresh && currentData is Resource.Success) return@launch

            _homePageResourceShow.value = Resource.Loading
            _homePageResourceShow.value = repository.getHomePageResourceShow(refresh)
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
