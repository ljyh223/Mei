package com.ljyh.mei.ui.screen.search

import com.ljyh.mei.data.model.domain.SearchResults
import com.ljyh.mei.data.network.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchPageState(
    val data: SearchResults? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
    val appending: Boolean = false,
    val appendError: Boolean = false,
)

/** Query generations protect both the visible state and cached tabs from late responses. */
internal class SearchResultsController(
    private val scope: CoroutineScope,
    private val search: suspend (String, SearchType, Int) -> Resource<SearchResults>,
) {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private val _tab = MutableStateFlow(SearchType.All)
    val tab = _tab.asStateFlow()
    private val _page = MutableStateFlow(SearchPageState())
    val page = _page.asStateFlow()
    private val pages = mutableMapOf<SearchType, SearchPageState>()
    private val jobs = mutableMapOf<SearchType, Job>()
    private var generation = 0

    fun initialize(query: String, type: Int) {
        val keyword = query.trim()
        if (keyword.isEmpty() || keyword == _query.value) return
        generation++
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        pages.clear()
        _query.value = keyword
        _tab.value = SearchType.entries.firstOrNull { it.type == type && it != SearchType.History } ?: SearchType.All
        fetch(_tab.value)
    }

    fun select(type: SearchType) {
        if (type == SearchType.History || type == _tab.value || _query.value.isBlank()) return
        _tab.value = type
        val cached = pages[type]
        if (cached != null) _page.value = cached else fetch(type)
    }

    fun retry() {
        if (_query.value.isNotBlank() && jobs[_tab.value]?.isActive != true) fetch(_tab.value)
    }

    fun loadMore() {
        val type = _tab.value
        val state = pages[type] ?: return
        if (type == SearchType.All || state.loading || state.appending || state.data?.hasMore != true) return
        fetch(type, append = true)
    }

    private fun publish(type: SearchType, state: SearchPageState) {
        pages[type] = state
        if (_tab.value == type) _page.value = state
    }

    private fun fetch(type: SearchType, append: Boolean = false) {
        val query = _query.value
        val requestGeneration = generation
        val previous = pages[type]?.data
        val offset = if (append) previous?.nextOffset ?: 0 else 0
        publish(type, SearchPageState(data = previous, loading = !append, appending = append))
        jobs[type] = scope.launch {
            val result = search(query, type, offset)
            if (generation != requestGeneration || _query.value != query) return@launch
            when (result) {
                is Resource.Success -> {
                    var data = if (append && previous != null) previous.append(result.data) else result.data
                    // A repeated/empty backend page must not trigger an endless pagination loop.
                    if (append && previous != null && (data.nextOffset <= offset ||
                        (data.songs.size == previous.songs.size && data.artists.size == previous.artists.size &&
                            data.albums.size == previous.albums.size && data.playlists.size == previous.playlists.size))) {
                        data = data.copy(hasMore = false)
                    }
                    publish(type, SearchPageState(data = data, loading = false))
                }
                is Resource.Error -> publish(type, SearchPageState(
                    data = previous, loading = false, error = !append, appendError = append,
                ))
                Resource.Loading -> Unit
            }
        }
    }
}
