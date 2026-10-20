package com.ljyh.mei.utils.lyric

import com.ljyh.mei.data.model.response.Lyric
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.ui.model.LyricVariant
import com.ljyh.unblockneteasemusic.model.MusicLyrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricCandidatesTest {
    private val netease = LyricSourceData.NetEase(
        Lyric(
            yrc = Lyric.Yrc("[1000,1000](1000,500,0)逐(1500,500,0)字"),
            lrc = Lyric.Lrc("[00:01.00]逐行"),
        )
    )
    private val qq = LyricSourceData.QQMusic(
        lyric = MusicLyrics("[1000,1000]逐(1000,500)字(1500,500)", wordSynced = true),
        isQRC = true,
        lrcContent = "[00:01.00]QQ 逐行",
    )

    @Test
    fun `separate formats remain available even when another source wins automatically`() {
        val candidates = buildLyricCandidates(listOf(netease, qq), isPureMusic = false)
        val variants = candidates.map { it.variant }

        assertEquals(
            listOf(LyricVariant.NeteaseYrc, LyricVariant.NeteaseLrc, LyricVariant.QqQrc, LyricVariant.QqLrc),
            variants,
        )
        assertTrue(candidates.all { it.preview.isNotBlank() })
    }

    @Test
    fun `manual choice takes precedence and unavailable choice falls back to auto`() {
        val candidates = buildLyricCandidates(listOf(netease, qq), isPureMusic = false)
        val autoData = mergeLyrics(listOf(netease, qq))

        val selected = resolveLyricChoice(LyricVariant.QqQrc, autoData, candidates)
        assertTrue(selected.preferredAvailable)
        assertEquals(LyricVariant.QqQrc, selected.active)

        val unavailable = resolveLyricChoice(LyricVariant.AppleMusic, autoData, candidates)
        assertFalse(unavailable.preferredAvailable)
        assertEquals(autoData, unavailable.data)
        assertEquals(LyricVariant.NeteaseYrc, unavailable.active)
    }
}
