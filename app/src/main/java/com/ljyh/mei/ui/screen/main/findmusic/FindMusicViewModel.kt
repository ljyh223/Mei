package com.ljyh.mei.ui.screen.main.findmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.domain.*
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.DiscoverDataSource
import com.ljyh.mei.data.repository.DiscoverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FindMusicViewModel internal constructor(
    private val source: DiscoverDataSource,
    private val scopeOverride: CoroutineScope?,
) : ViewModel() {
    @Inject constructor(repository: DiscoverRepository) : this(repository, null)
    private val requestScope get() = scopeOverride ?: viewModelScope
    private val _categories = MutableStateFlow<Resource<List<DiscoverCategory>>>(Resource.Loading)
    val categories = _categories.asStateFlow()
    private val _hotCategories = MutableStateFlow<Resource<List<String>>>(Resource.Loading)
    val hotCategories = _hotCategories.asStateFlow()
    private val _featured = MutableStateFlow<Resource<List<DiscoverPlaylist>>>(Resource.Loading)
    val featured = _featured.asStateFlow()
    private val _albums = MutableStateFlow<Resource<List<DiscoverAlbum>>>(Resource.Loading)
    val albums = _albums.asStateFlow()
    private var categoriesJob: Job? = null
    private var hotJob: Job? = null
    private var featuredJob: Job? = null
    private var albumsJob: Job? = null

    init {
        loadCategories()
        loadHotCategories()
        loadFeatured()
        loadAlbums()
    }

    fun loadCategories() {
        categoriesJob?.cancel()
        categoriesJob = requestScope.launch {
            _categories.value = Resource.Loading
            _categories.value = source.categories()
        }
    }
    fun loadHotCategories() {
        hotJob?.cancel()
        hotJob = requestScope.launch { _hotCategories.value = source.hotCategories() }
    }
    fun loadFeatured() {
        featuredJob?.cancel()
        featuredJob = requestScope.launch {
            _featured.value = Resource.Loading
            when (val result = source.playlists(DiscoverPlaylistQuery(), 0)) {
                is Resource.Success -> _featured.value = Resource.Success(result.data.items)
                is Resource.Error -> _featured.value = result
                Resource.Loading -> Unit
            }
        }
    }
    fun loadAlbums() {
        albumsJob?.cancel()
        albumsJob = requestScope.launch {
            _albums.value = Resource.Loading
            _albums.value = source.albums()
        }
    }

}
