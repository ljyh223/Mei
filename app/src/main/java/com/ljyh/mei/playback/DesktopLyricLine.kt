package com.ljyh.mei.playback

import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

internal data class DesktopLyricSyllable(val content: String, val start: Int, val end: Int)

internal data class DesktopLyricLine(
    val text: String,
    val translation: String?,
    val syllables: List<DesktopLyricSyllable> = emptyList(),
) {
    /** The current timed word is highlighted together with all completed words. */
    fun highlightedCharactersAt(positionMs: Long): Int = syllables
        .takeWhile { positionMs >= it.start }
        .sumOf { it.content.length }
        .coerceAtMost(text.length)

    fun completedCharactersAt(positionMs: Long): Int = syllables
        .takeWhile { positionMs >= it.end }
        .sumOf { it.content.length }
        .coerceAtMost(text.length)
}

internal fun LyricData.desktopLineAt(positionMs: Long): DesktopLyricLine? {
    if (source == LyricSource.Loading || source == LyricSource.Empty) return null

    val position = positionMs.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    val line = lyricLine.lines.lastOrNull { it.start <= position } ?: return null
    // Keep a line visible through short instrumental gaps, but clear it after the song ends.
    if (lyricLine.lines.none { it.start > position } && position > line.end + 2_000) return null
    val text = when (line) {
        is KaraokeLine -> line.syllables.joinToString("") { it.content }
        is SyncedLine -> line.content
        else -> return null
    }.trim()
    if (text.isEmpty()) return null

    val translation = when (line) {
        is KaraokeLine -> line.translation
        is SyncedLine -> line.translation
        else -> null
    }?.trim()?.takeIf { it.isNotEmpty() && it != text }
    val syllables = (line as? KaraokeLine)?.syllables?.map {
        DesktopLyricSyllable(it.content, it.start, it.end)
    }.orEmpty()
    return DesktopLyricLine(text, translation, syllables)
}
