package com.ljyh.mei.playback

import android.content.Context
import android.util.Log
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.io.File


object CacheManager {

    // 缓存大小常量，设置为 5 GB，这是一个比较合理的值
    private const val CACHE_SIZE_BYTES = 1024 * 1024 * 1024L * 5 // 5 GB

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

    /**
     * 建议在 Service 中新增此方法，将 CacheDataSource.Factory 的创建也集中管理。
     * 这样可以确保所有使用缓存的地方都使用同一个 OkHttpClient 实例。
     */
    fun getCacheDataSourceFactory(context: Context, okHttpClient: OkHttpClient): CacheDataSource.Factory {
        val upstreamFactory = OkHttpDataSource.Factory(okHttpClient)

        return CacheDataSource.Factory()
            .setCache(getSimpleCache(context))
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context, upstreamFactory))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
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