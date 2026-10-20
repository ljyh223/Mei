package com.ljyh.mei.utils.lyric

import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.assertEquals
import org.junit.Test

class LRCParserTest {
    @Test
    fun parsesPlainLinesAndDerivesTheirEndTimes() {
        val lines = LRCParser.parse("[00:01.00]第一句\n[00:03.00]第二句", null)
            .lines.filterIsInstance<SyncedLine>()

        assertEquals(
            listOf(
                SyncedLine("第一句", null, 1_000, 3_000),
                SyncedLine("第二句", null, 3_000, 8_000),
            ),
            lines,
        )
    }

    @Test
    fun mergesExternalTranslationAndRetainsTiming() {
        val lines = LRCParser.parse(
            "[00:01.00]原文\n[00:03.00]下一句",
            "[00:01.00]Translation\n[00:03.00]Next line",
        ).lines.filterIsInstance<SyncedLine>()

        assertEquals(
            listOf(
                SyncedLine("原文", "Translation", 1_000, 3_000),
                SyncedLine("下一句", "Next line", 3_000, 8_000),
            ),
            lines,
        )
    }

    @Test
    fun mergesInterleavedTranslationThroughParserInterface() {
        val lines = LRCParser.parse(listOf("[00:01.00]原文", "[00:01.00]Translation"))
            .lines.filterIsInstance<SyncedLine>()

        assertEquals(listOf(SyncedLine("原文", "Translation", 1_000, 6_000)), lines)
    }

    @Test
    fun expandsMultipleTimestampsWithoutLosingContent() {
        val lines = LRCParser.parse("[00:01.00][00:03.00]重复句", null)
            .lines.filterIsInstance<SyncedLine>()

        assertEquals(
            listOf(
                SyncedLine("重复句", null, 1_000, 3_000),
                SyncedLine("重复句", null, 3_000, 8_000),
            ),
            lines,
        )
    }
}
