package com.ljyh.mei.utils.lyric

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.parser.ILyricsParser

object YRCParser : ILyricsParser {

    private val lineRegex = "\\[(\\d+),(\\d+)\\](.*)".toRegex()
    private val wordRegex = "\\((\\d+),(\\d+),\\d+\\)([^\\(\\)]*)".toRegex()
    private val translationLineRegex = "\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\].*".toRegex()


    /**
     * 【推荐使用】解析 YRC 主歌词并合并可选的 LRC 翻译。
     *
     * @param yrcLyrics YRC 格式的主歌词文本。
     * @param translationLrc 可选的 LRC 格式的翻译文本。
     * @return 包含合并后歌词的 SyncedLyrics 对象。
     */
    fun parse(yrcLyrics: String, translationLrc: String?): SyncedLyrics {
        val karaokeLines = yrcLyrics.lines().mapNotNull { parseLine(it) }
        val mergedLines = TranslationHelper.merge(karaokeLines, translationLrc)
        return SyncedLyrics(lines = mergedLines)
    }

    /**
     * 【接口实现】解析一个混合了 YRC 和 LRC 翻译的字符串列表。
     * 不推荐直接调用，除非您必须使用 ILyricsParser 接口。
     */
    override fun parse(lines: List<String>): SyncedLyrics {
        // 智能分离主歌词行和翻译行
        val mainLyricsLines = lines.filter { lineRegex.matches(it) }
        val translationLines = lines.filter { translationLineRegex.matches(it) }

        return parse(
            yrcLyrics = mainLyricsLines.joinToString("\n"),
            translationLrc = translationLines.joinToString("\n")
        )
    }

    private fun parseLine(line: String): KaraokeLine? {
        val lineMatch = lineRegex.matchEntire(line.trim()) ?: return null
        val (lineStartStr, lineDurationStr, wordsText) = lineMatch.destructured
        val syllables = wordRegex.findAll(wordsText).mapNotNull { wordMatch ->
            try {
                val (wordStartStr, wordDurationStr, wordText) = wordMatch.destructured
                val startTime = wordStartStr.toInt()
                val duration = wordDurationStr.toInt()
                KaraokeSyllable(wordText, startTime, startTime + duration)
            } catch (e: NumberFormatException) { null }
        }.toList()

        if (syllables.isEmpty()) return null

        return try {
            val lineStartTime = lineStartStr.toInt()
            val lineEndTime = lineStartTime + lineDurationStr.toInt()
            KaraokeLine(syllables, null, false, KaraokeAlignment.Start, lineStartTime, lineEndTime)
        } catch (e: NumberFormatException) { null }
    }
}