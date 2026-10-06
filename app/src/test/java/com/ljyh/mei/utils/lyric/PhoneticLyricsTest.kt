package com.ljyh.mei.utils.lyric

import com.ljyh.mei.data.model.response.Lyric
import com.ljyh.mei.ui.model.LyricSourceData
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneticLyricsTest {
    @Test
    fun attachesTimestampedPronunciationWithoutLosingTranslation() {
        val original = SyncedLine("原文", "translation", 1_000, 2_000)

        val result = attachPhonetics(
            SyncedLyrics(lines = listOf(original)),
            "[00:01.20]jyun4 man4",
        ).lines.single() as KaraokeLine.MainKaraokeLine

        assertEquals("原文", result.syllables.single().content)
        assertEquals("translation", result.translation)
        assertEquals("jyun4 man4", result.phonetic)
    }

    @Test
    fun ignoresDistantPronunciationAndKeepsExistingPhonetics() {
        val existing = KaraokeLine.MainKaraokeLine(
            syllables = listOf(KaraokeSyllable("字", 1_000, 2_000, phonetic = "zi6")),
            translation = null,
            alignment = KaraokeAlignment.Start,
            start = 1_000,
            end = 2_000,
        )
        val plain = SyncedLine("下一句", null, 4_000, 5_000)

        val result = attachPhonetics(
            SyncedLyrics(lines = listOf(existing, plain)),
            "[00:01.00]wrong\n[00:06.00]too far",
        ).lines

        assertSame(existing, result[0])
        assertTrue(result[1] is SyncedLine)
    }

    @Test
    fun attachesQrcPronunciation() {
        val original = SyncedLine("原文", "translation", 1_000, 2_000)

        val result = attachPhonetics(
            SyncedLyrics(lines = listOf(original)),
            "[1000,1000]jyun4(1000,400) man4(1400,600)",
        ).lines.single() as KaraokeLine.MainKaraokeLine

        assertEquals("jyun4 man4", result.phonetic)
        assertEquals("translation", result.translation)
    }

    @Test
    fun mergesNeteaseRomanizationIntoDisplayedLyrics() {
        val result = mergeLyrics(listOf(LyricSourceData.NetEase(Lyric(
            lrc = Lyric.Lrc("[00:01.00]原文"),
            tlyric = Lyric.Tlyric("[00:01.00]translation"),
            romalrc = Lyric.Romalrc("[00:01.00]jyun4 man4"),
        ))))

        val line = result.lyricLine.lines.single() as KaraokeLine.MainKaraokeLine
        assertEquals("jyun4 man4", line.phonetic)
        assertEquals("translation", line.translation)
    }
}
