package com.ljyh.mei.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val repository: SearchRepository) : ViewModel() {
    private val input = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchSuggest = input.debounce(300).distinctUntilChanged().mapLatest { query ->
        if (query.isBlank()) Resource.Loading else repository.searchSuggest(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading)

    private val results = SearchResultsController(viewModelScope) { query, type, offset ->
        if (type == SearchType.All) repository.overview(query)
        else repository.search(query, type.type, limit = 30, offset = offset)
    }
    val currentQuery = results.query
    val currentTab = results.tab
    val pageState = results.page

    fun updateInputQuery(query: String) { input.value = query }
    fun onSearchInit(query: String, type: Int) = results.initialize(query, type)
    fun onTabChange(type: SearchType) = results.select(type)
    fun retry() = results.retry()
    fun loadMore() = results.loadMore()
}
