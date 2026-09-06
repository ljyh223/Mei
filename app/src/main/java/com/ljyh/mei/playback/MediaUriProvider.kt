package com.ljyh.mei.playback

import android.net.Uri
import androidx.core.net.toUri
import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.di.repository.SongRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

class SourceNotFoundException(message: String) : IOException(message)

@Singleton
class MediaUriProvider @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository,
) {
    private data class CachedUrl(
        val url: String,
        val expiresAtMs: Long,
    )

    private val urlCache = ConcurrentHashMap<String, CachedUrl>()
    private val resolutionLockStripes = Array(RESOLUTION_LOCK_STRIPE_COUNT) { Mutex() }

    /**
     * Song URLs returned by the provider are signed and short-lived. Cache them only briefly,
     * and keep qualities separate so a quality switch cannot reuse an incompatible URL.
     */
    suspend fun resolveMediaUri(mediaId: String, quality: String): Uri {
        val localPath = songRepository.getSong(mediaId).firstOrNull()?.path
            ?: songRepository.getSong("local_$mediaId").firstOrNull()?.path
        if (localPath != null) {
            if (localPath.startsWith("content://")) {
                return Uri.parse(localPath)
            }
            val file = File(localPath)
            if (file.exists()) {
                return Uri.fromFile(file)
            }
        }

        val cacheKey = cacheKey(mediaId, quality)
        val now = System.currentTimeMillis()
        urlCache[cacheKey]?.takeIf { it.expiresAtMs > now }?.let { return it.url.toUri() }

        return resolutionLockFor(cacheKey).withLock {
            val lockedNow = System.currentTimeMillis()
            urlCache[cacheKey]?.takeIf { it.expiresAtMs > lockedNow }?.let { return@withLock it.url.toUri() }

            try {
                val response = apiService.getSongUrlV1(
                    GetSongUrlV1(ids = "[$mediaId]", level = quality)
                )
                val url = response.data.getOrNull(0)?.url

                if (url.isNullOrBlank()) {
                    Timber.tag("MediaUriProvider").d(response.toString())
                    throw SourceNotFoundException("API returned empty URL for $mediaId")
                }

                urlCache[cacheKey] = CachedUrl(url, lockedNow + URL_CACHE_TTL_MS)
                url.toUri()
            } catch (e: Exception) {
                if (e is SourceNotFoundException) throw e
                throw IOException("Network error resolving URL for $mediaId", e)
            }
        }
    }

    private fun resolutionLockFor(cacheKey: String): Mutex =
        resolutionLockStripes[Math.floorMod(cacheKey.hashCode(), resolutionLockStripes.size)]

    /** Evicts every quality variant after a transport error so the next prepare gets a new URL. */
    fun invalidateRemoteUrl(mediaId: String) {
        val prefix = "$mediaId|"
        urlCache.keys.removeIf { it.startsWith(prefix) }
    }

    private fun cacheKey(mediaId: String, quality: String) = "$mediaId|${quality.lowercase()}"

    private companion object {
        const val RESOLUTION_LOCK_STRIPE_COUNT = 64
        const val URL_CACHE_TTL_MS = 10 * 60 * 1000L
    }
}
