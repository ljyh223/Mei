package com.ljyh.mei.utils.lyric

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import java.util.Locale

/** Encodes parsed karaoke lyrics using Enhanced LRC word timestamps. */
object EnhancedLrcEncoder {

    fun encode(lyrics: SyncedLyrics): String? {
        val records = lyrics.lines
            .flatMap(::encodeLine)
            .sortedBy { it.time }
        return records.joinToString("\n") { it.content }.takeIf(String::isNotBlank)
    }

    private fun encodeLine(line: ISyncedLine): List<Record> = when (line) {
        is KaraokeLine.MainKaraokeLine -> buildList {
            encodeKaraokeLine(line)?.let(::add)
            line.translation?.toSingleLine()?.takeIf(String::isNotBlank)?.let { translation ->
                add(Record(line.start, "${line.start.lineTimestamp()}$translation"))
            }
            line.accompanimentLines.orEmpty().forEach { accompaniment ->
                encodeKaraokeLine(accompaniment)?.let(::add)
                accompaniment.translation?.toSingleLine()?.takeIf(String::isNotBlank)?.let { translation ->
                    add(Record(accompaniment.start, "${accompaniment.start.lineTimestamp()}$translation"))
                }
            }
        }

        is KaraokeLine.AccompanimentKaraokeLine -> buildList {
            encodeKaraokeLine(line)?.let(::add)
            line.translation?.toSingleLine()?.takeIf(String::isNotBlank)?.let { translation ->
                add(Record(line.start, "${line.start.lineTimestamp()}$translation"))
            }
        }

        is SyncedLine -> buildList {
            line.content.toSingleLine().takeIf(String::isNotBlank)?.let { content ->
                add(Record(line.start, "${line.start.lineTimestamp()}$content"))
            }
            line.translation?.toSingleLine()?.takeIf(String::isNotBlank)?.let { translation ->
                add(Record(line.start, "${line.start.lineTimestamp()}$translation"))
            }
        }

        else -> emptyList()
    }

    private fun encodeKaraokeLine(line: KaraokeLine): Record? {
        if (line.syllables.isEmpty()) return null
        val content = buildString {
            append(line.start.lineTimestamp())
            line.syllables.forEach { syllable ->
                append(syllable.start.wordTimestamp())
                append(syllable.content.toSingleLine())
            }
            append(line.end.wordTimestamp())
        }
        return Record(line.start, content)
    }

    private fun Int.lineTimestamp(): String = "[${formatTimestamp()}]"

    private fun Int.wordTimestamp(): String = "<${formatTimestamp()}>"

    private fun Int.formatTimestamp(): String {
        val safeTime = coerceAtLeast(0)
        val minutes = safeTime / 60_000
        val seconds = safeTime % 60_000 / 1_000
        val millis = safeTime % 1_000
        return String.format(Locale.ROOT, "%02d:%02d.%03d", minutes, seconds, millis)
    }

    private fun String.toSingleLine(): String = replace(Regex("[\\r\\n]+"), " ").trim()

    private data class Record(val time: Int, val content: String)
}
