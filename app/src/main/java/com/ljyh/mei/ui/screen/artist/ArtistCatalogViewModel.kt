package com.ljyh.mei.ui.screen.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.ljyh.mei.data.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ArtistCatalogViewModel @Inject constructor(
    repository: ArtistRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId: String = checkNotNull(savedStateHandle["id"])

    val songs = Pager(PagingConfig(pageSize = 50, initialLoadSize = 50, enablePlaceholders = false)) {
        ArtistSongsPagingSource(repository, artistId)
    }.flow.cachedIn(viewModelScope)

    val albums = Pager(PagingConfig(pageSize = 30, initialLoadSize = 30, enablePlaceholders = false)) {
        ArtistAlbumsPagingSource(repository, artistId)
    }.flow.cachedIn(viewModelScope)
}
