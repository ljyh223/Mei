package com.ljyh.mei.utils.lyric

import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.model.LyricSourceData
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuetDetector @Inject constructor() {

    data class DuetSegment(
        val role: String,
        val startLine: Int,
        val endLine: Int
    )

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

        val winner = determineWinner(netVerbatim, qqVerbatim, netLine, qqLine) ?: return null

        val duet = if (netLine != null && isDuetLikely(netLine)) {
            detectDuetLocally(netLine)
        } else null

        return when (winner) {
            "netease" -> buildOutput(netVerbatim, netLine, netTranslation, duet, LyricSource.NetEaseCloudMusic)
            "qq" -> buildOutput(qqVerbatim, qqLine, qqTranslation, duet, LyricSource.QQMusic)
            else -> null
        }
    }

    fun singleDuet(source: LyricSourceData.NetEase): LyricData? {
        val verbatim = source.lyric.yrc?.lyric?.takeIf { it.isNotBlank() }
        val line = source.lyric.lrc.lyric.takeIf { it.isNotBlank() } ?: return null
        val translation = source.lyric.tlyric?.lyric?.takeIf { it.isNotBlank() }

        val duet = if (isDuetLikely(line)) detectDuetLocally(line) else null
        return buildOutput(verbatim, line, translation, duet, LyricSource.NetEaseCloudMusic)
    }

    fun isDuetLikely(lrc: String): Boolean {
        return hasStandaloneRoles(lrc) || hasRolePrefixes(lrc)
    }

    private fun detectDuetLocally(lrc: String): List<DuetSegment>? {
        if (hasStandaloneRoles(lrc)) {
            detectStandaloneSegments(lrc)?.let { return it }
        }
        if (hasRolePrefixes(lrc)) {
            detectPrefixSegments(lrc)?.let { return it }
        }
        return null
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

    private fun determineWinner(
        netVerbatim: String?, qqVerbatim: String?,
        netLine: String?, qqLine: String?
    ): String? {
        val hasNetVerb = netVerbatim != null
        val hasQqVerb = qqVerbatim != null
        if (hasNetVerb || hasQqVerb) return if (hasNetVerb) "netease" else "qq"
        val hasNetLine = netLine != null
        val hasQqLine = qqLine != null
        if (hasNetLine || hasQqLine) return if (hasNetLine) "netease" else "qq"
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

        val roleMarkerRegex = Regex("""^[A-Za-z0-9]+[:：]\s*$""")
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

        val filtered = newLines.filter { line ->
            if (line is KaraokeLine.MainKaraokeLine) {
                val text = line.syllables.joinToString("") { it.content }.trim()
                !roleMarkerRegex.matches(text)
            } else true
        }

        return SyncedLyrics(lines = filtered)
    }

    private fun extractTimestampMs(line: String): Long? {
        val match = Regex("""^\[(\d+):(\d+(?:\.\d+)?)\]""").find(line.trim()) ?: return null
        val min = match.groupValues[1].toInt()
        val sec = match.groupValues[2].toDouble()
        return (min * 60 * 1000 + sec * 1000).toLong()
    }

    companion object {
        private const val TAG = "DuetDetector"
    }
}
