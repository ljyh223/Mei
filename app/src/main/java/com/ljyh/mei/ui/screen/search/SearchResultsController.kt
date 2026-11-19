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
    val message: String? = null,
)

/** Query generations protect both the visible state and cached tabs from late responses. */
internal class SearchResultsController(
    private val scope: CoroutineScope,
    private val sourceSearch: suspend (String, String, Int) -> Resource<SearchResults> = { _, _, _ -> Resource.Error("音源不可用") },
    initialSongSource: String? = null,
    private val search: suspend (String, SearchType, Int) -> Resource<SearchResults>,
) {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private val _tab = MutableStateFlow(SearchType.All)
    val tab = _tab.asStateFlow()
    private val _page = MutableStateFlow(SearchPageState())
    val page = _page.asStateFlow()
    private val _songSource = MutableStateFlow(initialSongSource)
    val songSource = _songSource.asStateFlow()
    private data class PageKey(val type: SearchType, val source: String?)
    private fun key(type: SearchType) = PageKey(type, _songSource.value.takeIf { type == SearchType.Song })
    private val pages = mutableMapOf<PageKey, SearchPageState>()
    private val jobs = mutableMapOf<PageKey, Job>()
    private val requests = mutableMapOf<PageKey, Int>()
    private var requestId = 0
    private var generation = 0

    fun initialize(query: String, type: Int) {
        val keyword = query.trim()
        if (keyword.isEmpty() || keyword == _query.value) return
        generation++
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        pages.clear()
        requests.clear()
        _query.value = keyword
        _tab.value = SearchType.entries.firstOrNull { it.type == type && it != SearchType.History } ?: SearchType.All
        fetch(_tab.value)
    }

    fun select(type: SearchType) {
        if (type == SearchType.History || type == _tab.value || _query.value.isBlank()) return
        _tab.value = type
        val cached = pages[key(type)]
        if (cached != null) _page.value = cached else fetch(type)
    }

    fun selectSource(id: String?, selectSong: Boolean = true) {
        if (_songSource.value == id && (!selectSong || _tab.value == SearchType.Song)) return
        _songSource.value = id
        if (selectSong) _tab.value = SearchType.Song
        if (_query.value.isBlank() || _tab.value != SearchType.Song) return
        val cached = pages[key(SearchType.Song)]
        if (cached != null) _page.value = cached else fetch(SearchType.Song)
    }

    fun invalidateSource(id: String) {
        val key = PageKey(SearchType.Song, id)
        jobs.remove(key)?.cancel()
        requests.remove(key)
        pages.remove(key)
        if (_tab.value == SearchType.Song && _songSource.value == id && _query.value.isNotBlank()) fetch(SearchType.Song)
    }

    fun retry() {
        if (_query.value.isNotBlank() && jobs[key(_tab.value)]?.isActive != true) fetch(_tab.value)
    }

    fun loadMore() {
        val type = _tab.value
        val state = pages[key(type)] ?: return
        if (type == SearchType.All || state.loading || state.appending || state.data?.hasMore != true) return
        fetch(type, append = true)
    }

    private fun publish(key: PageKey, state: SearchPageState) {
        pages[key] = state
        if (key(_tab.value) == key) _page.value = state
    }

    private fun fetch(type: SearchType, append: Boolean = false) {
        val query = _query.value
        val requestGeneration = generation
        val key = key(type)
        val previous = pages[key]?.data
        val token = ++requestId
        requests[key] = token
        val offset = if (append) previous?.nextOffset ?: 0 else 0
        publish(key, SearchPageState(data = previous, loading = !append, appending = append))
        jobs[key] = scope.launch {
            val result = if (key.source != null) sourceSearch(query, key.source, offset) else search(query, type, offset)
            if (generation != requestGeneration || _query.value != query || requests[key] != token) return@launch
            when (result) {
                is Resource.Success -> {
                    var data = if (append && previous != null) previous.append(result.data) else result.data
                    // A repeated/empty backend page must not trigger an endless pagination loop.
                    if (append && previous != null && (data.nextOffset <= offset ||
                        (data.songs.size == previous.songs.size && data.artists.size == previous.artists.size &&
                            data.albums.size == previous.albums.size && data.playlists.size == previous.playlists.size))) {
                        data = data.copy(hasMore = false)
                    }
                    publish(key, SearchPageState(data = data, loading = false))
                }
                is Resource.Error -> publish(key, SearchPageState(
                    data = previous, loading = false, error = !append, appendError = append, message = result.message,
                ))
                Resource.Loading -> Unit
            }
        }
    }
}
