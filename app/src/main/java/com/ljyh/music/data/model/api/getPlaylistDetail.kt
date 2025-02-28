package com.ljyh.music.data.model.api

import com.google.gson.annotations.SerializedName

data class GetPlaylistDetail(
    @SerializedName("id")
    val id:String,
    @SerializedName("n")
    val n:String="5000",
    @SerializedName("s")
    val s:String="8"
)