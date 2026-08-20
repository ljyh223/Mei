package com.ljyh.mei.utils.lyric

import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricQualityTest {

    @Test
    fun `word synced lyrics are never replaced by line synced lyrics`() {
        val current = wordSynced(LyricSource.QQMusic, translation = null)
        val candidate = lineSynced(LyricSource.NetEaseCloudMusic, translation = "翻译")

        assertFalse(shouldApplyLyricUpdate(current, candidate))
    }

    @Test
    fun `line synced lyrics upgrade to word synced lyrics`() {
        val current = lineSynced(LyricSource.NetEaseCloudMusic, translation = "翻译")
        val candidate = wordSynced(LyricSource.QQMusic, translation = null)

        assertTrue(shouldApplyLyricUpdate(current, candidate))
    }

    @Test
    fun `same source can upgrade when translation becomes available`() {
        val current = wordSynced(LyricSource.QQMusic, translation = null)
        val candidate = wordSynced(LyricSource.QQMusic, translation = "翻译")

        assertTrue(shouldApplyLyricUpdate(current, candidate))
    }

    private fun lineSynced(source: LyricSource, translation: String?) = LyricData(
        isVerbatim = false,
        source = source,
        lyricLine = LRCParser.parse(
            "[00:01.00]main",
            translation?.let { "[00:01.00]$it" }
        )
    )

    private fun wordSynced(source: LyricSource, translation: String?) = LyricData(
        isVerbatim = true,
        source = source,
        lyricLine = YRCParser.parse(
            "[1000,1000](1000,500,0)逐(1500,500,0)字",
            translation?.let { "[00:01.000]$it" }
        )
    )
}
