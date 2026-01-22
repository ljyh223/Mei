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
import com.ljyh.mei.utils.netease.ChineseIpUtils
import com.ljyh.mei.utils.netease.NeteaseUtils.getWNMCID
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import java.io.IOException
import kotlin.apply

class NeteaseInterceptor : Interceptor {

    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Map::class.java, DynamicMapDeserializer())
            .disableHtmlEscaping()
            .create()
    }

    // 缓存随机值
    private val cachedNuid: String by lazy { createRandomKey(32) }
    private val cachedNmtid: String by lazy { createRandomKey(16) }
    private val fakeIP :String by lazy { ChineseIpUtils.generateRandomChineseIP() }
    private val cachedWnmcid: String by lazy { getWNMCID() } // 只计算一次保持会话一致性

    private val EAPI_CONFIG = mapOf(
        "os" to "pc",
        "osver" to "Microsoft-Windows-10-Professional-build-22631-64bit",
        "appver" to "3.0.18.203152",
        "channel" to "netease",
        "versioncode" to "6006066", // Android 高版本号
        "mobilename" to "Mi+A3",
        "buildver" to "1768990079",
        "resolution" to "2268x1080",
        "ua" to "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.0.18.203152"
    )

    // =========================================================================
    //  配置 B：普通 Android 模式 (用于 weapi/api - 保持手机端正常行为)
    // =========================================================================
    private val ANDROID_CONFIG = mapOf(
        "os" to "android",
        "osver" to "14",
        "appver" to "8.20.20.231215173437",
        "channel" to "xiaomi",
        "versioncode" to "6006066",
        "mobilename" to "Mi+A3",
        "buildver" to System.currentTimeMillis().toString().take(10),
        "resolution" to "2268x1080",
        "ua" to "NeteaseMusic/9.4.32.251222163637" // 或者你之前的 Android UA
    )

    // 公用常量
    private val CONST_NMDI = "Q1NKTQkBDAAMIEF4coQMHcb6TLA7AAAAciOiJ%2F%2FOO4VQ7m%2FLvLJ1pD9CIsJP5mfzI4SusB%2BaNScGLpThEYBcPxGzj0pL5hLdZ7LqB2UVULdYgc0%3D"
    private val CONST_URS_APPID = "F2219AE9D7828A7D73E2006D000C61031D196A37DB497E3885B8298504867886B6F0E44087D61EFC06BE92279CD6EEC6"
    private val CONST_CSRF = "40ab38f0a305fc4c7ff68e636bcf34aa"

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        val cryptoMode = determineCryptoMethod(url)
        val builder = originalRequest.newBuilder()

        val config = if (cryptoMode == "eapi") EAPI_CONFIG else ANDROID_CONFIG

        val deviceId = AppContext.instance.dataStore[DeviceIdKey] ?: getDeviceId()
        val musicU = AppContext.instance.dataStore[CookieKey] ?: ""
        val requestId = "${System.currentTimeMillis()}_${(Math.random() * 1000).toInt().toString().padStart(4, '0')}"

        val cookieMap = buildMap {
            // 基础字段 (动态从 config 取)
            put("os", config["os"]!!)
            put("appver", config["appver"]!!)
            put("osver", config["osver"]!!)
            put("channel", config["channel"]!!)
            put("versioncode", config["versioncode"]!!)
            put("mobilename", config["mobilename"]!!)
            put("buildver", config["buildver"]!!)
            put("resolution", config["resolution"]!!)

            // 固定字段
            put("deviceId", deviceId)
            put("sDeviceId", deviceId) // 部分接口需要这个
            put("ntes_kaola_ad", "1")
            put("_ntes_nuid", cachedNuid)
            put("WNMCID", cachedWnmcid)
            put("URS_APPID", CONST_URS_APPID)
            put("WEVNSM", "1.0.0")
            put("__csrf", CONST_CSRF)
            put("NMDI", CONST_NMDI)
            put("NMTID", cachedNmtid)

            if (musicU.isNotEmpty()) {
                put("MUSIC_U", musicU)
            }
        }

        val neteaseHeader = NeteaseHeader(
            osver = config["osver"]!!,
            deviceId = deviceId,
            os = config["os"]!!,
            appver = config["appver"]!!,
            versioncode = config["versioncode"]!!,
            mobilename = config["mobilename"]!!,
            buildver = config["buildver"]!!,
            resolution = config["resolution"]!!,
            __csrf = CONST_CSRF,
            channel = config["channel"]!!,
            requestId = requestId
        ).apply {
            if (musicU.isNotEmpty()) this.MUSIC_U = musicU
        }
        builder.addHeader("Cookie", buildCookieString(cookieMap))

        // UA 处理：
        // weapi: 永远使用 PC Web UA (Chrome/Edge)，这是 weapi 协议的特性
        // eapi: 使用 Config 中指定的 UA (PC Desktop)
        // api: 使用 Config 中指定的 UA (Android)
        val userAgent = when (cryptoMode) {
            "weapi" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0"
            else -> config["ua"]!!
        }
        builder.addHeader("User-Agent", userAgent)

        if (cryptoMode == "weapi") {
            builder.addHeader("Referer", "https://music.163.com")
        }

        builder.addHeader("X-Real-IP", fakeIP)
        builder.addHeader("X-Forwarded-For", fakeIP)

        handleRequestEncryption(builder, originalRequest, cryptoMode, url, neteaseHeader)

        val response = chain.proceed(builder.build())
        return handleResponseDecryption(response, cryptoMode)
    }

    private fun buildCookieString(map: Map<String, String>): String {
        return map.entries.joinToString("; ") {
            "${it.key}=${it.value}" // 抓包日志中并未对值进行过度 UrlEncode，保持原样即可，除非遇到特殊字符
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
                bodyMap["header"] = gson.toJson(headerObj)
                if(rawBody.contains("checkToken") && (cryptoMode == "api" || cryptoMode == "eapi")){
                    builder.addHeader("X-antiCheatToken","9ca17ae2e6ffcda170e2e6ee8af14fbabdb988f225b3868eb2c15a879b9a83d274a790ac8ff54a97b889d5d42af0feaec3b92af58cff99c470a7eafd88f75e839a9ea7c14e909da883e83fb692a3abdb6b92adee9e")
                }
                // bodyMap["e_r"] = true // 可选，如果遇到 buffer 问题可开启

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
                // API 模式通常用于 login，这里也可以简单处理
                val formBodyBuilder = FormBody.Builder()
                if (rawBody.isNotEmpty()) {
                    try {
                        val map = gson.fromJson(rawBody, Map::class.java)
                        for ((k, v) in map) formBodyBuilder.add(k.toString(), v.toString())
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