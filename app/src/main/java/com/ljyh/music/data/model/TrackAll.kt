package com.ljyh.music.data.model
import com.google.gson.annotations.SerializedName


data class TrackAll(
    @SerializedName("code")
    val code: Int,
    @SerializedName("privileges")
    val privileges: List<PlaylistDetail.Privilege>,
    @SerializedName("songs")
    val songs: List<PlaylistDetail.Playlist.Track>
)