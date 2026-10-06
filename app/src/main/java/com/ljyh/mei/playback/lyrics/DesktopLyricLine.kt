package com.ljyh.mei.playback.lyrics

import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

internal data class DesktopLyricLine(
    val text: String,
    val translation: String?,
    val karaokeLine: KaraokeLine? = null,
)

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
    return DesktopLyricLine(text, translation, line as? KaraokeLine)
}
