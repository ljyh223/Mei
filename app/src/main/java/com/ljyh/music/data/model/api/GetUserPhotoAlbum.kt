package com.ljyh.music.data.model.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class GetUserPhotoAlbum(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("page")
    var page: String="",
    @SerializedName("header")
    val header: String = "{}",
    @SerializedName("e_r")
    val e_r: Boolean = true

) {
    init {
        page= Gson().toJson(Page())
    }
    data class Page(
        @SerializedName("cursor")
        val cursor: String? = null,
        @SerializedName("size")
        val size: Int = 10
    )
}