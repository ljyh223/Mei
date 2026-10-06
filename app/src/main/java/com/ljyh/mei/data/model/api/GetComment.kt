package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable

data class GetComment(
    @SerialName("threadId")
    val threadId: String,
    @SerialName("pageNo")
    val pageNo: Int = 1,
    @SerialName("pageSize")
    val pageSize: Int = 20,
    @SerialName("sortType")
    val sortType: Int = 99,
    @SerialName("cursor")
    val cursor: String = "",
    @SerialName("showInner")
    val showInner: Boolean = true
)

enum class CommentSortType(val value: Int, val label: String) {
    RECOMMEND(99, "推荐"),
    HOT(2, "热度"),
    TIME(3, "时间");

    companion object {
        fun fromValue(value: Int): CommentSortType = entries.find { it.value == value } ?: RECOMMEND
    }
}