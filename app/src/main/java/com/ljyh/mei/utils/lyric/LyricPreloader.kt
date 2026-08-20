package com.ljyh.mei.utils.lyric

import android.content.Context
import com.ljyh.mei.constants.QqTimeout
import com.ljyh.mei.constants.QqTimeoutKey
import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.qq.u.LyricResult
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.PlayerRepository
import com.ljyh.mei.di.repository.QQSongRepository
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.encrypt.QRCUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class LyricSourceSnapshot(
    val netEase: Resource<Lyric> = Resource.Loading,
    val qq: Resource<LyricResult> = Resource.Loading,
    val ttml: Resource<String> = Resource.Loading,
)

/**
 * 歌词预加载器
 *
 * Owns shared per-song resolution sessions. Preloading starts a session and the player later
 * observes the same source states without issuing duplicate requests.
 */
@Singleton
class LyricPreloader @Inject constructor(
    private val repository: PlayerRepository,
    private val qqSongRepository: QQSongRepository,
    @param:ApplicationContext private val context: Context,
) {
    private val TAG = "LyricPreloader"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessions = LinkedHashMap<String, Session>(8, 0.75f, true)

    fun preload(metadata: MediaMetadata) {
        sourceFlow(metadata)
    }

    fun sourceFlow(
        metadata: MediaMetadata,
        forceReload: Boolean = false,
    ): StateFlow<LyricSourceSnapshot> = synchronized(sessions) {
        val songId = metadata.id.toString()
        if (forceReload) sessions.remove(songId)?.job?.cancel()
        sessions[songId]?.state ?: createSession(metadata).also { trimSessions() }.state
    }

    private fun createSession(metadata: MediaMetadata): Session {
        val songId = metadata.id.toString()
        val state = MutableStateFlow(LyricSourceSnapshot())
        val session = Session(state)
        sessions[songId] = session
        session.job = scope.launch {
            launch {
                state.update { it.copy(netEase = fetchNetEase(songId)) }
            }
            launch {
                state.update { it.copy(ttml = fetchAM(songId)) }
            }
            launch {
                val timeoutSeconds = runCatching {
                    QqTimeout.valueOf(
                        context.dataStore.data.firstOrNull()?.get(QqTimeoutKey)
                            ?: QqTimeout.Sec8.name
                    ).seconds
                }.getOrDefault(QqTimeout.Sec8.seconds)
                val result = try {
                    withTimeout(timeoutSeconds * 1_000L) {
                        fetchQQ(songId, metadata)
                    }
                } catch (_: TimeoutCancellationException) {
                    Resource.Error("QQ timed out")
                }
                if (!session.qqOverridden) {
                    state.update { it.copy(qq = result) }
                }
            }
        }
        return session
    }

    private fun trimSessions() {
        while (sessions.size > MAX_SESSIONS) {
            val eldest = sessions.entries.first()
            sessions.remove(eldest.key)
            eldest.value.job?.cancel()
        }
    }

    fun overrideQq(songId: String, result: Resource<LyricResult>) {
        synchronized(sessions) {
            sessions[songId]?.let { session ->
                session.qqOverridden = true
                session.state.update { it.copy(qq = result) }
            }
        }
    }

    private class Session(val state: MutableStateFlow<LyricSourceSnapshot>) {
        var job: Job? = null
        @Volatile var qqOverridden: Boolean = false
    }

    private companion object {
        const val MAX_SESSIONS = 5
    }

    private suspend fun fetchNetEase(id: String): Resource<Lyric> {
        return try {
            repository.getLyricV1(id)
        } catch (e: Exception) {
            Timber.e(e, "NetEase preload fetch error")
            Resource.Error("NetEase fetch failed")
        }
    }

    private suspend fun fetchAM(id: String): Resource<String> {
        return try {
            repository.getAMLLyric(id)
        } catch (e: Exception) {
            Timber.e(e, "AML preload fetch error")
            Resource.Error("AML fetch failed")
        }
    }

    private suspend fun fetchQQ(
        songId: String,
        metadata: MediaMetadata?
    ): Resource<LyricResult> {
        val localSong = qqSongRepository.getQQSong(songId).firstOrNull()
        if (localSong != null) {
            return try {
                repository.getLyricNew(
                    localSong.title, localSong.album, localSong.artist,
                    localSong.duration, localSong.qid.toLong()
                )
            } catch (e: Exception) {
                Timber.e(e, "QQ preload fetch error")
                Resource.Error("QQ fetch failed")
            }
        }

        // 静默搜索：查询但不写入任何共享 StateFlow
        val searchMetadata = metadata ?: return Resource.Error("QQ mapping not found")
        val best = searchSilent(searchMetadata) ?: return Resource.Error("No QQ match found")

        val qqSong = QQSong(
            id = songId,
            qid = best.id.toString(),
            title = best.title,
            artist = best.singer.joinToString(",") { it.name },
            album = best.album.title,
            duration = best.interval
        )
        qqSongRepository.insertSong(qqSong)

        return try {
            repository.getLyricNew(
                qqSong.title, qqSong.album, qqSong.artist,
                qqSong.duration, qqSong.qid.toLong()
            )
        } catch (e: Exception) {
            Timber.e(e, "QQ preload fetch error after search")
            Resource.Error("QQ fetch failed")
        }
    }

    /** Resolves and decodes the same QQ lyric source used by player preloading. */
    internal suspend fun resolveQqSource(
        songId: String,
        metadata: MediaMetadata? = null
    ): LyricSourceData.QQMusic? {
        val result = fetchQQ(songId, metadata) as? Resource.Success ?: return null
        return try {
            val data = result.data.musicMusichallSongPlayLyricInfoGetPlayLyricInfo.data
            val decoded = data.copy(
                lyric = QRCUtils.decodeLyric(data.lyric),
                trans = QRCUtils.decodeLyric(data.trans, true),
                roma = QRCUtils.decodeLyric(data.roma)
            )
            LyricSourceData.QQMusic(decoded, isQRC = data.qrcT != 0, lrcContent = null)
        } catch (e: Exception) {
            Timber.e(e, "QRC decoding failed while resolving QQ lyrics")
            null
        }
    }

    // ==================== 静默搜索（不写入共享 StateFlow） ====================

    /**
     * 搜索 QQ 音乐并匹配最佳结果，不产生任何副作用。
     *
     * Uses the same title cleanup and duration matching rules for preload and active playback.
     */
    private suspend fun searchSilent(
        metadata: MediaMetadata
    ): SearchResult.Request.Data.Body.ItemSong? {
        val currentDurationSec = metadata.duration / 1000
        val artistName = metadata.artists.firstOrNull()?.name ?: ""
        val title = metadata.title
        val cleanedTitle = cleanTitle(title)

        if (cleanedTitle != title) {
            Timber.tag(TAG).d("silent QQ search: $cleanedTitle")
            trySearchSilent(cleanedTitle, currentDurationSec)?.let { return it }
        }

        Timber.tag(TAG).d("silent QQ search retry: $title")
        trySearchSilent(title, currentDurationSec)?.let { return it }

        if (artistName.isNotBlank() && cleanedTitle != title) {
            val combined = "$cleanedTitle $artistName"
            trySearchSilent(combined, currentDurationSec)?.let { return it }
        }

        if (artistName.isNotBlank()) {
            val combined = "$title $artistName"
            trySearchSilent(combined, currentDurationSec)?.let { return it }
        }

        return null
    }

    private suspend fun trySearchSilent(
        keyword: String,
        targetDurationSec: Long
    ): SearchResult.Request.Data.Body.ItemSong? {
        val result = repository.searchNew(keyword)
        if (result !is Resource.Success) return null
        val songs = result.data.request.data.body.itemSong
        return songs.take(5).firstOrNull { song ->
            abs(targetDurationSec - song.interval) <= 5
        }
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("""[\(（][^)）]*[\)）]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
