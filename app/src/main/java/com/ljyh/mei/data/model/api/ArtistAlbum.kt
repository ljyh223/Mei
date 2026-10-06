package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetArtistAlbum(
    val limit: Int = 50,
    val offset: Int = 0,
    val total: Boolean = true,
)

/** Album fields displayed in the artist page's album carousel. */
@Serializable
data class ArtistAlbum(
    @SerialName("hotAlbums") val hotAlbums: List<HotAlbum> = emptyList(),
    @SerialName("more") val more: Boolean = false,
    @SerialName("code") val code: Int = 0,
) {
    @Serializable
    data class HotAlbum(
        @SerialName("artists") val artists: List<Artist> = emptyList(),
        @SerialName("picUrl") val picUrl: String = "",
        @SerialName("name") val name: String = "",
        @SerialName("id") val id: Int = 0,
        @SerialName("size") val size: Int = 0,
    ) {
        @Serializable
        data class Artist(
            @SerialName("id") val id: Long = 0,
            @SerialName("name") val name: String = "",
        )
    }
}
