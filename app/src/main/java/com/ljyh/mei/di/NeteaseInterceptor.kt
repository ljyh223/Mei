package com.ljyh.mei.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.DeviceIdKey
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.encrypt.OSInfo
import com.ljyh.mei.utils.encrypt.chooseUserAgent
import com.ljyh.mei.utils.encrypt.decryptEApi
import com.ljyh.mei.utils.encrypt.encryptEApi
import com.ljyh.mei.utils.encrypt.encryptWeAPI
import com.ljyh.mei.utils.encrypt.generateCookie
import com.ljyh.mei.utils.get
import com.ljyh.mei.utils.getDeviceId
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

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        val cryptoMode = determineCryptoMethod(url)

        // 1. 准备基础请求构建器
        val builder = originalRequest.newBuilder()

        // 2. 获取对应的 OS 信息
        val osType = when (cryptoMode) {
            "weapi", "eapi" -> "pc"
            "api" -> "iphone"
            else -> "pc"
        }
        val osInfo = osMap[osType] ?: osMap["pc"]!!

        // 3. 处理 Header 和 Cookie
        handleHeadersAndCookie(builder, url, cryptoMode, osInfo)

        // 4. 处理请求体加密 (修改 method 和 body)
        handleRequestEncryption(builder, originalRequest, cryptoMode, url)

        // 5. 执行请求
        val response = chain.proceed(builder.build())

        // 6. 处理响应解密
        return handleResponseDecryption(response, cryptoMode)
    }

    private fun handleHeadersAndCookie(
        builder: Request.Builder,
        url: String,
        cryptoMode: String,
        osInfo: OSInfo
    ) {
        val cookie = generateCookie(osInfo)
        builder.addHeader("Cookie", cookie)
        builder.addHeader("User-Agent", chooseUserAgent(cryptoMode, if(cryptoMode == "api") "iphone" else "pc"))

        if (cryptoMode == "api") {
            // API 模式特有的 Header
            builder.apply {
                addHeader("osver", osInfo.osver)
                addHeader("deviceId", AppContext.instance.dataStore[DeviceIdKey] ?: getDeviceId())
                addHeader("os", osInfo.os)
                addHeader("appver", osInfo.appver)
                addHeader("versioncode", "140")
                addHeader("mobilename", "")
                addHeader("buildver", System.currentTimeMillis().toString().take(10))
                addHeader("resolution", "1920x1080")
                addHeader("__csrf", "40ab38f0a305fc4c7ff68e636bcf34aa")
                addHeader("channel", osInfo.channel)
                addHeader("requestId", "${System.currentTimeMillis()}_${(Math.random() * 1000).toInt().toString().padStart(4, '0')}")

                if (cookie.contains("MUSIC_U")) {
                    // 注意：这里 dataStore 取值可能为空，建议做空安全处理
                    AppContext.instance.dataStore[CookieKey]?.let {
                        addHeader("MUSIC_U", it)
                    }
                }
            }
        } else if (cryptoMode == "weapi") {
            builder.addHeader("Referer", "https://music.163.com")
        }
    }

    private fun handleRequestEncryption(
        builder: Request.Builder,
        originalRequest: Request,
        cryptoMode: String,
        url: String
    ) {
        val requestData = getBodyString(originalRequest.body)
        Timber.tag("Encrypted Data").d("Mode: $cryptoMode | Data: $requestData")

        when (cryptoMode) {
            "weapi" -> {
                val encryptedData = encryptWeAPI(requestData)
                val formBody = FormBody.Builder()
                    .add("params", encryptedData.params)
                    .add("encSecKey", encryptedData.encSecKey)
                    .build()
                builder.post(formBody)
            }
            "eapi" -> {
                val apiPath = url.replace("https://interface.music.163.com", "").replace("eapi", "api")
                val encryptedData = encryptEApi(apiPath, requestData)
                val formBody = FormBody.Builder()
                    .add("params", encryptedData.params)
                    .build()
                builder.post(formBody)
            }
            "api" -> {
                // api 模式：将 JSON Body 转为 FormBody
                val formBodyBuilder = FormBody.Builder()
                if (requestData.isNotEmpty()) {
                    try {
                        val map = gson.fromJson(requestData, Map::class.java)
                        for ((k, v) in map) {
                            formBodyBuilder.add(k.toString(), v.toString())
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "JSON parse failed in api mode")
                    }
                }
                builder.post(formBodyBuilder.build())
            }
            // 其他模式保持原样
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