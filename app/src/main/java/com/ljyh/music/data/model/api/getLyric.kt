package com.ljyh.music.data.model.api



data class GetLyric(
    val id: String,
    val lv: String = "-1",
    val kv: String = "-1",
    val tv: String = "-1"
)

data class GetLyricV1(
    val id: String,
    val cp: Boolean=false,
    val tv: Int=0,
    val lv: Int=0,
    val rv: Int=0,
    val kv: Int=0,
    val yv: Int=0,
    val ytv: Int=0,
    val yrv: Int=0,
)