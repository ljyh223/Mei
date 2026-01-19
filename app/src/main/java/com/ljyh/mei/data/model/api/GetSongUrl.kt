package com.ljyh.mei.data.model.api

import com.google.gson.annotations.SerializedName

data class GetSongUrlV1(
    @SerializedName("ids")
    var ids: String,
    @SerializedName("level")
    //采用 standard, exhigh, lossless, hires, jyeffect(高清环绕声), sky(沉浸环绕声), jymaster(超清母带) 进行音质判断
    var level: String,
    @SerializedName("encodeType")
    var encodeType: String = "flac",
    @SerializedName("immerseType")
    var immerseType: String? = null
) {
    init {
        if (level == "sky") {
            immerseType = "c51"
        }
    }
}


data class GetSongUrl(
    @SerializedName("ids")
    var ids: String,
    @SerializedName("br")
    var br: Int = 999000,
) {
    init {
        ids = "[${ids}]"
    }
}