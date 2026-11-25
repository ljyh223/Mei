package com.ljyh.mei.ui.screen.main.home

import com.ljyh.mei.data.model.eapi.HomePageResourceShow
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.safeApiCall
import com.ljyh.mei.data.repository.HomeFeedSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Owned by HomeViewModel; returning to the page shares requests and already displayed content. */
internal class HomeFeedController(private val source: HomeFeedSource, private val scope: CoroutineScope) {
    private val mutableContent = MutableStateFlow<Resource<List<HomePageResourceShow.Data.Block>>>(Resource.Loading)
    val content: StateFlow<Resource<List<HomePageResourceShow.Data.Block>>> = mutableContent
    val loadingMore = MutableStateFlow(false)
    val loadMoreError = MutableStateFlow(false)
    val refreshing = MutableStateFlow(false)
    val refreshError = MutableStateFlow(false)
    val hasMore = MutableStateFlow(false)
    private var feed: HomePageResourceShow.Data? = null
    private var requestJob: Job? = null
    private var account: String? = null
    private var generation = 0L

    fun load(refresh: Boolean = false, userId: String = account.orEmpty()) {
        val changed = account != null && account != userId
        if (!changed && !refresh && requestJob?.isActive == true) return
        if (!changed && !refresh && mutableContent.value is Resource.Success && source.isHomePageCacheFresh(userId)) return
        account = userId
        val request = ++generation
        requestJob?.cancel()
        if (changed) {
            feed = null
            mutableContent.value = Resource.Loading
            hasMore.value = false
        }
        loadingMore.value = false
        loadMoreError.value = false
        refreshError.value = false
        requestJob = scope.launch {
            try {
                refreshing.value = true
                if (feed == null && !refresh) {
                    val cached = (safeApiCall { source.getCachedHomePageResourceShow(userId) } as? Resource.Success)?.data
                    if (request != generation) return@launch
                    if (cached != null) publish(cached)
                }
                val result = source.getHomePageResourceShow(refresh)
                if (request != generation) return@launch
                when (result) {
                    is Resource.Success -> publish(result.data)
                    is Resource.Error -> if (feed == null) mutableContent.value = result else refreshError.value = true
                    Resource.Loading -> Unit
                }
            } finally {
                if (request == generation) refreshing.value = false
            }
        }
    }

    fun loadMore() {
        val previous = feed ?: return
        if (!hasMore.value || refreshing.value || loadingMore.value) return
        if (!source.isHomePageCacheFresh(account.orEmpty())) {
            load()
            return
        }
        val request = ++generation
        loadingMore.value = true
        loadMoreError.value = false
        requestJob = scope.launch {
            try {
                val result = source.loadMore(previous)
                if (request != generation) return@launch
                when (result) {
                    is Resource.Success -> publish(result.data)
                    is Resource.Error -> loadMoreError.value = true
                    Resource.Loading -> Unit
                }
            } finally {
                if (request == generation) loadingMore.value = false
            }
        }
    }

    private fun publish(data: HomePageResourceShow.Data) {
        feed = data
        hasMore.value = data.hasMore
        mutableContent.value = Resource.Success(data.blocks)
    }
}
