package com.ljyh.music.di

import com.ljyh.music.data.network.ApiService
import com.ljyh.music.data.network.QQMusicApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.InetAddress
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
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
    @Named("netEaseMusicRetrofit")
    fun provideRetrofit(): Retrofit {
//        val BASE_URL= "http://192.168.1.3:3000/"
        val BASE_URL= "http://172.245.119.194:3000/"
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HeaderInterceptor())
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideApiService(@Named("netEaseMusicRetrofit") retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }


    @Singleton
    @Provides
    @Named("qqMusicRetrofit")
    fun provideQQMusicRetrofit(): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .build()

        return Retrofit.Builder()
            .baseUrl("https://c.y.qq.com/")  // 另一个 API 的 baseUrl
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideQQMusicApiService( @Named("qqMusicRetrofit") retrofit: Retrofit): QQMusicApiService {
        return retrofit.create(QQMusicApiService::class.java)
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
                // 将xxxx替换成你的 Cookie,
                "MUSIC_U=xxxx"
            )
            .build()
        return chain.proceed(request)
    }
}


