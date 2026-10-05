package com.ljyh.mei.utils.lyric

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import java.util.Locale

/** Exports readable, line-synced LRC for audio tags and sidecar files. */
internal object DownloadLrcEncoder {
    fun encode(lyrics: SyncedLyrics): String? = lyrics.lines
        .flatMap(::encodeLine)
        .sortedBy { it.time }
        .joinToString("\n") { "[${it.time.timestamp()}]${it.text}" }
        .takeIf(String::isNotBlank)

    private fun encodeLine(line: ISyncedLine): List<Record> = when (line) {
        is KaraokeLine.MainKaraokeLine -> buildList {
            addKaraokeLine(line)
            line.accompanimentLines.orEmpty().forEach { addKaraokeLine(it) }
        }
        is KaraokeLine.AccompanimentKaraokeLine -> buildList { addKaraokeLine(line) }
        is SyncedLine -> buildList {
            line.content.singleLine().takeIf(String::isNotBlank)?.let { add(Record(line.start, it)) }
            line.translation?.singleLine()?.takeIf(String::isNotBlank)
                ?.let { add(Record(line.start, it)) }
        }
        else -> emptyList()
    }

    private fun MutableList<Record>.addKaraokeLine(line: KaraokeLine) {
        line.syllables.joinToString("") { it.content }.singleLine()
            .takeIf(String::isNotBlank)?.let { add(Record(line.start, it)) }
        line.translation?.singleLine()?.takeIf(String::isNotBlank)
            ?.let { add(Record(line.start, it)) }
    }

    private fun Int.timestamp(): String {
        val safeTime = coerceAtLeast(0)
        return String.format(
            Locale.ROOT,
            "%02d:%02d.%03d",
            safeTime / 60_000,
            safeTime % 60_000 / 1_000,
            safeTime % 1_000,
        )
    }

    private fun String.singleLine(): String = replace(Regex("\\s+"), " ").trim()

    private data class Record(val time: Int, val text: String)
}
