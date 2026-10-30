package com.ljyh.mei.playback.lyrics

import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class ColorOsLyricPayloadTest {
    private val song = MediaMetadata(
        id = 42, title = "Song \"A\"", coverUrl = "", duration = 180_000,
        artists = listOf(MediaMetadata.Artist(1, "Artist")),
        album = MediaMetadata.Album(1, "Album"),
    )
    private val lyrics = LyricData(
        source = LyricSource.NetEaseCloudMusic,
        lyricLine = SyncedLyrics(lines = listOf(
            SyncedLine("第二句", null, 65_234, 67_000),
            SyncedLine("First line", "第一句", 1_000, 2_000),
        )),
    )

    @Test
    fun publishesEscapedIdentitySortedTimelineAndRealTranslation() {
        val json = Json.parseToJsonElement(requireNotNull(colorOsLyricPayload("local_42", song, lyrics))).jsonObject
        assertEquals("local_42", json.getValue("songId").jsonPrimitive.content)
        assertEquals(song.title, json.getValue("songName").jsonPrimitive.content)
        assertEquals("[00:01.000]First line\n[01:05.234]第二句", json.getValue("lyric").jsonPrimitive.content)
        assertEquals("[00:01.000]第一句", json.getValue("translationLyric").jsonPrimitive.content)
        assertFalse(json.containsKey("rawLyric"))
    }

    @Test
    fun preservesAbsoluteWordTimesSpacesAndLastWordEnd() {
        val line = KaraokeLine.MainKaraokeLine(
            syllables = listOf(KaraokeSyllable("Hello ", 1_000, 1_400), KaraokeSyllable("world", 1_500, 2_000)),
            translation = "你好", alignment = KaraokeAlignment.Start, start = 1_000, end = 3_000,
        )
        val data = lyrics.copy(isVerbatim = true, lyricLine = SyncedLyrics(lines = listOf(line)))
        val json = Json.parseToJsonElement(requireNotNull(colorOsLyricPayload("42", song, data))).jsonObject
        assertEquals("[00:01.000]Hello world", json.getValue("lyric").jsonPrimitive.content)
        assertEquals("[00:01.000]<00:01.000>Hello <00:01.500>world<00:02.000>",
            json.getValue("rawLyric").jsonPrimitive.content)
    }

    @Test
    fun skipsPlaceholderInstrumentalAndEmptyLyrics() {
        assertNull(colorOsLyricPayload("42", song, lyrics.copy(source = LyricSource.Loading)))
        assertNull(colorOsLyricPayload("42", song, lyrics.copy(source = LyricSource.Empty)))
        assertNull(colorOsLyricPayload("42", song, lyrics.copy(isPureMusic = true)))
        assertNull(colorOsLyricPayload("42", song, lyrics.copy(lyricLine = SyncedLyrics(lines = emptyList()))))
    }

    @Test
    fun phoneticOnlySyllablesDoNotInventWordTimingOrDuplicateTranslation() {
        val line = KaraokeLine.MainKaraokeLine(
            syllables = listOf(KaraokeSyllable("你好", 1_000, 2_000, phonetic = "ni hao")),
            translation = "你好", alignment = KaraokeAlignment.Start, start = 1_000, end = 2_000,
        )
        val data = lyrics.copy(lyricLine = SyncedLyrics(lines = listOf(line)))
        val json = Json.parseToJsonElement(requireNotNull(colorOsLyricPayload("42", song, data))).jsonObject
        assertFalse(json.containsKey("rawLyric"))
        assertFalse(json.containsKey("translationLyric"))
    }

    @Test
    fun blankRowsAreSkippedAndOversizedPayloadIsRejected() {
        val blank = lyrics.copy(lyricLine = SyncedLyrics(lines = listOf(
            SyncedLine("  ", null, 1, 2),
        )))
        assertNull(colorOsLyricPayload("42", song, blank))
        val huge = lyrics.copy(lyricLine = SyncedLyrics(lines = listOf(
            SyncedLine("字".repeat(100_000), null, 0, 1_000),
        )))
        assertNull(colorOsLyricPayload("42", song, huge))
    }
}
