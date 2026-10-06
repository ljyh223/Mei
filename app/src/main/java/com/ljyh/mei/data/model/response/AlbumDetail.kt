package com.ljyh.mei.data.model.response

import com.ljyh.mei.data.model.domain.MediaMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Album detail fields consumed by the album screen and its MediaMetadata mapper. */
@Serializable
data class AlbumDetail(
    @SerialName("resourceState") val resourceState: Boolean = true,
    @SerialName("songs") val songs: List<Song> = emptyList(),
    @SerialName("code") val code: Int = 0,
    @SerialName("album") val album: Album = Album(),
) {
    @Serializable
    data class Song(
        @SerialName("ar") val artists: List<Artist> = emptyList(),
        @SerialName("al") val album: SongAlbum = SongAlbum(),
        @SerialName("dt") val duration: Long = 0,
        @SerialName("name") val name: String = "",
        @SerialName("id") val id: Long = 0,
    ) {
        @Serializable
        data class Artist(
            @SerialName("id") val id: Long = 0,
            @SerialName("name") val name: String = "",
        )

        @Serializable
        data class SongAlbum(
            @SerialName("id") val id: Long = 0,
            @SerialName("name") val name: String = "",
            @SerialName("pic") val pictureId: Long = 0,
        )
    }

    @Serializable
    data class Album(
        @SerialName("artists") val artists: List<Artist> = emptyList(),
        @SerialName("description") val description: String = "",
        @SerialName("id") val id: Long = 0,
        @SerialName("name") val name: String = "",
        @SerialName("picUrl") val picUrl: String = "",
        @SerialName("size") val size: Int = 0,
    ) {
        @Serializable
        data class Artist(@SerialName("name") val name: String = "")
    }
}
