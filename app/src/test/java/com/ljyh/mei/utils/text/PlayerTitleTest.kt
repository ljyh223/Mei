package com.ljyh.mei.utils.text

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerTitleTest {
    @Test
    fun disabledPreservesOriginalTitleExactly() {
        assertEquals("  歌名 (Live)  ", formatPlayerTitle("  歌名 (Live)  ", false))
    }

    @Test
    fun removesChineseAndEnglishBracketContents() {
        listOf(
            "歌名 (Live)", "歌名（现场版）", "歌名 [Remix]", "歌名【伴奏】",
            "歌名［Live］", "歌名〔Live〕", "歌名 {Live}", "歌名｛Live｝",
            "歌名「Live」", "歌名『Live』", "歌名〈Live〉", "歌名《Live》",
            "歌名 <Live>", "歌名＜Live＞",
        ).forEach { assertEquals(it, "歌名", formatPlayerTitle(it, true)) }
    }

    @Test
    fun handlesNestedAndMultipleSections() {
        assertEquals("歌名", formatPlayerTitle("歌名（现场版 (2026)） [Remix]", true))
        assertEquals("Song Remix", formatPlayerTitle("Song (Live) Remix 【2026】", true))
        assertEquals("歌名", formatPlayerTitle("【原声】歌名（Live）", true))
    }

    @Test
    fun preservesUnmatchedBracketsAndRemainingTitle() {
        listOf("歌名 (Live", "歌名 Live)", "歌名（Live)", "歌名 [Live)").forEach {
            assertEquals(it, formatPlayerTitle(it, true))
        }
        assertEquals("歌名 (Live", formatPlayerTitle("歌名 (Live [2026]", true))
    }

    @Test
    fun fallsBackWhenRemovingBracketsWouldEraseTitle() {
        listOf("(Live)", "（无题）【Live】", "", "   ").forEach {
            assertEquals(it, formatPlayerTitle(it, true))
        }
    }

    @Test
    fun leavesTitlesWithoutBracketSectionsUnchanged() {
        assertEquals("Song  Name", formatPlayerTitle("Song  Name", true))
        assertEquals("歌名 feat. Artist", formatPlayerTitle("歌名 feat. Artist", true))
    }
}
