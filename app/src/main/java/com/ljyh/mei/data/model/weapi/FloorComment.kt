package com.ljyh.mei.data.model.weapi
import com.google.gson.annotations.SerializedName


data class FloorComment(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: FData,
    @SerializedName("message")
    val message: String
)

data class FData(
    @SerializedName("bestComments")
    val bestComments: List<Any?>,
    @SerializedName("comments")
    val comments: List<FComment>,
    @SerializedName("currentComment")
    val currentComment: Any,
    @SerializedName("hasMore")
    val hasMore: Boolean,
    @SerializedName("ownerComment")
    val ownerComment: OwnerComment,
    @SerializedName("time")
    val time: Long,
    @SerializedName("totalCount")
    val totalCount: Int
)

data class FComment(
    @SerializedName("aiCommentLabel")
    val aiCommentLabel: Any,
    @SerializedName("beReplied")
    val beReplied: Any,
    @SerializedName("commentId")
    val commentId: Long,
    @SerializedName("commentLocationType")
    val commentLocationType: Int,
    @SerializedName("content")
    val content: String,
    @SerializedName("contentResource")
    val contentResource: Any,
    @SerializedName("decoration")
    val decoration: Decoration,
    @SerializedName("expressionUrl")
    val expressionUrl: Any,
    @SerializedName("favorited")
    val favorited: Boolean,
    @SerializedName("grade")
    val grade: Any,
    @SerializedName("ipLocation")
    val ipLocation: IpLocation,
    @SerializedName("likeAnimationMap")
    val likeAnimationMap: Any,
    @SerializedName("liked")
    val liked: Boolean,
    @SerializedName("likedCount")
    val likedCount: Int,
    @SerializedName("medal")
    val medal: Any,
    @SerializedName("needDisplayTime")
    val needDisplayTime: Boolean,
    @SerializedName("owner")
    val owner: Boolean,
    @SerializedName("parentCommentId")
    val parentCommentId: Int,
    @SerializedName("pendantData")
    val pendantData: Any,
    @SerializedName("repliedMark")
    val repliedMark: Any,
    @SerializedName("richContent")
    val richContent: Any,
    @SerializedName("showFloorComment")
    val showFloorComment: Any,
    @SerializedName("status")
    val status: Int,
    @SerializedName("time")
    val time: Long,
    @SerializedName("timeStr")
    val timeStr: String,
    @SerializedName("user")
    val user: User,
    @SerializedName("userBizLevels")
    val userBizLevels: Any
)

data class OwnerComment(
    @SerializedName("aiCommentLabel")
    val aiCommentLabel: Any,
    @SerializedName("beReplied")
    val beReplied: List<Any?>,
    @SerializedName("commentId")
    val commentId: Int,
    @SerializedName("commentLocationType")
    val commentLocationType: Int,
    @SerializedName("content")
    val content: String,
    @SerializedName("contentResource")
    val contentResource: Any,
    @SerializedName("decoration")
    val decoration: Decoration,
    @SerializedName("expressionUrl")
    val expressionUrl: Any,
    @SerializedName("favorited")
    val favorited: Boolean,
    @SerializedName("grade")
    val grade: Any,
    @SerializedName("ipLocation")
    val ipLocation: IpLocation,
    @SerializedName("likeAnimationMap")
    val likeAnimationMap: Any,
    @SerializedName("liked")
    val liked: Boolean,
    @SerializedName("likedCount")
    val likedCount: Int,
    @SerializedName("medal")
    val medal: Any,
    @SerializedName("needDisplayTime")
    val needDisplayTime: Boolean,
    @SerializedName("owner")
    val owner: Boolean,
    @SerializedName("parentCommentId")
    val parentCommentId: Int,
    @SerializedName("pendantData")
    val pendantData: Any,
    @SerializedName("repliedMark")
    val repliedMark: Any,
    @SerializedName("richContent")
    val richContent: Any,
    @SerializedName("showFloorComment")
    val showFloorComment: Any,
    @SerializedName("status")
    val status: Int,
    @SerializedName("time")
    val time: Long,
    @SerializedName("timeStr")
    val timeStr: String,
    @SerializedName("user")
    val user: User,
    @SerializedName("userBizLevels")
    val userBizLevels: Any
)


