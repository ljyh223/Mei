package com.ljyh.mei.data.model.api

data class CheckSongLike(
    var trackIds: String
)


data class CheckSongLikeResult(
    val code: Int,
    val ids: List<Long>
)