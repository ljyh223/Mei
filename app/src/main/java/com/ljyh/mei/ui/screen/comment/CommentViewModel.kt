package com.ljyh.mei.ui.screen.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ljyh.mei.data.model.api.CommentSortType
import com.ljyh.mei.data.model.domain.CommentEntry
import com.ljyh.mei.data.model.domain.FloorCommentEntry
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.CommentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CommentViewModel @Inject constructor(
    private val repository: CommentRepository
) : ViewModel() {

    private val _songId = MutableStateFlow("")
    val songId: StateFlow<String> = _songId

    private val _sortType = MutableStateFlow(CommentSortType.RECOMMEND)
    val sortType: StateFlow<CommentSortType> = _sortType

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total

    val pagingData: Flow<PagingData<CommentEntry>> = combine(_songId, _sortType) { songId, sort ->
        songId to sort
    }.flatMapLatest { (songId, sort) ->
        createPager(songId, sort)
    }.cachedIn(viewModelScope)

    private fun createPager(songId: String, sortType: CommentSortType): Flow<PagingData<CommentEntry>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                CommentPagingSource(
                    repository = repository,
                    songId = songId,
                    sortType = sortType,
                    onTotalReceived = { _total.value = it }
                )
            }
        ).flow
    }

    fun setSongId(id: String) {
        if (_songId.value == id) return
        _songId.value = id
        _total.value = 0
        _expandedCommentId.value = null
        _expandedFloorCount.value = 0
        resetFloorComments()
    }

    fun setSortType(type: CommentSortType) {
        _sortType.value = type
    }

    private val _floorComments = MutableStateFlow<Resource<List<FloorCommentEntry>>>(Resource.Loading)
    val floorComments: StateFlow<Resource<List<FloorCommentEntry>>> = _floorComments

    private val _expandedCommentId = MutableStateFlow<Long?>(null)
    val expandedCommentId: StateFlow<Long?> = _expandedCommentId

    private val _expandedFloorCount = MutableStateFlow(0)
    val expandedFloorCount: StateFlow<Int> = _expandedFloorCount

    private val _floorHasMore = MutableStateFlow(false)
    val floorHasMore: StateFlow<Boolean> = _floorHasMore
    private val _floorLoadingMore = MutableStateFlow(false)
    val floorLoadingMore: StateFlow<Boolean> = _floorLoadingMore
    private val _floorLoadMoreError = MutableStateFlow(false)
    val floorLoadMoreError: StateFlow<Boolean> = _floorLoadMoreError

    private var floorJob: Job? = null
    private var prefetchedFloor: List<FloorCommentEntry>? = null
    private var nextFloorTime = -1L
    private val _floorWaitingForReveal = MutableStateFlow(false)
    val floorWaitingForReveal: StateFlow<Boolean> = _floorWaitingForReveal

    private var revealWhenLoaded = false
    private var floorServerHasMore = false

    private fun resetFloorComments() {
        floorJob?.cancel()
        floorJob = null
        prefetchedFloor = null
        nextFloorTime = -1L
        revealWhenLoaded = false
        _floorWaitingForReveal.value = false
        floorServerHasMore = false
        _floorComments.value = Resource.Loading
        _floorHasMore.value = false
        _floorLoadingMore.value = false
        _floorLoadMoreError.value = false
    }

    fun toggleFloorComments(commentId: Long, floorCount: Int) {
        if (_expandedCommentId.value == commentId) {
            _expandedCommentId.value = null
            _expandedFloorCount.value = 0
            resetFloorComments()
            return
        }
        resetFloorComments()
        _expandedCommentId.value = commentId
        _expandedFloorCount.value = floorCount
        loadFloorPage(initial = true)
    }

    fun showMoreFloorComments() {
        if (!_floorHasMore.value) return
        val page = prefetchedFloor
        if (page != null) {
            revealFloorPage(page)
        } else if (_floorLoadingMore.value) {
            revealWhenLoaded = true
            _floorWaitingForReveal.value = true
        } else {
            revealWhenLoaded = true
            _floorWaitingForReveal.value = true
            loadFloorPage(initial = false)
        }
    }

    private fun revealFloorPage(page: List<FloorCommentEntry>) {
        prefetchedFloor = null
        val current = (_floorComments.value as? Resource.Success)?.data.orEmpty()
        _floorComments.value = Resource.Success(current + page)
        _floorHasMore.value = floorServerHasMore
        if (floorServerHasMore) loadFloorPage(initial = false)
    }

    private fun loadFloorPage(initial: Boolean) {
        val commentId = _expandedCommentId.value ?: return
        val songId = _songId.value
        val cursor = nextFloorTime
        _floorLoadingMore.value = !initial
        _floorLoadMoreError.value = false
        floorJob = viewModelScope.launch {
            when (val result = repository.getFloorComment(
                parentCommentId = commentId,
                id = songId,
                limit = FLOOR_PAGE_SIZE,
                time = cursor,
            )) {
                is Resource.Success -> {
                    val page = result.data
                    val lastTime = page.lastOrNull()?.time ?: -1L
                    floorServerHasMore = page.size == FLOOR_PAGE_SIZE && lastTime > 0 && lastTime != cursor
                    _floorHasMore.value = floorServerHasMore
                    nextFloorTime = lastTime
                    _floorLoadingMore.value = false
                    if (initial) {
                        _floorComments.value = Resource.Success(page)
                        if (_floorHasMore.value) loadFloorPage(initial = false)
                    } else if (revealWhenLoaded) {
                        revealWhenLoaded = false
                        _floorWaitingForReveal.value = false
                        revealFloorPage(page)
                    } else {
                        prefetchedFloor = page
                        // A prefetched final page is still available to reveal.
                        if (page.isNotEmpty()) _floorHasMore.value = true
                    }
                }
                is Resource.Error -> {
                    _floorLoadingMore.value = false
                    if (initial) _floorComments.value = Resource.Error(result.message)
                    else {
                        revealWhenLoaded = false
                        _floorWaitingForReveal.value = false
                        _floorLoadMoreError.value = true
                    }
                }
                Resource.Loading -> Unit
            }
        }
    }

    private companion object {
        const val FLOOR_PAGE_SIZE = 6
    }
}
