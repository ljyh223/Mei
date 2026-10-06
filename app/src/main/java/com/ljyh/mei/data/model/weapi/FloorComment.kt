package com.ljyh.mei.data.model.weapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FloorComment(
    @SerialName("code") val code: Int = 0,
    @SerialName("data") val data: FData = FData(),
    @SerialName("message") val message: String = "",
)

@Serializable
data class FData(@SerialName("comments") val comments: List<FComment> = emptyList())

@Serializable
data class FComment(
    @SerialName("content") val content: String = "",
    @SerialName("ipLocation") val ipLocation: IpLocation = IpLocation(),
    @SerialName("likedCount") val likedCount: Int = 0,
    @SerialName("timeStr") val timeStr: String = "",
    @SerialName("user") val user: User = User(),
)
