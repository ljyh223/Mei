package com.ljyh.mei.playback

import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DesktopLyricLineTest {
    @Test
    fun selectsCurrentLineAndKeepsItThroughShortGap() {
        val lyrics = LyricData(
            source = LyricSource.NetEaseCloudMusic,
            lyricLine = SyncedLyrics(
                lines = listOf(
                    SyncedLine("第一句", "First line", 1_000, 2_000),
                    SyncedLine("第二句", null, 3_000, 4_000),
                )
            )
        )

        assertNull(lyrics.desktopLineAt(999))
        assertEquals(DesktopLyricLine("第一句", "First line"), lyrics.desktopLineAt(1_500))
        assertEquals(DesktopLyricLine("第一句", "First line"), lyrics.desktopLineAt(2_500))
        assertEquals(DesktopLyricLine("第二句", null), lyrics.desktopLineAt(3_000))
        assertNull(lyrics.desktopLineAt(6_001))
    }

    @Test
    fun joinsKaraokeSyllablesAndSkipsLoadingState() {
        val karaoke = KaraokeLine.MainKaraokeLine(
            syllables = listOf(
                KaraokeSyllable("你", 100, 200),
                KaraokeSyllable("好", 200, 300),
            ),
            translation = "Hello",
            alignment = KaraokeAlignment.Start,
            start = 100,
            end = 300,
        )
        val lyrics = LyricData(
            source = LyricSource.QQMusic,
            lyricLine = SyncedLyrics(lines = listOf(karaoke)),
        )

        assertEquals(DesktopLyricLine("你好", "Hello"), lyrics.desktopLineAt(250))
        assertNull(lyrics.copy(source = LyricSource.Loading).desktopLineAt(250))
    }
}
