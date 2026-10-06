package com.ljyh.mei.data.model

/** Presentation-ready comment values mapped from Netease response DTOs in CommentRepository. */
data class CommentPage(
    val items: List<CommentEntry>,
    val hasMore: Boolean,
    val totalCount: Int,
)

data class CommentEntry(
    val id: Long,
    val content: String,
    val user: CommentUser,
    val location: String?,
    val liked: Boolean,
    val likedCount: Int,
    val replyCount: Int,
    val time: Long,
    val timeText: String,
)

data class FloorCommentEntry(
    val content: String,
    val user: CommentUser,
    val location: String,
    val likedCount: Int,
    val timeText: String,
)

data class CommentUser(val avatarUrl: String, val nickname: String)
