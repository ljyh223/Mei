package com.ljyh.mei.ui.screen.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ljyh.mei.data.model.api.CommentSortType
import com.ljyh.mei.data.model.weapi.CommentX
import com.ljyh.mei.data.model.weapi.FComment
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.CommentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentViewModel @Inject constructor(
    private val repository: CommentRepository
) : ViewModel() {

    private val _songId = MutableStateFlow("")
    val songId: StateFlow<String> = _songId

    private val _sortType = MutableStateFlow(CommentSortType.RECOMMEND)
    val sortType: StateFlow<CommentSortType> = _sortType

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total

    val pagingData: Flow<PagingData<CommentX>> = _sortType.flatMapLatest { sort ->
        createPager(sort)
    }.cachedIn(viewModelScope)

    private fun createPager(sortType: CommentSortType): Flow<PagingData<CommentX>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                CommentPagingSource(
                    repository = repository,
                    songId = _songId.value,
                    sortType = sortType,
                    onTotalReceived = { _total.value = it }
                )
            }
        ).flow
    }

    fun setSongId(id: String) {
        _songId.value = id
    }

    fun setSortType(type: CommentSortType) {
        _sortType.value = type
    }

    private val _floorComments = MutableStateFlow<Resource<List<FComment>>>(Resource.Loading)
    val floorComments: StateFlow<Resource<List<FComment>>> = _floorComments

    private val _expandedCommentId = MutableStateFlow<Long?>(null)
    val expandedCommentId: StateFlow<Long?> = _expandedCommentId

    fun toggleFloorComments(commentId: Long, floorCount: Int) {
        if (_expandedCommentId.value == commentId) {
            _expandedCommentId.value = null
            return
        }
        _expandedCommentId.value = commentId
        loadFloorComments(commentId, floorCount)
    }

    private fun loadFloorComments(parentCommentId: Long, limit: Int) {
        viewModelScope.launch {
            _floorComments.value = Resource.Loading
            _floorComments.value = repository.getFloorComment(
                parentCommentId = parentCommentId,
                id = _songId.value,
                limit = limit
            ).let { resource ->
                when (resource) {
                    is Resource.Success -> Resource.Success(resource.data.data.comments)
                    is Resource.Error -> Resource.Error(resource.message)
                    Resource.Loading -> Resource.Loading
                }
            }
        }
    }
}