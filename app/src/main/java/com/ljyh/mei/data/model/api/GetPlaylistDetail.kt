package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable

data class GetPlaylistDetail(
    @SerialName("id")
    val id:String,
    @SerialName("n")
    val n:String="5000",
    @SerialName("s")
    val s:String="8"
)