package com.ljyh.mei.utils.lyric

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.parser.ILyricsParser

object YRCParser : ILyricsParser {

    private val YRC_LINE_REGEX = Regex("""^\[(\d+),(\d+)\](.*)$""")
    private val YRC_SYLLABLE_REGEX = Regex("""\((\d+),(\d+),\d+\)""")
    private val BG_LINE_REGEX = Regex("""^\[bg:(.*)\](.*)$""")
    private val translationLineRegex = "\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\].*".toRegex()
    private val JSON_VERBATIM_REGEX = Regex("""^\s*\{.*"t"\s*:\s*\d+.*"c"\s*:\s*\[.*""".trimMargin())

    fun parse(yrcLyrics: String, translationLrc: String?): SyncedLyrics {
        val karaokeLines = parseInternal(yrcLyrics.lineSequence())
        val mergedLines = TranslationHelper.merge(karaokeLines, translationLrc)
        return SyncedLyrics(lines = mergedLines)
    }

    override fun canParse(content: String): Boolean {
        val lineTimeRegex = """^\[\d+,\d+\]""".toRegex()
        val wordTimeRegex = """\(\d+,\d+,\d+\).{1}""".toRegex()

        return content.lineSequence()
            .map { it.trim() }
            .any { line ->
                lineTimeRegex.containsMatchIn(line) && wordTimeRegex.containsMatchIn(line)
            }
    }

    override fun parse(lines: List<String>): SyncedLyrics {
        val mainLyricsLines = lines.filter { line ->
            val trimmed = line.trim()
            YRC_LINE_REGEX.matches(trimmed) || trimmed.startsWith("[bg:")
        }
        val translationLines = lines.filter { translationLineRegex.matches(it.trim()) }

        return parse(
            yrcLyrics = mainLyricsLines.joinToString("\n"),
            translationLrc = translationLines.joinToString("\n").ifBlank { null }
        )
    }

    override fun parse(content: String): SyncedLyrics {
        return parse(content.lines())
    }

    private fun parseInternal(rawLinesSequence: Sequence<String>): List<KaraokeLine> {
        val resultLines = mutableListOf<KaraokeLine>()

        val roleState = RoleState()
        var lastLineStartTime = -1

        for (raw in rawLinesSequence) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("[bg:")) {
                parseBackgroundLine(line)?.let { bgLine ->
                    if (resultLines.isNotEmpty()) {
                        val last = resultLines.last()
                        if (last is KaraokeLine.MainKaraokeLine) {
                            resultLines[resultLines.size - 1] = last.copy(
                                accompanimentLines = (last.accompanimentLines
                                    ?: emptyList()) + bgLine
                            )
                        } else {
                            resultLines.add(bgLine)
                        }
                    } else {
                        resultLines.add(bgLine)
                    }
                }
                continue
            }

            val jsonLine = parseJsonVerbatimLine(line)
            if (jsonLine != null) {
                resultLines.add(jsonLine)
                continue
            }

            val match = YRC_LINE_REGEX.find(line) ?: continue

            var lineStart = match.groupValues[1].toInt()
            if (lastLineStartTime != -1 && lineStart <= lastLineStartTime) {
                lineStart = lastLineStartTime + 3
            }
            lastLineStartTime = lineStart

            val contentPart = match.groupValues[3]
            val rawSyllables = parseSyllablesAndMergeColons(contentPart, lineStart)

            val (alignment, finalSyllables) = determineRole(rawSyllables, roleState)

