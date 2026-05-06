package com.ljyh.mei.utils.lyric

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.parser.ILyricsParser

object QRCParser : ILyricsParser {

    // QRC 行正则：[起始毫秒,持续毫秒]后续内容
    private val QRC_LINE_REGEX = Regex("""^\[(\d+),(\d+)\](.*)$""")
    // QRC 字词正则：文本(起始毫秒,持续毫秒) —— 注意文本在括号前面
    private val QRC_SYLLABLE_REGEX = Regex("""([^\(\)]*)\((\d+),(\d+)\)""")
    private val BG_LINE_REGEX = Regex("""^\[bg:(.*)\](.*)$""")
    private val translationLineRegex = "\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\].*".toRegex()

    /**
     * 【推荐使用】解析 QRC 主歌词并合并可选的 LRC 翻译。
     *
     * @param qrcLyrics QRC 格式的主歌词文本。
     * @param translationLrc 可选的 LRC 格式的翻译文本。
     * @return 包含合并后歌词的 SyncedLyrics 对象。
     */
    fun parse(qrcLyrics: String, translationLrc: String?): SyncedLyrics {
        val karaokeLines = parseInternal(qrcLyrics.lineSequence())
        val mergedLines = TranslationHelper.merge(karaokeLines, translationLrc)
        return SyncedLyrics(lines = mergedLines)
    }

    override fun canParse(content: String): Boolean {
        // QRC 特征：行首是 [数字,数字]，且包含 文本(数字,数字) 的结构
        val lineTimeRegex = """^\[\d+,\d+\]""".toRegex()
        val wordTimeRegex = """[^\(\)]+\(\d+,\d+\)""".toRegex()

        return content.lineSequence()
            .map { it.trim() }
            .any { line ->
                lineTimeRegex.containsMatchIn(line) && wordTimeRegex.containsMatchIn(line)
            }
    }

    /**
     * 【接口实现】解析一个混合了 QRC 和 LRC 翻译的字符串列表。
     * 不推荐直接调用，除非您必须使用 ILyricsParser 接口。
     */
    override fun parse(lines: List<String>): SyncedLyrics {
        // 智能分离主歌词行和翻译行
        val mainLyricsLines = lines.filter { line ->
            val trimmed = line.trim()
            QRC_LINE_REGEX.matches(trimmed) || trimmed.startsWith("[bg:")
        }
        val translationLines = lines.filter { translationLineRegex.matches(it.trim()) }

        return parse(
            qrcLyrics = mainLyricsLines.joinToString("\n"),
            translationLrc = translationLines.joinToString("\n").ifBlank { null }
        )
    }

    /**
     * 【接口实现】解析混合字符串。
     * 不推荐直接调用，除非您必须使用 ILyricsParser 接口。
     */
    override fun parse(content: String): SyncedLyrics {
        return parse(content.lines())
    }

    private fun parseInternal(rawLinesSequence: Sequence<String>): List<KaraokeLine> {
        val resultLines = mutableListOf<KaraokeLine>()

        var currentRoleState = KaraokeAlignment.Start
        var lastLineStartTime = -1

        for (raw in rawLinesSequence) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            // 参考Kugou：处理背景音行
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

            val match = QRC_LINE_REGEX.find(line) ?: continue

            // 参考Kugou：防止时间戳回退
            var lineStart = match.groupValues[1].toInt()
            if (lastLineStartTime != -1 && lineStart <= lastLineStartTime) {
                lineStart = lastLineStartTime + 3
            }
            lastLineStartTime = lineStart

            val contentPart = match.groupValues[3]
            val rawSyllables = parseSyllablesAndMergeColons(contentPart, lineStart)

            // 参考Kugou：判断对唱角色（主/副）
            val (alignment, finalSyllables, nextState) = determineRole(
                rawSyllables,
                currentRoleState
            )
            currentRoleState = nextState

            if (finalSyllables.isNotEmpty()) {
                resultLines.add(
                    KaraokeLine.MainKaraokeLine(
                        syllables = finalSyllables,
                        translation = null, // 翻译统一在外层处理
                        alignment = alignment,
                        start = finalSyllables.first().start,
                        end = finalSyllables.last().end
                    )
                )
            }
        }

        return resultLines
    }

    private fun parseBackgroundLine(line: String): KaraokeLine.AccompanimentKaraokeLine? {
        val m = BG_LINE_REGEX.find(line) ?: return null
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
     * 针对 QRC 格式 (文本在时间标签前) 定制的解析方式：先提取Token，再将冒号合并至前一个音节
     */
private fun parseSyllablesAndMergeColons(
        content: String,
        baseStartTime: Int
    ): List<KaraokeSyllable> {
        data class TempToken(val offset: Int, val duration: Int, val text: String)

        val tokens = mutableListOf<TempToken>()

        for (m in QRC_SYLLABLE_REGEX.findAll(content)) {
            val text = m.groupValues[1]
            val offset = m.groupValues[2].toIntOrNull() ?: 0
            val duration = m.groupValues[3].toIntOrNull() ?: 0

            if (text.isNotEmpty()) {
                tokens.add(TempToken(offset, duration, text))
            }
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
     * 参考Kugou：根据歌词首尾是否带有冒号(如 "男：" 或 "女：") 来自动切换对唱角色
     */
    private fun determineRole(
        syllables: List<KaraokeSyllable>,
        currentState: KaraokeAlignment
    ): Triple<KaraokeAlignment, List<KaraokeSyllable>, KaraokeAlignment> {
        if (syllables.isEmpty()) return Triple(KaraokeAlignment.Unspecified, syllables, currentState)

        val rawText = syllables.joinToString("") { it.content }
        val hasMarker = rawText.startsWith("：") || rawText.startsWith(":") ||
                rawText.endsWith("：") || rawText.endsWith(":")

        if (hasMarker) {
            val newState =
                if (currentState == KaraokeAlignment.Start) KaraokeAlignment.End else KaraokeAlignment.Start
            return Triple(newState, syllables, newState)
        }

        return Triple(currentState, syllables, currentState)
    }
}
