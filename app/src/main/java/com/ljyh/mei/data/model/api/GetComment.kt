package com.ljyh.mei.data.model.api

import com.google.gson.annotations.SerializedName

data class GetComment(
    @SerializedName("threadId")
    val threadId: String,
    @SerializedName("pageNo")
    val pageNo: Int = 1,
    @SerializedName("pageSize")
    val pageSize: Int = 20,
    @SerializedName("sortType")
    val sortType: Int = 99,
    @SerializedName("cursor")
    val cursor: String = "",
    @SerializedName("showInner")
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