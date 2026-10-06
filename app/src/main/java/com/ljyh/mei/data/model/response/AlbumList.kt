package com.ljyh.mei.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class UserAlbumList(
    val code: Int = 0,
    val data: List<Data> = emptyList(),
) {
    @Serializable
    data class Data(
        val id: Long = 0,
        val name: String = "",
        val picUrl: String = "",
        val size: Int = 0,
        val subTime: Long = 0,
        val artists: List<Artist> = emptyList(),
    ) {
        @Serializable
        data class Artist(val id: Long = 0, val name: String = "")
    }
}
