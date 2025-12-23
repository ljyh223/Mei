package com.ljyh.mei.playback

import android.net.Uri
import androidx.core.net.toUri
import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.data.network.api.ApiService // 假设你的API都在这
import com.ljyh.mei.di.SongRepository // 假设你处理本地文件
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File
import java.io.IOException

// 自定义异常，用于精准捕获
class SourceNotFoundException(message: String) : IOException(message)

@Singleton
class MediaUriProvider @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository,
) {
    private val urlCache =  java.util.concurrent.ConcurrentHashMap<String, String>()

    suspend fun resolveMediaUri(mediaId: String, quality: String): Uri {
        // 1. 检查本地文件 (你的原有逻辑)
        val localPath = songRepository.getSong(mediaId).firstOrNull()?.path
        if (localPath != null && File(localPath).exists()) {
            return Uri.fromFile(File(localPath))
        }
        urlCache[mediaId]?.let { return it.toUri() }
        return try {
            val response = apiService.getSongUrlV1(
                GetSongUrlV1(ids = "[$mediaId]", level = quality)
            )
            val url = response.data.getOrNull(0)?.url

            if (url.isNullOrBlank()) {
                throw SourceNotFoundException("API returned empty URL for $mediaId")
            }

            urlCache[mediaId] = url
            url.toUri()
        } catch (e: Exception) {
            if (e is SourceNotFoundException) throw e
            throw IOException("Network error resolving URL for $mediaId", e)
        }
    }
}