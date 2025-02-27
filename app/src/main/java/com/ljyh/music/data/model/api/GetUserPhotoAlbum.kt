package com.ljyh.music.data.model.api

data class GetUserPhotoAlbum(
    val userId: String,
    val page: Page = Page(),
    val header: String = "{}",
    val e_r: Boolean = true

) {
    data class Page(
        val cursor: Int? = null,
        val size: Int = 10
    )
}