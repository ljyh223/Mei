package com.ljyh.music.di

import android.util.Log
import com.ljyh.music.data.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
//    private const val BASE_URL = "http://192.168.2.25:3000/"
//    private const val BASE_URL = "https://neteasecloudmusicapi.ljyh.link/"
//    private const val BASE_URL = "http://172.245.119.194:3000/"
//    private const val BASE_URL = "http://127.0.0.1:3000/"

    private val BASE_URLS= listOf(
        "http://192.168.246.127:3000/",
        "http://172.245.119.194:3000/",
        "https://neteasecloudmusicapi.ljyh.link/",
        "http://127.0.0.1:3000/",
        "http://192.168.2.25:3000/",
        "http://172.245.119.194:3000/",
    )

    @Singleton
    @Provides
    fun provideRetrofit(): Retrofit {
//        val BASE_URL=BASE_URLS.find { isServerReachable(it) }?: BASE_URLS.last()
        val BASE_URL= "http://192.168.246.127:3000/"
        Log.d("BASE_URL", BASE_URL)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HeaderInterceptor()) // Add the interceptor
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}

fun isServerReachable(serverUrl: String): Boolean {
    return try {
        val address = InetAddress.getByName(
            serverUrl
                .replace("https://", "")
                .replace("http://", "")
                .replace("/", "")
        )
        address.isReachable(2000) // 2秒超时
    } catch (e: Exception) {
        false
    }
}

class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader(
                "Cookie",
                "MUSIC_U=006503750A4B416ACE83191355213EC46491BFF92105213CD7A0101B11154F3FED93F869B0D0825102E06C02015190F81595D4185F02B7731650640BEA3F9C7F2BEA930E6D0F38C7486D8AF272284E56ECAB1A5727A6ACD8626FD01F2F53081A35C698F3575AB57141D46503F32176EC64F51EAA2469577AD63278642866EC5A305F4B332ECE583EC28FF34476F6A11F46B7608E193D7E9ECA1700B285AE1F231CC8BCF9E0533F742F6B00B05B9254571CB4D506708CF3993607B69CEA8B8143402BBDC918B433E9ACE157578C1E95E568C1A0BE38FA2033E9EBB646C7AAB79D19760949851141F75A1F966ABB8714709137B58AFBF20289AF9C374B74CE569D7B9D51CC5CAB37C94385A31E30C002C039FB5EA6C18C94C84762F498B9D897396C3918429C846D502E3454A25BC9F5F5035986801A3FB92CE445BBE521437224DC10F60CFBE5323B0563A89D262D1AD82F"
            )
            .build()
        return chain.proceed(request)
    }
}