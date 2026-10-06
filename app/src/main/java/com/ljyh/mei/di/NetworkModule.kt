package com.ljyh.mei.di

import com.ljyh.mei.data.network.NeteaseHttp
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.utils.log.NetworkLogInterceptor
import com.ljyh.unblockneteasemusic.qq.QqMusicProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .addInterceptor(NeteaseInterceptor())
        .addInterceptor(NetworkLogInterceptor())
        .build()

    @Provides
    @Singleton
    fun provideNeteaseJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideNeteaseHttp(okHttpClient: OkHttpClient, json: Json): NeteaseHttp =
        NeteaseHttp(HttpClient(OkHttp) {
            expectSuccess = true
            engine { preconfigured = okHttpClient }
        }, json)

    @Provides
    @Singleton
    fun provideApiService(http: NeteaseHttp): ApiService = ApiService(http)

    @Provides
    @Singleton
    fun provideWeApiService(http: NeteaseHttp): WeApiService = WeApiService(http)

    @Provides
    @Singleton
    fun provideEApiService(http: NeteaseHttp): EApiService = EApiService(http)

    @Provides
    @Singleton
    fun provideQqMusicProvider(): QqMusicProvider = QqMusicProvider()
}
