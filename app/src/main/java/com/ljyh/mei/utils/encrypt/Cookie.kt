package com.ljyh.mei.utils.encrypt

import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.AndroidIdKey
import com.ljyh.mei.constants.AndroidUserAgent
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.get

// 可以放在同文件或单独的 NetworkUtils.kt 中
data class OSInfo(
    val os: String,
    val appver: String,
    val osver: String,
    val channel: String
)


fun generateCookie(osInfo: OSInfo): String {
    val deviceId = AppContext.instance.dataStore[AndroidIdKey] ?: getAndroidId()
    val musicU = AppContext.instance.dataStore[CookieKey]?: ""

    // 使用 buildMap 更加直观
    val cookieMap = buildMap {
        put("ntes_kaola_ad", "1")
        put("_ntes_nuid", createRandomKey(32))
        put("WNMCID", getWNMCID())
        put("versioncode", "6006066") // 建议抽取为常量
        put("URS_APPID", "F2219AE9D7828A7D73E2006D000C61031D196A37DB497E3885B8298504867886B6F0E44087D61EFC06BE92279CD6EEC6")
        put("buildver", System.currentTimeMillis().toString().take(10))
        put("resolution", "2268x1080")
        put("WEVNSM", "1.0.0")
        put("sDeviceId", deviceId)
        put("mobilename", "Mi+A3")
        put("deviceId", deviceId)
        put("__csrf", "40ab38f0a305fc4c7ff68e636bcf34aa")
        put("NMDI", "Q1NKTQkBDAAMIEF4coQMHcb6TLA7AAAAciOiJ%2F%2FOO4VQ7m%2FLvLJ1pD9CIsJP5mfzI4SusB%2BaNScGLpThEYBcPxGzj0pL5hLdZ7LqB2UVULdYgc0%3D")
        put("osver", osInfo.osver)
        put("os", osInfo.os)
        put("channel", osInfo.channel)
        put("appver", osInfo.appver)
        put("NMTID", createRandomKey(16))
        if (musicU.isNotEmpty()) {
            put("MUSIC_U", musicU)
        }
    }

    return cookieMap.entries.joinToString(";") { "${it.key}=${it.value}" }
}

fun chooseUserAgent(crypto: String, os: String): String {
    return when (crypto) {
        "weapi" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0"
        "linuxapi" -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36"
        else -> when (os) {
            "android" -> AndroidUserAgent
            "iphone" -> "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)"
            else -> "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.0.18.203152"
        }
    }
}

fun getWNMCID(): String {
    val characters = "abcdefghijklmnopqrstuvwxyz"
    var randomString = ""
    for (i in 0 until 6) {
        randomString += characters.random()
    }
    return "$randomString.${System.currentTimeMillis()}.01.0"
}