package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetSearch(
    @SerialName("s") val s: String,
    @SerialName("type") val type: Int = 1,
    @SerialName("limit") val limit: Int = 30,
    @SerialName("offset") val offset: Int = 0
)

@Serializable
data class SearchResult(
    @SerialName("result") val result: Result,
    @SerialName("code") val code: Int
) {
    @Serializable
    data class Result(
        @SerialName("songs") val songs: List<Song>? = null,
        @SerialName("artists") val artists: List<Artist>? = null,
        @SerialName("playlists") val playlists: List<Playlist>? = null,
        @SerialName("albums") val albums: List<Album>? = null
    ) {
        @Serializable
        data class Song(
            @SerialName("album") val album: Album,
            @SerialName("duration") val duration: Long,
            @SerialName("artists") val artists: List<Artist>,
            @SerialName("name") val name: String,
            @SerialName("id") val id: Long
        ) {
            @Serializable
            data class Artist(
                @SerialName("name") val name: String,
                @SerialName("id") val id: Long,
                @SerialName("picId") val picId: Long,
                @SerialName("alias") val alias: List<String> = emptyList()
            )

            @Serializable
            data class Album(
                @SerialName("name") val name: String,
                @SerialName("id") val id: Long,
                @SerialName("picId") val picId: Long
            )
        }

        @Serializable
        data class Artist(
            @SerialName("id") val id: Long,
            @SerialName("name") val name: String,
            @SerialName("picUrl") val picUrl: String? = null,
            @SerialName("alias") val alias: List<String> = emptyList()
        )

        @Serializable
        data class Album(
            @SerialName("name") val name: String,
            @SerialName("id") val id: Long,
            @SerialName("picUrl") val picUrl: String,
            @SerialName("size") val size: Int,
            @SerialName("artists") val artists: List<Artist>
        )

        @Serializable
        data class Playlist(
            @SerialName("id") val id: Long,
            @SerialName("name") val name: String,
            @SerialName("coverImgUrl") val coverImgUrl: String,
            @SerialName("creator") val creator: Creator,
            @SerialName("trackCount") val trackCount: Int
        ) {
            @Serializable
            data class Creator(
                @SerialName("nickname") val nickname: String,
                @SerialName("userId") val userId: Long,
                @SerialName("avatarUrl") val avatarUrl: String
            )
        }
    }
}
