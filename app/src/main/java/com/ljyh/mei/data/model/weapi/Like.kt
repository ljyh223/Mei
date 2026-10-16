package com.ljyh.mei.data.model.weapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
