package com.ljyh.mei.utils.lyric

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ljyh.mei.constants.AiTriggerMode
import com.ljyh.mei.constants.AiBaseUrlKey
import com.ljyh.mei.constants.AiApiKeyKey
import com.ljyh.mei.constants.AiModelKey
import com.ljyh.mei.constants.AiTriggerModeKey
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.network.AiLyricClient
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.utils.dataStore
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiLyricProcessor @Inject constructor(
    private val client: AiLyricClient,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) {
    data class DuetSegment(
        val role: String,
        val startLine: Int,
        val endLine: Int
    )

    private data class DuetRaw(
        val role: String,
        val start: Int,
        val end: Int
    )

    private val duetListType = object : TypeToken<List<DuetRaw>>() {}.type

    /**
     * 判断单源是否需要 AI 增强
     *
     * 条件：AI 已开启 + 非逐字 + 非纯音乐 + API 配置有效
     *   + (Always 模式 或 缺少翻译)
     */
    suspend fun shouldEnhance(
        lyricData: LyricData,
        sources: List<LyricSourceData>
    ): Boolean {
        val mode = try {
            AiTriggerMode.valueOf(
                context.dataStore.data.first()[AiTriggerModeKey] ?: AiTriggerMode.Off.name
            )
        } catch (_: Exception) {
            AiTriggerMode.Off
        }

        if (mode == AiTriggerMode.Off) { Timber.tag(TAG).d("shouldEnhance → false: Off"); return false }
        if (lyricData.isVerbatim) { Timber.tag(TAG).d("shouldEnhance → false: isVerbatim"); return false }
        if (lyricData.isPureMusic) { Timber.tag(TAG).d("shouldEnhance → false: isPureMusic"); return false }

        val baseUrl = context.dataStore.data.first()[AiBaseUrlKey] ?: ""
        val apiKey = context.dataStore.data.first()[AiApiKeyKey] ?: ""
        if (baseUrl.isBlank() || apiKey.isBlank()) { Timber.tag(TAG).d("shouldEnhance → false: config"); return false }

        if (mode == AiTriggerMode.Always) { Timber.tag(TAG).d("shouldEnhance → true: Always"); return true }

        val hasTranslation = when (val source = sources.firstOrNull()) {
            is LyricSourceData.NetEase -> !source.lyric.tlyric?.lyric.isNullOrBlank() || !source.lyric.ytlrc?.lyric.isNullOrBlank()
            is LyricSourceData.QQMusic -> !source.lyric.trans.isNullOrBlank()
            is LyricSourceData.AM -> true
            else -> false
        }

        val result = !hasTranslation
        Timber.tag(TAG).d("shouldEnhance → $result: mode=$mode hasTranslation=$hasTranslation")
        return result
    }

    /**
     * 单源 AI 增强
     *
     * 仅网易云/QQ 单源可用时调用。
     * 流程：本地检测对唱 → AI 对唱(如需要) → 本地检测翻译 → AI 翻译(如需要)
     */
    suspend fun singleEnhance(
        sources: List<LyricSourceData>,
        metadata: MediaMetadata
    ): LyricData? {
        val baseUrl = context.dataStore.data.first()[AiBaseUrlKey] ?: run { Timber.tag(TAG).w("singleEnhance → null: baseUrl"); return null }
        val apiKey = context.dataStore.data.first()[AiApiKeyKey] ?: run { Timber.tag(TAG).w("singleEnhance → null: apiKey"); return null }
        val model = context.dataStore.data.first()[AiModelKey] ?: run { Timber.tag(TAG).w("singleEnhance → null: model"); return null }
        if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) { Timber.tag(TAG).w("singleEnhance → null: blank config"); return null }

        Timber.tag(TAG).d("singleEnhance: song=${metadata.title}, source=${sources.firstOrNull()?.source}")

        return when (val source = sources.firstOrNull()) {
            is LyricSourceData.NetEase -> {
                val lrc = source.lyric.lrc.lyric.takeIf { it.isNotBlank() }
                if (lrc == null) { Timber.tag(TAG).w("singleEnhance → null: lrc blank"); return null }
                val yrc = source.lyric.yrc?.lyric?.takeIf { it.isNotBlank() }
                val translation = source.lyric.tlyric?.lyric?.takeIf { it.isNotBlank() }

                val duet = if (isDuetLikely(lrc)) {
                    taskDetectDuet(lrc, baseUrl, apiKey, model)
                } else null

                if (yrc != null) {
                    // 有逐字 → 逐字输出，翻译不足时 AI 补全
                    val finalTranslation = if (isTranslationSerious(lrc, translation)) {
                        taskTranslate(lrc, duet, metadata, baseUrl, apiKey, model)?.tlyric
                    } else translation

                    val lyrics = YRCParser.parse(yrc, finalTranslation ?: "")
                    LyricData(
                        isVerbatim = true,
                        isPureMusic = false,
                        source = LyricSource.AIEnhanced,
                        lyricLine = applyDuetAlignment(lyrics, duet, lrc)
                    )
                } else {
                    // 无逐字 → 逐行输出 + 翻译 + 对唱 alignment
                    val finalTranslation = if (isTranslationSerious(lrc, translation)) {
                        taskTranslate(lrc, duet, metadata, baseUrl, apiKey, model)?.tlyric
                    } else translation

                    val lyrics = LRCParser.parse(lrc, finalTranslation)
                    LyricData(
                        isVerbatim = false,
                        isPureMusic = false,
                        source = LyricSource.AIEnhanced,
                        lyricLine = applyDuetAlignment(lyrics, duet)
                    )
                }
            }
            is LyricSourceData.QQMusic -> {
                val lrc = source.lrcContent?.takeIf { it.isNotBlank() }
                    ?: source.lyric.lyric.takeIf { it.isNotBlank() }
                if (lrc == null) { Timber.tag(TAG).w("singleEnhance → null: qq lrc blank"); return null }
                val isQrc = source.isQRC
                val translation = source.lyric.trans.takeIf { it.isNotBlank() }

                if (isQrc) {
                    // 逐字 → QRCParser
                    val finalTranslation = if (isTranslationSerious(lrc, translation)) {
                        taskTranslate(lrc, null, metadata, baseUrl, apiKey, model)?.tlyric
                    } else translation

                    val lyrics = QRCParser.parse(source.lyric.lyric, finalTranslation ?: source.lyric.trans ?: "")
                    LyricData(
                        isVerbatim = true,
                        isPureMusic = false,
                        source = LyricSource.AIEnhanced,
                        lyricLine = applyDuetAlignment(lyrics, null)
                    )
                } else {
                    // 逐行
                    if (translation.isNullOrBlank()) {
                        val result = taskTranslate(lrc, null, metadata, baseUrl, apiKey, model) ?: return null
                        LyricData(
                            isVerbatim = false, isPureMusic = false,
                            source = LyricSource.AIEnhanced,
                            lyricLine = LRCParser.parse(result.lrc ?: "", result.tlyric)
                        )
                    } else {
                        Timber.tag(TAG).d("singleEnhance: QQ has translation, skip")
                        null
                    }
                }
            }
            else -> { Timber.tag(TAG).w("singleEnhance → null: unknown"); null }
        }
    }

    /**
     * 双源智能合并
     *
     * 流程：
     * 1. [determineWinnerLocal] 本地判胜
     * 2. 若 netease 有逐行 → [isDuetLikely] 本地检测 → AI 对唱识别
     * 3. 若 winner 缺翻译 → AI 翻译补全
     * 4. 逐字输出或逐行输出（逐字不降级到逐行）
     */
    suspend fun smartMerge(
        netease: LyricSourceData.NetEase,
        qq: LyricSourceData.QQMusic,
        metadata: MediaMetadata
    ): LyricData? {
        val baseUrl = context.dataStore.data.first()[AiBaseUrlKey] ?: run { Timber.tag(TAG).w("smartMerge → null: baseUrl"); return null }
        val apiKey = context.dataStore.data.first()[AiApiKeyKey] ?: run { Timber.tag(TAG).w("smartMerge → null: apiKey"); return null }
        val model = context.dataStore.data.first()[AiModelKey] ?: run { Timber.tag(TAG).w("smartMerge → null: model"); return null }
        if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) { Timber.tag(TAG).w("smartMerge → null: blank config"); return null }

        Timber.tag(TAG).d("smartMerge: song=${metadata.title}")

        // 提取逐字和逐行
        val netVerbatim = netease.lyric.yrc?.lyric?.takeIf { it.isNotBlank() }
        val qqVerbatim = qq.lyric.lyric.takeIf { qq.lyric.qrcT != 0 && it.isNotBlank() }
        val netLine = netease.lyric.lrc.lyric.takeIf { it.isNotBlank() }
        val qqLine = qq.lrcContent?.takeIf { it.isNotBlank() }
            ?: qq.lyric.lyric.takeIf { it.isNotBlank() }

        val netTranslation = netease.lyric.tlyric?.lyric?.takeIf { it.isNotBlank() }
        val qqTranslation = qq.lyric.trans.takeIf { it.isNotBlank() }

        Timber.tag(TAG).d("smartMerge: netVerb=${netVerbatim != null} qqVerb=${qqVerbatim != null} netLine=${netLine != null} qqLine=${qqLine != null}")
        Timber.tag(TAG).d("smartMerge: netTrans=${netTranslation != null} qqTrans=${qqTranslation != null}")

        // ===== Step 1: 本地判胜 =====
        val winner = determineWinnerLocal(netVerbatim, qqVerbatim, netLine, qqLine)
        if (winner == null) {
            Timber.tag(TAG).w("smartMerge → null: no lyrics")
            return null
        }
        Timber.tag(TAG).d("smartMerge: winner=$winner")

        // ===== Step 2: 对唱检测 =====
        val duet = if (netLine != null && isDuetLikely(netLine)) {
            taskDetectDuet(netLine, baseUrl, apiKey, model)
        } else null
        Timber.tag(TAG).d("smartMerge: duet=$duet")

        // ===== Step 3 + 4: 翻译 + 输出 =====
        return when (winner) {
            "netease" -> {
                if (netVerbatim != null) {
                    // 逐字输出
                    val finalTrans = if (isTranslationSerious(netLine ?: "", netTranslation)) {
                        val otherTrans = qqTranslation
                        val srcLrc = netLine ?: ""
                        if (otherTrans != null) {
                            taskMergeTranslation(srcLrc, otherTrans, duet, metadata, baseUrl, apiKey, model)?.tlyric
                        } else {
                            taskTranslate(srcLrc, duet, metadata, baseUrl, apiKey, model)?.tlyric
                        }
                    } else netTranslation

                    Timber.tag(TAG).d("smartMerge: netease verbatim output, duet=${duet?.size}")
                    val lyrics = YRCParser.parse(netVerbatim, finalTrans ?: "")
                    LyricData(
                        isVerbatim = true,
                        isPureMusic = false,
                        source = LyricSource.AIEnhanced,
                        lyricLine = applyDuetAlignment(lyrics, duet, netLine)
                    )
                } else if (netLine != null) {
                    // 逐行输出
                    val finalTrans = if (isTranslationSerious(netLine, netTranslation)) {
                        val otherTrans = qqTranslation
                        if (otherTrans != null) {
                            taskMergeTranslation(netLine, otherTrans, duet, metadata, baseUrl, apiKey, model)?.tlyric
                        } else {
                            taskTranslate(netLine, duet, metadata, baseUrl, apiKey, model)?.tlyric
                        }
                    } else netTranslation

                    Timber.tag(TAG).d("smartMerge: netease line output, duet=${duet?.size}")
                    val lyrics = LRCParser.parse(netLine, finalTrans)
                    LyricData(
                        isVerbatim = false,
                        isPureMusic = false,
                        source = LyricSource.AIEnhanced,
                        lyricLine = applyDuetAlignment(lyrics, duet)
                    )
                } else null
            }
            "qq" -> {
                if (qqVerbatim != null) {
                    // 逐字输出
                    val finalTrans = if (isTranslationSerious(qqLine ?: "", qqTranslation)) {
                        val otherTrans = netTranslation
                        val srcLrc = qqLine ?: ""
                        if (otherTrans != null) {
                            taskMergeTranslation(srcLrc, otherTrans, duet, metadata, baseUrl, apiKey, model)?.tlyric
                        } else {
                            taskTranslate(srcLrc, duet, metadata, baseUrl, apiKey, model)?.tlyric
                        }
                    } else qqTranslation

                    val lyrics = QRCParser.parse(qqVerbatim, finalTrans ?: "")
                    LyricData(
                        isVerbatim = true,
                        isPureMusic = false,
                        source = LyricSource.AIEnhanced,
                        lyricLine = applyDuetAlignment(lyrics, duet, qqLine)
                    )
                } else if (qqLine != null) {
                    // 逐行输出
                    val finalTrans = if (isTranslationSerious(qqLine, qqTranslation)) {
                        val otherTrans = netTranslation
                        if (otherTrans != null) {
                            taskMergeTranslation(qqLine, otherTrans, duet, metadata, baseUrl, apiKey, model)?.tlyric
                        } else {
                            taskTranslate(qqLine, duet, metadata, baseUrl, apiKey, model)?.tlyric
                        }
                    } else qqTranslation

                    val lyrics = LRCParser.parse(qqLine, finalTrans)
                    LyricData(
                        isVerbatim = false,
                        isPureMusic = false,
                        source = LyricSource.AIEnhanced,
                        lyricLine = applyDuetAlignment(lyrics, duet)
                    )
                } else null
            }
            else -> null
        }
    }

    // ==================== 本地判断 ====================

    /**
     * 本地判胜（纯规则，0 次 AI 调用）
     *
     * 优先级：逐字 > 逐行，同级别 NetEase 优先
     *
     * @return "netease" | "qq" | null (双方都无歌词)
     */
    private fun determineWinnerLocal(
        netVerbatim: String?,
        qqVerbatim: String?,
        netLine: String?,
        qqLine: String?
    ): String? {
        val hasNetVerb = netVerbatim != null
        val hasQqVerb = qqVerbatim != null

        if (hasNetVerb || hasQqVerb) {
            val winner = if (hasNetVerb) "netease" else "qq"
            Timber.tag(TAG).d("determineWinnerLocal → $winner (verbatim: net=$hasNetVerb qq=$hasQqVerb)")
            return winner
        }

        val hasNetLine = netLine != null
        val hasQqLine = qqLine != null

        if (hasNetLine || hasQqLine) {
            val winner = when {
                hasNetLine && hasQqLine -> "netease"
                hasNetLine -> "netease"
                else -> "qq"
            }
            Timber.tag(TAG).d("determineWinnerLocal → $winner (line-level: net=$hasNetLine qq=$hasQqLine)")
            return winner
        }

        Timber.tag(TAG).w("determineWinnerLocal → null: no lyrics")
        return null
    }

    /**
     * 本地检测 LRC 中是否可能存在对唱
     *
     * 检测三种格式：
     * 1. 独立角色标记行 "Name:" → 下一行歌词时间接近
     * 2. 嵌入式 【Name】lyrics（如 【クロハ】これが運命）
     * 3. 嵌入式 Name：lyrics（如 マリー：痛いくらいに）
     *
     * ≥ 2 个不同角色 → 可能对唱
     */
    private fun isDuetLikely(lrc: String): Boolean {
        // 检测独立角色标记行
        if (hasStandaloneRoles(lrc)) return true
        // 检测嵌入式角色前缀
        if (hasRolePrefixes(lrc)) return true
        return false
    }

    /** 独立行 "Name:" 格式检测 */
    private fun hasStandaloneRoles(lrc: String): Boolean {
        val lines = lrc.lines()
        val markRegex = Regex("""^\[(\d+):(\d+(?:\.\d+)?)\]\s*([A-Za-z0-9]+)[:：]\s*${'$'}""")
        val roles = mutableSetOf<String>()

        for (i in lines.indices) {
            val match = markRegex.find(lines[i].trim()) ?: continue
            val role = match.groupValues[3]
            val tsMs = match.groupValues[1].toInt() * 60 * 1000 + (match.groupValues[2].toDouble() * 1000).toLong()
            if (i + 1 < lines.size) {
                val nextMatch = Regex("""^\[(\d+):(\d+(?:\.\d+)?)\]""").find(lines[i + 1].trim())
                if (nextMatch != null) {
                    val nextTs = nextMatch.groupValues[1].toInt() * 60 * 1000 + (nextMatch.groupValues[2].toDouble() * 1000).toLong()
                    if (nextTs - tsMs <= 1000) roles.add(role.lowercase())
                }
            }
        }

        val result = roles.size >= 2
        Timber.tag(TAG).d("isDuetLikely(standalone) → $result (roles=$roles)")
        return result
    }

    /**
     * 检测嵌入式角色前缀：{@code 【Name】lyrics} 或 {@code Name：lyrics}
     *
     * 直接扫描逐行歌词，提取时间戳后紧跟的角色名。
     * [03:03.930]【クロハ】これが運命  →  クロハ
     * [03:28.650]マリー：痛いくらいに →  マリー
     */
    private fun hasRolePrefixes(lrc: String): Boolean {
        val roleRegex = Regex("""^\[\d+:\d+(?:\.\d+)?\](?:【([^】]+)】|(\S{1,8})[：:])\s*""")
        val roles = mutableSetOf<String>()

        for (line in lrc.lines()) {
            val match = roleRegex.find(line.trim()) ?: continue
            val role = (match.groupValues[1].ifBlank { match.groupValues[2] }).trim()
            if (role.isNotBlank()) roles.add(role.lowercase())
        }

        val result = roles.size >= 2
        Timber.tag(TAG).d("isDuetLikely(rolePrefixes) → $result (roles=$roles)")
        return result
    }

    /**
     * 判断翻译是否严重不足
     *
     * 条件：翻译为空 或 翻译行数 < LRC 行数的 50%
     */
    private fun isTranslationSerious(lrc: String, translation: String?): Boolean {
        if (translation.isNullOrBlank()) return true
        val lrcCount = countLrcLines(lrc)
        val transCount = countLrcLines(translation)
        val result = transCount < lrcCount * 0.5
        Timber.tag(TAG).d("isTranslationSerious → $result (lrcLines=$lrcCount transLines=$transCount)")
        return result
    }

    /** 统计 LRC 中有效歌词行数（过滤空行和元数据行） */
    private fun countLrcLines(text: String): Int {
        return text.lines().count { line ->
            val trimmed = line.trim()
            trimmed.matches(Regex("""^\[\d+:\d+(\.\d+)?\].+"""))
        }
    }

    // ==================== AI Tasks ====================

    /** Task2: AI 对唱识别 */
    private suspend fun taskDetectDuet(
        netLrc: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): List<DuetSegment>? {
        val systemPrompt = buildString {
            append("分析以下LRC歌词，识别是否有对唱/合唱部分。\n")
            append("如果有对唱，返回JSON数组，每个元素包含 role（男/女/合或其他角色名）、start（起始行号,0-based）、end（结束行号,不包含）。\n")
            append("如果没有对唱，返回空数组 []。\n")
            append("只返回JSON数组，不要任何其他内容。\n")
            append("示例：[{\"role\":\"男\",\"start\":0,\"end\":4}, {\"role\":\"女\",\"start\":4,\"end\":8}]")
        }

        return try {
            val json = withContext(Dispatchers.IO) {
                client.chat(baseUrl, apiKey, model, systemPrompt, netLrc)
            }?.trim()
            if (json == null || json == "[]" || json.isBlank()) {
                Timber.tag(TAG).d("taskDetectDuet → null: ${if (json == null) "no response" else "empty"}")
                return null
            }

            val rawList: List<DuetRaw> = gson.fromJson(json, duetListType)
            if (rawList.isEmpty()) { Timber.tag(TAG).d("taskDetectDuet → null: parsed empty"); return null }

            val result = rawList.map { DuetSegment(it.role, it.start, it.end) }
            Timber.tag(TAG).d("taskDetectDuet → $result")
            result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "taskDetectDuet failed")
            null
        }
    }

    /** Task3: AI 合并翻译（winner 逐行 + other 翻译） */
    private suspend fun taskMergeTranslation(
        baseLrc: String,
        otherTranslation: String,
        duet: List<DuetSegment>?,
        metadata: MediaMetadata,
        baseUrl: String,
        apiKey: String,
        model: String
    ): AiLyricClient.AiLyricResultRaw? {
        val duetHint = duet?.let { buildDuetHint(it) } ?: ""
        val userMessage = buildString {
            append("歌曲：${metadata.title}")
            if (metadata.artists.isNotEmpty()) append(" - ${metadata.artists.joinToString(",") { it.name }}")
            append("\n\n[逐字歌词]\n$baseLrc")
            append("\n\n[翻译]\n$otherTranslation")
            if (duetHint.isNotBlank()) append("\n\n[对唱标注]\n$duetHint")
            append("\n\n请将翻译整合到逐字歌词上。")
        }

        val systemPrompt = buildString {
            append("将以下翻译对应到逐字歌词的每一行。\n")
            append("lrc 字段保留完整的逐字歌词。\n")
            if (duet != null && duet.isNotEmpty()) {
                for (d in duet) append("第${d.startLine}-${d.endLine - 1}行为${d.role}；")
                append("\n")
            }
            append("tlyric 字段包含对应的英文翻译，与原LRC匹配时间戳。\n\n")
            append("返回严格的JSON：\n{\"lrc\":\"...\", \"tlyric\":\"...\"}")
        }

        Timber.tag(TAG).d("taskMergeTranslation start")
        val result = parseLyricResult(baseUrl, apiKey, model, systemPrompt, userMessage)
        if (result == null) Timber.tag(TAG).w("taskMergeTranslation → null")
        return result
    }

    /** Task3: AI 翻译（无现有翻译） */
    private suspend fun taskTranslate(
        baseLrc: String,
        duet: List<DuetSegment>?,
        metadata: MediaMetadata,
        baseUrl: String,
        apiKey: String,
        model: String
    ): AiLyricClient.AiLyricResultRaw? {
        val duetHint = duet?.let { buildDuetHint(it) } ?: ""
        val userMessage = buildString {
            append("歌曲：${metadata.title}")
            if (metadata.artists.isNotEmpty()) append(" - ${metadata.artists.joinToString(",") { it.name }}")
            append("\n\n$baseLrc")
            if (duetHint.isNotBlank()) append("\n\n[对唱标注]\n$duetHint")
        }

        val systemPrompt = buildString {
            append("为以下LRC歌词生成英文翻译。\n")
            append("lrc 字段保留完整原始歌词。\n")
            if (duet != null && duet.isNotEmpty()) {
                for (d in duet) append("第${d.startLine}-${d.endLine - 1}行为${d.role}；")
                append("\n")
            }
            append("tlyric 字段包含逐行英文翻译，与原文时间戳匹配。\n\n")
            append("返回严格的JSON：\n{\"lrc\":\"...\", \"tlyric\":\"...\"}")
        }

        Timber.tag(TAG).d("taskTranslate start")
        val result = parseLyricResult(baseUrl, apiKey, model, systemPrompt, userMessage)
        if (result == null) Timber.tag(TAG).w("taskTranslate → null")
        return result
    }

    /** 调用 AI 并解析 {lrc, tlyric} JSON 响应 */
    private suspend fun parseLyricResult(
        baseUrl: String, apiKey: String, model: String,
        systemPrompt: String, userMessage: String
    ): AiLyricClient.AiLyricResultRaw? {
        return try {
            val json = withContext(Dispatchers.IO) {
                client.chat(baseUrl, apiKey, model, systemPrompt, userMessage)
            } ?: run { Timber.tag(TAG).w("parseLyricResult → null: no response"); return null }
            val result = gson.fromJson(json, AiLyricClient.AiLyricResultRaw::class.java)
            Timber.tag(TAG).d("parseLyricResult: lrc=${result.lrc?.take(80)}... tlyric=${result.tlyric?.take(80)}...")
            result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "parseLyricResult failed")
            null
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 将对唱标注应用到已解析的 SyncedLyrics
     *
     * 根据 AI 识别的 duet segments，在对应行设置 KaraokeAlignment。
     * 同一角色保持相同 alignment（Start/End 交替）。
     *
     * @param lrcForTimestamps 可选，LRC 逐行文本，用于将 AI 行号映射到解析后行的时间戳。
     *                         传入时按时间范围匹配；不传时按 line index 直接匹配。
     */
    private fun applyDuetAlignment(
        lyrics: SyncedLyrics,
        duet: List<DuetSegment>?,
        lrcForTimestamps: String? = null
    ): SyncedLyrics {
        if (duet.isNullOrEmpty()) return lyrics
        val roleAlign = mutableMapOf<String, KaraokeAlignment>()
        var lastAlign = KaraokeAlignment.End

        // 先清除所有已有 alignment
        val cleared = lyrics.lines.map { line ->
            if (line is KaraokeLine.MainKaraokeLine) line.copy(alignment = KaraokeAlignment.Unspecified) else line
        }

        // 如果有 LRC 文本，按时间戳匹配
        val timeRanges: List<Pair<Long, Long>>? = lrcForTimestamps?.let { lrc ->
            val lrcLines = lrc.lines()
            duet.map { seg ->
                val startTs = extractTimestampMs(lrcLines.getOrNull(seg.startLine) ?: "") ?: 0L
                val endTs = extractTimestampMs(lrcLines.getOrNull(seg.endLine - 1) ?: "") ?: startTs + 3000L
                startTs to endTs
            }
        }

        val newLines = cleared.mapIndexed { idx, line ->
            val fallbackSeg = duet.firstOrNull { idx in it.startLine until it.endLine }
            val seg = if (timeRanges != null) {
                // 按时间范围找匹配的 segment
                val start = (line as? KaraokeLine)?.start?.toLong() ?: return@mapIndexed line
                duet.zip(timeRanges).firstOrNull { (_, range) ->
                    start in range.first..range.second
                }?.first ?: return@mapIndexed line
            } else {
                fallbackSeg ?: return@mapIndexed line
            }
            if (line !is KaraokeLine.MainKaraokeLine) return@mapIndexed line
            val name = seg.role.lowercase()
            val align = roleAlign.getOrPut(name) {
                lastAlign = if (lastAlign == KaraokeAlignment.Start) KaraokeAlignment.End else KaraokeAlignment.Start
                lastAlign
            }
            line.copy(alignment = align)
        }

        // 移除独占一行的角色标记（如 "mizuki:"、"All:"），对齐已由 alignment 体现
        val roleMarkerRegex = Regex("""^[A-Za-z0-9]+[:：]\s*$""")
        val filtered = newLines.filter { line ->
            if (line is KaraokeLine.MainKaraokeLine) {
                val text = line.syllables.joinToString("") { it.content }.trim()
                !roleMarkerRegex.matches(text)
            } else true
        }

        Timber.tag(TAG).d("applyDuetAlignment: roles=${roleAlign.size}, lines=${newLines.size}, filtered=${newLines.size - filtered.size}")
        return SyncedLyrics(lines = filtered)
    }

    private fun buildDuetHint(duet: List<DuetSegment>): String {
        return duet.joinToString("\n") { "第${it.startLine}-${it.endLine - 1}行: ${it.role}" }
    }

    /** 从 LRC 行提取毫秒时间戳 */
    private fun extractTimestampMs(line: String): Long? {
        val match = Regex("""^\[(\d+):(\d+(?:\.\d+)?)\]""").find(line.trim()) ?: return null
        val min = match.groupValues[1].toInt()
        val sec = match.groupValues[2].toDouble()
        return (min * 60 * 1000 + sec * 1000).toLong()
    }

    companion object {
        private const val TAG = "AiLyricProcessor"
    }
}
