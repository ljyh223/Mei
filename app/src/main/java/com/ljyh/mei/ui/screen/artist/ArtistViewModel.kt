package com.ljyh.mei.ui.screen.artist

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.ArtistRepository
import com.ljyh.mei.di.repository.ColorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repository: ArtistRepository,
    private val colorRepository: ColorRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _artistDetail = MutableStateFlow<Resource<ArtistDetail>>(Resource.Loading)
    val artistDetail: StateFlow<Resource<ArtistDetail>> = _artistDetail

    private val _artistAlbums = MutableStateFlow<Resource<ArtistAlbum>>(Resource.Loading)
    val artistAlbums: StateFlow<Resource<ArtistAlbum>> = _artistAlbums

    private val _artistSongs = MutableStateFlow<Resource<ArtistSong>>(Resource.Loading)
    val artistSongs: StateFlow<Resource<ArtistSong>> = _artistSongs

    fun cachedArtistColor(url: String): Color? = colorRepository.getFromMemory(url)

    suspend fun artistColor(url: String): Color = colorRepository.getColorOrExtract(context, url)

    fun getArtistDetail(id: String) {
        viewModelScope.launch {
            _artistDetail.value = repository.getArtistDetail(id)
        }
    }

    fun getArtistAlbums(id: String) {
        viewModelScope.launch {
            _artistAlbums.value = repository.getArtistAlbums(id)
        }
    }

    fun getArtistSongs(id: String) {
        viewModelScope.launch {
            _artistSongs.value = repository.getArtistSongs(id)
        }
    }
}
