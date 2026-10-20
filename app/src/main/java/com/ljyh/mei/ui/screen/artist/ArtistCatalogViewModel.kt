package com.ljyh.mei.ui.screen.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistCatalogSearchState<T>(
    val query: String = "",
    val matches: List<T> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ArtistCatalogViewModel @Inject constructor(
    private val repository: ArtistRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId: String = checkNotNull(savedStateHandle["id"])
    private val cachedSongs = mutableListOf<ArtistSong.HotSong>()
    private val cachedAlbums = mutableListOf<ArtistAlbum.HotAlbum>()
    private var allSongsLoaded = false
    private var allAlbumsLoaded = false
    private var songSearchJob: Job? = null
    private var albumSearchJob: Job? = null

    private val _songSearchState = MutableStateFlow(ArtistCatalogSearchState<ArtistSong.HotSong>())
    val songSearchState: StateFlow<ArtistCatalogSearchState<ArtistSong.HotSong>> = _songSearchState

    private val _albumSearchState = MutableStateFlow(ArtistCatalogSearchState<ArtistAlbum.HotAlbum>())
    val albumSearchState: StateFlow<ArtistCatalogSearchState<ArtistAlbum.HotAlbum>> = _albumSearchState

    val songs = Pager(PagingConfig(pageSize = 50, initialLoadSize = 50, enablePlaceholders = false)) {
        ArtistSongsPagingSource(repository, artistId)
    }.flow.cachedIn(viewModelScope)

    val albums = Pager(PagingConfig(pageSize = 30, initialLoadSize = 30, enablePlaceholders = false)) {
        ArtistAlbumsPagingSource(repository, artistId)
    }.flow.cachedIn(viewModelScope)

    fun searchSongs(rawQuery: String) {
        val query = rawQuery.trim()
        songSearchJob?.cancel()
        if (query.isEmpty()) {
            _songSearchState.value = ArtistCatalogSearchState()
            return
        }
        fun matches() = cachedSongs.filter { song ->
            song.name.contains(query, ignoreCase = true) ||
                song.album.name.contains(query, ignoreCase = true)
        }
        _songSearchState.value = ArtistCatalogSearchState(query, matches(), !allSongsLoaded)
        if (allSongsLoaded) return
        songSearchJob = viewModelScope.launch {
            delay(250)
            while (!allSongsLoaded) {
                when (val result = repository.getArtistSongsPage(artistId, cachedSongs.size, 100)) {
                    is Resource.Success -> {
                        val page = result.data
                        if (page.code != 200) {
                            _songSearchState.value = ArtistCatalogSearchState(query, matches(), error = "接口返回 ${page.code}")
                            return@launch
                        }
                        cachedSongs += page.songs
                        allSongsLoaded = !page.more || page.songs.isEmpty()
                        _songSearchState.value = ArtistCatalogSearchState(query, matches(), !allSongsLoaded)
                    }
                    is Resource.Error -> {
                        _songSearchState.value = ArtistCatalogSearchState(query, matches(), error = result.message)
                        return@launch
                    }
                    Resource.Loading -> {
                        _songSearchState.value = ArtistCatalogSearchState(query, matches(), error = "歌曲仍在加载，请重试")
                        return@launch
                    }
                }
            }
        }
    }

    fun searchAlbums(rawQuery: String) {
        val query = rawQuery.trim()
        albumSearchJob?.cancel()
        if (query.isEmpty()) {
            _albumSearchState.value = ArtistCatalogSearchState()
            return
        }
        fun matches() = cachedAlbums.filter { it.name.contains(query, ignoreCase = true) }
        _albumSearchState.value = ArtistCatalogSearchState(query, matches(), !allAlbumsLoaded)
        if (allAlbumsLoaded) return
        albumSearchJob = viewModelScope.launch {
            delay(250)
            while (!allAlbumsLoaded) {
                when (val result = repository.getArtistAlbums(artistId, cachedAlbums.size, 100)) {
                    is Resource.Success -> {
                        val page = result.data
                        if (page.code != 200) {
                            _albumSearchState.value = ArtistCatalogSearchState(query, matches(), error = "接口返回 ${page.code}")
                            return@launch
                        }
                        cachedAlbums += page.hotAlbums
                        allAlbumsLoaded = !page.more || page.hotAlbums.isEmpty()
                        _albumSearchState.value = ArtistCatalogSearchState(query, matches(), !allAlbumsLoaded)
                    }
                    is Resource.Error -> {
                        _albumSearchState.value = ArtistCatalogSearchState(query, matches(), error = result.message)
                        return@launch
                    }
                    Resource.Loading -> {
                        _albumSearchState.value = ArtistCatalogSearchState(query, matches(), error = "专辑仍在加载，请重试")
                        return@launch
                    }
                }
            }
        }
    }
}
