package com.ljyh.music.data.model.api

data class GetSongUrlV1(
    val ids: String = "[]",
    val level: String = "standard",
    val encodeType: String = "flac",
)


data class GetSongUrl(
    var ids: String,
    val br: Int = 999000,
){
    init {
        ids= "[${ids}]"
    }
}