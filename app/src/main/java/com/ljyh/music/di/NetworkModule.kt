package com.ljyh.music.di

import android.util.Log
import com.google.gson.Gson
import com.ljyh.music.data.network.api.ApiService
import com.ljyh.music.data.network.api.EApiService
import com.ljyh.music.data.network.QQMusicCApiService
import com.ljyh.music.data.network.QQMusicUApiService
import com.ljyh.music.data.network.api.WeApiService
import com.ljyh.music.utils.encrypt.createRandomKey
import com.ljyh.music.utils.encrypt.decryptEApi
import com.ljyh.music.utils.encrypt.encryptEApi
import com.ljyh.music.utils.encrypt.encryptWeAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
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
import kotlin.reflect.full.memberProperties


@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

    private const val BASE_URL = "https://interface.music.163.com/"

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

            // 模拟设置 Cookie 和其他头信息
            val cookie = generateCookie(crypto) // 根据加密方式生成 Cookie
            headersBuilder.add("Cookie", cookie)

            // 设置 User-Agent
            val userAgent = chooseUserAgent(crypto, "pc")
            headersBuilder.add("User-Agent", userAgent)

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
                    val res = Gson().fromJson(requestData, Map::class.java)
                    val formBodyBuilder = FormBody.Builder()
                    for ((key, value) in res) {
                        formBodyBuilder.add(key.toString(), value.toString())
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
            if (requestData.contains("e_r")) {
                val decryptedResponseBody = responseBody?.let { body ->
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

        // 创建一个不验证主机名的主机名验证器
        val hostnameVerifier = HostnameVerifier { _, _ -> true }

        // 创建 SSL 上下文并初始化
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier(hostnameVerifier)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(encryptionInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
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

    private fun chooseUserAgent(crypto: String, uaType: String): String {
        // 根据加密方式和设备类型选择 User-Agent
        return when (crypto) {
            "weapi" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0"
            "linuxapi" -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36"
            "api" -> "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.0.18.203152"
            else -> ""
        }
    }

    private fun generateCookie(crypto: String): String {
        // 根据加密方式生成 Cookie
        // 这里可以根据需要实现具体的 Cookie 生成逻辑
        val MUSIC_U =
            "006503750A4B416ACE83191355213EC46491BFF92105213CD7A0101B11154F3FED93F869B0D0825102E06C02015190F81595D4185F02B7731650640BEA3F9C7F2BEA930E6D0F38C7486D8AF272284E56ECAB1A5727A6ACD8626FD01F2F53081A35C698F3575AB57141D46503F32176EC64F51EAA2469577AD63278642866EC5A305F4B332ECE583EC28FF34476F6A11F46B7608E193D7E9ECA1700B285AE1F231CC8BCF9E0533F742F6B00B05B9254571CB4D506708CF3993607B69CEA8B8143402BBDC918B433E9ACE157578C1E95E568C1A0BE38FA2033E9EBB646C7AAB79D19760949851141F75A1F966ABB8714709137B58AFBF20289AF9C374B74CE569D7B9D51CC5CAB37C94385A31E30C002C039FB5EA6C18C94C84762F498B9D897396C3918429C846D502E3454A25BC9F5F5035986801A3FB92CE445BBE521437224DC10F60CFBE5323B0563A89D262D1AD82F"
        val _ntes_nuid = createRandomKey(32)
        val os = when (crypto) {
            "weapi" -> "pc"
            "eapi" -> "pc"
            "api" -> "android"
            else -> "pc"
        }
        val osInfo = osMap[os]
        val cookie = mapOf(
            "ntes_kaola_ad" to 1,
            "_ntes_nuid" to _ntes_nuid,
            "os" to osInfo?.os,
            "MUSIC_U" to MUSIC_U
        )

        return cookie.map { (key, value) -> "$key=$value" }.joinToString(";")
    }
}


// 自定义 Converter.Factory
class FormUrlEncodedConverterFactory : Converter.Factory() {
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<Any, RequestBody>? {
        return if (type is Class<*> && type.kotlin.isData) {
            Converter<Any, RequestBody> { value ->
                val map = value.toMap()
                val formBody = FormBody.Builder().apply {
                    map.forEach { (key, value) ->
                        add(key, value.toString())
                    }
                }.build()
                formBody
            }
        } else {
            null
        }
    }

    private inline fun <reified T : Any> T.toMap(): Map<String, Any?> {
        return T::class.memberProperties.associate { prop ->
            prop.name to prop.get(this)
        }
    }


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