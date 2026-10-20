package com.ljyh.mei.utils.lyric

import com.ljyh.mei.data.model.response.Lyric
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.unblockneteasemusic.model.MusicLyrics
import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.mei.data.model.room.CachedLyric
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.PlayerRepository
import com.ljyh.mei.di.repository.CachedLyricRepository
import com.ljyh.mei.di.repository.QQSongRepository
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricCandidate
import com.ljyh.mei.ui.model.LyricSelectionState
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.ui.model.LyricVariant
import com.ljyh.unblockneteasemusic.qq.QRCUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@OptIn(kotlinx.coroutines.FlowPreview::class)

/**
 * 歌词管理器
 *
 * 协调歌词的获取、候选版本、选择、缓存和预加载。
 *
 * 数据流概要：
 * 1. [loadLyrics] 被调用时重置所有源状态为 Loading
 * 2. 并行拉取三源（网易云、AM、QQ），各自更新对应的 StateFlow
 * 3. [combine] 监听三个 StateFlow，任一变化触发 [mergeAndApply]
 * 4. [mergeLyrics] 生成自动结果，手动选择可覆盖它
 * 5. 内存缓存 (lyricCache) 和 Room 持久化 (cached_lyric) 加速后续加载
 * 6. 歌词预加载 ([preloadLyrics]) 提前缓存下一首
 * 7. 本地对唱检测 ([DuetDetector]) 合并后对对唱歌词进行对齐
 */
