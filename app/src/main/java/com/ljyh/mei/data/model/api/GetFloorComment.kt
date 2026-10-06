package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable

data class GetFloorComment(
    @SerialName("parentCommentId")
    val parentCommentId: Long,
    @SerialName("threadId")
    val threadId: String,
    @SerialName("limit")
    val limit: Int = 20,
    @SerialName("time")
    val time: Long = -1
)

enum class CommentResourceType(val prefix: String, val value: Int) {
    SONG("R_SO_4_", 0),
    MV("R_MV_5_", 1),
    PLAYLIST("A_PL_0_", 2),
    ALBUM("R_AL_3_", 3),
    RADIO("A_DJ_1_", 4),
    VIDEO("R_VI_62_", 5),
    DYNAMIC("", 6),
    DJ("A_DJ_1_", 7);

    fun threadId(id: String): String = prefix + id
}