package com.ljyh.mei.playback

import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.ljyh.mei.constants.UserAgent
import okhttp3.OkHttpClient
import java.io.File


object CacheManager {

    // 缓存大小常量，设置为 5 GB，这是一个比较合理的值
    private const val CACHE_SIZE_BYTES = 1024 * 1024 * 1024L * 10 // 5 GB

    // 使用 @Volatile 注解确保多线程环境下的可见性
    @Volatile
    private var simpleCache: SimpleCache? = null

    // 锁对象，用于同步
    private val LOCK = Any()

    /**
     * 获取 SimpleCache 的单例。
     * 使用双重检查锁定（Double-Checked Locking）模式来确保线程安全和高效。
     */
    fun getSimpleCache(context: Context): SimpleCache {
        // 第一次检查，避免每次都进入同步块，提高性能
        return simpleCache ?: synchronized(LOCK) {
            // 第二次检查，防止在等待锁的过程中其他线程已经创建了实例
            simpleCache ?: createSimpleCache(context.applicationContext).also {
                simpleCache = it
            }
        }
    }

    /**
     * 创建一个新的 SimpleCache 实例。
     */
    private fun createSimpleCache(context: Context): SimpleCache {
        val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
        val databaseProvider: DatabaseProvider = StandaloneDatabaseProvider(context)
        val cacheDir = File(context.cacheDir, "media")
        return SimpleCache(cacheDir, evictor, databaseProvider)
    }


    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        Log.d("SimpleCache", "Creating CacheDataSource instance")
        val simpleCache = CacheManager.getSimpleCache(context)

        // 配置 OkHttp
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .addHeader("User-Agent", UserAgent)
                            .build()
                    )
                }
                .build()
        )

        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(okHttpDataSourceFactory)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun isContentFullyCached(cache: Cache, key: String): Boolean {
        // 获取缓存元数据
        val contentMetadata = cache.getContentMetadata(key)
        // 获取总长度 (Content-Length)
        val contentLength = ContentMetadata.getContentLength(contentMetadata)

        // 如果不知道总长度，说明还没下载完或者没存长度信息，视为未完全缓存
        if (contentLength == C.LENGTH_UNSET.toLong()) return false
        // 检查缓存的字节数是否 >= 总长度
        val cachedBytes = cache.getCachedBytes(key, 0, contentLength)
        return cachedBytes >= contentLength
    }

    /**
     * 释放缓存资源。应该在应用进程结束时调用。
     */
    fun release() {
        // 在同步块中操作，确保安全
        synchronized(LOCK) {
            simpleCache?.release()
            simpleCache = null
        }
    }
}