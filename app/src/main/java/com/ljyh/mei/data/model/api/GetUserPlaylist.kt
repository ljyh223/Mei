package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable

data class GetUserPlaylist(
    @SerialName("uid")
    val uid: String,
    @SerialName("limit")
    val limit: String = "100",
    @SerialName("offset")
    val offset: String = "0",
    @SerialName("includeVideo")
    val includeVideo: String = "false"
)