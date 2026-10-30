package com.ljyh.mei.playback.lyrics

import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** ColorOS's native lyricInfo extension; this is not an Android-wide lyric API. */
internal fun colorOsLyricPayload(mediaId: String, metadata: MediaMetadata, data: LyricData): String? {
    if (data.source == LyricSource.Loading || data.source == LyricSource.Empty || data.isPureMusic) return null

    val rows = data.lyricLine.lines.sortedBy { it.start }.mapNotNull { line ->
        if (line.start < 0 || line.end < line.start) return@mapNotNull null
        val text = when (line) {
            is KaraokeLine -> line.syllables.joinToString("") { it.content }
            is SyncedLine -> line.content
            else -> return@mapNotNull null
        }.lyricText()
        if (text.isBlank()) return@mapNotNull null
        val translation = when (line) {
            is KaraokeLine -> line.translation
            is SyncedLine -> line.translation
            else -> null
        }?.lyricText()?.takeIf { it.isNotBlank() && it != text }
        val tag = "[${lyricTimestamp(line.start)}]"
        val words = (line as? KaraokeLine)?.syllables?.takeIf { syllables ->
            data.isVerbatim && syllables.isNotEmpty() &&
                syllables.all { it.start >= line.start && it.end >= it.start && it.end <= line.end } &&
                syllables.zipWithNext().all { (a, b) -> a.start <= b.start }
        }
        val raw = words?.joinToString("", prefix = tag) {
            "<${lyricTimestamp(it.start)}>${it.content.lyricText()}"
        }?.plus("<${lyricTimestamp(words.last().end)}>")
        LyricRow(tag + text, raw, translation?.let { tag + it })
    }
    if (rows.isEmpty()) return null

    return buildJsonObject {
        put("songName", metadata.title)
        put("artist", metadata.artists.joinToString("、") { it.name })
        put("songId", mediaId)
        put("album", metadata.album.title)
        put("lyricType", 0)
        put("lyric", rows.joinToString("\n") { it.line })
        if (rows.any { it.raw != null }) put("rawLyric", rows.joinToString("\n") { it.raw ?: it.line })
        rows.mapNotNull { it.translation }.takeIf { it.isNotEmpty() }?.let {
            put("translationLyric", it.joinToString("\n"))
        }
        put("noLyric", false)
    }.toString().takeIf { it.toByteArray(Charsets.UTF_8).size <= 256 * 1024 }
}

private data class LyricRow(val line: String, val raw: String?, val translation: String?)

private fun String.lyricText(): String = replace('\r', ' ').replace('\n', ' ')

private fun lyricTimestamp(timeMs: Int): String {
    val minutes = (timeMs / 60_000).toString().padStart(2, '0')
    val seconds = (timeMs / 1_000 % 60).toString().padStart(2, '0')
    val millis = (timeMs % 1_000).toString().padStart(3, '0')
    return "$minutes:$seconds.$millis"
}
