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

    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Map::class.java, DynamicMapDeserializer())
            .disableHtmlEscaping()
            .create()
    }

    // 缓存随机值
    private val cachedNuid: String by lazy { createRandomKey(32) }
    private val cachedNmtid: String by lazy { createRandomKey(16) }
    private val cachedWnmcid: String by lazy { getWNMCID() } // 只计算一次保持会话一致性

    // =========================================================================
    //  配置区域：完全复刻 Success 抓包日志中的 PC 混合模式
    // =========================================================================

    // 1. 身份伪装：PC
    private val CONFIG_OS = "pc"
    private val CONFIG_OSVER = "Microsoft-Windows-10-Professional-build-22631-64bit"
    private val CONFIG_APPVER = "3.0.18.203152"
    private val CONFIG_CHANNEL = "netease"

    // 2. 关键混合参数：使用 Android 的高版本号和特定设备名
    private val CONFIG_VERSION_CODE = "6006066"
    private val CONFIG_MOBILENAME = "Mi+A3"
    private val CONFIG_BUILDVER = "1768990079" // 对应抓包中的 buildver
    private val CONFIG_RESOLUTION = "2268x1080"

    // 3. 固定指纹：来自抓包
    private val CONST_NMDI = "Q1NKTQkBDAAMIEF4coQMHcb6TLA7AAAAciOiJ%2F%2FOO4VQ7m%2FLvLJ1pD9CIsJP5mfzI4SusB%2BaNScGLpThEYBcPxGzj0pL5hLdZ7LqB2UVULdYgc0%3D"
    private val CONST_URS_APPID = "F2219AE9D7828A7D73E2006D000C61031D196A37DB497E3885B8298504867886B6F0E44087D61EFC06BE92279CD6EEC6"
    private val CONST_CSRF = "40ab38f0a305fc4c7ff68e636bcf34aa"

    // 4. User-Agent：必须使用 PC 的 UA
    private val CONFIG_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.0.18.203152"

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        val cryptoMode = determineCryptoMethod(url)
        val builder = originalRequest.newBuilder()

        // 1. 准备动态数据
        val deviceId = AppContext.instance.dataStore[DeviceIdKey] ?: getDeviceId()
        val musicU = AppContext.instance.dataStore[CookieKey] ?: ""
        val requestId = "${System.currentTimeMillis()}_${(Math.random() * 1000).toInt().toString().padStart(4, '0')}"

        // 2. 构建 Cookie Map (严格按照抓包顺序和内容)
        val cookieMap = buildMap {
            put("ntes_kaola_ad", "1")
            put("_ntes_nuid", cachedNuid)
            put("WNMCID", cachedWnmcid)
            put("versioncode", CONFIG_VERSION_CODE)
            put("URS_APPID", CONST_URS_APPID)
            put("buildver", CONFIG_BUILDVER)
            put("resolution", CONFIG_RESOLUTION)
            put("WEVNSM", "1.0.0")
            put("sDeviceId", deviceId)
            put("mobilename", CONFIG_MOBILENAME)
            put("deviceId", deviceId)
            put("__csrf", CONST_CSRF)
            put("NMDI", CONST_NMDI)
            // 这里是关键差异点：强制设为 PC
            put("osver", CONFIG_OSVER)
            put("os", CONFIG_OS)
            put("channel", CONFIG_CHANNEL)
            put("appver", CONFIG_APPVER)

            put("NMTID", cachedNmtid)

            if (musicU.isNotEmpty()) {
                put("MUSIC_U", musicU)
            }
        }

        // 3. 构建 Header 对象 (用于 EAPI Body)
        // 这里的字段必须与 Cookie 中的字段逻辑一致
        val neteaseHeader = NeteaseHeader(
            osver = CONFIG_OSVER,
            deviceId = deviceId,
            os = CONFIG_OS, // 强制 PC
            appver = CONFIG_APPVER, // 强制 PC 版本
            versioncode = CONFIG_VERSION_CODE,
            mobilename = CONFIG_MOBILENAME,
            buildver = CONFIG_BUILDVER,
            resolution = CONFIG_RESOLUTION,
            __csrf = CONST_CSRF,
            channel = CONFIG_CHANNEL,
            requestId = requestId
        ).apply {
            if (musicU.isNotEmpty()) this.MUSIC_U = musicU
        }

        // 4. 注入 Header 和 Cookie
        builder.addHeader("Cookie", buildCookieString(cookieMap))

        // 覆盖 UA：weapi 用 Mac/PC UA，其他请求强制用抓包里的 PC UA
        // 这样可以确保服务器认为我们是 PC 客户端
        val finalUserAgent = if (cryptoMode == "weapi") {
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0"
        } else {
            CONFIG_USER_AGENT
        }
        builder.addHeader("User-Agent", finalUserAgent)

        // 保持 Referer 逻辑
        if (cryptoMode == "weapi") {
            builder.addHeader("Referer", "https://music.163.com")
        }

        // 5. 处理加密 (EAPI Body 注入)
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

                // [关键]：将 Header 注入 Body
                // 此时 headerObj.os 是 "pc"，服务器解密后会根据这个 os 字段下发 PC 版数据
                bodyMap["header"] = headerObj
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