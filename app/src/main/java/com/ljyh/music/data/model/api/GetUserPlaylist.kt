package com.ljyh.music.data.model.api

import com.google.gson.annotations.SerializedName


data class GetUserPlaylist(
    @SerializedName("uid")
    val uid: String,
    @SerializedName("limit")
    val limit: String = "50",
    @SerializedName("offset")
    val offset: String = "0",
    @SerializedName("includeVideo")
    val includeVideo: String = "false"
)