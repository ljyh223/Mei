package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable

data class GetSongUrlV1(
    @SerialName("ids")
    var ids: String,
    @SerialName("level")
    //采用 standard, exhigh, lossless, hires, jyeffect(高清环绕声), sky(沉浸环绕声), jymaster(超清母带) 进行音质判断
    var level: String,
    @SerialName("encodeType")
    var encodeType: String = "flac",
    @SerialName("immerseType")
    var immerseType: String? = null
) {
    init {
        if (level == "sky") {
            immerseType = "c51"
        }
    }
}

@Serializable

data class GetSongUrl(
    @SerialName("ids")
    var ids: String,
    @SerialName("br")
    var br: Int = 999000,
) {
    init {
        ids = "[${ids}]"
    }
}