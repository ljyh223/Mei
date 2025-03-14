package com.ljyh.music.di

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.ljyh.music.AppContext
import com.ljyh.music.constants.CookieKey
import com.ljyh.music.constants.DeviceIdKey
import com.ljyh.music.data.network.QQMusicCApiService
import com.ljyh.music.data.network.QQMusicUApiService
import com.ljyh.music.data.network.api.ApiService
import com.ljyh.music.data.network.api.EApiService
import com.ljyh.music.data.network.api.WeApiService
import com.ljyh.music.utils.dataStore
import com.ljyh.music.utils.encrypt.createRandomKey
import com.ljyh.music.utils.encrypt.decryptEApi
import com.ljyh.music.utils.encrypt.encryptEApi
import com.ljyh.music.utils.encrypt.encryptWeAPI
import com.ljyh.music.utils.get
import com.ljyh.music.utils.getDeviceId
import com.ljyh.music.utils.getRandomString
import com.ljyh.music.utils.getWNMCID
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
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

    private const val BASE_URL = "https://interface.music.163.com/"
    private const val DEBUG = true

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
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
            val osInfo = osMap[os]?:OSInfo("","","","")

            // 模拟设置 Cookie 和其他头信息
            val cookie = generateCookie(crypto, osInfo) // 根据加密方式生成 Cookie
            headersBuilder.add("Cookie", cookie)

            if (crypto == "api") {
                headersBuilder.apply {
                    add("User-Agent", chooseUserAgent(crypto, "iphone"))
                    add("osver", osInfo.osver )
                    add("deviceId", AppContext.instance.dataStore[DeviceIdKey] ?: getDeviceId())
                    add("os", osInfo.os )
                    add("appver", osInfo.appver)
                    add("versioncode", "140")
                    add("mobilename", "")
                    add("buildver", System.currentTimeMillis().toString().substring(0, 10))
                    add("resolution", "1920x1080")
                    add("__csrf", "")
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

            } else {
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
                    headersBuilder.add("Referer", BASE_URL)
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
                    val encryptedData = encryptEApi(url, requestData)
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
            if (crypto == "weapi") {
                val decryptedResponseBody = responseBody?.let { body ->
                    Log.d("Decrypted Response", "weapi")
                    Log.d("Decrypted Response", body.toString())
                    val encryptedBytes = body.bytes()
                    val decryptedBytes = decryptEApi(encryptedBytes)
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
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideWeApiService(retrofit: Retrofit): WeApiService {
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
    val MUSIC_U =
        "006503750A4B416ACE83191355213EC46491BFF92105213CD7A0101B11154F3FED93F869B0D0825102E06C02015190F81595D4185F02B7731650640BEA3F9C7F2BEA930E6D0F38C7486D8AF272284E56ECAB1A5727A6ACD8626FD01F2F53081A35C698F3575AB57141D46503F32176EC64F51EAA2469577AD63278642866EC5A305F4B332ECE583EC28FF34476F6A11F46B7608E193D7E9ECA1700B285AE1F231CC8BCF9E0533F742F6B00B05B9254571CB4D506708CF3993607B69CEA8B8143402BBDC918B433E9ACE157578C1E95E568C1A0BE38FA2033E9EBB646C7AAB79D19760949851141F75A1F966ABB8714709137B58AFBF20289AF9C374B74CE569D7B9D51CC5CAB37C94385A31E30C002C039FB5EA6C18C94C84762F498B9D897396C3918429C846D502E3454A25BC9F5F5035986801A3FB92CE445BBE521437224DC10F60CFBE5323B0563A89D262D1AD82F"


    val _ntes_nuid = createRandomKey(32)

    val cookie = mapOf(
        "ntes_kaola_ad" to 1,
        "_ntes_nuid" to _ntes_nuid,
        "_ntes_nnid" to "${_ntes_nuid},${System.currentTimeMillis()}",
        "WNMCID" to getWNMCID(),
        "WEVNSM" to "1.0.0",
        "deviceId" to AppContext.instance.dataStore[DeviceIdKey],
        "osver" to osInfo?.osver,
        "os" to osInfo?.os,
        "channel" to osInfo?.channel,
        "appver" to osInfo?.appver,
        "NMTID" to getRandomString(),
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

