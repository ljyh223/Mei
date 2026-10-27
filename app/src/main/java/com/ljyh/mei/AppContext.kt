package com.ljyh.mei

import android.app.Application
import androidx.datastore.preferences.core.edit
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.ImageCacheLimitMbKey
import com.ljyh.mei.constants.UserAgent
import com.ljyh.mei.constants.UserAvatarUrlKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.constants.UserNicknameKey
import com.ljyh.mei.constants.dynamicCoverSources
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.DynamicCoverRepository
import com.ljyh.mei.data.repository.LikedSongsRepository
import com.ljyh.mei.data.repository.UserRepository
import com.ljyh.mei.utils.image.ArtworkRequestInterceptor
import com.ljyh.mei.utils.power.canUseBatteryIntensiveFeatures
import com.ljyh.mei.utils.preferences.dataStore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient

@HiltAndroidApp
class AppContext : Application(), SingletonImageLoader.Factory {
    @Inject lateinit var dynamicCoverRepository: DynamicCoverRepository
    @Inject lateinit var likedSongsRepository: LikedSongsRepository
    @Inject lateinit var userRepository: UserRepository
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks(com.ljyh.mei.playback.usb.UsbAppVisibility)
        startupScope.launch {
            dataStore.data
                .map { preferences -> preferences[CookieKey].orEmpty() to preferences[UserIdKey].orEmpty() }
                .distinctUntilChanged()
                .collectLatest { (cookie, userId) ->
                    if (cookie.isBlank()) return@collectLatest
                    if (userId.isBlank()) {
                        val account = (userRepository.getUserAccount() as? Resource.Success)
                            ?.data?.takeIf { it.code == 200 }?.profile ?: return@collectLatest
                        dataStore.edit { preferences ->
                            if (preferences[CookieKey] == cookie) {
                                preferences[UserIdKey] = account.userId.toString()
                                preferences[UserNicknameKey] = account.nickname
                                preferences[UserAvatarUrlKey] = account.avatarUrl
                            }
                        }
                    } else {
                        likedSongsRepository.refresh(userId, force = false)
                    }
                }
        }
        startupScope.launch {
            try {
                if (dataStore.data.first().dynamicCoverSources().appleMusic &&
                    canUseBatteryIntensiveFeatures()) {
                    dynamicCoverRepository.warmWebToken()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Motion artwork is optional; startup must remain usable offline.
            }
        }
    }

    companion object {
        @JvmStatic
        lateinit var instance: AppContext

    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val headerInterceptor = Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("User-Agent", UserAgent)
                .build()
            chain.proceed(newRequest)
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .build()

        return ImageLoader.Builder(this)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this, 0.1)
                    .build()
            }
            .diskCache {
                newDiskCache()
            }
            .components {
                add(ArtworkRequestInterceptor())
                add(AnimatedImageDecoder.Factory())
                add(OkHttpNetworkFetcherFactory(okHttpClient))
            }
            .build()
    }
    private fun newDiskCache(): DiskCache {
        val limitMb = kotlinx.coroutines.runBlocking {
            dataStore.data.first()[ImageCacheLimitMbKey] ?: 250
        }
        return DiskCache.Builder()
            .directory(cacheDir.resolve("image_cache"))
            .maxSizeBytes(limitMb.coerceIn(50, 2048) * 1024L * 1024L)
            .build()
    }
}
