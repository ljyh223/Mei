package com.ljyh.mei.playback.source

import android.net.Uri
import android.content.Context
import androidx.core.net.toUri
import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.data.model.api.GetSongDetails
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.di.repository.SongRepository
import com.ljyh.mei.playback.source.lx.*
import com.ljyh.unblockneteasemusic.model.MusicAlbum
import com.ljyh.unblockneteasemusic.model.MusicArtist
import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.unblockneteasemusic.model.TrackId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
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
data class PlaybackSourceInfo(val name: String, val quality: String?)

@Singleton
class MediaUriProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val songRepository: SongRepository,
    private val lxSources: LxSourceResolver,
    private val scriptStore: LxScriptStore,
    private val probe: AudioUrlProbe,
) {
    private data class CachedUrl(
        val url: String, val expiresAtMs: Long, val playbackCacheKey: String,
        val identity: String, val source: PlaybackSourceInfo,
    )
    private val urlCache = ConcurrentHashMap<String, CachedUrl>()
    private val lastResolved = ConcurrentHashMap<String, CachedUrl>()
    private val recovery = SourceRecoveryPolicy()
    private val resolutionLocks = Array(64) { Mutex() }
    private val _sourceInfo = MutableStateFlow<Map<String, PlaybackSourceInfo>>(emptyMap())
    val sourceInfo = _sourceInfo.asStateFlow()

    suspend fun resolveMediaUri(mediaId: String, quality: String): Uri = resolveMedia(mediaId, quality, recordPlayback = false).uri

    suspend fun resolveMedia(mediaId: String, quality: String, recordPlayback: Boolean = true): ResolvedMediaUri {
        val baseKey = "$mediaId|${quality.lowercase()}|full-v1"
        val localPath = songRepository.getSong(mediaId).firstOrNull()?.path
            ?: songRepository.getSong("local_$mediaId").firstOrNull()?.path
        if (localPath != null && (localPath.startsWith("content://") || File(localPath).exists())) {
            if (recordPlayback) _sourceInfo.update { it + (mediaId to PlaybackSourceInfo("本地", null)) }
            return ResolvedMediaUri(if (localPath.startsWith("content://")) Uri.parse(localPath) else Uri.fromFile(File(localPath)), baseKey)
        }
        // Disabling/reordering/replacing a script invalidates the short-lived resolution cache.
        val scripts = scriptStore.snapshot()
        val revision = scripts.joinToString { "${it.id}:${it.enabled}" }.hashCode()
        val lookupKey = "$baseKey|$revision"
        fun cached(): ResolvedMediaUri? = urlCache[lookupKey]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let {
            selected(mediaId, it, recordPlayback)
        }
        cached()?.let { return it }
        return resolutionLocks[Math.floorMod(lookupKey.hashCode(), resolutionLocks.size)].withLock {
            cached()?.let { return@withLock it }
            val (skipNetease, excluded) = recovery.snapshot(mediaId)
            try {
                if (!skipNetease) {
                    val response = apiService.getSongUrlV1(GetSongUrlV1(ids = "[$mediaId]", level = quality))
                    val data = response.data.firstOrNull { it.id.toString() == mediaId } ?: response.data.firstOrNull()
                    val url = data?.url?.takeIf { it.isNotBlank() }
                    if (url != null && data.freeTrialInfo == null) {
                        val duration = data.time?.takeIf { it > 0 } ?: probe.durationMs(url)
                        if (duration != null && !PreviewPolicy.isPreview(duration)) {
                            val resolved = CachedUrl(url, System.currentTimeMillis() + 10 * 60_000, baseKey,
                                "netease", PlaybackSourceInfo("网易云", data.level))
                            urlCache[lookupKey] = resolved
                            return@withLock selected(mediaId, resolved, recordPlayback)
                        }
                    }
                }
                val target = target(mediaId) ?: throw SourceNotFoundException("歌曲信息不可用")
                val audio = lxSources.resolve(target, quality, excluded)
                if (audio != null) {
                    val name = scripts.firstOrNull { it.id == audio.scriptId }?.name ?: "自定义音源"
                    val resolved = CachedUrl(audio.url, System.currentTimeMillis() + 2 * 60_000,
                        "$baseKey|lx:${audio.identity}", audio.identity, PlaybackSourceInfo("$name · ${sourceDisplayName(audio.source)}", audio.quality))
                    urlCache[lookupKey] = resolved
                    return@withLock selected(mediaId, resolved, recordPlayback)
                }
                throw SourceNotFoundException("LX 音源未找到可播放的完整音频")
            } catch (c: CancellationException) { throw c }
            catch (e: SourceNotFoundException) { throw e }
            catch (e: Exception) {
                // Never put signed URLs, script code or credentials into logs/errors.
                Timber.tag("AudioSource").w("Resolution failed: id=%s type=%s", mediaId, e.javaClass.simpleName)
                throw IOException("获取播放地址失败", e)
            }
        }
    }

    private fun selected(mediaId: String, cached: CachedUrl, recordPlayback: Boolean): ResolvedMediaUri {
        if (!recordPlayback) return ResolvedMediaUri(cached.url.toUri(), cached.playbackCacheKey)
        lastResolved[mediaId] = cached
        _sourceInfo.update { it + (mediaId to cached.source) }
        if (lastResolved.size > 128) {
            lastResolved.keys.firstOrNull { it != mediaId }?.let { old ->
                lastResolved.remove(old)
                _sourceInfo.update { it - old }
            }
        }
        if (urlCache.size > 256) urlCache.keys.firstOrNull()?.let { urlCache.remove(it) }
        return ResolvedMediaUri(cached.url.toUri(), cached.playbackCacheKey)
    }

    /** First refresh the official URL once; then exclude failing candidates and try other sources. */
    fun recoverAfterError(mediaId: String): Boolean {
        val previous = lastResolved.remove(mediaId) ?: return false
        invalidateRemoteUrl(mediaId)
        return recovery.failed(mediaId, previous.identity)
    }

    fun invalidateRemoteUrl(mediaId: String) { urlCache.keys.removeIf { it.startsWith("$mediaId|") } }

    private suspend fun target(mediaId: String): MusicTrack? {
        val song = apiService.getSongDetail(GetSongDetails(mediaId)).songs.firstOrNull { it.id.toString() == mediaId } ?: return null
        return MusicTrack(TrackId("netease", mediaId), song.name,
            song.ar.mapNotNull { a -> a.name?.takeIf(String::isNotBlank)?.let { MusicArtist(it, a.Id.toString()) } },
            MusicAlbum(song.al.name.orEmpty(), song.al.Id.toString()), song.dt)
    }
}