            if (finalSyllables.isNotEmpty()) {
                resultLines.add(
                    KaraokeLine.MainKaraokeLine(
                        syllables = finalSyllables,
                        translation = null,
                        alignment = alignment,
                        start = finalSyllables.first().start,
                        end = finalSyllables.last().end
                    )
                )
            }
        }

        return resultLines
    }

    private data class JsonVerbatimChar(val tx: String, val li: String?, val or: String?)

    private fun parseJsonVerbatimLine(line: String): KaraokeLine.MainKaraokeLine? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("{")) return null

        val tMatch = Regex(""""t"\s*:\s*(\d+)""").find(trimmed) ?: return null
        val lineStart = tMatch.groupValues[1].toIntOrNull() ?: return null

        val txPattern = Regex(""""tx"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val chars = mutableListOf<KaraokeSyllable>()
        var offset = 0

        for (m in txPattern.findAll(trimmed)) {
            val tx = m.groupValues[1]
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
            if (tx.isEmpty()) continue
            for (ch in tx) {
                chars.add(KaraokeSyllable(ch.toString(), lineStart + offset, lineStart + offset + 1))
                offset++
            }
        }

        if (chars.isEmpty()) return null

        val text = chars.joinToString("") { it.content }
        val duration = offset.toLong().coerceAtLeast(1)

        return KaraokeLine.MainKaraokeLine(
            syllables = chars,
            translation = null,
            alignment = KaraokeAlignment.Unspecified,
            start = lineStart,
            end = lineStart + duration.toInt()
        )
    }

    private fun parseBackgroundLine(line: String): KaraokeLine.AccompanimentKaraokeLine? {
        val m = BG_LINE_REGEX.find(line) ?: return null
        // 兼容可能存在的后续内容
        val content = m.groupValues[1] + m.groupValues[2]
        val syllables = parseSyllablesAndMergeColons(content, 0)
        if (syllables.isEmpty()) return null

        return KaraokeLine.AccompanimentKaraokeLine(
            syllables = syllables,
            translation = null,
            alignment = KaraokeAlignment.Unspecified,
            start = syllables.first().start,
            end = syllables.last().end
        )
    }

    /**
     * 参考Kugou的解析方式：先提取Token，再将冒号合并至前一个音节
     */
    private fun parseSyllablesAndMergeColons(
        content: String,
        baseStartTime: Int
    ): List<KaraokeSyllable> {
        data class TempToken(val offset: Int, val duration: Int, val text: String)

        val tokens = mutableListOf<TempToken>()
        var cursor = 0

        while (cursor < content.length) {
            val m = YRC_SYLLABLE_REGEX.find(content, cursor) ?: break
            val offset = m.groupValues[1].toIntOrNull() ?: 0
            val duration = m.groupValues[2].toIntOrNull() ?: 0

            val textStart = m.range.last + 1
            val nextMatch = YRC_SYLLABLE_REGEX.find(content, textStart)
            val textEnd = nextMatch?.range?.first ?: content.length

            if (textStart > textEnd) break

            val text = content.substring(textStart, textEnd)
            tokens.add(TempToken(offset, duration, text))
            cursor = textEnd
        }

        if (tokens.isEmpty()) return emptyList()

        val useAbsoluteTime = tokens.isNotEmpty() && tokens[0].offset >= baseStartTime

        val mergedSyllables = mutableListOf<KaraokeSyllable>()
        var i = 0
        while (i < tokens.size) {
            val current = tokens[i]
            val next = tokens.getOrNull(i + 1)

            val startTime = if (useAbsoluteTime) current.offset else baseStartTime + current.offset

            if (next != null && (next.text == "：" || next.text == ":")) {
                val nextStartTime = if (useAbsoluteTime) next.offset else baseStartTime + next.offset
                val e = nextStartTime + next.duration
                mergedSyllables.add(KaraokeSyllable(current.text + next.text, startTime, e))
                i += 2
            } else {
                val e = startTime + current.duration
                mergedSyllables.add(KaraokeSyllable(current.text, startTime, e))
                i++
            }
        }
        return mergedSyllables
    }

    /**
     * 对唱角色状态管理：记录每个角色名 -> 分配的 alignment，
     * 支持多角色（非二元 toggle），同一角色名多次出现复用相同 alignment。
     */
    private class RoleState {
        private val roleMap = mutableMapOf<String, KaraokeAlignment>()
        var lastAlignment = KaraokeAlignment.Start
            private set

        fun getOrAssign(roleName: String): KaraokeAlignment {
            return roleMap.getOrPut(roleName) {
                val alignment = lastAlignment
                lastAlignment = if (lastAlignment == KaraokeAlignment.Start)
                    KaraokeAlignment.End else KaraokeAlignment.Start
                alignment
            }
        }
    }

    /**
     * 检测当前行的对唱角色。
     *
     * 独立标记行：整行仅包含 "name：" 或 "name:" → 作为角色指示器显示，不剔除
     * 内嵌标记：  行首有 "name：lyrics" → 剔除 name：前缀，仅显示歌词部分
     */
    private fun determineRole(
        syllables: List<KaraokeSyllable>,
        roleState: RoleState
    ): Pair<KaraokeAlignment, List<KaraokeSyllable>> {
        if (syllables.isEmpty()) return Pair(KaraokeAlignment.Unspecified, syllables)

        val rawText = syllables.joinToString("") { it.content }

        val colonIdx = rawText.indexOfFirst { it == '：' || it == ':' }
        if (colonIdx < 0) return Pair(roleState.lastAlignment, syllables)

        val roleName = rawText.substring(0, colonIdx)
        if (roleName.length > 15) return Pair(roleState.lastAlignment, syllables)

        val alignment = roleState.getOrAssign(roleName)

        if (colonIdx + 1 >= rawText.length) {
            // 独立标记行 "mizuki：" → 完整保留
            return Pair(alignment, syllables)
        }

        // 内嵌标记 "女：歌词" → 剔除角色前缀音节
        val adjusted = stripRolePrefix(syllables, colonIdx + 1)
        return Pair(alignment, adjusted)
    }

    /**
     * 剔除内嵌角色标记的前缀音节。
     * 冒号合并后，首个音节形如 "女：" —— 直接 drop，仅保留后续歌词音节。
     */
    private fun stripRolePrefix(
        syllables: List<KaraokeSyllable>,
        lyricsStartPos: Int
    ): List<KaraokeSyllable> {
        if (syllables.size <= 1) return syllables

        val firstContent = syllables[0].content
        if (firstContent.contains("：") || firstContent.contains(":")) {
            return syllables.drop(1)
        }

        // 冒号未被合并的极端情况：手动跳过前两个音节（名称 + 冒号）
        val secondContent = syllables.getOrNull(1)?.content ?: ""
        if (secondContent == "：" || secondContent == ":") {
            return syllables.drop(2)
        }

        return syllables
    }
}
