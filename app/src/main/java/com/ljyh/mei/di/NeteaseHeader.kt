package com.ljyh.mei.di

data class NeteaseHeader(
    val osver: String,
    val deviceId: String,
    val os: String,
    val appver: String,
    val versioncode: String = "140",
    val mobilename: String = "",
    val buildver: String = System.currentTimeMillis().toString().take(10),
    val resolution: String = "1920x1080",
    val __csrf: String = "",
    val channel: String = "",
    val requestId: String = ""
) {
    // 允许动态添加 token (MUSIC_U/MUSIC_A)
    var MUSIC_U: String? = null
    var MUSIC_A: String? = null
}