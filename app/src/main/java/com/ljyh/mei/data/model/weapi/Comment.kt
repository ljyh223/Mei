package com.ljyh.mei.data.model.weapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Compact comment page response for paging and the comment list UI. */
@Serializable
data class Comment(
    @SerialName("code") val code: Int = 0,
    @SerialName("data") val data: Data1 = Data1(),
    @SerialName("message") val message: String? = null,
)

@Serializable
data class Data1(
    @SerialName("comments") val comments: List<CommentX> = emptyList(),
    @SerialName("hasMore") val hasMore: Boolean = false,
    @SerialName("totalCount") val totalCount: Int = 0,
)

@Serializable
data class CommentX(
    @SerialName("commentId") val commentId: Long = 0,
    @SerialName("content") val content: String = "",
    @SerialName("ipLocation") val ipLocation: IpLocation? = null,
    @SerialName("liked") val liked: Boolean = false,
    @SerialName("likedCount") val likedCount: Int = 0,
    @SerialName("showFloorComment") val showFloorComment: ShowFloorComment? = null,
    @SerialName("time") val time: Long = 0,
    @SerialName("timeStr") val timeStr: String = "",
    @SerialName("user") val user: User = User(),
)

@Serializable
data class IpLocation(@SerialName("location") val location: String = "")

@Serializable
data class ShowFloorComment(@SerialName("replyCount") val replyCount: Int = 0)

@Serializable
data class User(
    @SerialName("avatarUrl") val avatarUrl: String = "",
    @SerialName("nickname") val nickname: String = "",
)
