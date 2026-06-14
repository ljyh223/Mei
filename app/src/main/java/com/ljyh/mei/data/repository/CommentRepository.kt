package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.api.CommentResourceType
import com.ljyh.mei.data.model.api.CommentSortType
import com.ljyh.mei.data.model.api.GetComment
import com.ljyh.mei.data.model.api.GetFloorComment
import com.ljyh.mei.data.model.weapi.Comment
import com.ljyh.mei.data.model.weapi.FloorComment
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommentRepository(
    private val apiService: ApiService,
    private val weApiService: WeApiService
) {
    suspend fun getComment(
        id: String,
        resourceType: CommentResourceType = CommentResourceType.SONG,
        sortType: CommentSortType = CommentSortType.RECOMMEND,
        pageNo: Int = 1,
        pageSize: Int = 20,
        cursor: String = ""
    ): Resource<Comment> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getComment(
                    GetComment(
                        threadId = resourceType.threadId(id),
                        pageNo = pageNo,
                        pageSize = pageSize,
                        sortType = sortType.value,
                        cursor = cursor
                    )
                )
            }
        }
    }

    suspend fun getFloorComment(
        parentCommentId: Long,
        id: String,
        resourceType: CommentResourceType = CommentResourceType.SONG,
        limit: Int = 20,
        time: Long = -1
    ): Resource<FloorComment> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                weApiService.getFloorComment(
                    GetFloorComment(
                        parentCommentId = parentCommentId,
                        threadId = resourceType.threadId(id),
                        limit = limit,
                        time = time
                    )
                )
            }
        }
    }
}