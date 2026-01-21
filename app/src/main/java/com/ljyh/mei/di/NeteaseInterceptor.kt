package com.ljyh.mei.di

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.DeviceIdKey
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.encrypt.createRandomKey
import com.ljyh.mei.utils.encrypt.decryptEApi
import com.ljyh.mei.utils.encrypt.encryptEApi
import com.ljyh.mei.utils.encrypt.encryptWeAPI
import com.ljyh.mei.utils.get
import com.ljyh.mei.utils.getDeviceId
import com.ljyh.mei.utils.netease.NeteaseUtils
import com.ljyh.mei.utils.netease.NeteaseUtils.chooseUserAgent
import com.ljyh.mei.utils.netease.NeteaseUtils.getRandomChineseIp
import com.ljyh.mei.utils.netease.NeteaseUtils.getWNMCID
import com.ljyh.mei.utils.netease.OSInfo
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import java.io.IOException
import java.net.URLEncoder
import kotlin.apply

class NeteaseInterceptor : Interceptor {

    val osMap = mapOf(
        "pc" to OSInfo(
            os = "pc",
            appver = "3.0.18.203152",
            osver = "Microsoft-Windows-10-Professional-build-22631-64bit",
            channel = "netease"
        ),
        "linux" to OSInfo(
            os = "linux",
            appver = "1.2.1.0428",
            osver = "Deepin 20.9",
            channel = "netease"
        ),
        "android" to OSInfo(
            os = "android",
            appver = "8.20.20.231215173437",
            osver = "14",
            channel = "xiaomi"
        ),
        "iphone" to OSInfo(
            os = "iPhone OS",
            appver = "9.0.90",
            osver = "16.2",
            channel = "distribution"
        )
    )
    private val ANDROID_VERSION_CODE = "6006066"
    private val ANDROID_RESOLUTION = "2268x1080"
    private val ANDROID_MOBILENAME = "Mi+A3"
    private val ANDROID_BUILDVER = System.currentTimeMillis().toString().take(10)
    // 这是一个特定的设备指纹，尽量保持固定或从你的抓包中获取
    private val CONST_NMDI = "Q1NKTQkBDAAMIEF4coQMHcb6TLA7AAAAciOiJ%2F%2FOO4VQ7m%2FLvLJ1pD9CIsJP5mfzI4SusB%2BaNScGLpThEYBcPxGzj0pL5hLdZ7LqB2UVULdYgc0%3D"
    private val CONST_URS_APPID = "F2219AE9D7828A7D73E2006D000C61031D196A37DB497E3885B8298504867886B6F0E44087D61EFC06BE92279CD6EEC6"
    // 复用 Gson 实例，避免每次请求创建
    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Map::class.java, DynamicMapDeserializer())
            .create()
    }
    private val cachedNuid: String by lazy { createRandomKey(32) }
    private val cachedNmtid: String by lazy { createRandomKey(16) }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        val cryptoMode = determineCryptoMethod(url)

        val builder = originalRequest.newBuilder()

        // 1. 获取基础信息 (保持你的 OSMap 逻辑)
        val osType = if (cryptoMode == "api") "iphone" else "android" // EAPI 强制走 Android 逻辑
        val osInfo = osMap[osType] ?: osMap["android"]!!

        // 2. 准备数据源
        val deviceId = AppContext.instance.dataStore[DeviceIdKey] ?: getDeviceId()
        val musicU = AppContext.instance.dataStore[CookieKey] ?: ""
        val requestId = "${System.currentTimeMillis()}_${(Math.random() * 1000).toInt().toString().padStart(4, '0')}"

        // 3. 构建完整的 Cookie Map (复刻你的 Cookie.kt)
        val cookieMap = buildMap {
            // 基础字段
            put("os", osInfo.os)
            put("appver", osInfo.appver)
            put("osver", osInfo.osver)
            put("deviceId", deviceId)
            put("channel", osInfo.channel)

            // 关键标识符 (来自 Cookie.kt)
            put("ntes_kaola_ad", "1")
            put("_ntes_nuid", cachedNuid)
            put("WNMCID", getWNMCID())
            put("versioncode", ANDROID_VERSION_CODE) // 必须一致
            put("URS_APPID", CONST_URS_APPID)
            put("buildver", ANDROID_BUILDVER)
            put("resolution", ANDROID_RESOLUTION)
            put("WEVNSM", "1.0.0")
            put("sDeviceId", deviceId)
            put("mobilename", ANDROID_MOBILENAME)
            put("__csrf", "40ab38f0a305fc4c7ff68e636bcf34aa")
            put("NMDI", CONST_NMDI) // 必须包含
            put("NMTID", cachedNmtid)

            if (musicU.isNotEmpty()) {
                put("MUSIC_U", musicU)
            }
        }

        // 4. 构建 Header 对象 (用于 EAPI Body)
        // 这里的关键是：versioncode、mobilename 等必须和 Cookie 中的一致
        val neteaseHeader = NeteaseHeader(
            osver = osInfo.osver,
            deviceId = deviceId,
            os = osInfo.os,
            appver = osInfo.appver,
            versioncode = ANDROID_VERSION_CODE, // 关键：高版本
            mobilename = ANDROID_MOBILENAME,
            buildver = ANDROID_BUILDVER,
            resolution = ANDROID_RESOLUTION,
            __csrf = "40ab38f0a305fc4c7ff68e636bcf34aa",
            channel = osInfo.channel,
            requestId = requestId
        ).apply {
            if (musicU.isNotEmpty()) this.MUSIC_U = musicU
        }

        // 5. 设置 Header
        builder.addHeader("Cookie", buildCookieString(cookieMap))
        builder.addHeader("User-Agent", chooseUserAgent(cryptoMode, osType))

        // 伪造 IP
        val fakeIp = getRandomChineseIp()
        builder.addHeader("X-Real-IP", fakeIp)
        builder.addHeader("X-Forwarded-For", fakeIp)

        if (cryptoMode == "weapi") {
            builder.addHeader("Referer", "https://music.163.com")
        }

        // 6. 处理加密 (关键：EAPI 注入 Header)
        handleRequestEncryption(builder, originalRequest, cryptoMode, url, neteaseHeader)

        val response = chain.proceed(builder.build())
        return handleResponseDecryption(response, cryptoMode)
    }

    // 将 Map 转为 Cookie 字符串
    private fun buildCookieString(map: Map<String, String>): String {
        return map.entries.joinToString("; ") {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }
    }

    private fun handleRequestEncryption(
        builder: Request.Builder,
        originalRequest: Request,
        cryptoMode: String,
        url: String,
        headerObj: NeteaseHeader
    ) {
        val rawBody = getBodyString(originalRequest.body)

        when (cryptoMode) {
            "eapi" -> {
                // EAPI: 将 Header 注入 JSON Body
                val mapType = object : TypeToken<MutableMap<String, Any>>() {}.type
                val bodyMap: MutableMap<String, Any> = if (rawBody.isNotEmpty()) {
                    try {
                        gson.fromJson(rawBody, mapType)
                    } catch (e: Exception) {
                        mutableMapOf()
                    }
                } else {
                    mutableMapOf()
                }

                // [关键] 注入 header 对象
//                bodyMap["header"] = headerObj
                // 部分接口可能还需要 e_r 参数
                // bodyMap["e_r"] = true

                val newBodyJson = gson.toJson(bodyMap)
                val apiPath = url.replace("https://interface.music.163.com", "").replace("eapi", "api")

                val encryptedData = encryptEApi(apiPath, newBodyJson)
                builder.post(FormBody.Builder().add("params", encryptedData.params).build())
            }
            "weapi" -> {
                val encryptedData = encryptWeAPI(rawBody)
                builder.post(FormBody.Builder()
                    .add("params", encryptedData.params)
                    .add("encSecKey", encryptedData.encSecKey)
                    .build())
            }
            "api" -> {
                // API 模式处理 (略，与之前相同)
                val formBodyBuilder = FormBody.Builder()
                if (rawBody.isNotEmpty()) {
                    try {
                        val map = gson.fromJson(rawBody, Map::class.java)
                        for ((k, v) in map) {
                            formBodyBuilder.add(k.toString(), v.toString())
                        }
                    } catch (e: Exception) {}
                }
                builder.post(formBodyBuilder.build())
            }
        }
    }



    private fun handleResponseDecryption(response: Response, cryptoMode: String): Response {
        if (cryptoMode == "eapi" && response.isSuccessful) {
            response.body.let { body ->
                try {
                    val encryptedBytes = body.bytes()
                    if (encryptedBytes.isEmpty()) return response

                    Timber.tag("Decrypted Response").d("eapi")
                    val decryptedBytes = decryptEApi(encryptedBytes)
                    val newBody = decryptedBytes.toResponseBody(body.contentType())

                    return response.newBuilder().body(newBody).build()
                } catch (e: IOException) {
                    Timber.e(e, "Decrypt EAPI response failed")
                }
            }
        }
        return response
    }

    private fun getBodyString(requestBody: RequestBody?): String {
        if (requestBody == null) return ""
        return try {
            val buffer = okio.Buffer()
            requestBody.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            ""
        }
    }

    private fun determineCryptoMethod(url: String): String {
        return when {
            url.contains("/weapi/") -> "weapi"
            url.contains("/eapi/") -> "eapi"
            else -> "api"
        }
    }
}