package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggest(
    @SerialName("s") val s: String,
    @SerialName("type") val type: String = "mobile"
)

@Serializable
data class SearchSuggest(
    @SerialName("result") val result: Result,
    @SerialName("code") val code: Int
) {
    @Serializable
    data class Result(
        @SerialName("albums") val albums: List<Album>? = null,
        @SerialName("artists") val artists: List<Artist>? = null,
        @SerialName("songs") val songs: List<Song>? = null
    ) {
        @Serializable
        data class Album(
            @SerialName("id") val id: Int,
            @SerialName("name") val name: String
        )

        @Serializable
        data class Artist(
            @SerialName("id") val id: Int,
            @SerialName("name") val name: String,
            @SerialName("picUrl") val picUrl: String? = null,
            @SerialName("alias") val alias: List<String> = emptyList()
        )

        @Serializable
        data class Song(
            @SerialName("id") val id: Long,
            @SerialName("name") val name: String
        )
    }
}
