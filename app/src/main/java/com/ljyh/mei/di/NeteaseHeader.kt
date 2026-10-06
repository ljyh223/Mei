package com.ljyh.mei.di

import kotlinx.serialization.Serializable

@Serializable
data class NeteaseHeader(
    val osver: String,
    val deviceId: String,
    val os: String,
    val appver: String,
    val versioncode: String,     // 关键：必须是高版本号
    val mobilename: String,      // 关键：手机型号
    val buildver: String,
    val resolution: String,
    val __csrf: String = "",
    val channel: String = "",
    val requestId: String = ""
) {
    // Kotlin serialization includes these values when present in the encrypted EAPI header.
    var MUSIC_U: String? = null
    var MUSIC_A: String? = null
}
