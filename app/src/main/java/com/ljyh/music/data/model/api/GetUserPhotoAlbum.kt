package com.ljyh.music.data.model.api

import com.google.gson.annotations.SerializedName

data class GetUserPhotoAlbum(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("page")
    val page: Page = Page(),
    @SerializedName("header")
    val header: String = "{}",
    @SerializedName("e_r")
    val e_r: Boolean = true

) {
    data class Page(
        @SerializedName("cursor")
        val cursor: Int? = null,
        @SerializedName("size")
        val size: Int = 10
    )
}