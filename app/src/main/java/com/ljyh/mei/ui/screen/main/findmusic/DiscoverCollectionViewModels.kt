package com.ljyh.mei.ui.screen.main.findmusic

import androidx.lifecycle.SavedStateHandle
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

data class DiscoverPlaylistState(
    val query: DiscoverPlaylistQuery = DiscoverPlaylistQuery(),
    val items: List<DiscoverPlaylist> = emptyList(),
    val nextOffset: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val failed: Boolean = false,
)

/** Each navigation entry owns its own filters, pages and request generation. */
@HiltViewModel
class DiscoverPlaylistsViewModel internal constructor(
    private val source: DiscoverDataSource,
    private val savedState: SavedStateHandle,
    private val scopeOverride: CoroutineScope?,
) : ViewModel() {
    @Inject constructor(repository: DiscoverRepository, savedState: SavedStateHandle) : this(repository, savedState, null)
    private val requestScope get() = scopeOverride ?: viewModelScope
    private val initialQuery = DiscoverPlaylistQuery(
        category = savedState.get<String>("category")?.takeIf(String::isNotBlank) ?: "全部",
        order = DiscoverOrder.entries.firstOrNull { it.wireValue == savedState.get<String>("order") } ?: DiscoverOrder.Hot,
    )
    private val _playlists = MutableStateFlow(DiscoverPlaylistState(query = initialQuery))
    val playlists = _playlists.asStateFlow()
    private val _categories = MutableStateFlow<Resource<List<DiscoverCategory>>>(Resource.Loading)
    val categories = _categories.asStateFlow()
    private val cache = linkedMapOf<DiscoverPlaylistQuery, DiscoverPlaylistState>()
    private var playlistJob: Job? = null
    private var playlistGeneration = 0
    private var categoriesJob: Job? = null

    init { loadCategories(); loadPlaylists(initialQuery) }

    fun selectCategory(category: String) { loadPlaylists(_playlists.value.query.copy(category = category)) }
    fun selectOrder(order: DiscoverOrder) { loadPlaylists(_playlists.value.query.copy(order = order)) }
    fun retryPlaylists() {
        if (_playlists.value.items.isEmpty()) loadPlaylists(_playlists.value.query, force = true)
        else loadMore()
    }
    fun loadCategories() {
        categoriesJob?.cancel()
        categoriesJob = requestScope.launch {
            _categories.value = Resource.Loading
            _categories.value = source.categories()
        }
    }

    private fun loadPlaylists(query: DiscoverPlaylistQuery, force: Boolean = false) {
        savedState["category"] = query.category
        savedState["order"] = query.order.wireValue
        val generation = ++playlistGeneration
        playlistJob?.cancel()
        if (!force) cache[query]?.let { _playlists.value = it; return }
        _playlists.value = DiscoverPlaylistState(query = query, loading = true)
        playlistJob = requestScope.launch {
            val result = source.playlists(query, 0)
            if (generation != playlistGeneration) return@launch
            _playlists.value = when (result) {
                is Resource.Success -> DiscoverPlaylistState(query, result.data.items,
                    result.data.nextOffset, result.data.hasMore)
                else -> DiscoverPlaylistState(query = query, failed = true)
            }
            rememberPage()
        }
    }

    fun loadMore() {
        val previous = _playlists.value
        if (previous.loading || previous.loadingMore || !previous.hasMore) return
        val generation = playlistGeneration
        _playlists.value = previous.copy(loadingMore = true, failed = false)
        playlistJob = requestScope.launch {
            val result = source.playlists(previous.query, previous.nextOffset)
            if (generation != playlistGeneration) return@launch
            _playlists.value = when (result) {
                is Resource.Success -> {
                    val combined = (previous.items + result.data.items).distinctBy { it.id }
                    previous.copy(items = combined, nextOffset = result.data.nextOffset,
                        hasMore = result.data.hasMore && result.data.nextOffset > previous.nextOffset && combined.size > previous.items.size,
                        loadingMore = false, failed = false)
                }
                else -> previous.copy(loadingMore = false, failed = true)
            }
            rememberPage()
        }
    }

    private fun rememberPage() {
        val current = _playlists.value
        if (current.items.isEmpty()) return
        cache[current.query] = current.copy(failed = false)
        if (cache.size > 8) cache.remove(cache.keys.first())
    }
}

@HiltViewModel
class DiscoverAlbumsViewModel @Inject constructor(private val source: DiscoverRepository) : ViewModel() {
    private val _albums = MutableStateFlow<Resource<List<DiscoverAlbum>>>(Resource.Loading)
    val albums = _albums.asStateFlow()
    private var job: Job? = null
    init { reload() }
    fun reload() {
        job?.cancel()
        job = viewModelScope.launch { _albums.value = Resource.Loading; _albums.value = source.albums() }
    }
}

@HiltViewModel
class DiscoverChartsViewModel @Inject constructor(private val source: DiscoverRepository) : ViewModel() {
    private val _charts = MutableStateFlow<Resource<List<DiscoverChart>>>(Resource.Loading)
    val charts = _charts.asStateFlow()
    private var job: Job? = null
    init { reload() }
    fun reload() {
        job?.cancel()
        job = viewModelScope.launch { _charts.value = Resource.Loading; _charts.value = source.charts() }
    }
}
