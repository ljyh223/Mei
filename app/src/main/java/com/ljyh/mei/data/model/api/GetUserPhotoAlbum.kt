package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable

data class GetUserPhotoAlbum(
    @SerialName("userId")
    val userId: String,
    @SerialName("page")
    var page: String="",
    @SerialName("header")
    val header: String = "{}",
    @SerialName("e_r")
    val e_r: Boolean = true

) {
    init {
        page = Json.encodeToString(Page())
    }
    @Serializable
    data class Page(
        @SerialName("cursor")
        val cursor: String? = null,
        @SerialName("size")
        val size: Int = 10
    )
}
