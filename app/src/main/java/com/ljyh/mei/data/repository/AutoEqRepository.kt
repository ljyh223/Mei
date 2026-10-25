package com.ljyh.mei.data.repository

import com.ljyh.mei.data.network.XeApiClient
import com.ljyh.mei.playback.equalizer.auto.TrackAudioFeatures
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Fetches and caches the per-song analysis used by auto EQ.
 *
 * The endpoint returns one entry per requested song, so the cache both avoids repeat requests when
 * a song comes back and keeps the response off the playback thread. Failures are not cached
 * (network hiccups should retry); an empty analysis is cached so we do not ask again for songs the
 * service has nothing on.
 */
@Singleton
class AutoEqRepository @Inject constructor(
    private val xeApiClient: XeApiClient,
) {
    private val cache = object : LinkedHashMap<Long, TrackAudioFeatures>(32, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<Long, TrackAudioFeatures>?,
        ): Boolean = size > MaxEntries
    }
    private val noData = HashSet<Long>()
    private val lock = Mutex()

    suspend fun features(songId: Long): TrackAudioFeatures? {
        if (songId <= 0) return null
        synchronized(cache) { cache[songId] }?.let { return it }
        if (synchronized(noData) { songId in noData }) return null
        return lock.withLock {
            synchronized(cache) { cache[songId] }?.let { return@withLock it }
            val result = runCatching { xeApiClient.getEmotionAudioEffect(listOf(songId)) }
            val features = result.getOrNull()?.firstOrNull()
            result.exceptionOrNull()?.let {
                Timber.tag("AutoEq").w(it, "analysis request failed for $songId")
                return@withLock null
            }
            if (features == null || !features.hasAnalysis) {
                synchronized(noData) { noData += songId }
                null
            } else {
                synchronized(cache) { cache[songId] = features }
                features
            }
        }
    }

    private companion object {
        const val MaxEntries = 64
    }
}
