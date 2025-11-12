package com.ljyh.mei.utils.lyric

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.UncheckedSyncedLine
import kotlin.math.abs

/**
 * 一个用于处理歌词翻译的帮助对象。
 * 它可以解析标准的 LRC 格式翻译，并将其高效地合并到逐字歌词列表中。
 */
internal object TranslationHelper {

    /** 用于临时存储解析出的 LRC 翻译行的数据类。 */
    private data class TranslationLine(val time: Int, val text: String)

    // 正则表达式，用于匹配标准 LRC 格式的行（支持 xx 和 xxx 毫秒）
    // e.g., [00:20.570] 或 [00:05.85]
    private val lrcLineRegex = "\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\](.*)".toRegex()

    /**
     * 将一个 LRC 格式的翻译字符串解析成 TranslationLine 列表。
     * @param translationLrc 完整的翻译歌词文本。
     * @return 解析后的 TranslationLine 列表。
     */
    private fun parseTranslation(translationLrc: String): List<TranslationLine> {
        return translationLrc.lines().mapNotNull { line ->
            val match = lrcLineRegex.matchEntire(line.trim()) ?: return@mapNotNull null
            try {
                val minutes = match.groupValues[1].toInt()
                val seconds = match.groupValues[2].toInt()
                val millisStr = match.groupValues[3]
                // 将两位或三位的毫秒统一为三位数
                val milliseconds = when (millisStr.length) {
                    2 -> millisStr.toInt() * 10
                    3 -> millisStr.toInt()
                    else -> 0
                }
                val totalTime = minutes * 60 * 1000 + seconds * 1000 + milliseconds
                val text = match.groupValues[4].trim()

                // 忽略 QRC 翻译中常见的注释行
                if (text.startsWith("//")) null else TranslationLine(totalTime, text)

            } catch (e: NumberFormatException) {
                null
            }
        }
    }

    /**
     * 使用双指针算法，将翻译行高效地合并到主歌词行列表中。
     * @param karaokeLines 解析出的主歌词行。
     * @param translationLrc 完整的 LRC 格式翻译文本。如果为 null 或空，则直接返回原列表。
     * @return 返回一个新的列表，其中包含了已添加翻译的 KaraokeLine 对象。
     */
    fun merge(karaokeLines: List<KaraokeLine>, translationLrc: String?): List<ISyncedLine> {
        if (translationLrc.isNullOrBlank()) {
            return karaokeLines
        }

        val translationLines = parseTranslation(translationLrc)
        if (translationLines.isEmpty()) {
            return karaokeLines
        }

        val finalLines = mutableListOf<ISyncedLine>()
        var j = 0 // 翻译歌词的索引

        for (karaokeLine in karaokeLines) {
            // 移动翻译指针 j，使其指向与当前主歌词行时间戳最接近的翻译行
            while (j < translationLines.size - 1 &&
                abs(translationLines[j].time - karaokeLine.start) > abs(translationLines[j + 1].time - karaokeLine.start)
            ) {
                j++
            }

            // 如果找到了匹配的翻译行，则通过 copy 创建一个带翻译的新对象
            if (j < translationLines.size) {
                finalLines.add(karaokeLine.copy(translation = translationLines[j].text))
            } else {
                finalLines.add(karaokeLine)
            }
        }
        return finalLines
    }

    fun mergeLRC(lrcLines: List<UncheckedSyncedLine>, translationLrc: String?): List<ISyncedLine> {
        if (translationLrc.isNullOrBlank()) {
            return lrcLines
        }

        val translationLines = parseTranslation(translationLrc)
        if (translationLines.isEmpty()) {
            return lrcLines
        }

        val finalLines = mutableListOf<ISyncedLine>()
        var j = 0 // 翻译歌词的索引

        for (karaokeLine in lrcLines) {
            // 移动翻译指针 j，使其指向与当前主歌词行时间戳最接近的翻译行
            while (j < translationLines.size - 1 &&
                abs(translationLines[j].time - karaokeLine.start) > abs(translationLines[j + 1].time - karaokeLine.start)
            ) {
                j++
            }

            // 如果找到了匹配的翻译行，则通过 copy 创建一个带翻译的新对象
            if (j < translationLines.size) {
                finalLines.add(karaokeLine.copy(translation = translationLines[j].text))
            } else {
                finalLines.add(karaokeLine)
            }
        }
        return finalLines

    }


}