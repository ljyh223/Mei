package com.ljyh.mei.data.model.api

import kotlinx.serialization.Serializable

@Serializable
data class CheckSongLike(
    var trackIds: String
)

@Serializable
data class CheckSongLikeResult(
    val code: Int,
    val ids: List<Long>
)
