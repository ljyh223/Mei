package com.ljyh.mei

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.Interceptor
import okhttp3.OkHttpClient

@HiltAndroidApp
class AppContext : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @JvmStatic
        lateinit var instance: AppContext

    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val headerInterceptor = Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0")
                .build()
            chain.proceed(newRequest)
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .build()

        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this, 0.1)
                    .build()
            }
            .diskCache {
                newDiskCache()
            }
            .components {
                add(OkHttpNetworkFetcherFactory(okHttpClient))
            }
            .build()
    }
    private fun newDiskCache(): DiskCache {
        return DiskCache.Builder()
            .directory(cacheDir.resolve("image_cache"))
            .maxSizePercent(0.1)
            .build()
    }
}

