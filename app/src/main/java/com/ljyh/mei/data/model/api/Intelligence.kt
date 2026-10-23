package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetIntelligence(
    @SerialName("songId") val songId: String,
    val playlistId: String,
    val startMusicId: String,
    val type: String = "fromPlayOne",
    val count: Int = 1,
)

@Serializable
data class Intelligence(
    val code: Int,
    val data: List<Data> = emptyList(),
    val message: String? = null,
)

@Serializable
data class Data(
    val id: Long = 0,
    val songInfo: SongInfo? = null,
    val alg: String? = null,
    val recommended: Boolean = false,
)

// Only playback metadata is required. Quality, privilege and cloud-upload fields may be null or absent.
@Serializable
data class SongInfo(
    val id: Long,
    val name: String = "",
    val al: Al? = null,
    val ar: List<Ar> = emptyList(),
    val dt: Long = 0,
    val tns: List<String>? = null,
)

@Serializable
data class Al(
    val id: Long = 0,
    val name: String? = null,
    val picUrl: String? = null,
)

@Serializable
data class Ar(
    val id: Long = 0,
    val name: String? = null,
    val tns: List<String>? = null,
)
