package com.ljyh.mei.data.model.api

import kotlinx.serialization.Serializable

@Serializable
data class LikedSongListResponse(
    val code: Int,
    val ids: List<Long>
)
