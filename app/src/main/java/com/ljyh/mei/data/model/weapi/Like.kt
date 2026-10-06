package com.ljyh.mei.data.model.weapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable

data class Like(
    @SerialName("alg")
    val alg: String = "itembased",
    @SerialName("trackId")
    val trackId: String,
    @SerialName("like")
    val like: Boolean,
    @SerialName("time")
    val time: String = "3"
)

@Serializable

data class LikeResult(
    @SerialName("songs")
    val songs: List<JsonElement>,
    @SerialName("playlistId")
    val playlistId: Long,
    @SerialName("code")
    val code: Int
)