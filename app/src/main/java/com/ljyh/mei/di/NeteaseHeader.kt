package com.ljyh.mei.di

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
    // 允许动态添加 token，这些字段不会自动序列化进 JSON，除非你使用 Gson 的 enableComplexMapKeySerialization 或者手动处理
    // 但在我们的拦截器中，我们会手动把这个对象转成 Map 或者直接注入
    var MUSIC_U: String? = null
    var MUSIC_A: String? = null
}