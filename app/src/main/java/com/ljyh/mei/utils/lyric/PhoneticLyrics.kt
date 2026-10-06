package com.ljyh.mei.utils.lyric

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.karaoke.copy
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlin.math.abs

/** Add timestamped romanization without replacing translations or existing TTML phonetics. */
internal fun attachPhonetics(lyrics: SyncedLyrics, phoneticLrc: String?): SyncedLyrics {
    if (phoneticLrc.isNullOrBlank()) return lyrics
    val phoneticLines = if (QRCParser.canParse(phoneticLrc)) {
        QRCParser.parse(phoneticLrc, null).lines.filterIsInstance<KaraokeLine>()
            .map { it.start to it.syllables.joinToString("") { syllable -> syllable.content } }
    } else {
        LRCParser.parse(phoneticLrc, null).lines.filterIsInstance<SyncedLine>()
            .map { it.start to it.content }
    }
    if (phoneticLines.isEmpty()) return lyrics

    val used = mutableSetOf<Int>()
    val lines = lyrics.lines.map { line ->
        if (line !is KaraokeLine && line !is SyncedLine) return@map line
        if (line is KaraokeLine && (line.phonetic?.isNotBlank() == true ||
                line.syllables.any { !it.phonetic.isNullOrBlank() })) return@map line

        val match = phoneticLines.indices
            .filterNot(used::contains)
            .minByOrNull { abs(phoneticLines[it].first - line.start) }
            ?.takeIf { abs(phoneticLines[it].first - line.start) <= MAX_PHONETIC_OFFSET_MS }
            ?: return@map line
        used += match
        val phonetic = phoneticLines[match].second
        when (line) {
            is KaraokeLine -> line.copy(phonetic = phonetic)
            is SyncedLine -> KaraokeLine.MainKaraokeLine(
                syllables = listOf(KaraokeSyllable(line.content, line.start, line.end)),
                translation = line.translation,
                alignment = KaraokeAlignment.Start,
                start = line.start,
                end = line.end,
                phonetic = phonetic,
            )
            else -> line
        }
    }
    return SyncedLyrics(lines = lines)
}

private const val MAX_PHONETIC_OFFSET_MS = 1_000
