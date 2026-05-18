package com.ljyh.mei.utils.lyric

import android.content.Context
import com.ljyh.mei.constants.QqTimeout
import com.ljyh.mei.constants.QqTimeoutKey
import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.qq.u.LyricResult
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.data.model.room.CachedLyric
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.PlayerRepository
import com.ljyh.mei.di.CachedLyricRepository
import com.ljyh.mei.di.QQSongRepository
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.encrypt.QRCUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.abs
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.coroutines.FlowPreview::class)

/**
 * 歌词管理器
 *
 * 协调歌词的获取、合并、缓存、预加载和AI增强全过程。
 *
 * 数据流概要：
 * 1. [loadLyrics] 被调用时重置所有源状态为 Loading
 * 2. 并行拉取三源（网易云、AM、QQ），各自更新对应的 StateFlow
 * 3. [combine] 监听三个 StateFlow，任一变化触发 [mergeAndApply]
 * 4. [mergeLyrics] 按优先级选出最佳歌词
 * 5. 内存缓存 (lyricCache) 和 Room 持久化 (cached_lyric) 加速后续加载
 * 6. 歌词预加载 ([preloadLyrics]) 提前缓存下一首
 * 7. AI 增强 ([AiLyricProcessor]) 在合并后补充翻译和对唱标注
 */
