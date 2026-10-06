package com.ljyh.mei.ui.screen.comment

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ljyh.mei.data.model.api.CommentSortType
import com.ljyh.mei.data.model.CommentEntry
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.CommentRepository
import timber.log.Timber

class CommentPagingSource(
    private val repository: CommentRepository,
    private val songId: String,
    private val sortType: CommentSortType,
    private val onTotalReceived: (Int) -> Unit = {}
) : PagingSource<Int, CommentEntry>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CommentEntry> {
        val pageNo = params.key ?: 1
        return try {
            val cursor = when (sortType) {
                CommentSortType.TIME -> {
                    if (pageNo > 1) lastCursor else "0"
                }
                CommentSortType.HOT -> "normalHot#${(pageNo - 1) * params.loadSize}"
                CommentSortType.RECOMMEND -> "${(pageNo - 1) * params.loadSize}"
            }
            Timber.d(songId.toString())

            val result = repository.getComment(
                id = songId,
                sortType = sortType,
                pageNo = pageNo,
                pageSize = params.loadSize,
                cursor = cursor
            )

            when (result) {
                is Resource.Success -> {
                    val page = result.data
                    if (pageNo == 1) onTotalReceived(page.totalCount)
                    if (sortType == CommentSortType.TIME && page.items.isNotEmpty()) {
                        lastCursor = page.items.last().time.toString()
                    }
                    LoadResult.Page(
                        data = page.items,
                        prevKey = null,
                        nextKey = if (page.hasMore) pageNo + 1 else null
                    )
                }
                is Resource.Error -> LoadResult.Error(Exception(result.message))
                Resource.Loading -> LoadResult.Page(emptyList(), null, null)
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CommentEntry>): Int? = null

    companion object {
        var lastCursor: String = "0"
    }
}
