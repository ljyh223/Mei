package com.ljyh.mei.di

import android.util.Log
import androidx.compose.ui.text.toLowerCase
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.DeviceIdKey
import com.ljyh.mei.data.network.QQMusicCApiService
import com.ljyh.mei.data.network.QQMusicUApiService
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.encrypt.createRandomKey
import com.ljyh.mei.utils.encrypt.decryptEApi
import com.ljyh.mei.utils.encrypt.encryptEApi
import com.ljyh.mei.utils.encrypt.encryptWeAPI
import com.ljyh.mei.utils.get
import com.ljyh.mei.utils.getDeviceId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.lang.reflect.Type
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

    private const val APIDOMAIN = "https://interface.music.163.com"
    private const val DOMAIN = "https://music.163.com"
    private const val DEBUG = true

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val encryptionInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()


            val url = originalRequest.url.toString()
            val headersBuilder = originalRequest.headers.newBuilder()


            // 确定加密方式
            val crypto = determineCryptoMethod(url)

            val os = when (crypto) {
                "weapi" -> "pc"
                "eapi" -> "pc"
                "api" -> "iphone"
                else -> "pc"
            }
            val osInfo = osMap[os] ?: OSInfo("", "", "", "")

            // 模拟设置 Cookie 和其他头信息
            val cookie = generateCookie(crypto, osInfo) // 根据加密方式生成 Cookie
            headersBuilder.add("Cookie", cookie)

            if (crypto == "api") {
                headersBuilder.apply {
                    add("User-Agent", chooseUserAgent(crypto, "iphone"))
                    add("osver", osInfo.osver)
                    add("deviceId", AppContext.instance.dataStore[DeviceIdKey] ?: getDeviceId())
                    add("os", osInfo.os)
                    add("appver", osInfo.appver)
                    add("versioncode", "140")
                    add("mobilename", "")
                    add("buildver", System.currentTimeMillis().toString().substring(0, 10))
                    add("resolution", "1920x1080")
                    add("__csrf", "40ab38f0a305fc4c7ff68e636bcf34aa")
                    add("channel", osInfo.channel)
                    add(
                        "requestId",
                        "${System.currentTimeMillis()}_${
                            (Math.random() * 1000).toInt().toString().padStart(4, '0')
                        }"
                    )
                    if (cookie.contains("MUSIC_U")) {
                        add("MUSIC_U", AppContext.instance.dataStore[CookieKey].toString())
                    }
                }

            }



            else {
                val userAgent = chooseUserAgent(crypto, "pc")
                headersBuilder.add("User-Agent", userAgent)
            }

            // 获取请求体
            val originalBody = originalRequest.body
            val requestData = originalBody?.let { body ->
                // 假设请求体是 JSON 格式
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            } ?: ""

            Log.d("Encrypted Data", crypto)
            // 根据加密方式加密请求数据
            val newRequest = when (crypto) {
                "weapi" -> {
                    headersBuilder.add("Referer", DOMAIN)
                    val encryptedData = encryptWeAPI(requestData)
                    val formBody = FormBody.Builder()

                        .add("params", encryptedData.params)
                        .add("encSecKey", encryptedData.encSecKey)
                        .build()


                    originalRequest.newBuilder()
                        .headers(headersBuilder.build())
                        .method(originalRequest.method, formBody)
                        .build()
                }

                "eapi" -> {
                    val api = url.replace(APIDOMAIN, "").replace("eapi", "api")
                    val encryptedData = encryptEApi(api, requestData)
                    val formBody = FormBody.Builder()
                        .add("params", encryptedData.params)
                        .build()


                    originalRequest.newBuilder()
                        .headers(headersBuilder.build())
                        .method(originalRequest.method, formBody)
                        .build()
                }

                "api" -> {

                    println(requestData)
                    val formBodyBuilder = FormBody.Builder()
                    // 注册自定义解析器
                    if (requestData != "") {
                        val gson = GsonBuilder()
                            .registerTypeAdapter(Map::class.java, DynamicMapDeserializer())
                            .create()
                        val res = gson.fromJson(requestData, Map::class.java)
                        for ((key, value) in res) {
                            formBodyBuilder.add(key.toString(), value.toString())
                        }
                    }
                    originalRequest.newBuilder()
                        .headers(headersBuilder.build())
                        .method(originalRequest.method, formBodyBuilder.build())
                        .build()
                }

                else -> originalRequest.newBuilder()
                    .headers(headersBuilder.build())
                    .method(originalRequest.method, originalBody)
                    .build()
            }
            val response = chain.proceed(newRequest)
            val responseBody = response.body
            // 解密响应数据
            if (crypto == "eapi") {
                val decryptedResponseBody = responseBody.let { body ->
                    Log.d("Decrypted Response", "eapi")
                    val encryptedBytes = body.bytes()
                    val decryptedBytes = decryptEApi(encryptedBytes)
//                    val decryptedBytes = replaceRandomKey(decryptEApi(encryptedBytes))
                    decryptedBytes.toResponseBody(body.contentType())
                }

                response.newBuilder()
                    .body(decryptedResponseBody)
                    .build()
            } else {
                response
            }
        }

        return OkHttpClient.Builder().apply {
            if (DEBUG) {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {
                    }

                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val hostnameVerifier = HostnameVerifier { _, _ -> true }
                // 创建 SSL 上下文并初始化
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                hostnameVerifier(hostnameVerifier)
            }

            addInterceptor(loggingInterceptor)
            addInterceptor(encryptionInterceptor)
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)

        }.build()

    }

    @Provides
    @Singleton
    @Named("WeApiRetrofit")
    fun provideWeApiRetrofit(okHttpClient: OkHttpClient): Retrofit {


        return Retrofit.Builder()
            .baseUrl(DOMAIN)
            .addConverterFactory(GsonConverterFactory.create()) // 仅 WeApiService 使用
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(APIDOMAIN)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }


    // WeApiService 使用特定的 Retrofit
    @Provides
    @Singleton
    fun provideWeApiService(@Named("WeApiRetrofit") retrofit: Retrofit): WeApiService {
        return retrofit.create(WeApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideEApiService(retrofit: Retrofit): EApiService {
        return retrofit.create(EApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }


    @Singleton
    @Provides
    @Named("qqMusicRetrofitC")
    fun provideQQMusicRetrofitC(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://c.y.qq.com/")  // 另一个 API 的 baseUrl
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideQQMusicApiServiceC(@Named("qqMusicRetrofitC") retrofit: Retrofit): QQMusicCApiService {
        return retrofit.create(QQMusicCApiService::class.java)
    }


    @Singleton
    @Provides
    @Named("qqMusicRetrofitU")
    fun provideQQMusicRetrofitU(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://u.y.qq.com/")  // 另一个 API 的 baseUrl
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideQQMusicApiServiceU(@Named("qqMusicRetrofitU") retrofit: Retrofit): QQMusicUApiService {
        return retrofit.create(QQMusicUApiService::class.java)
    }

    private fun determineCryptoMethod(url: String): String {
        // 根据 URL 或其他条件确定加密方式
        return when {
            url.contains("/weapi/") -> "weapi"
            url.contains("/eapi/") -> "eapi"
            else -> "api"
        }
    }


    private fun chooseUserAgent(crypto: String, os: String): String {
        val userAgentMap = mapOf(
            "weapi" to mapOf(
                "pc" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0",
            ),
            "linuxapi" to mapOf(
                "linux" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36"
            ),
            "api" to mapOf(
                "pc" to "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.0.18.203152",
                "android" to "NeteaseMusic/9.1.65.240927161425(9001065);Dalvik/2.1.0 (Linux; U; Android 14; 23013RK75C Build/UKQ1.230804.001",
                "iphone" to "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)"
            )
        )

        return userAgentMap[crypto]?.get(os) ?: ""
    }
}

private fun generateCookie(crypto: String, osInfo: OSInfo): String {
    // 根据加密方式生成 Cookie
    // 这里可以根据需要实现具体的 Cookie 生成逻辑

    val _ntes_nuid = createRandomKey(32)

    val cookie = mapOf(
        "ntes_kaola_ad" to 1,
        "_ntes_nuid" to _ntes_nuid,
        "WNMCID" to getWNMCID(),
        "versioncode" to "6006066",
        "URS_APPID" to "F2219AE9D7828A7D73E2006D000C61031D196A37DB497E3885B8298504867886B6F0E44087D61EFC06BE92279CD6EEC6",
        "buildver" to System.currentTimeMillis().toString().substring(0, 10),
        "resolution" to "2268x1080",
        "WEVNSM" to "1.0.0",
        "sDeviceId" to "bnVsbAkwMjowMDowMDowMDowMDowMAk5MzQwMWVlNWU4MzBlODIzCWVhMmY2OTJlYTQ3NDFhZmQ%3D",
        "mobilename" to "23013RK75C",
        "deviceId" to "bnVsbAkwMjowMDowMDowMDowMDowMAk5MzQwMWVlNWU4MzBlODIzCWVhMmY2OTJlYTQ3NDFhZmQ%3D",
        "__csrf" to "40ab38f0a305fc4c7ff68e636bcf34aa",
        "NMDI" to "Q1NKTQkBDAAMIEF4coQMHcb6TLA7AAAAciOiJ%2F%2FOO4VQ7m%2FLvLJ1pD9CIsJP5mfzI4SusB%2BaNScGLpThEYBcPxGzj0pL5hLdZ7LqB2UVULdYgc0%3D",
        "osver" to osInfo.osver,
        "os" to osInfo.os,
        "channel" to osInfo.channel,
        "appver" to osInfo.appver,
        "NMTID" to createRandomKey(16),
        "MUSIC_U" to AppContext.instance.dataStore[CookieKey].toString()
    )

    return cookie.map { (key, value) -> "$key=$value" }.joinToString(";")
}


data class OSInfo(
    val os: String,
    val appver: String,
    val osver: String,
    val channel: String
)

fun getWNMCID(): String {
    val characters = "abcdefghijklmnopqrstuvwxyz"
    var randomString = ""
    for (i in 0 until 6) {
        randomString += characters.random()
    }
    return "$randomString.${System.currentTimeMillis()}.01.0"
}

// 使用 Map 来存储操作系统信息
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

// 自定义适配器，动态处理 JSON 中的数字类型
class DynamicMapDeserializer : JsonDeserializer<Map<String, Any>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Map<String, Any> {
        return parseJsonElement(json) as Map<String, Any>
    }

    // 递归解析 JsonElement，自动判断类型
    private fun parseJsonElement(json: JsonElement): Any {
        return when {
            json.isJsonObject -> {
                val map = mutableMapOf<String, Any>()
                json.asJsonObject.entrySet().forEach { (key, value) ->
                    map[key] = parseJsonElement(value)
                }
                map
            }

            json.isJsonArray -> {
                json.asJsonArray.map { parseJsonElement(it) }
            }

            json.isJsonPrimitive -> {
                val primitive = json.asJsonPrimitive
                when {
                    primitive.isNumber -> {
                        // 自动根据数字是否有小数点，决定使用 Int 还是 Double
                        val number = primitive.asNumber
                        if (number.toDouble() % 1 == 0.0) number.toInt() else number.toDouble()
                    }

                    primitive.isBoolean -> primitive.asBoolean
                    else -> primitive.asString
                }
            }

            else -> json.toString() // 其他情况，直接返回字符串形式
        }
    }
}

// 替换随机键
//fun replaceRandomKey(json: String): String {
//    val regex = Regex("\"(rcmd_rank_module|home_common_rcmd_songs_module)_[a-zA-Z0-9_-]+\"")
//    return json.replace(regex)  { matchResult ->
//        when (matchResult.value.substring(1,  matchResult.value.indexOf('_')))  {
//            "rcmd_rank_module" -> "\"${SpecialKey.Rank}\""
//            "home_common_rcmd_songs_module" -> "\"${SpecialKey.HomeCommon}\""
//            else -> matchResult.value
//        }
//    }
//}


fun replaceRandomKey(json: String): String {
    val regexRank = Regex("\"rcmd_rank_module_[a-zA-Z0-9]+\"")
    val regexHomeCommon = Regex("\"home_common_rcmd_songs_module_[a-zA-Z0-9]+\"")
    val regexRadar=Regex("\"home_radar_playlist_module_[a-zA-Z0-9]+\"")
    val regexCommonPlaylist=Regex("\"home_page_common_playlist_module_[a-zA-Z0-9]+\"")

    val replacements = mapOf(
        regexRank to "\"${SpecialKey.Rank}\"",
        regexHomeCommon to "\"${SpecialKey.HomeCommon}\"",
        regexRadar to "\"${SpecialKey.Radar}\"",
        regexCommonPlaylist to "\"${SpecialKey.CommonPlaylist}\""
    )
    var modifiedJson = json
    replacements.forEach { (regex, replacement) ->
        val matches = regex.findAll(modifiedJson)
        Log.d("replaceRandomKey", "regex: $regex, replacement: $replacement")
        Log.d("replaceRandomKey", matches.count().toString())
        Log.d("replaceRandomKey", matches.toString())
        modifiedJson = modifiedJson.replace(regex, replacement)
    }
    return modifiedJson
}

object SpecialKey {
    const val Rank = "rank"
    const val HomeCommon = "home_common"
    const val Radar = "radar"
    const val CommonPlaylist="common_playlist"
}
