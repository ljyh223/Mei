package com.ljyh.mei.utils.lyric

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.UncheckedSyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.ILyricsParser
import com.mocharealm.accompanist.lyrics.core.parser.LrcMetadataHelper
import kotlin.math.abs

private data class TranslationLine(val time: Int, val text: String)
object LRCParser : ILyricsParser {
    private val lrcLineRegex = Regex("\\[(\\d{1,2}:\\d{1,2}[:.]\\d{2,3})](.*)")
    // A separate regex for parsing translation files
    private val translationLrcRegex = Regex("\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})](.*)")

    /**
     * 【推荐使用】解析 LRC 主歌词并合并一个可选的、独立的 LRC 翻译文件。
     *
     * @param mainLrc LRC 格式的主歌词文本。
     * @param translationLrc 可选的、LRC 格式的翻译文本。
     * @return 包含合并后歌词的 SyncedLyrics 对象。
     */
    fun parse(mainLrc: String, translationLrc: String?): SyncedLyrics {
        val lyricsLines = LrcMetadataHelper.removeAttributes(mainLrc.lines())

        var data = lyricsLines
            .flatMap { line -> parseLine(line) }
            .sortedBy { it.start } // Sort early to ensure correct order before merging

        // If a translation file is provided, merge it using the efficient algorithm
        if (!translationLrc.isNullOrBlank()) {
            data = data.mergeWithExternalTranslation(translationLrc)
        }

        // Continue with the original processing pipeline
        val finalLines = data
            .rearrangeTime()
            .map { it.toSyncedLine() }
            .filter { it.content.isNotBlank() }

        return SyncedLyrics(lines = finalLines)
    }

    /**
     * 【接口实现】解析一个字符串列表。
     * 这个实现保留了原始功能：处理元数据和在同一文件中交错的翻译行。
     */
    override fun parse(lines: List<String>): SyncedLyrics {
        val lyricsLines = LrcMetadataHelper.removeAttributes(lines)
        val data = lyricsLines
            .flatMap { line -> parseLine(line) }
            // Note: Uses the original method for interleaved translations
            .combineRawWithTranslation()
            .rearrangeTime()
            .map { it.toSyncedLine() }
            .filter { it.content.isNotBlank() }
            .sortedBy { it.start }
        return SyncedLyrics(lines = data)
    }

    private fun parseLine(content: String): List<UncheckedSyncedLine> {
        return lrcLineRegex.findAll(content).map { matchResult ->
            val (time, lyric) = matchResult.destructured
            UncheckedSyncedLine(
                start = time.parseAsTime(),
                end = 0,
                content = lyric.trim(),
                translation = null
            )
        }.toList()
    }

    /**
     * 新增：高效地将外部翻译文本合并到歌词行列表中。
     */
    private fun List<UncheckedSyncedLine>.mergeWithExternalTranslation(translationLrc: String): List<UncheckedSyncedLine> {
        val mainLines = this

        val translationLines = translationLrc.lines().mapNotNull { line ->
            translationLrcRegex.matchEntire(line.trim())?.let { match ->
                try {
                    val time = match.groupValues[1] + ":" + match.groupValues[2] + "." + match.groupValues[3]
                    val text = match.groupValues[4].trim()
                    if (text.startsWith("//")) null
                    else TranslationLine(time.parseAsTime(), text)
                } catch (e: Exception) { null }
            }
        }

        if (translationLines.isEmpty()) return mainLines
        val finalLines = mutableListOf<UncheckedSyncedLine>()
        var j = 0

        for (mainLine in mainLines) {
            while (j < translationLines.size - 1 &&
                abs(translationLines[j].time - mainLine.start) > abs(translationLines[j + 1].time - mainLine.start)
            ) {
                j++
            }

            // Assign the translation if found
            if (j < translationLines.size) {
                finalLines.add(mainLine.copy(translation = translationLines[j].text))
            } else {
                finalLines.add(mainLine)
            }
        }
        return finalLines
    }

    /**
     * 原始方法：用于处理在同一文件中、时间戳完全相同的交错翻译。
     */
    private fun List<UncheckedSyncedLine>.combineRawWithTranslation(): List<UncheckedSyncedLine> {
        val sortedList = this.sortedBy { it.start } // Ensure sorting before combining
        val list = mutableListOf<UncheckedSyncedLine>()
        var i = 0
        while (i < sortedList.size) {
            val line = sortedList[i]
            val nextLine = sortedList.getOrNull(i + 1)
            if (nextLine != null && line.start == nextLine.start) {
                list.add(line.copy(translation = nextLine.content))
                i += 2
            } else {
                list.add(line)
                i++
            }
        }
        return list
    }

    private fun List<UncheckedSyncedLine>.rearrangeTime(): List<UncheckedSyncedLine> =
        this.mapIndexed { index, line ->
            val end = this.getOrNull(index + 1)?.start ?: (line.start + 5000) // Or a default duration
            line.copy(end = end)
        }
}