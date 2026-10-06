package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetLyric(
    @SerialName("id")
    val id: String,
    @SerialName("lv")
    val lv: String = "-1",
    @SerialName("kv")
    val kv: String = "-1",
    @SerialName("tv")
    val tv: String = "-1"
)

@Serializable

data class GetLyricV1(
    @SerialName("id")
    val id: String,
    @SerialName("cp")
    val cp: Boolean=false,
    @SerialName("tv")
    val tv: Int=0,
    @SerialName("lv")
    val lv: Int=0,
    @SerialName("rv")
    val rv: Int=0,
    @SerialName("kv")
    val kv: Int=0,
    @SerialName("yv")
    val yv: Int=0,
    @SerialName("ytv")
    val ytv: Int=0,
    @SerialName("yrv")
    val yrv: Int=0,
)
