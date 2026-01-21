package com.ljyh.mei.di

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.ljyh.mei.constants.AndroidUserAgent
import com.ljyh.mei.data.network.QQMusicCApiService
import com.ljyh.mei.data.network.QQMusicUApiService
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.utils.log.NetworkLogInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import timber.log.Timber
import java.lang.reflect.Type
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
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
        return OkHttpClient.Builder().apply {
            // 基础配置
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)

            // 日志拦截器
            addInterceptor(HttpLoggingInterceptor().apply {
                level = if (DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            })

            // 核心业务拦截器 (使用我们提取出来的类)
            addInterceptor(NeteaseInterceptor())

            // 网络错误日志拦截器
            addInterceptor(NetworkLogInterceptor())

            // SSL 配置 (仅在 Debug 模式下忽略证书，防止中间人攻击)
            if (DEBUG) {
                configureUnsafeSSL(this)
            }
        }.build()
    }

    private fun configureUnsafeSSL(builder: OkHttpClient.Builder) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create unsafe SSL context")
        }
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
                "android" to AndroidUserAgent,
                "iphone" to "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)"
            )
        )

        return userAgentMap[crypto]?.get(os) ?: ""
    }
}

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
object SpecialKey {
    const val Rank = "rank"
    const val HomeCommon = "home_common"
    const val Radar = "radar"
    const val CommonPlaylist="common_playlist"
}
