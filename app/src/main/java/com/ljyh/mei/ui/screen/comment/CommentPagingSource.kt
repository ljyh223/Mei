package com.ljyh.mei.ui.screen.comment

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ljyh.mei.data.model.api.CommentSortType
import com.ljyh.mei.data.model.weapi.CommentX
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.CommentRepository

class CommentPagingSource(
    private val repository: CommentRepository,
    private val songId: String,
    private val sortType: CommentSortType,
    private val onTotalReceived: (Int) -> Unit = {}
) : PagingSource<Int, CommentX>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CommentX> {
        val pageNo = params.key ?: 1
        return try {
            val cursor = when (sortType) {
                CommentSortType.TIME -> {
                    if (pageNo > 1) lastCursor else "0"
                }
                CommentSortType.HOT -> "normalHot#${(pageNo - 1) * params.loadSize}"
                CommentSortType.RECOMMEND -> "${(pageNo - 1) * params.loadSize}"
            }

            val result = repository.getComment(
                id = songId,
                sortType = sortType,
                pageNo = pageNo,
                pageSize = params.loadSize,
                cursor = cursor
            )

            when (result) {
                is Resource.Success -> {
                    val data = result.data.data
                    if (pageNo == 1) onTotalReceived(data.totalCount)
                    if (sortType == CommentSortType.TIME && data.comments.isNotEmpty()) {
                        lastCursor = data.comments.last().time.toString()
                    }
                    LoadResult.Page(
                        data = data.comments,
                        prevKey = null,
                        nextKey = if (data.hasMore) pageNo + 1 else null
                    )
                }
                is Resource.Error -> LoadResult.Error(Exception(result.message))
                Resource.Loading -> LoadResult.Page(emptyList(), null, null)
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CommentX>): Int? = null

    companion object {
        var lastCursor: String = "0"
    }
}