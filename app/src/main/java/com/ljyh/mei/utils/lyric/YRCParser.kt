package com.ljyh.mei.utils.lyric

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.parser.ILyricsParser

object YRCParser : ILyricsParser {

    private val YRC_LINE_REGEX = Regex("""^\[(\d+),(\d+)\](.*)${'$'}""")
    private val YRC_SYLLABLE_REGEX = Regex("""\((\d+),(\d+),\d+\)""")
    private val BG_LINE_REGEX = Regex("""^\[bg:(.*)\](.*)${'$'}""")
    private val translationLineRegex = "\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\].*".toRegex()

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
            .any { line -> lineTimeRegex.containsMatchIn(line) && wordTimeRegex.containsMatchIn(line) }
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

        for (raw in rawLinesSequence) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            val match = YRC_LINE_REGEX.find(line) ?: continue
            val lineStart = match.groupValues[1].toInt()
            val contentPart = match.groupValues[3]
            val rawSyllables = parseSyllablesAndMergeColons(contentPart, lineStart)

            if (rawSyllables.isNotEmpty()) {
                resultLines.add(
                    KaraokeLine.MainKaraokeLine(
                        syllables = rawSyllables,
                        translation = null,
                        alignment = KaraokeAlignment.Unspecified,
                        start = rawSyllables.first().start,
                        end = rawSyllables.last().end
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
            syllables = syllables, translation = null, alignment = KaraokeAlignment.Unspecified,
            start = syllables.first().start, end = syllables.last().end
        )
    }

    private fun parseSyllablesAndMergeColons(
        content: String, baseStartTime: Int
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
        val merged = mutableListOf<KaraokeSyllable>()
        var i = 0
        while (i < tokens.size) {
            val cur = tokens[i]
            val nxt = tokens.getOrNull(i + 1)
            val st = if (useAbsoluteTime) cur.offset else baseStartTime + cur.offset
            if (nxt != null && (nxt.text == "：" || nxt.text == ":")) {
                val ns = if (useAbsoluteTime) nxt.offset else baseStartTime + nxt.offset
                merged.add(KaraokeSyllable(cur.text + nxt.text, st, ns + nxt.duration))
                i += 2
            } else {
                merged.add(KaraokeSyllable(cur.text, st, st + cur.duration))
                i++
            }
        }
        return merged
    }
}
