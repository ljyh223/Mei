package com.ljyh.music.data.model.weapi

import com.google.gson.annotations.SerializedName

data class Like(
    @SerializedName("alg")
    val alg: String = "itembased",
    @SerializedName("trackId")
    val trackId: String,
    @SerializedName("like")
    val like: Boolean,
    @SerializedName("time")
    val time: String = "3"
)


data class LikeResult(
    @SerializedName("songs")
    val songs: List<Any>,
    @SerializedName("playlistId")
    val playlistId: Long,
    @SerializedName("code")
    val code: Int
)