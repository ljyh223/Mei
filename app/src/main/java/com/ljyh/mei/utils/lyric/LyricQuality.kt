package com.ljyh.mei.utils.lyric

import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

internal fun shouldApplyLyricUpdate(current: LyricData, candidate: LyricData): Boolean {
    if (candidate == current) return false
    if (!current.hasResolvedLyrics()) return true
    return candidate.qualityScore() >= current.qualityScore()
}

internal fun LyricData.qualityScore(): Int {
    if (!hasResolvedLyrics()) return 0
    if (isPureMusic) return 100

    val karaokeLines = lyricLine.lines.filterIsInstance<KaraokeLine>()
    val hasWordTiming = karaokeLines.any { it.syllables.isNotEmpty() }
    val hasTranslation = lyricLine.lines.any { line ->
        when (line) {
            is KaraokeLine -> !line.translation.isNullOrBlank()
            is SyncedLine -> !line.translation.isNullOrBlank()
            else -> false
        }
    }
    val formatScore = if (hasWordTiming) 400 else 200
    val translationScore = if (hasTranslation) 20 else 0
    val sourceScore = when (source) {
        LyricSource.AM -> 3
        LyricSource.NetEaseCloudMusic -> 2
        LyricSource.QQMusic -> 1
        else -> 0
    }
    return formatScore + translationScore + sourceScore
}

private fun LyricData.hasResolvedLyrics(): Boolean =
    source != LyricSource.Loading && source != LyricSource.Empty && lyricLine.lines.isNotEmpty()
