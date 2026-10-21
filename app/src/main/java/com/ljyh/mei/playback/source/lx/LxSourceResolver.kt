package com.ljyh.mei.playback.source.lx

import com.ljyh.unblockneteasemusic.model.MusicTrack
import kotlinx.coroutines.*
import kotlinx.serialization.json.jsonObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LxSourceResolver @Inject constructor(
    private val store: LxScriptStore,
    private val catalog: LxCatalog,
    private val probe: AudioUrlProbe,
) {
    suspend fun resolve(target: MusicTrack, quality: String, excluded: Set<String>): LxAudio? = coroutineScope {
        // Searches are shared across scripts in this resolution, not repeated per script.
        val searches = mutableMapOf<String, Deferred<List<LxCandidate>>>()
        val lock = Mutex()
        suspend fun search(source: String): List<LxCandidate> = lock.withLock {
            searches.getOrPut(source) {
                async {
                    try { withTimeout(7_000) { catalog.candidates(target, source) } }
                    catch (_: TimeoutCancellationException) { emptyList() }
                    catch (c: CancellationException) { throw c }
                    catch (_: Exception) { emptyList() }
                }
            }
        }.await()
        try {
            firstUsable(store.snapshot().filter { it.enabled }) { script ->
                store.status(script.id, "正在获取播放地址")
                try {
                    store.runtime().use(script, store.code(script)) {
                        val sessionLock = Mutex()
                        // Race the platforms too: a slow catalog must not block this script's wy URL.
                        firstUsable(sources.keys.filter { it != "local" }.sortedBy { if (it == "wy") 0 else 1 }, timeoutMs = 13_000) platform@ { source ->
                            for (candidate in search(source)) {
                                val capability = sources.getValue(source)
                                val qualities = PreviewPolicy.qualities(quality, capability.qualitys)
                                    .filter { source != "kg" || candidate.musicInfo["_types"]?.let { types -> it in types.jsonObject } == true }
                                for (type in qualities) {
                                    val identity = "${script.id}|$source|${candidate.musicInfo.string("songmid")}|$type"
                                    if (identity in excluded) continue
                                    // QuickJS has a single event loop. Serialise calls within this instance.
                                    val url = try { sessionLock.withLock { musicUrl(candidate, type) } }
                                    catch (c: CancellationException) { throw c }
                                    catch (_: Exception) { continue }
                                    val duration = probe.durationMs(url)
                                    if (duration == null || PreviewPolicy.isPreview(duration)) continue
                                    val expectedDuration = candidate.durationMs.takeIf { it > 0 } ?: target.durationMs
                                    if (expectedDuration > 30_000 && kotlin.math.abs(expectedDuration - duration) > 15_000) continue
                                    store.status(script.id, "可用 · ${sourceDisplayName(source)} · $type")
                                    return@platform LxAudio(url, script.id, source, candidate.musicInfo.string("songmid"), type)
                                }
                            }
                            null
                        }
                    }.also { if (it == null) store.status(script.id, "未找到通过校验的完整音频") }
                } catch (c: TimeoutCancellationException) { store.status(script.id, "获取超时"); null }
                catch (c: CancellationException) { store.status(script.id, "本次获取已结束"); throw c }
                catch (_: Exception) { store.status(script.id, "解析失败，可重试或更换脚本"); null }
            }
        } finally { searches.values.forEach { it.cancel() } }
    }

}
