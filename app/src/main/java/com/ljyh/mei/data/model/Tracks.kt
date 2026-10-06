package com.ljyh.mei.data.model
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable

data class Tracks(
    @SerialName("code")
    val code: Int,
    @SerialName("songs")
    val songs: List<PlaylistDetail.Playlist.Track>
)
