package com.ljyh.mei.di

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.DeviceIdKey
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.encrypt.decryptEApi
import com.ljyh.mei.utils.encrypt.encryptEApi
import com.ljyh.mei.utils.encrypt.encryptWeAPI
import com.ljyh.mei.utils.get
import com.ljyh.mei.utils.getDeviceId
import com.ljyh.mei.utils.netease.NeteaseUtils
import com.ljyh.mei.utils.netease.NeteaseUtils.chooseUserAgent
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
    // 复用 Gson 实例，避免每次请求创建
    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Map::class.java, DynamicMapDeserializer())
            .create()
    }
    private val cachedNuid: String by lazy { NeteaseUtils.getRandomHex(32) }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        val cryptoMode = determineCryptoMethod(url)

        val builder = originalRequest.newBuilder()

        // 0. 伪造国内 IP (解决部分地区限制和风控)
        val fakeIp = NeteaseUtils.getRandomChineseIp()
        builder.addHeader("X-Real-IP", fakeIp)
        builder.addHeader("X-Forwarded-For", fakeIp)

        // 1. 获取基础 OS 信息
        // 注意：EAPI 实际上主要使用 Android/iPhone 的配置，Node 版中 api/eapi 模式下主要用 mobile 配置
        val osInfo = when (cryptoMode) {
            "weapi" -> osMap["pc"]!!
            "eapi", "api" -> osMap["android"]!! // 建议 EAPI 统一伪装成 Android
            else -> osMap["android"]!!
        }

        // 2. 准备 Cookie Map (关键：对标 Node 的 processCookieObject)
        // 必须包含用户登录的 MUSIC_U
        val musicU = AppContext.instance.dataStore[CookieKey] ?: ""
        val cookieMap = mutableMapOf<String, String>()

        // 基础 Cookie
        cookieMap["os"] = osInfo.os
        cookieMap["appver"] = osInfo.appver
        cookieMap["osver"] = osInfo.osver
        cookieMap["deviceId"] = AppContext.instance.dataStore[DeviceIdKey] ?: getDeviceId()
        cookieMap["channel"] = osInfo.channel

        // 关键身份标识 (复刻 Node 逻辑)
        cookieMap["_ntes_nuid"] = cachedNuid
        cookieMap["_ntes_nnid"] = "$cachedNuid,${System.currentTimeMillis()}"
        cookieMap["WNMCID"] = "${NeteaseUtils.getRandomHex(6)}.${System.currentTimeMillis()}.01.0"

        // 登录凭证
        if (musicU.isNotEmpty()) {
            cookieMap["MUSIC_U"] = musicU
        }
        // 如果需要 csrf token (通常从 __csrf cookie 获取，这里简化处理)
        val csrfToken = ""

        // 3. 构建内部 Header 对象 (用于 EAPI Body 和 API Header)
        val neteaseHeader = NeteaseHeader(
            osver = osInfo.osver,
            deviceId = cookieMap["deviceId"]!!,
            os = osInfo.os,
            appver = osInfo.appver,
            channel = osInfo.channel,
            requestId = NeteaseUtils.generateRequestId(),
            __csrf = csrfToken
        ).apply {
            if (musicU.isNotEmpty()) this.MUSIC_U = musicU
        }
        Timber.tag("NeteaseInterceptor").d(neteaseHeader.toString())
        Timber.tag("NeteaseInterceptor").d(cookieMap.toString())

        // 4. 将 Cookie Map 转为 String 并添加到 Header
        // 对于 EAPI/API，Cookie 中也包含了 header 对象里的字段
        val cookieString = buildCookieString(cookieMap, neteaseHeader)
        builder.addHeader("Cookie", cookieString)
        builder.addHeader("User-Agent", chooseUserAgent(cryptoMode, "android"))

        // Referer 处理
        if (cryptoMode == "weapi") {
            builder.addHeader("Referer", "https://music.163.com")
        }

        // 5. 处理加密 (Body 修改)
        handleRequestEncryption(builder, originalRequest, cryptoMode, url, neteaseHeader)

        val response = chain.proceed(builder.build())

        // 6. 响应解密
        return handleResponseDecryption(response, cryptoMode)
    }


    private fun buildCookieString(baseCookie: Map<String, String>, header: NeteaseHeader): String {
        val sb = StringBuilder()

        // 添加基础 Cookie
        baseCookie.forEach { (k, v) ->
            sb.append(URLEncoder.encode(k, "UTF-8")).append("=").append(URLEncoder.encode(v, "UTF-8")).append("; ")
        }

        // 许多接口 (尤其是 api/eapi) 期望 Cookie 中也包含 header 的信息
        // 这里简单地把 header 的某些关键字段也追加进去，模拟 Node 的 createHeaderCookie
        sb.append("requestId=").append(header.requestId).append("; ")
        // osver, appver 等已经在 baseCookie 里了，不需要重复

        return sb.toString()
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
            "weapi" -> {
                // 你的原始逻辑保持不变
                val encryptedData = encryptWeAPI(rawBody)
                val formBody = FormBody.Builder()
                    .add("params", encryptedData.params)
                    .add("encSecKey", encryptedData.encSecKey)
                    .build()
                builder.post(formBody)
            }
            "eapi" -> {
                // [关键修正] EAPI 必须把 Header 塞进 Body
                val mapType = object : TypeToken<MutableMap<String, Any>>() {}.type
                val bodyMap: MutableMap<String, Any> = if (rawBody.isNotEmpty()) {
                    try {
                        gson.fromJson(rawBody, mapType)
                    } catch (e: Exception) {
                        Timber.tag("NeteaseInterceptor").e("rawBody fromJson error: $e")
                        mutableMapOf()
                    }
                } else {
                    Timber.tag("NeteaseInterceptor").d("rawBody is empty")
                    mutableMapOf()
                }

                // Node: data.header = header
//                bodyMap["header"] = gson.toJson(headerObj)
                Timber.tag("NeteaseInterceptor").d("rawBody: $rawBody")
                 bodyMap["e_r"] = true

                val newBodyJson = gson.toJson(bodyMap)

                val apiPath = url.replace("https://interface.music.163.com", "").replace("eapi", "api")
                val encryptedData = encryptEApi(apiPath, newBodyJson)

                val formBody = FormBody.Builder()
                    .add("params", encryptedData.params)
                    .build()
                builder.post(formBody)
            }
            "api" -> {
                // API 模式逻辑，Header 已经在上面添加到 HTTP Headers 里了
                // 将 JSON Body 转为 FormBody 即可
                val formBodyBuilder = FormBody.Builder()
                if (rawBody.isNotEmpty()) {
                    try {
                        val map = gson.fromJson(rawBody, Map::class.java)
                        for ((k, v) in map) {
                            formBodyBuilder.add(k.toString(), v.toString())
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "JSON parse failed in api mode")
                    }
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