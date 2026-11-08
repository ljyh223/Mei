package com.ljyh.mei.ui.screen.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ljyh.mei.data.model.api.CommentSortType
import com.ljyh.mei.data.model.domain.CommentEntry
import com.ljyh.mei.data.repository.CommentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CommentViewModel @Inject constructor(
    private val repository: CommentRepository
) : ViewModel() {

    private val _songId = MutableStateFlow("")
    val songId: StateFlow<String> = _songId

    private val replies = CommentReplies(viewModelScope) { songId, commentId, limit, time ->
        repository.getFloorComment(parentCommentId = commentId, id = songId, limit = limit, time = time)
    }
    internal val expandedReplies = replies.expanded

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
        replies.collapseAll()
    }

    fun setSortType(type: CommentSortType) {
        _sortType.value = type
    }

    fun toggleFloorComments(commentId: Long, replyCount: Int) {
        replies.toggle(_songId.value, commentId, replyCount)
    }

    fun showMoreFloorComments(commentId: Long) = replies.showMore(commentId)

    fun retryFloorComments(commentId: Long) = replies.retry(commentId)

    fun collapseAllFloorComments() = replies.collapseAll()
}
