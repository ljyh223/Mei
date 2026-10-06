package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetArtistSong(
    val limit: Int = 50,
    val offset: Int = 0,
    val total: Boolean = true,
)

/** Only the song fields consumed by the artist screen and MediaMetadata mapper. */
@Serializable
data class ArtistSong(
    @SerialName("hotSongs") val hotSongs: List<HotSong> = emptyList(),
    @SerialName("more") val more: Boolean = false,
    @SerialName("code") val code: Int = 0,
) {
    @Serializable
    data class HotSong(
        @SerialName("ar") val artists: List<Artist> = emptyList(),
        @SerialName("al") val album: Album = Album(),
        @SerialName("dt") val duration: Long = 0,
        @SerialName("name") val name: String = "",
        @SerialName("id") val id: Long = 0,
    ) {
        @Serializable
        data class Artist(
            @SerialName("id") val id: Long = 0,
            @SerialName("name") val name: String = "",
            @SerialName("alia") val aliases: List<String> = emptyList(),
        )

        @Serializable
        data class Album(
            @SerialName("id") val id: Long = 0,
            @SerialName("name") val name: String = "",
            @SerialName("pic") val pictureId: Long = 0,
        )
    }
}
