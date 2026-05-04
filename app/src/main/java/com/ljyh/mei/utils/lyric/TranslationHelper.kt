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

                val ignoreKeywords = listOf("著作权", "Provided by", "字幕", "制作", "出品", "TME")
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

        val maxDelay = 800
        val finalLines = mutableListOf<ISyncedLine>()
        var j = 0

        for (i in karaokeLines.indices) {
            val karaokeLine = karaokeLines[i]
            val lineStart = karaokeLine.start
            val lineEnd = karaokeLine.end
            val lineDuration = lineEnd - lineStart

            while (j < translationLines.size - 1) {
                val currDist = abs(translationLines[j].time - lineStart)
                val nextDist = abs(translationLines[j + 1].time - lineStart)
                if (nextDist < currDist) {
                    j++
                } else {
                    break
                }
            }

            val bestTransMatch = translationLines[j]
            val dist = abs(bestTransMatch.time - lineStart)

            var shouldAssign = true

            if (dist > maxDelay) {
                shouldAssign = false
            }

            if (shouldAssign && i + 1 < karaokeLines.size) {
                val nextKaraokeLine = karaokeLines[i + 1]
                val distToNext = abs(bestTransMatch.time - nextKaraokeLine.start)
                if (distToNext < dist) {
                    shouldAssign = false
                }
            }

            // Interval-based check: if the translation timestamp falls inside
            // a very short metadata line (< 300ms duration), skip assignment
            if (shouldAssign && lineDuration < 300 && lineDuration > 0) {
                shouldAssign = false
            }

            if (shouldAssign) {
                finalLines.add(karaokeLine.copy(translation = bestTransMatch.text))
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

        val maxDelay = 800
        val finalLines = mutableListOf<ISyncedLine>()
        var j = 0

        for (lrcLine in lrcLines) {
            val lineStart = lrcLine.start
            val lineEnd = lrcLine.end

            while (j < translationLines.size - 1) {
                val currDist = abs(translationLines[j].time - lineStart)
                val nextDist = abs(translationLines[j + 1].time - lineStart)
                if (nextDist < currDist) {
                    j++
                } else {
                    break
                }
            }

            val bestMatch = if (j < translationLines.size) {
                val candidate = translationLines[j]
                val dist = abs(candidate.time - lineStart)

                val inInterval = candidate.time in lineStart until lineEnd.coerceAtLeast(lineStart + 1)
                if (inInterval || dist <= maxDelay) candidate else null
            } else null

            finalLines.add(lrcLine.copy(translation = bestMatch?.text))
        }
        return finalLines
    }
}