@Singleton
class LyricManager @Inject constructor(
    private val repository: PlayerRepository,
    private val qqSongRepository: QQSongRepository,
    private val cachedLyricRepository: CachedLyricRepository,
    private val duetDetector: DuetDetector,
    private val preloader: LyricPreloader,
    private val selectionStore: LyricSelectionStore,
) {

    private val TAG = "LyricManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ==================== 状态暴露 ====================

    /** 当前歌词数据，UI 层通过 collectAsState 消费 */
    private val _lyricData =
        MutableStateFlow(createDefaultLyricData("歌词加载中", source = LyricSource.Loading))
    val lyricData: StateFlow<LyricData> = _lyricData.asStateFlow()

    private val _selectionState = MutableStateFlow(LyricSelectionState())
    val selectionState: StateFlow<LyricSelectionState> = _selectionState.asStateFlow()
    private var latestCandidates: List<LyricCandidate> = emptyList()
    private var latestAutoData: LyricData? = null
    private var selectionReady = false

    /** QQ 音乐搜索结果，供手动选歌 sheet 使用 */
    private val _qqSearchResult = MutableStateFlow<Resource<List<MusicTrack>>>(Resource.Loading)
    val qqSearchResult: StateFlow<Resource<List<MusicTrack>>> = _qqSearchResult.asStateFlow()

    // ==================== 当前歌曲状态 ====================

    /** 当前正在加载歌词的歌曲 ID，用于防止重复加载 */
    private var currentSongId: String? = null

    /** 当前歌词拉取协程 Job，切歌时 cancel */
    private var fetchJob: Job? = null

    private var manualQqFetchJob: Job? = null
    private var lyricLoadRevision = 0L

    /** 标记 QQ 源是否已终结（Success 或 Error），防止同一首歌多个 combine 触发重复处理 */
    private var qqFinalized = false

    // ==================== 歌词缓存 ====================

    /** 内存缓存，FIFO 淘汰，最多 5 首 */
    private val lyricCache = LinkedHashMap<String, LyricData>()
    private val cacheWriteMutex = Mutex()

    // ==================== 三源 StateFlow ====================

    /** 网易云歌词拉取状态 */
    private val netLyricResult = MutableStateFlow<Resource<Lyric>>(Resource.Loading)

    /** QQ 音乐歌词拉取状态 */
    private val qqLyricResult = MutableStateFlow<Resource<MusicLyrics>>(Resource.Loading)

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
        }.sample(50.milliseconds)
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
        lyricLoadRevision++
        fetchJob?.cancel()
        manualQqFetchJob?.cancel()
        if (forceReload) lyricCache.remove(songId)
        qqFinalized = false
        selectionReady = false
        latestCandidates = emptyList()
        latestAutoData = null
        _selectionState.value = LyricSelectionState(songId = songId)

        // 重置所有源状态
        netLyricResult.value = Resource.Loading
        qqLyricResult.value = Resource.Loading
        amLyricResult.value = Resource.Loading
        _qqSearchResult.value = Resource.Loading
        lrcFallbackContent = null
        currentQQSong = null

        _lyricData.value = createDefaultLyricData("歌词加载中", source = LyricSource.Loading)

        lastMetadata = metadata

        val revision = lyricLoadRevision
        fetchJob = scope.launch {
            val requested = selectionStore.get(songId)
            if (currentSongId != songId || lyricLoadRevision != revision) return@launch
            _selectionState.value = _selectionState.value.copy(
                requested = requested,
                preferenceLoaded = true,
            )
            selectionReady = true

            if (!forceReload) {
                lyricCache.remove(songId)?.let { cached ->
                    if (requested == LyricVariant.Auto || lyricVariantOf(cached) == requested) {
                        _lyricData.value = cached
                        _selectionState.value = _selectionState.value.copy(active = lyricVariantOf(cached))
                    }
                }
            }

            // Room 缓存查找（异步，不阻塞主线程）
            if (!forceReload && currentSongId == songId) {
                val dbCached = withContext(Dispatchers.IO) {
                    cachedLyricRepository.get(songId).firstOrNull()
                }
                if (dbCached != null && currentSongId == songId && lyricLoadRevision == revision &&
                    (dbCached.sourceName != LyricSource.QQMusic.name ||
                        !preloader.isQqMatchRejected(songId))) {
                    val data = dbCached.toLyricData()
                    if (requested == LyricVariant.Auto || lyricVariantOf(data) == requested) {
                        _lyricData.value = data
                        _selectionState.value = _selectionState.value.copy(active = lyricVariantOf(data))
                        lyricCache[songId] = data
                    }
                }
            }

            preloader.sourceFlow(metadata, forceReload).collect { snapshot ->
                netLyricResult.value = snapshot.netEase
                qqLyricResult.value = snapshot.qq
                amLyricResult.value = snapshot.ttml
                if (snapshot.qqLrc != lrcFallbackContent) {
                    lrcFallbackContent = snapshot.qqLrc
                    remergeLyrics()
                }
            }
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
        manualQqFetchJob?.cancel()
        manualQqFetchJob = scope.launch {
            if (currentSongId != song.id) return@launch
            currentQQSong = song
            qqLyricResult.value = Resource.Loading
            try {
                val result = repository.getLyricNew(
                    song.title, song.album, song.artist, song.duration, song.qid.toLong()
                )
                if (currentSongId != song.id) return@launch
                if (result is Resource.Success) {
                    selectLyricVariant(
                        song.id,
                        if (result.data.wordSynced) LyricVariant.QqQrc else LyricVariant.QqLrc,
                    )
                }
                qqLyricResult.value = result
                currentSongId?.let { preloader.overrideQq(it, result) }
                if (result is Resource.Success) {
                    if (result.data.wordSynced) {
                        fetchQQLyricLrc(song)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Timber.e(e, "QQ fetch error")
                val error = Resource.Error("QQ fetch failed")
                qqLyricResult.value = error
                currentSongId?.let { preloader.overrideQq(it, error) }
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
                if (currentSongId != song.id) return
                val lrcContent = QRCUtils.decodeLyric(
                    lrcResult.data.original
                )
                lrcFallbackContent = lrcContent
                remergeLyrics()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
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
     * 3. 解析各个可用版本，并生成自动结果
     * 4. 缓存结果（内存 + Room）
     * 5. 自动模式下，QQ 源终结后尝试本地对唱处理
     */
    private suspend fun mergeAndApply(
        net: Resource<Lyric>,
        qq: Resource<MusicLyrics>,
        am: Resource<String>
    ) {
        val songIdAtStart = currentSongId
        val revisionAtStart = lyricLoadRevision
        if (!selectionReady) return

        // 守卫：三源全 Loading 且已有有效歌词 → 不覆盖
        val hasValidLyrics = _lyricData.value.let {
            it.source != LyricSource.Loading && it.source != LyricSource.Empty
                    && it.lyricLine.lines.isNotEmpty()
        }
        if (hasValidLyrics && net is Resource.Loading && qq is Resource.Loading && am is Resource.Loading) return

        data class MergeResult(
            val autoData: LyricData,
            val candidates: List<LyricCandidate>,
            val sources: List<LyricSourceData>,
        )

        val mergeResult = withContext(Dispatchers.IO) {
            val isPureMusic = (net as? Resource.Success)?.data?.pureMusic == true
            val sources = mutableListOf<LyricSourceData>()

            (am as? Resource.Success)?.let { sources.add(LyricSourceData.AM(it.data)) }
            (net as? Resource.Success)?.data?.let { sources.add(LyricSourceData.NetEase(it)) }
            (qq as? Resource.Success)?.data?.let { data ->
                try {
                    val isQRC = data.wordSynced
                    val decoded = data.copy(
                        original = QRCUtils.decodeLyric(data.original),
                        translation = QRCUtils.decodeLyric(data.translation, true),
                        romanization = QRCUtils.decodeLyric(data.romanization)
                    )
                    sources.add(LyricSourceData.QQMusic(decoded, isQRC, lrcFallbackContent))
                } catch (e: Exception) {
                    Timber.e(e, "QRC decoding failed")
                }
            }

            MergeResult(
                autoData = mergeLyrics(sources, isPureMusic),
                candidates = buildLyricCandidates(sources, isPureMusic),
                sources = sources,
            )
        }

        // 歌曲已切换，放弃旧结果
        if (currentSongId != songIdAtStart || lyricLoadRevision != revisionAtStart) return
        if (songIdAtStart == null) return

        latestAutoData = mergeResult.autoData
        latestCandidates = mergeResult.candidates
        val requested = _selectionState.value.requested
        val choice = resolveLyricChoice(requested, mergeResult.autoData, mergeResult.candidates)
        val candidateData = choice.data
        val activeVariant = choice.active
        val loading = net is Resource.Loading || qq is Resource.Loading || am is Resource.Loading
        _selectionState.value = _selectionState.value.copy(
            candidates = mergeResult.candidates,
            isLoading = loading,
        )

        // 守卫：不拿空结果覆盖已有有效歌词（竞态保护）
        if (_lyricData.value.lyricLine.lines.isNotEmpty()
            && candidateData.lyricLine.lines.isEmpty()
        ) return

        // Progressive results may arrive out of order. Never replace a better lyric with a
        // lower-quality automatic candidate. A manual choice always takes precedence.
        val currentData = _lyricData.value
        val forceChoice = requested != LyricVariant.Auto && (choice.preferredAvailable || !loading)
        val skipUiUpdate = if (forceChoice) {
            currentData == candidateData && _selectionState.value.active == activeVariant
        } else {
            !shouldApplyLyricUpdate(currentData, candidateData)
        }

        if (!skipUiUpdate) {
            _lyricData.value = candidateData
            _selectionState.value = _selectionState.value.copy(active = activeVariant)
            lyricCache[songIdAtStart] = candidateData
            trimCache()

            val (cacheContent, cacheTranslation, cacheParserType) =
                buildCacheInfo(mergeResult.sources, candidateData)
            if (cacheContent != null) {
                cacheWriteMutex.withLock {
                    if (currentSongId == songIdAtStart && lyricLoadRevision == revisionAtStart) {
                        cachedLyricRepository.insert(
                            CachedLyric(
                                songId = songIdAtStart,
                                content = cacheContent,
                                translation = cacheTranslation,
                                isVerbatim = candidateData.isVerbatim,
                                isPureMusic = candidateData.isPureMusic,
                                sourceName = candidateData.source.name,
                                parserType = cacheParserType,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }

        // ===== 本地对唱合并（仅在 QQ 源终结后触发一次） =====
        val amSuccess = am as? Resource.Success
        if (amSuccess != null) return

        val netSuccess = net as? Resource.Success
        val qqSuccess = qq as? Resource.Success
        val qqTerminal = qq is Resource.Success || qq is Resource.Error

        if (requested == LyricVariant.Auto && qqTerminal && !qqFinalized) {
            qqFinalized = true

            // 仅在有对唱标记的网易云歌词时触发本地对唱解析
            val neteaseData = netSuccess?.data
            val lrcText = neteaseData?.lrc?.lyric?.takeIf { it.isNotBlank() }
            val hasDuet = lrcText != null && duetDetector.isDuetLikely(lrcText)
            Timber.tag(TAG)
                .d("duet check: hasLrc=${lrcText != null}, isDuet=$hasDuet, lrcLen=${lrcText?.length}")
            if (hasDuet) {
                val netease = LyricSourceData.NetEase(neteaseData)
                val qqParsed = qqSuccess?.let { parseQQSource(it) }
                val dueted = if (qqParsed != null) {
                    duetDetector.mergeWithDuet(netease, qqParsed)
                } else {
                    duetDetector.singleDuet(netease)
                }
                if (dueted != null) {
                    _lyricData.value = dueted
                    _selectionState.value = _selectionState.value.copy(active = lyricVariantOf(dueted))
                    lyricCache[songIdAtStart] = dueted
                }
            }

        }
    }

    /**
     * 从 QQ Resource.Success 中构建 LyricSourceData.QQMusic
     *
     * 解码 QRC 加密字段，提取 lyric、trans、roma 和 isQRC 标记。
     */
    private fun parseQQSource(qq: Resource.Success<MusicLyrics>): LyricSourceData.QQMusic? {
        val data = qq.data
        return try {
            val isQRC = data.wordSynced
            val decoded = data.copy(
                original = QRCUtils.decodeLyric(data.original),
                translation = QRCUtils.decodeLyric(data.translation, true),
                romanization = QRCUtils.decodeLyric(data.romanization)
            )
            LyricSourceData.QQMusic(decoded, isQRC, lrcFallbackContent)
        } catch (e: Exception) {
            Timber.e(e, "parseQQSource failed")
            null
        }
    }

    private var lastMetadata: MediaMetadata? = null

    private fun getCurrentMetadata(): MediaMetadata? = lastMetadata

    /**
     * 手动搜索 QQ 音乐（供选歌 sheet 使用）
     *
     * 结果写入 [_qqSearchResult]，UI 层通过 [qqSearchResult] 观察。
     */
    fun searchQQSong(keyword: String) {
        _qqSearchResult.value = Resource.Loading
        scope.launch {
            _qqSearchResult.value = repository.searchNew(keyword)
        }
    }

    /** A manual per-song choice overrides the automatic quality ranking. */
    suspend fun selectLyricVariant(songId: String, variant: LyricVariant) {
        if (currentSongId == songId) lyricLoadRevision++
        selectionStore.set(songId, variant)
        cacheWriteMutex.withLock { cachedLyricRepository.delete(songId) }
        lyricCache.remove(songId)
        if (currentSongId != songId) return

        _selectionState.value = _selectionState.value.copy(
            requested = variant,
            preferenceLoaded = true,
        )
        latestAutoData?.let { autoData ->
            val choice = resolveLyricChoice(variant, autoData, latestCandidates)
            _lyricData.value = choice.data
            _selectionState.value = _selectionState.value.copy(active = choice.active)
            lyricCache[songId] = choice.data
        }
        remergeLyrics()
    }

    /**
     * 用户手动选择 QQ 歌曲作为歌词来源
     *
     * 插入 QQSong 映射到 Room，然后拉取歌词。
     */
    fun selectQQSongForLyric(metadata: MediaMetadata, song: MusicTrack) {
        scope.launch {
            preloader.allowQqMatch(metadata.id.toString())
            val qqSong = QQSong(
                id = metadata.id.toString(),
                qid = song.id.value,
                title = song.title,
                artist = song.artists.joinToString(",") { it.name },
                album = song.album?.name.orEmpty(),
                duration = song.durationMs / 1_000
            )
            qqSongRepository.insertSong(qqSong)
            fetchQQLyric(qqSong)
        }
    }

    /** Remove a rejected QQ match from every cache before resolving the remaining sources. */
    suspend fun deleteQqLyric(songId: String) {
        if (currentSongId == songId) {
            lyricLoadRevision++
            manualQqFetchJob?.cancel()
        }
        preloader.rejectQqMatch(songId)
        if (selectionStore.get(songId) in setOf(LyricVariant.QqQrc, LyricVariant.QqLrc)) {
            selectionStore.set(songId, LyricVariant.Auto)
        }
        qqSongRepository.deleteSongById(songId)
        cacheWriteMutex.withLock { cachedLyricRepository.delete(songId) }
        lyricCache.remove(songId)
        if (currentSongId == songId) {
            lastMetadata?.takeIf { it.id.toString() == songId }
                ?.let { loadLyrics(it, forceReload = true) }
        }
    }

    /**
     * 取消当前所有拉取和预加载任务
     */
    fun cancelAll() {
        fetchJob?.cancel()
        manualQqFetchJob?.cancel()
        currentSongId = null
    }

    // ==================== 歌词预加载 ====================

    /**
     * 预加载指定歌曲的歌词到内存缓存
     *
     * 由 [com.ljyh.mei.ui.component.player.state.PlayerStateContainer] 在切歌时调用，提前拉取下一首。
     * 委托给 [LyricPreloader] 执行，不干扰当前歌词展示。
     */
    fun preloadLyrics(metadata: MediaMetadata) {
        val songId = metadata.id.toString()
        if (lyricCache.containsKey(songId) || currentSongId == songId) return
        preloader.preload(metadata)
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
     *   TTML/YRC 暂不写入 Room；QRC 使用已解码的原文缓存
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
                val lrc = if (lyricData.isVerbatim) {
                    qq?.lyric?.original?.takeIf { it.isNotBlank() }
                } else {
                    qq?.lrcContent?.takeIf { it.isNotBlank() }
                        ?: qq?.lyric?.original?.takeIf { it.isNotBlank() }
                }
                val translation = qq?.lyric?.translation?.takeIf { it.isNotBlank() }
                if (lyricData.isVerbatim) {
                    Triple(lrc, translation, "QRC")
                } else {
                    Triple(lrc, translation, "LRC")
                }
            }

            else -> Triple(null, null, "LRC")
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
        source = try {
            LyricSource.valueOf(sourceName)
        } catch (_: Exception) {
            LyricSource.Empty
        },
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


}
