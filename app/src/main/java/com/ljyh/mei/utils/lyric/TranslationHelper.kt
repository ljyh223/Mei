package com.ljyh.mei.utils.lyric

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.copy
import com.mocharealm.accompanist.lyrics.core.model.synced.UncheckedSyncedLine
import kotlin.math.abs

internal object TranslationHelper {

    private data class TranslationLine(val time: Int, val text: String)

    private val lrcLineRegex = "\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\](.*)".toRegex()

    private fun parseTranslation(translationLrc: String): List<TranslationLine> {
        return translationLrc.lines().mapNotNull { line ->
            val match = lrcLineRegex.matchEntire(line.trim()) ?: return@mapNotNull null
            try {
                val minutes = match.groupValues[1].toInt()
                val seconds = match.groupValues[2].toInt()
                val millisStr = match.groupValues[3]
                val milliseconds = when (millisStr.length) {
                    2 -> millisStr.toInt() * 10
                    3 -> millisStr.toInt()
                    else -> 0
                }
                val totalTime = minutes * 60 * 1000 + seconds * 1000 + milliseconds
                val text = match.groupValues[4].trim()

                if (text.isEmpty() || text.startsWith("//")) return@mapNotNull null

                val ignoreKeywords = listOf(
                    "著作权", "QQ音乐", "腾讯音乐", "未经许可", "不得转载",
                    "Provided by", "字幕", "制作", "出品", "TME"
                )
                if (ignoreKeywords.any { text.contains(it, ignoreCase = true) }) {
                    return@mapNotNull null
                }

                TranslationLine(totalTime, text)
            } catch (e: NumberFormatException) {
                null
            }
        }
    }

    fun merge(karaokeLines: List<KaraokeLine>, translationLrc: String?): List<ISyncedLine> {
        if (translationLrc.isNullOrBlank()) return karaokeLines
        val translationLines = parseTranslation(translationLrc)
        if (translationLines.isEmpty()) return karaokeLines

        val maxDelay = 3000
        val usedOrig = mutableSetOf<Int>()
        val result = karaokeLines.toMutableList<ISyncedLine>()

        for (tl in translationLines) {
            var bestIdx = -1
            var bestDiff = maxDelay + 1

            for (i in result.indices) {
                if (i in usedOrig) continue
                val line = result[i]
                val lineDuration = line.end - line.start
                if (lineDuration < 300 && lineDuration > 0) continue

                val diff = abs(tl.time - line.start)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestIdx = i
                } else if (diff == bestDiff && bestIdx >= 0) {
                    // 时间相同，取文本更长的一方（长文本更可能是实际歌词而非角色标记）
                    val curTextLen = (line as KaraokeLine).syllables.joinToString("") { it.content }.length
                    val bestTextLen = (result[bestIdx] as KaraokeLine).syllables.joinToString("") { it.content }.length
                    if (curTextLen > bestTextLen) {
                        bestIdx = i
                    }
                }
            }

            if (bestIdx >= 0) {
                val line = result[bestIdx] as KaraokeLine
                result[bestIdx] = line.copy(translation = tl.text)
                usedOrig.add(bestIdx)
            }
        }

        return result
    }

    fun mergeLRC(lrcLines: List<UncheckedSyncedLine>, translationLrc: String?): List<ISyncedLine> {
        if (translationLrc.isNullOrBlank()) return lrcLines
        val translationLines = parseTranslation(translationLrc)
        if (translationLines.isEmpty()) return lrcLines

        val maxDelay = 3000
        val usedOrig = mutableSetOf<Int>()
        val result = lrcLines.toMutableList<ISyncedLine>()

        for (tl in translationLines) {
            var bestIdx = -1
            var bestDiff = maxDelay + 1

            for (i in result.indices) {
                if (i in usedOrig) continue
                val line = result[i]

                val diff = abs(tl.time - line.start)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestIdx = i
                } else if (diff == bestDiff && bestIdx >= 0) {
                    val curLen = (line as UncheckedSyncedLine).content.length
                    val bestLen = (result[bestIdx] as UncheckedSyncedLine).content.length
                    if (curLen > bestLen) {
                        bestIdx = i
                    }
                }
            }

            if (bestIdx >= 0) {
                val line = result[bestIdx] as UncheckedSyncedLine
                result[bestIdx] = line.copy(translation = tl.text)
                usedOrig.add(bestIdx)
            }
        }

        return result
    }
}