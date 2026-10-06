package com.ljyh.mei.data.model.weapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** FM response fields used to build queue MediaMetadata. */
@Serializable
data class Radio(
    @SerialName("code") val code: Int = 0,
    @SerialName("data") val data: List<Data> = emptyList(),
)

@Serializable
data class Data(
    @SerialName("album") val album: Album = Album(),
    @SerialName("artists") val artists: List<ArtistXX> = emptyList(),
    @SerialName("duration") val duration: Int = 0,
    @SerialName("id") val id: Long = 0,
    @SerialName("name") val name: String = "",
    @SerialName("transNames") val transNames: List<String>? = null,
)

@Serializable
data class Album(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("picUrl") val picUrl: String = "",
)

@Serializable
data class ArtistXX(
    @SerialName("alias") val alias: List<String>? = null,
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
)
