package com.ljyh.mei.data.model.api

import com.google.gson.annotations.SerializedName

data class GetSongUrlV1(
    @SerializedName("ids")
    var ids: String,
    @SerializedName("br")
    var br: Int = 999000,
    @SerializedName("level")
    var level: String = "standard",
    @SerializedName("encodeType")
    var encodeType: String = "flac",
)


data class GetSongUrl(
    @SerializedName("ids")
    var ids: String,
    @SerializedName("br")
    var br: Int = 999000,
){
    init {
        ids= "[${ids}]"
    }
}