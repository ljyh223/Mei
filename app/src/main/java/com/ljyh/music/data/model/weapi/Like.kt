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