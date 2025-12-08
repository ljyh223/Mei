package com.ljyh.mei.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.api.SearchResult
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // --- 搜索建议相关 ---
    // 用于输入框的实时输入流
    private val _inputQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    val searchSuggest = _inputQuery
        .debounce(300)
        .distinctUntilChanged()
        .map { query ->
            if (query.isBlank()) {
                Resource.Loading // 或者 Resource.Success(empty) 取决于你的UI处理
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
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading
        )

    fun updateInputQuery(query: String) {
        _inputQuery.value = query
    }

    // --- 搜索结果相关 ---

    // 当前确定的搜索词（点击搜索后的词）
    private val _currentQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()

    private val _currentTab = MutableStateFlow(SearchType.Song)
    val currentTab: StateFlow<SearchType> = _currentTab.asStateFlow()

    private val _searchResult = MutableStateFlow<Resource<SearchResult>>(Resource.Loading)
    val searchResult: StateFlow<Resource<SearchResult>> = _searchResult.asStateFlow()

    // 缓存：Key是SearchType, Value是该Tab下的数据
    // 只有当 Query 变化时，这个 Cache 才会被清空
    private val resultCache = mutableMapOf<SearchType, Resource<SearchResult>>()

    /**
     * 初始化搜索或恢复状态
     * @param query 搜索关键词
     * @param type 初始 Tab 类型 (Int)
     */
    fun onSearchInit(query: String, type: Int) {
        if (query.isBlank()) return

        // 如果 Query 变了，说明是新的搜索，重置所有状态
        if (_currentQuery.value != query) {
            _currentQuery.value = query
            resultCache.clear() // 清空旧 Query 的缓存

            // 设定初始 Tab (默认为 Song)
            val initialType = SearchType.entries.find { it.type == type } ?: SearchType.Song
            _currentTab.value = initialType

            // 发起请求
            fetchResultsInternal(query, initialType)
        }
        // else: 如果 Query 没变，说明是导航返回，ViewModel 状态还在，保持原样即可，不需要做任何事
    }

    /**
     * 用户点击 Tab 切换
     */
    fun onTabChange(newType: SearchType) {
        if (_currentTab.value == newType) return
        _currentTab.value = newType

        // 切换 Tab 时，检查是否有缓存
        fetchResultsInternal(_currentQuery.value, newType)
    }

    private fun fetchResultsInternal(query: String, type: SearchType, limit: Int = 30) {
        // 1. 检查缓存
        val cached = resultCache[type]
        if (cached != null && cached is Resource.Success) {
            _searchResult.value = cached
            return
        }

        // 2. 无缓存，发起网络请求
        _searchResult.value = Resource.Loading

        viewModelScope.launch {
            // 二次检查，防止快速切换导致的竞态条件 (可选，视情况而定)
            if (_currentQuery.value != query) return@launch

            val result = try {
                searchRepository.search(query, type.type, limit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Unknown Error")
            }

            // 3. 存入缓存并更新 UI
            if (result is Resource.Success) {
                resultCache[type] = result
            }
            // 只有当当前 Tab 还是请求的 Tab 时才更新 UI，防止切太快 UI 闪烁
            if (_currentTab.value == type) {
                _searchResult.value = result
            }
        }
    }
}