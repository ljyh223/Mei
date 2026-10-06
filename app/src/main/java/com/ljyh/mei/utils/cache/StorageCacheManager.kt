package com.ljyh.mei.utils.cache

import android.content.Context
import androidx.media3.common.util.UnstableApi
import coil3.SingletonImageLoader
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.playback.source.CacheManager
import java.io.File
import java.nio.file.Files

enum class CacheCategory { IMAGE, MUSIC, OTHER }

data class CacheUsage(val imageBytes: Long, val musicBytes: Long, val otherBytes: Long) {
    val totalBytes: Long get() = imageBytes + musicBytes + otherBytes
}

/** Manages only disposable files under cacheDir; downloaded songs and app data are never touched. */
@UnstableApi
object StorageCacheManager {
    private const val IMAGE_DIRECTORY = "image_cache"
    private const val MEDIA_DIRECTORY = "media"
    private const val ACTIVE_DOWNLOAD_DIRECTORY = "download"

    fun usage(context: Context): CacheUsage {
        val app = context.applicationContext
        val media = CacheManager.getSimpleCache(app)
        var musicBytes = 0L
        var otherMediaBytes = 0L
        for (key in media.keys) {
            val bytes = media.getCachedSpans(key).sumOf { it.length }
            if (isMusicKey(key)) musicBytes += bytes else otherMediaBytes += bytes
        }
        return CacheUsage(
            imageBytes = directorySize(File(app.cacheDir, IMAGE_DIRECTORY)),
            musicBytes = musicBytes,
            otherBytes = otherMediaBytes + otherFiles(app).sumOf(::directorySize),
        )
    }

    fun clear(context: Context, category: CacheCategory) {
        val app = context.applicationContext
        when (category) {
            CacheCategory.IMAGE -> {
                val loader = SingletonImageLoader.get(app)
                loader.memoryCache?.clear()
                loader.diskCache?.clear()
            }
            CacheCategory.MUSIC -> clearMediaKeys(app, music = true)
            CacheCategory.OTHER -> {
                clearMediaKeys(app, music = false)
                // DownloadWorker can be writing here; never delete its temporary files.
                otherFiles(app).forEach { file ->
                    if (!deleteCacheTree(file)) error("无法删除 ${file.name}")
                }
            }
        }
    }

    private fun clearMediaKeys(context: Context, music: Boolean) {
        val cache = CacheManager.getSimpleCache(context)
        cache.keys.toList().filter { isMusicKey(it) == music }.forEach(cache::removeResource)
    }

    internal fun isMusicKey(key: String): Boolean =
        MusicQuality.entries.any { key.endsWith("|${it.text}") }

    private fun otherFiles(context: Context): List<File> = context.cacheDir.listFiles()
        ?.filterNot {
            it.name in setOf(IMAGE_DIRECTORY, MEDIA_DIRECTORY, ACTIVE_DOWNLOAD_DIRECTORY) ||
                Files.isSymbolicLink(it.toPath())
        }
        .orEmpty()

    private fun directorySize(file: File): Long {
        if (!file.exists() || Files.isSymbolicLink(file.toPath())) return 0L
        if (!file.isDirectory) return file.length()
        return file.listFiles()?.sumOf(::directorySize) ?: 0L
    }

    private fun deleteCacheTree(file: File): Boolean {
        if (Files.isSymbolicLink(file.toPath())) return false
        if (file.isDirectory && file.listFiles()?.all(::deleteCacheTree) != true) return false
        return !file.exists() || file.delete()
    }
}
