package com.ljyh.mei.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Netease user playlist fields used to persist and display the user's playlists. */
@Serializable
data class UserPlaylist(
    @SerialName("code") val code: Int = 0,
    @SerialName("more") val more: Boolean = false,
    @SerialName("playlist") val playlist: List<Playlist> = emptyList(),
) {
    @Serializable
    data class Playlist(
        @SerialName("id") val id: Long = 0,
        @SerialName("name") val name: String = "",
        @SerialName("coverImgUrl") val coverImgUrl: String = "",
        @SerialName("creator") val creator: Creator = Creator(),
        @SerialName("playCount") val playCount: Long = 0,
        @SerialName("trackCount") val trackCount: Int = 0,
    ) {
        @Serializable
        data class Creator(
            @SerialName("avatarUrl") val avatarUrl: String = "",
            @SerialName("nickname") val nickname: String = "",
            @SerialName("userId") val userId: Long = 0,
        )
    }
}
