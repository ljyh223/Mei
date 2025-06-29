package com.ljyh.mei.data.model.api

import com.google.gson.annotations.SerializedName


// 添加@SerializedName注解
data class GetLyric(
    @SerializedName("id")
    val id: String,
    @SerializedName("lv")
    val lv: String = "-1",
    @SerializedName("kv")
    val kv: String = "-1",
    @SerializedName("tv")
    val tv: String = "-1"
)

data class GetLyricV1(
    @SerializedName("id")
    val id: String,
    @SerializedName("cp")
    val cp: Boolean=false,
    @SerializedName("tv")
    val tv: Int=0,
    @SerializedName("lv")
    val lv: Int=0,
    @SerializedName("rv")
    val rv: Int=0,
    @SerializedName("kv")
    val kv: Int=0,
    @SerializedName("yv")
    val yv: Int=0,
    @SerializedName("ytv")
    val ytv: Int=0,
    @SerializedName("yrv")
    val yrv: Int=0,
)


