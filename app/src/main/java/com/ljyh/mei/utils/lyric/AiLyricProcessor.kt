package com.ljyh.mei.utils.lyric

import android.content.Context
import com.google.gson.Gson
import com.ljyh.mei.constants.AiBaseUrlKey
import com.ljyh.mei.constants.AiApiKeyKey
import com.ljyh.mei.constants.AiModelKey
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.network.AiLyricClient
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.utils.dataStore
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

    /**
     * 纯本地双源合并（无 AI）
     *
     * 流程：判胜 → 本地对唱检测+对齐 → 输出
     */
    fun mergeWithDuet(
        netease: LyricSourceData.NetEase,
        qq: LyricSourceData.QQMusic
    ): LyricData? {
        val netVerbatim = netease.lyric.yrc?.lyric?.takeIf { it.isNotBlank() }
        val qqVerbatim = qq.lyric.lyric.takeIf { qq.lyric.qrcT != 0 && it.isNotBlank() }
        val netLine = netease.lyric.lrc.lyric.takeIf { it.isNotBlank() }
        val qqLine = qq.lrcContent?.takeIf { it.isNotBlank() }
            ?: qq.lyric.lyric.takeIf { it.isNotBlank() }

        val netTranslation = netease.lyric.tlyric?.lyric?.takeIf { it.isNotBlank() }
        val qqTranslation = qq.lyric.trans.takeIf { it.isNotBlank() }

        Timber.tag(TAG).d("mergeWithDuet: netVerb=${netVerbatim != null} qqVerb=${qqVerbatim != null} netLine=${netLine != null} qqLine=${qqLine != null}")

        val winner = determineWinnerLocal(netVerbatim, qqVerbatim, netLine, qqLine)
            ?: return null
        Timber.tag(TAG).d("mergeWithDuet: winner=$winner")

        val duet = if (netLine != null && isDuetLikely(netLine)) {
            detectDuetLocally(netLine)
        } else null
        Timber.tag(TAG).d("mergeWithDuet: duet=$duet")

        return when (winner) {
            "netease" -> buildOutput(netVerbatim, netLine, netTranslation, duet, LyricSource.NetEaseCloudMusic)
            "qq" -> buildOutput(qqVerbatim, qqLine, qqTranslation, duet, LyricSource.QQMusic)
            else -> null
        }
    }

    /**
     * 单源本地对唱检测+对齐（仅网易云）
     */
    fun singleDuet(source: LyricSourceData.NetEase): LyricData? {
        val verbatim = source.lyric.yrc?.lyric?.takeIf { it.isNotBlank() }
        val line = source.lyric.lrc.lyric.takeIf { it.isNotBlank() } ?: return null
        val translation = source.lyric.tlyric?.lyric?.takeIf { it.isNotBlank() }

        val duet = if (isDuetLikely(line)) detectDuetLocally(line) else null
        Timber.tag(TAG).d("singleDuet: duet=${duet?.size}")
        return buildOutput(verbatim, line, translation, duet, LyricSource.NetEaseCloudMusic)
    }

    private fun buildOutput(
        verbatim: String?,
        line: String?,
        translation: String?,
        duet: List<DuetSegment>?,
        source: LyricSource
    ): LyricData? {
        if (verbatim != null) {
            val lyrics = YRCParser.parse(verbatim, translation ?: "")
            return LyricData(
                isVerbatim = true, isPureMusic = false,
                source = source,
                lyricLine = if (duet != null) applyDuetAlignment(lyrics, duet, line) else lyrics
            )
        }
        if (line != null) {
            val lyrics = LRCParser.parse(line, translation)
            return LyricData(
                isVerbatim = false, isPureMusic = false,
                source = source,
                lyricLine = if (duet != null) applyDuetAlignment(lyrics, duet) else lyrics
            )
        }
        return null
    }

    /**
     * 手动触发 AI 翻译
     *
     * 仅当双方都无翻译时由 UI 触发。
     * 返回 AIEnhanced 的 LyricData，调用方决定是否持久化。
     */
    suspend fun manualTranslate(
        sources: List<LyricSourceData>,
        metadata: MediaMetadata
    ): LyricData? {
        val baseUrl = context.dataStore.data.first()[AiBaseUrlKey] ?: run {
            Timber.tag(TAG).w("manualTranslate → null: baseUrl"); return null
        }
        val apiKey = context.dataStore.data.first()[AiApiKeyKey] ?: run {
            Timber.tag(TAG).w("manualTranslate → null: apiKey"); return null
        }
        val model = context.dataStore.data.first()[AiModelKey] ?: run {
            Timber.tag(TAG).w("manualTranslate → null: model"); return null
        }
        if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) return null

        return when (val source = sources.firstOrNull()) {
            is LyricSourceData.NetEase -> {
                val lrc = source.lyric.lrc.lyric.takeIf { it.isNotBlank() } ?: return null
                val yrc = source.lyric.yrc?.lyric?.takeIf { it.isNotBlank() }
                val duet = if (isDuetLikely(lrc)) detectDuetLocally(lrc) else null
                val result = taskTranslate(lrc, duet, metadata, baseUrl, apiKey, model) ?: return null

                if (yrc != null) {
                    val lyrics = YRCParser.parse(yrc, result.tlyric ?: "")
                    LyricData(isVerbatim = true, isPureMusic = false, source = LyricSource.AIEnhanced,
                        lyricLine = applyDuetAlignment(lyrics, duet, lrc))
                } else {
                    val lyrics = LRCParser.parse(result.lrc ?: "", result.tlyric)
                    LyricData(isVerbatim = false, isPureMusic = false, source = LyricSource.AIEnhanced,
                        lyricLine = applyDuetAlignment(lyrics, duet))
                }
            }
            is LyricSourceData.QQMusic -> {
                val lrc = source.lrcContent?.takeIf { it.isNotBlank() }
                    ?: source.lyric.lyric.takeIf { it.isNotBlank() } ?: return null
                val isQrc = source.isQRC
                val result = taskTranslate(lrc, null, metadata, baseUrl, apiKey, model) ?: return null

                if (isQrc) {
                    val lyrics = QRCParser.parse(source.lyric.lyric, result.tlyric ?: "")
                    LyricData(isVerbatim = true, isPureMusic = false, source = LyricSource.AIEnhanced,
                        lyricLine = applyDuetAlignment(lyrics, null))
                } else {
                    val lyrics = LRCParser.parse(result.lrc ?: "", result.tlyric)
                    LyricData(isVerbatim = false, isPureMusic = false, source = LyricSource.AIEnhanced,
                        lyricLine = applyDuetAlignment(lyrics, null))
                }
            }
            else -> null
        }
    }

    // ==================== 本地判胜 ====================

    private fun determineWinnerLocal(
        netVerbatim: String?,
        qqVerbatim: String?,
        netLine: String?,
        qqLine: String?
    ): String? {
        val hasNetVerb = netVerbatim != null
        val hasQqVerb = qqVerbatim != null
        if (hasNetVerb || hasQqVerb) return if (hasNetVerb) "netease" else "qq"
        val hasNetLine = netLine != null
        val hasQqLine = qqLine != null
        if (hasNetLine || hasQqLine) return if (hasNetLine) "netease" else "qq"
        return null
    }

    // ==================== 本地对唱检测 ====================

    fun isDuetLikely(lrc: String): Boolean {
        return hasStandaloneRoles(lrc) || hasRolePrefixes(lrc)
    }

    fun detectDuetLocally(lrc: String): List<DuetSegment>? {
        if (hasStandaloneRoles(lrc)) {
            val result = detectStandaloneSegments(lrc)
            if (result != null) return result
        }
        if (hasRolePrefixes(lrc)) {
            val result = detectPrefixSegments(lrc)
            if (result != null) return result
        }
        return null
    }

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
        return roles.size >= 2
    }

    private fun hasRolePrefixes(lrc: String): Boolean {
        val roleRegex = Regex("""^\[\d+:\d+(?:\.\d+)?\](?:【([^】]+)】|(\S{1,8})[：:])\s*""")
        val roles = mutableSetOf<String>()
        for (line in lrc.lines()) {
            val match = roleRegex.find(line.trim()) ?: continue
            val role = (match.groupValues[1].ifBlank { match.groupValues[2] }).trim()
            if (role.isNotBlank()) roles.add(role.lowercase())
        }
        return roles.size >= 2
    }

    /** 从独立 Name: 标记行提取对唱 segment */
    private fun detectStandaloneSegments(lrc: String): List<DuetSegment>? {
        val lines = lrc.lines()
        val markRegex = Regex("""^\[(\d+):(\d+(?:\.\d+)?)\]\s*([A-Za-z0-9]+)[:：]\s*${'$'}""")
        val wordCharRegex = Regex("""[A-Za-z0-9]""")
        val segments = mutableListOf<DuetSegment>()
        var currentRole: String? = null
        var currentStart = -1

        for (i in lines.indices) {
            val line = lines[i].trim()
            val markMatch = markRegex.find(line)
            if (markMatch != null) {
                val role = markMatch.groupValues[3]
                val tsMs = markMatch.groupValues[1].toInt() * 60 * 1000 + (markMatch.groupValues[2].toDouble() * 1000).toLong()
                val nextIsLyric = i + 1 < lines.size &&
                    Regex("""^\[(\d+):(\d+(?:\.\d+)?)\]""").find(lines[i + 1].trim())?.let {
                        val nt = it.groupValues[1].toInt() * 60 * 1000 + (it.groupValues[2].toDouble() * 1000).toLong()
                        nt - tsMs in 1..1000
                    } ?: false

                if (nextIsLyric && role.length in 1..15 && wordCharRegex.containsMatchIn(role)) {
                    if (currentRole != null && currentStart >= 0) {
                        segments.add(DuetSegment(currentRole, currentStart, i))
                    }
                    currentRole = role
                    currentStart = i + 1
                }
            }
        }
        if (currentRole != null && currentStart >= 0) {
            segments.add(DuetSegment(currentRole, currentStart, lines.size))
        }
        return if (segments.size >= 2) segments else null
    }

    /** 从嵌入式 【Name】/Name： 提取对唱 segment */
    private fun detectPrefixSegments(lrc: String): List<DuetSegment>? {
        val roleRegex = Regex("""^\[\d+:\d+(?:\.\d+)?\](?:【([^】]+)】|(\S{1,8})[：:])\s*""")
        val segments = mutableListOf<DuetSegment>()
        var currentRole: String? = null
        var currentStart = -1

        for ((i, line) in lrc.lines().withIndex()) {
            val match = roleRegex.find(line.trim())
            val role = match?.let {
                (it.groupValues[1].ifBlank { it.groupValues[2] }).trim().takeIf { r -> r.isNotBlank() }
            }
            if (role != null) {
                if (currentRole != null && currentRole != role.lowercase() && currentStart >= 0) {
                    segments.add(DuetSegment(currentRole, currentStart, i))
                }
                if (currentRole == null || currentRole != role.lowercase()) {
                    currentStart = i
                }
                currentRole = role.lowercase()
            }
        }
        if (currentRole != null && currentStart >= 0) {
            segments.add(DuetSegment(currentRole, currentStart, lrc.lines().size))
        }
        return if (segments.size >= 2) segments else null
    }

    // ==================== AI 翻译 ====================

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
        return parseLyricResult(baseUrl, apiKey, model, systemPrompt, userMessage)
    }

    private suspend fun parseLyricResult(
        baseUrl: String, apiKey: String, model: String,
        systemPrompt: String, userMessage: String
    ): AiLyricClient.AiLyricResultRaw? {
        return try {
            val json = withContext(Dispatchers.IO) {
                client.chat(baseUrl, apiKey, model, systemPrompt, userMessage)
            } ?: return null
            gson.fromJson(json, AiLyricClient.AiLyricResultRaw::class.java)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "parseLyricResult failed")
            null
        }
    }

    // ==================== 对唱对齐 ====================

    private fun applyDuetAlignment(
        lyrics: SyncedLyrics,
        duet: List<DuetSegment>?,
        lrcForTimestamps: String? = null
    ): SyncedLyrics {
        if (duet.isNullOrEmpty()) return lyrics
        val roleAlign = mutableMapOf<String, KaraokeAlignment>()
        var lastAlign = KaraokeAlignment.End

        val cleared = lyrics.lines.map { line ->
            if (line is KaraokeLine.MainKaraokeLine) line.copy(alignment = KaraokeAlignment.Unspecified) else line
        }

        val timeRanges: List<Pair<Long, Long>>? = lrcForTimestamps?.let { lrc ->
            val lrcLines = lrc.lines()
            duet.mapIndexed { i, seg ->
                val startTs = extractTimestampMs(lrcLines.getOrNull(seg.startLine) ?: "") ?: 0L
                val endTs = if (i + 1 < duet.size) {
                    extractTimestampMs(lrcLines.getOrNull(duet[i + 1].startLine) ?: "") ?: (startTs + 3000L)
                } else {
                    startTs + 3000L
                }
                startTs to endTs
            }
        }

        val newLines = cleared.mapIndexed { idx, line ->
            val fallbackSeg = duet.firstOrNull { idx in it.startLine until it.endLine }
            val seg = if (timeRanges != null) {
                val start = (line as? KaraokeLine)?.start?.toLong() ?: return@mapIndexed line
                duet.zip(timeRanges).firstOrNull { (_, range) -> start in range.first..range.second }?.first
                    ?: return@mapIndexed line
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

    // ==================== 工具方法 ====================

    private fun buildDuetHint(duet: List<DuetSegment>): String {
        return duet.joinToString("\n") { "第${it.startLine}-${it.endLine - 1}行: ${it.role}" }
    }

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
