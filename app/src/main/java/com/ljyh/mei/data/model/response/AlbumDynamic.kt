package com.ljyh.mei.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlbumDynamic(
    val code: Int = 0,
    @SerialName("isSub") val isSubscribed: Boolean? = null,
    val subCount: Long = 0,
    val commentCount: Long = 0,
    val shareCount: Long = 0,
)
