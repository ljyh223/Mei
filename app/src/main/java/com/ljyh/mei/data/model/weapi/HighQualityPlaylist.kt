package com.ljyh.mei.data.model.weapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HighQualityPlaylist(
    @SerialName("cat") val category: String = "全部",
    @SerialName("limit") val limit: Int = 50,
    @SerialName("lasttime") val lastTime: Long = 0L,
    @SerialName("total") val total: Boolean = true,
)

/** Only grid card and pagination data used by FindMusicScreen. */
@Serializable
data class HighQualityPlaylistResult(
    @SerialName("code") val code: Int = 0,
    @SerialName("lasttime") val lasttime: Long = 0,
    @SerialName("more") val more: Boolean = false,
    @SerialName("playlists") val playlists: List<Playlists> = emptyList(),
    @SerialName("total") val total: Int = 0,
)

@Serializable
data class Playlists(
    @SerialName("coverImgUrl") val coverImgUrl: String = "",
    @SerialName("id") val id: Long = 0,
    @SerialName("name") val name: String = "",
    @SerialName("playCount") val playCount: Int = 0,
)
