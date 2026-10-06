package com.ljyh.mei.di

import android.content.Context
import com.ljyh.mei.constants.QqCookieKey
import com.ljyh.mei.data.network.NeteaseHttp
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.utils.log.NetworkLogInterceptor
import com.ljyh.unblockneteasemusic.qq.QqMusicProvider
import com.ljyh.unblockneteasemusic.kuwo.KuwoMusicProvider
import com.ljyh.unblockneteasemusic.migu.MiguMusicProvider
import com.ljyh.unblockneteasemusic.unblock.UnblockResolver
import com.ljyh.unblockneteasemusic.unblock.UnblockStage
import com.ljyh.mei.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import timber.log.Timber

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
    fun provideQqMusicProvider(@ApplicationContext context: Context): QqMusicProvider =
        QqMusicProvider {
            context.dataStore.data.first()[QqCookieKey]?.takeIf(String::isNotBlank)
        }

    @Provides
    @Singleton
    fun provideKuwoMusicProvider(): KuwoMusicProvider = KuwoMusicProvider()

    @Provides
    @Singleton
    fun provideMiguMusicProvider(): MiguMusicProvider = MiguMusicProvider()

    @Provides
    @Singleton
    fun provideUnblockResolver(
        qq: QqMusicProvider,
        kuwo: KuwoMusicProvider,
        migu: MiguMusicProvider,
    ): UnblockResolver = UnblockResolver(
        catalogs = listOf(qq, kuwo, migu),
        audioProviders = listOf(qq, kuwo, migu),
        onEvent = { event ->
            val details = "id=${event.targetId ?: "-"} stage=${event.stage} source=${event.sourceId ?: "-"} " +
                "count=${event.count ?: "-"} candidate=${event.candidateId ?: "-"} " +
                "error=${event.errorType ?: "-"}"
            when (event.stage) {
                UnblockStage.SEARCH_FAILED, UnblockStage.URL_FAILED, UnblockStage.EXHAUSTED ->
                    Timber.tag("UnblockResolver").w(details)
                UnblockStage.SELECTED -> Timber.tag("UnblockResolver").i(details)
                else -> Timber.tag("UnblockResolver").d(details)
            }
        },
    )
}
