package com.ljyh.mei.playback.source

import android.net.Uri
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.net.toUri
import com.ljyh.mei.BuildConfig
import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.data.model.api.GetSongDetails
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.di.repository.SongRepository
import com.ljyh.unblockneteasemusic.model.MusicAlbum
import com.ljyh.unblockneteasemusic.model.MusicArtist
import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.unblockneteasemusic.model.PlayableAudio
import com.ljyh.unblockneteasemusic.model.TrackId
import com.ljyh.unblockneteasemusic.unblock.UnblockResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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

data class ResolvedMediaUri(val uri: Uri, val cacheKey: String)

@Singleton
class MediaUriProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val songRepository: SongRepository,
    private val unblockResolver: UnblockResolver,
) {
    private data class CachedUrl(
        val url: String,
        val expiresAtMs: Long,
        val playbackCacheKey: String,
    )

    private val urlCache = ConcurrentHashMap<String, CachedUrl>()
    private val resolutionLockStripes = Array(RESOLUTION_LOCK_STRIPE_COUNT) { Mutex() }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentDebugToast: Toast? = null

    /**
     * Song URLs returned by the provider are signed and short-lived. Cache them only briefly,
     * and keep qualities separate so a quality switch cannot reuse an incompatible URL.
     */
    suspend fun resolveMediaUri(mediaId: String, quality: String): Uri =
        resolveMedia(mediaId, quality).uri

    suspend fun resolveMedia(mediaId: String, quality: String): ResolvedMediaUri {
        val cacheKey = cacheKey(mediaId, quality)
        val localPath = songRepository.getSong(mediaId).firstOrNull()?.path
            ?: songRepository.getSong("local_$mediaId").firstOrNull()?.path
        if (localPath != null) {
            if (localPath.startsWith("content://")) {
                return ResolvedMediaUri(Uri.parse(localPath), cacheKey)
            }
            val file = File(localPath)
            if (file.exists()) {
                return ResolvedMediaUri(Uri.fromFile(file), cacheKey)
            }
        }

        val now = System.currentTimeMillis()
        urlCache[cacheKey]?.takeIf { it.expiresAtMs > now }?.let {
            return ResolvedMediaUri(it.url.toUri(), it.playbackCacheKey)
        }

        return resolutionLockFor(cacheKey).withLock {
            val lockedNow = System.currentTimeMillis()
            urlCache[cacheKey]?.takeIf { it.expiresAtMs > lockedNow }?.let {
                return@withLock ResolvedMediaUri(it.url.toUri(), it.playbackCacheKey)
            }

            var fallbackAttempted = false
            try {
                val response = apiService.getSongUrlV1(
                    GetSongUrlV1(ids = "[$mediaId]", level = quality)
                )
                val url = response.data.getOrNull(0)?.url

                if (url.isNullOrBlank()) {
                    fallbackAttempted = true
                    Timber.tag("Unblock").i("NetEase URL empty; fallback started: id=%s quality=%s", mediaId, quality)
                    debugToast("网易云无音源，正在查找备用音源")
                    val fallback = resolveFallback(mediaId)
                        ?: throw SourceNotFoundException("No playable source found for $mediaId")
                    Timber.tag("Unblock").i(
                        "Fallback selected: id=%s source=%s candidate=%s",
                        mediaId, fallback.track.id.source, fallback.track.id.value,
                    )
                    debugToast("解灰成功：${sourceName(fallback.track.id.source)}")
                    val fallbackCacheKey = "$cacheKey|${fallback.track.id.source}:${fallback.track.id.value}"
                    urlCache[cacheKey] = CachedUrl(
                        fallback.url, lockedNow + FALLBACK_URL_CACHE_TTL_MS, fallbackCacheKey
                    )
                    return@withLock ResolvedMediaUri(fallback.url.toUri(), fallbackCacheKey)
                }

                urlCache[cacheKey] = CachedUrl(url, lockedNow + URL_CACHE_TTL_MS, cacheKey)
                ResolvedMediaUri(url.toUri(), cacheKey)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (fallbackAttempted) {
                    Timber.tag("Unblock").w(
                        "Fallback failed: id=%s error=%s", mediaId, e.javaClass.simpleName,
                    )
                    debugToast(
                        if (e is SourceNotFoundException) "解灰失败：未找到可用音源"
                        else "解灰失败：请求异常"
                    )
                }
                if (e is SourceNotFoundException) throw e
                throw IOException("Network error resolving URL for $mediaId", e)
            }
        }
    }

    private fun debugToast(message: String) {
        if (!BuildConfig.DEBUG) return
        mainHandler.post {
            currentDebugToast?.cancel()
            currentDebugToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).also(Toast::show)
        }
    }

    private fun sourceName(sourceId: String): String = when (sourceId) {
        "qq" -> "QQ 音乐"
        "kuwo" -> "酷我音乐"
        "migu" -> "咪咕音乐"
        else -> sourceId
    }

    private suspend fun resolveFallback(mediaId: String): PlayableAudio? {
        val song = apiService.getSongDetail(GetSongDetails(mediaId)).songs
            .firstOrNull { it.id.toString() == mediaId }
        if (song == null) {
            Timber.tag("Unblock").w("NetEase song detail missing: id=%s", mediaId)
            return null
        }
        Timber.tag("Unblock").d(
            "NetEase song detail loaded: id=%s artists=%s durationMs=%s",
            mediaId, song.ar.size, song.dt,
        )
        return unblockResolver.resolve(
            MusicTrack(
                id = TrackId("netease", mediaId),
                title = song.name,
                artists = song.ar.mapNotNull { artist ->
                    artist.name?.takeIf(String::isNotBlank)?.let { MusicArtist(it, artist.Id.toString()) }
                },
                album = MusicAlbum(song.al.name.orEmpty(), song.al.Id.toString()),
                durationMs = song.dt,
            )
        )
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
        const val FALLBACK_URL_CACHE_TTL_MS = 2 * 60 * 1000L
    }
}
