package com.ljyh.mei.ui.screen.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.api.SearchResult
import com.ljyh.mei.data.model.api.SearchSuggest
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    // Track the last query to invalidate cache when keyword changes
    private var lastSearchQuery = ""

    var currentTab by mutableStateOf(SearchType.Song)

    // Cache: Key is the Tab Type, Value is the API Result
    private val cache = mutableMapOf<SearchType, Resource<SearchResult>>()

    private val _searchResult = MutableStateFlow<Resource<SearchResult>>(Resource.Loading)
    val searchResult: StateFlow<Resource<SearchResult>> = _searchResult

    @OptIn(FlowPreview::class)
    val searchSuggest = _query
        .debounce(300)
        .distinctUntilChanged()
        .map { query ->
            if (query.isBlank()) {
                Resource.Loading
            } else {
                try {
                    searchRepository.searchSuggest(query)
                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Unknown error")
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = WhileSubscribed(5000),
            initialValue = Resource.Loading
        )

    fun onTabChange(type: SearchType) {
        currentTab = type
        // Do not call search() here directly.
        // The UI observes 'currentTab' and 'query' in LaunchedEffect,
        // which will trigger search() automatically.
    }

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun search(keyword: String, type: Int, limit: Int = 30) {
        if (keyword.isBlank()) return

        // FIX: If the query changed, clear the old cache
        if (keyword != lastSearchQuery) {
            cache.clear()
            lastSearchQuery = keyword
        }

        // Check cache for the current tab
        if (cache[currentTab] != null && cache[currentTab] is Resource.Success) {
            _searchResult.value = cache[currentTab]!!
        } else {
            // Set loading only if we don't have data (optional, prevents flickering)
            _searchResult.value = Resource.Loading

            viewModelScope.launch {
                val result = searchRepository.search(keyword, type, limit)
                // Only cache if successful
                if (result is Resource.Success) {
                    cache[currentTab] = result
                }
                _searchResult.value = result
            }
        }
    }
}