@Singleton
class LyricManager @Inject constructor(
    private val repository: PlayerRepository,
    private val qqSongRepository: QQSongRepository,
    private val cachedLyricRepository: CachedLyricRepository,
    private val aiProcessor: AiLyricProcessor,
    @ApplicationContext private val context: Context
) {

    private val TAG = "LyricManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ==================== 状态暴露 ====================

    /** 当前歌词数据，UI 层通过 collectAsState 消费 */
    private val _lyricData = MutableStateFlow(createDefaultLyricData("歌词加载中", source = LyricSource.Loading))
    val lyricData: StateFlow<LyricData> = _lyricData.asStateFlow()

    /** QQ 音乐搜索结果，供手动选歌 sheet 使用 */
    private val _qqSearchResult = MutableStateFlow<Resource<SearchResult>>(Resource.Loading)
    val qqSearchResult: StateFlow<Resource<SearchResult>> = _qqSearchResult.asStateFlow()

    // ==================== 当前歌曲状态 ====================

    /** 当前正在加载歌词的歌曲 ID，用于防止重复加载 */
    private var currentSongId: String? = null
    /** 当前歌词拉取协程 Job，切歌时 cancel */
    private var fetchJob: Job? = null
    /** 标记 QQ 源是否已终结（Success 或 Error），防止同一首歌多个 combine 触发重复处理 */
    private var qqFinalized = false

    // ==================== 歌词缓存 ====================

    /** 内存缓存，FIFO 淘汰，最多 5 首 */
    private val lyricCache = LinkedHashMap<String, LyricData>()
    /** 预加载协程 Job */
    private var preloadJob: Job? = null

    // ==================== 三源 StateFlow ====================

    /** 网易云歌词拉取状态 */
    private val netLyricResult = MutableStateFlow<Resource<Lyric>>(Resource.Loading)
    /** QQ 音乐歌词拉取状态 */
    private val qqLyricResult = MutableStateFlow<Resource<LyricResult>>(Resource.Loading)
    /** AM (Apple Music TTML) 歌词拉取状态 */
    private val amLyricResult = MutableStateFlow<Resource<String>>(Resource.Loading)

    // ==================== 合并入口 ====================

    /**
     * 组合三个源的拉取状态，任一完成即触发合并。
     * sample(50) 防抖，避免短时间内多次触发。
     */
    init {
        combine(netLyricResult, qqLyricResult, amLyricResult) { net, qq, am ->
            Triple(net, qq, am)
        }.sample(50)
         .onEach { (net, qq, am) ->
            mergeAndApply(net, qq, am)
        }.launchIn(scope)
    }

    // ==================== 公开 API ====================

    /**
     * 为指定歌曲加载歌词
     *
     * 流程：
     * 1. 检查缓存（内存 → Room）
     * 2. 并行拉取网易云、AM、QQ 三源
     * 3. 通过 combine → mergeAndApply 渐进式更新歌词
     *
     * @param metadata 歌曲元数据
     * @param forceReload 是否强制重新拉取，true 时跳过缓存和 currentSongId 拦截
     */
    fun loadLyrics(metadata: MediaMetadata, forceReload: Boolean = false) {
        val songId = metadata.id.toString()
        if (!forceReload && currentSongId == songId) return

        currentSongId = songId
        fetchJob?.cancel()
        qqFinalized = false

        // 重置所有源状态
        netLyricResult.value = Resource.Loading
        qqLyricResult.value = Resource.Loading
        amLyricResult.value = Resource.Loading
        _qqSearchResult.value = Resource.Loading
        lrcFallbackContent = null

        _lyricData.value = createDefaultLyricData("歌词加载中", source = LyricSource.Loading)

        lastMetadata = metadata

        // 缓存查找：内存 → Room
        if (!forceReload) {
            lyricCache.remove(songId)?.let { cached ->
                _lyricData.value = cached
            } ?: run {
                val dbCached = runBlocking(Dispatchers.IO) {
                    cachedLyricRepository.get(songId).firstOrNull()
                }
                if (dbCached != null) {
                    val data = dbCached.toLyricData()
                    _lyricData.value = data
                    lyricCache[songId] = data
                }
            }
        }

        // 网络拉取
        fetchJob = scope.launch {
                delay(100)

                launch { fetchNetEaseLyric(songId) }
                launch { fetchAMLLyric(songId) }

                // QQ 音乐拉取（带超时控制）
                val localSong = qqSongRepository.getQQSong(songId).firstOrNull()
                val qqTimeout = try {
                    QqTimeout.valueOf(
                        context.dataStore.data.first()[QqTimeoutKey] ?: QqTimeout.Sec8.name
                    ).seconds
                } catch (_: Exception) {
                    8
                }
                try {
                    withTimeout(qqTimeout * 1000L) {
                        if (localSong != null) {
                            fetchQQLyric(localSong)
                        } else {
                            autoSearchAndPickBest(metadata)
                        }
                    }
                } catch (_: TimeoutCancellationException) {
                    qqLyricResult.value = Resource.Error("QQ timed out")
                }
            }
    }

    /**
     * 自动搜索 QQ 音乐并选择最佳匹配
     *
     * 先按歌名搜索，无 duration 匹配时回退为"歌名+歌手"搜索
     */
    private suspend fun autoSearchAndPickBest(metadata: MediaMetadata) {
        val best = searchAndMatchBest(metadata)
        if (best != null) {
            val qqSong = QQSong(
                id = metadata.id.toString(),
                qid = best.id.toString(),
                title = best.title,
                artist = best.singer.joinToString(",") { it.name },
                album = best.album.title,
                duration = best.interval
            )
            qqSongRepository.insertSong(qqSong)
            fetchQQLyric(qqSong)
        }
    }

    /**
     * 两级搜索匹配：先按歌名，无匹配则按"歌名+歌手"
     *
     * @return 匹配到的 QQ 歌曲，未匹配到返回 null
     */
    private suspend fun searchAndMatchBest(metadata: MediaMetadata): SearchResult.Req0.Data.Body.Song.S? {
        val currentDurationSec = metadata.duration / 1000
        val artistName = metadata.artists.firstOrNull()?.name ?: ""
        val title = metadata.title
        val cleanedTitle = cleanTitle(title)

        // 1. 清洗后的歌名
        if (cleanedTitle != title) {
            Timber.tag(TAG).d("QQ search : $cleanedTitle")
            val best = trySearchMatch(cleanedTitle, currentDurationSec)
            _qqSearchResult.value = trySearchLastResult
            if (best != null) return best
        }

        // 2. 原始歌名
        Timber.tag(TAG).d("QQ search retry with: $title")
        val bestByTitle = trySearchMatch(title, currentDurationSec)
        _qqSearchResult.value = trySearchLastResult
        if (bestByTitle != null) return bestByTitle

        // 3. 清洗后歌名+歌手
        if (artistName.isNotBlank() && cleanedTitle != title) {
            val combined = "$cleanedTitle $artistName"
            Timber.tag(TAG).d("QQ search retry with cleanedTitle+artist: $combined")
            val best = trySearchMatch(combined, currentDurationSec)
            _qqSearchResult.value = trySearchLastResult
            if (best != null) return best
        }

        // 4. 原始歌名+歌手
        if (artistName.isNotBlank()) {
            val combined = "$title $artistName"
            Timber.tag(TAG).d("QQ search retry with title+artist: $combined")
            val best = trySearchMatch(combined, currentDurationSec)
            _qqSearchResult.value = trySearchLastResult
            if (best != null) return best
        }

        return null
    }

    /**
     * 去除歌名中的括号内容（如 feat./with/remix 等附加信息）
     *
     * 处理中文括号（）和英文括号 ()。
     * 例如 "abc (feat. xxx)" → "abc"
     */
    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("""[\(（][^)）]*[\)）]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /** 缓存最后一次 QQ 搜索结果，供 _qqSearchResult 和预加载流程使用 */
    private var trySearchLastResult: Resource<SearchResult> = Resource.Loading

    /**
     * 搜索 QQ 音乐并在前 5 条结果中匹配时长（±5 秒）
     *
     * @param keyword 搜索关键词
     * @param targetDurationSec 目标时长（秒）
     * @return 匹配到的歌曲，未匹配到返回 null
     */
    private suspend fun trySearchMatch(
        keyword: String,
        targetDurationSec: Long
    ): SearchResult.Req0.Data.Body.Song.S? {
        val result = repository.searchNew(keyword)
        trySearchLastResult = result
        if (result !is Resource.Success) return null
        val songs = result.data.req0.data.body.song.list
        return songs.take(5).firstOrNull { song ->
            abs(targetDurationSec - song.interval) <= 5
        }
    }

    /**
     * 拉取网易云歌词
     *
     * 写入 netLyricResult（Lyric 结构体，含 lrc/yrc/tlyric/ytlrc 等字段）。
     */
    private suspend fun fetchNetEaseLyric(id: String) {
        try {
            netLyricResult.value = repository.getLyricV1(id)
        } catch (e: Exception) {
            Timber.e(e, "NetEase fetch error")
            netLyricResult.value = Resource.Error("NetEase fetch failed")
        }
    }

    /**
     * 拉取 Apple Music TTML 逐字歌词
     *
     * 写入 amLyricResult（原始 TTML 字符串）。
     */
    private suspend fun fetchAMLLyric(id: String) {
        try {
            amLyricResult.value = repository.getAMLLyric(id)
        } catch (e: Exception) {
            Timber.e(e, "AML fetch error")
            amLyricResult.value = Resource.Error("AML fetch failed")
        }
    }

    /** 当前 QQ 歌曲引用（用于 LRC 回退） */
    private var currentQQSong: QQSong? = null

    /**
     * 拉取 QQ 音乐歌词
     *
     * 写入 qqLyricResult。若返回 QRC 格式（qrcT ≠ 0），额外拉取 LRC 兜底。
     */
    fun fetchQQLyric(song: QQSong) {
        scope.launch {
            currentQQSong = song
            qqLyricResult.value = Resource.Loading
            try {
                val result = repository.getLyricNew(
                    song.title, song.album, song.artist, song.duration, song.qid.toLong()
                )
                qqLyricResult.value = result
                if (result is Resource.Success) {
                    val qrcT = result.data.musicMusichallSongPlayLyricInfoGetPlayLyricInfo.data.qrcT
                    if (qrcT != 0) {
                        fetchQQLyricLrc(song)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "QQ fetch error")
                qqLyricResult.value = Resource.Error("QQ fetch failed")
            }
        }
    }

    /**
     * QQ 歌词 LRC 回退
     *
     * 当主歌词是 QRC 逐字格式时，额外拉取纯 LRC 作为兜底（qrc=0, qrcT=0）。
     * 解码后存入 lrcFallbackContent 并触发重合并。
     */
    private suspend fun fetchQQLyricLrc(song: QQSong) {
        try {
            val lrcResult = repository.getLyricLrc(
                song.title, song.album, song.artist, song.duration, song.qid.toLong()
            )
            if (lrcResult is Resource.Success) {
                val lrcContent = QRCUtils.decodeLyric(
                    lrcResult.data.musicMusichallSongPlayLyricInfoGetPlayLyricInfo.data.lyric
                )
                lrcFallbackContent = lrcContent
                remergeLyrics()
            }
        } catch (e: Exception) {
            Timber.e(e, "QQ LRC fallback fetch error")
        }
    }

    /** QQ LRC 回退内容，供 mergeLyrics 中 QQ 源使用 */
    private var lrcFallbackContent: String? = null

    /**
     * 触发重合并
     *
     * 当 LRC 回退数据到达时，用当前三个源的现有状态重新合并歌词。
     */
    private fun remergeLyrics() {
        scope.launch {
            mergeAndApply(
                netLyricResult.value,
                qqLyricResult.value,
                amLyricResult.value
            )
        }
    }

    // ==================== 合并逻辑 ====================

    /**
     * 合并三个源的数据并更新 UI
     *
     * 流程：
     * 1. 守卫：全 Loading 且已有歌词 → 跳过
     * 2. 组装 LyricSourceData 列表
     * 3. 调用 [mergeLyrics] 按优先级选出最佳
     * 4. 缓存结果（内存 + Room）
     * 5. 根据 QQ 源是否终结决定触发 AI（双源 smartMerge / 单源 singleEnhance）
     */
    private suspend fun mergeAndApply(
        net: Resource<Lyric>,
        qq: Resource<LyricResult>,
        am: Resource<String>
    ) {
        val songIdAtStart = currentSongId

        // 守卫：三方 Loading 且已有缓存歌词（非 Loading/Empty），不覆盖
        if (net is Resource.Loading && qq is Resource.Loading && am is Resource.Loading) {
            val source = _lyricData.value.source
            if (source != LyricSource.Loading && source != LyricSource.Empty) {
                return
            }
        }

        data class MergeResult(
            val lyricData: LyricData,
            val cacheContent: String?,
            val cacheTranslation: String?,
            val cacheParserType: String,
            val sources: List<LyricSourceData>
        )

        val mergeResult = withContext(Dispatchers.IO) {
            val isPureMusic = (net as? Resource.Success)?.data?.pureMusic == true
            val sources = mutableListOf<LyricSourceData>()

            (am as? Resource.Success)?.let { sources.add(LyricSourceData.AM(it.data)) }
            (net as? Resource.Success)?.data?.let { sources.add(LyricSourceData.NetEase(it)) }
            (qq as? Resource.Success)?.data?.musicMusichallSongPlayLyricInfoGetPlayLyricInfo?.data?.let { data ->
                try {
                    val isQRC = data.qrcT != 0
                    val decoded = data.copy(
                        lyric = QRCUtils.decodeLyric(data.lyric),
                        trans = QRCUtils.decodeLyric(data.trans, true),
                        roma = QRCUtils.decodeLyric(data.roma)
                    )
                    sources.add(LyricSourceData.QQMusic(decoded, isQRC, lrcFallbackContent))
                } catch (e: Exception) {
                    Timber.e(e, "QRC decoding failed")
                }
            }

            val lyricData = mergeLyrics(sources, isPureMusic)

            val (cacheContent, cacheTranslation, cacheParserType) = buildCacheInfo(sources, lyricData)

            MergeResult(lyricData, cacheContent, cacheTranslation, cacheParserType, sources)
        }

        // 歌曲已切换，放弃旧结果
        if (currentSongId != songIdAtStart) return
        if (songIdAtStart == null) return

        // 守卫：同一源不重复更新 UI
        val currentSource = _lyricData.value.source
        val skipUiUpdate = currentSource != LyricSource.Loading && currentSource != LyricSource.Empty
            && mergeResult.lyricData.source == currentSource
        val cacheContent = mergeResult.cacheContent

        if (!skipUiUpdate) {
            _lyricData.value = mergeResult.lyricData
            lyricCache[songIdAtStart] = mergeResult.lyricData
            trimCache()

            if (cacheContent != null) {
                cachedLyricRepository.insert(
                    CachedLyric(
                        songId = songIdAtStart,
                        content = cacheContent,
                        translation = mergeResult.cacheTranslation,
                        isVerbatim = mergeResult.lyricData.isVerbatim,
                        isPureMusic = mergeResult.lyricData.isPureMusic,
                        sourceName = mergeResult.lyricData.source.name,
                        parserType = mergeResult.cacheParserType,
                        aiProcessed = false,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        // ===== 本地对唱合并（仅在 QQ 源终结后触发一次） =====
        val amSuccess = am as? Resource.Success
        if (amSuccess != null) return

        val netSuccess = net as? Resource.Success
        val qqSuccess = qq as? Resource.Success
        val qqTerminal = qq is Resource.Success || qq is Resource.Error

        if (qqTerminal && !qqFinalized) {
            qqFinalized = true

            // 仅在有对唱标记的网易云歌词时触发本地对唱解析
            val neteaseData = netSuccess?.data
            val lrcText = neteaseData?.lrc?.lyric?.takeIf { it.isNotBlank() }
            val hasDuet = lrcText != null && aiProcessor.isDuetLikely(lrcText)
            Timber.tag(TAG).d("duet check: hasLrc=${lrcText != null}, isDuet=$hasDuet, lrcLen=${lrcText?.length}")
            if (hasDuet) {
                val netease = LyricSourceData.NetEase(neteaseData)
                val qqParsed = qqSuccess?.let { parseQQSource(it) }
                val dueted = if (qqParsed != null) {
                    aiProcessor.mergeWithDuet(netease, qqParsed)
                } else {
                    aiProcessor.singleDuet(netease)
                }
                if (dueted != null) {
                    _lyricData.value = dueted
                    lyricCache[songIdAtStart] = dueted
                }
            }

            // AI 缓存失效：网络返回翻译 → 删除本地 AI 翻译缓存
            val winnerHasTrans = when (mergeResult.lyricData.source) {
                LyricSource.NetEaseCloudMusic -> (netSuccess?.data?.tlyric?.lyric?.isNotBlank() ?: false)
                LyricSource.QQMusic -> (qqSuccess?.data?.musicMusichallSongPlayLyricInfoGetPlayLyricInfo?.data?.trans?.isNotBlank() ?: false)
                else -> false
            }
            if (winnerHasTrans) {
                scope.launch {
                    val cached = cachedLyricRepository.get(songIdAtStart).firstOrNull()
                    if (cached != null && cached.aiProcessed) {
                        cachedLyricRepository.delete(songIdAtStart)
                    }
                }
            }
        }
    }

    /**
     * 从 QQ Resource.Success 中构建 LyricSourceData.QQMusic
     *
     * 解码 QRC 加密字段，提取 lyric、trans、roma 和 isQRC 标记。
     */
    private fun parseQQSource(qq: Resource.Success<LyricResult>): LyricSourceData.QQMusic? {
        val data = qq.data.musicMusichallSongPlayLyricInfoGetPlayLyricInfo?.data ?: return null
        return try {
            val isQRC = data.qrcT != 0
            val decoded = data.copy(
                lyric = QRCUtils.decodeLyric(data.lyric),
                trans = QRCUtils.decodeLyric(data.trans, true),
                roma = QRCUtils.decodeLyric(data.roma)
            )
            LyricSourceData.QQMusic(decoded, isQRC, lrcFallbackContent)
        } catch (e: Exception) {
            Timber.e(e, "parseQQSource failed")
            null
        }
    }

    /**
     * 单源 AI 增强
     *
     * 检查触发条件后调用 [AiLyricProcessor.singleEnhance]。
     * 增强结果写入 _lyricData 和 Room。
     */
    private var lastMetadata: MediaMetadata? = null

    private fun getCurrentMetadata(): MediaMetadata? = lastMetadata

    /**
     * 手动搜索 QQ 音乐（供选歌 sheet 使用）
     *
     * 结果写入 [_qqSearchResult]，UI 层通过 [qqSearchResult] 观察。
     */
    fun searchQQSong(keyword: String) {
        scope.launch {
            _qqSearchResult.value = Resource.Loading
            _qqSearchResult.value = repository.searchNew(keyword)
        }
    }

    /**
     * 用户手动选择 QQ 歌曲作为歌词来源
     *
     * 插入 QQSong 映射到 Room，然后拉取歌词。
     */
    fun selectQQSongForLyric(metadata: MediaMetadata, song: SearchResult.Req0.Data.Body.Song.S) {
        scope.launch {
            val qqSong = QQSong(
                id = metadata.id.toString(),
                qid = song.id.toString(),
                title = song.title,
                artist = song.singer.joinToString(",") { it.name },
                album = song.album.title,
                duration = song.interval
            )
            qqSongRepository.insertSong(qqSong)
            fetchQQLyric(qqSong)
        }
    }

    /**
     * 取消当前所有拉取和预加载任务
     */
    fun manualTranslate() {
        val metadata = lastMetadata ?: return
        val songId = metadata.id.toString()
        scope.launch {
            val sources = mutableListOf<LyricSourceData>()
            (netLyricResult.value as? Resource.Success)?.data?.let { sources.add(LyricSourceData.NetEase(it)) }
            (qqLyricResult.value as? Resource.Success)?.let { qqRes ->
                parseQQSource(qqRes)?.let { sources.add(it) }
            }
            if (sources.isEmpty()) return@launch

            val result = aiProcessor.manualTranslate(sources, metadata)
            if (result != null && currentSongId == songId) {
                _lyricData.value = result
                lyricCache[songId] = result
                val netLine = (sources.filterIsInstance<LyricSourceData.NetEase>().firstOrNull())
                    ?.lyric?.lrc?.lyric?.takeIf { it.isNotBlank() }
                if (netLine != null) {
                    cachedLyricRepository.insert(
                        CachedLyric(songId, netLine, null, false, false, result.source.name, "LRC", true, System.currentTimeMillis())
                    )
                }
            }
        }
    }

    fun cancelAll() {
        fetchJob?.cancel()
        preloadJob?.cancel()
        currentSongId = null
    }

    // ==================== 歌词预加载 ====================

    /**
     * 预加载指定歌曲的歌词到内存缓存
     *
     * 由 [PlayerStateContainer] 在切歌时调用，提前拉取下一首。
     * 效果：下一首命中缓存 → 歌词瞬间显示。
     */
    fun preloadLyrics(metadata: MediaMetadata) {
        val songId = metadata.id.toString()
        if (lyricCache.containsKey(songId) || currentSongId == songId) return

        preloadJob?.cancel()
        preloadJob = scope.launch {
            val result = fetchAndMergeLyrics(metadata, songId)
            if (result != null) {
                lyricCache[songId] = result
                trimCache()
            }
        }
    }

    /**
     * 独立拉取三源并合并（预加载专用）
     *
     * 与主流程的 [mergeAndApply] 不同，此方法使用独立的 fetch*Result
     * 变体（不更新共享 StateFlow），因此不会干扰当前歌词展示。
     */
    private suspend fun fetchAndMergeLyrics(metadata: MediaMetadata, songId: String): LyricData? {
        val (netResult, amResult, qqResult) = coroutineScope {
            val netDeferred = async { fetchNetEaseLyricResult(songId) }
            val amDeferred = async { fetchAMLLyricResult(songId) }
            val qqDeferred = async { fetchQQLyricResult(metadata) }
            Triple(netDeferred.await(), amDeferred.await(), qqDeferred.await())
        }

        return withContext(Dispatchers.IO) {
            val isPureMusic = (netResult as? Resource.Success)?.data?.pureMusic == true
            val sources = mutableListOf<LyricSourceData>()

            (amResult as? Resource.Success)?.let { sources.add(LyricSourceData.AM(it.data)) }
            (netResult as? Resource.Success)?.data?.let { sources.add(LyricSourceData.NetEase(it)) }
            (qqResult as? Resource.Success)?.data?.musicMusichallSongPlayLyricInfoGetPlayLyricInfo?.data?.let { data ->
                try {
                    val isQRC = data.qrcT != 0
                    val decoded = data.copy(
                        lyric = QRCUtils.decodeLyric(data.lyric),
                        trans = QRCUtils.decodeLyric(data.trans, true),
                        roma = QRCUtils.decodeLyric(data.roma)
                    )
                    sources.add(LyricSourceData.QQMusic(decoded, isQRC, null))
                } catch (e: Exception) {
                    Timber.e(e, "QRC decoding failed in preload")
                }
            }

            mergeLyrics(sources, isPureMusic)
        }
    }

    /** 预加载用——网易云歌词拉取（返回结果，不更新共享 StateFlow） */
    private suspend fun fetchNetEaseLyricResult(id: String): Resource<Lyric> {
        return try {
            repository.getLyricV1(id)
        } catch (e: Exception) {
            Timber.e(e, "NetEase preload fetch error")
            Resource.Error("NetEase fetch failed")
        }
    }

    /** 预加载用——AM 歌词拉取（返回结果，不更新共享 StateFlow） */
    private suspend fun fetchAMLLyricResult(id: String): Resource<String> {
        return try {
            repository.getAMLLyric(id)
        } catch (e: Exception) {
            Timber.e(e, "AML preload fetch error")
            Resource.Error("AML fetch failed")
        }
    }

    /** 预加载用——QQ 歌词拉取（含搜索匹配，返回结果，不更新共享 StateFlow） */
    private suspend fun fetchQQLyricResult(metadata: MediaMetadata): Resource<LyricResult> {
        val songId = metadata.id.toString()

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

        val best = searchAndMatchBest(metadata) ?: return Resource.Error("No QQ match found")

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

    // ==================== 缓存管理 ====================

    /** 内存缓存 FIFO 逐出，保留最近 5 首 */
    private fun trimCache() {
        while (lyricCache.size > 5) {
            lyricCache.remove(lyricCache.entries.first().key)
        }
    }

    /**
     * 从合并源和结果中提取 Room 持久化所需信息
     *
     * @return Triple(原始歌词文本, 翻译文本, 解析器类型)
     *   对逐字歌词（TTML/YRC/QRC），第一项为 null 表示不缓存
     */
    private fun buildCacheInfo(
        sources: List<LyricSourceData>,
        lyricData: LyricData
    ): Triple<String?, String?, String> {
        return when (lyricData.source) {
            LyricSource.AM -> Triple(null, null, "TTML")
            LyricSource.NetEaseCloudMusic -> {
                val netease = sources.filterIsInstance<LyricSourceData.NetEase>().firstOrNull()
                if (lyricData.isVerbatim) {
                    Triple(null, null, "YRC")
                } else {
                    val lrc = netease?.lyric?.lrc?.lyric?.takeIf { it.isNotBlank() }
                    val translation = netease?.lyric?.tlyric?.lyric?.takeIf { it.isNotBlank() }
                    Triple(lrc, translation, "LRC")
                }
            }
            LyricSource.QQMusic -> {
                val qq = sources.filterIsInstance<LyricSourceData.QQMusic>().firstOrNull()
                val lrc = qq?.lrcContent?.takeIf { it.isNotBlank() }
                    ?: qq?.lyric?.lyric?.takeIf { it.isNotBlank() }
                val translation = qq?.lyric?.trans?.takeIf { it.isNotBlank() }
                if (lyricData.isVerbatim) {
                    Triple(lrc, translation, "QRC")
                } else {
                    Triple(lrc, translation, "LRC")
                }
            }
            else -> Triple(null, null, "LRC")
        }
    }
}

/**
 * 从 Room 缓存恢复 [LyricData]
 *
 * 根据 parserType 选择对应的解析器：
 * - TTML → TTMLParser
 * - YRC  → YRCParser
 * - QRC  → QRCParser (decoded trans)
 * - LRC  → LRCParser (default)
 */
fun CachedLyric.toLyricData(): LyricData = LyricData(
    isVerbatim = isVerbatim,
    isPureMusic = isPureMusic,
    source = try { LyricSource.valueOf(sourceName) } catch (_: Exception) { LyricSource.Empty },
    lyricLine = when (parserType) {
        "TTML" -> TTMLParser().parse(content)
        "YRC" -> YRCParser.parse(content, translation ?: "")
        "QRC" -> {
            val decoded = translation?.let { QRCUtils.decodeLyric(it) } ?: ""
            QRCParser.parse(content, decoded)
        }
        else -> LRCParser.parse(content, translation)
    }
)
