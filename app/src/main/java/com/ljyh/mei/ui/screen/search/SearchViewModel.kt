package com.ljyh.mei.ui.screen.search

import android.util.Log
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
    var currentTab by mutableStateOf(SearchType.Song)
    private val cache = mutableMapOf<SearchType, Resource<SearchResult>>()
    var uiState by mutableStateOf<SearchResult?>(null)
        private set
    private val _searchSuggest = MutableStateFlow<Resource<SearchSuggest>>(Resource.Loading)

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
        search(_query.value, type.type)
    }
    fun updateQuery(query: String) {
        _query.value = query
    }

    fun search(keyword: String, type: Int, limit: Int = 30) {
        if(keyword.isEmpty() || keyword.isBlank()) return
        if (cache[currentTab]!=null && cache[currentTab] is Resource.Success){
            _searchResult.value = cache[currentTab]!!

        }else{
            viewModelScope.launch {
                val result=searchRepository.search(keyword, type, limit)
                cache[currentTab] = result
                _searchResult.value = result
            }
        }

    }


}