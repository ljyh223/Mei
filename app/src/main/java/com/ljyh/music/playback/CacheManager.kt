package com.ljyh.music.playback

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
    private var simpleCache: SimpleCache? = null

    fun getSimpleCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val evictor = LeastRecentlyUsedCacheEvictor((100 * 1024 * 1024 * 1024L))
            val databaseProvider: DatabaseProvider = StandaloneDatabaseProvider(context)
            val cacheDir = File(context.cacheDir, "media")
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }

    fun release() {
        simpleCache?.release()
        simpleCache = null
    }
}

