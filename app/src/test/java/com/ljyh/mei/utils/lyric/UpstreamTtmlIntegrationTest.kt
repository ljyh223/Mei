package com.ljyh.mei.utils.lyric

import com.ljyh.mei.data.model.response.Lyric
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.model.LyricSourceData
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.*
import org.junit.Test

/** Exercise app entry points so parser changes cover both display and download exports. */
class UpstreamTtmlIntegrationTest {
    @Test fun playbackPreservesMainSyllableTimingInlineTranslationAndPronunciation() {
        val result = mergeLyrics(listOf(LyricSourceData.AM(document("""
            <p begin="00:01.000" end="00:03.000">
              <span begin="00:01.000" end="00:01.500">A &amp; </span><span begin="00:01.500" end="00:03.000">B</span>
              <span ttm:role="x-roman">a and b</span>
              <span ttm:role="x-translation">甲与乙</span>
            </p>
        """))))
        assertEquals(LyricSource.AM, result.source)
        assertTrue(result.isVerbatim)
        val line = result.lyricLine.lines.single() as KaraokeLine.MainKaraokeLine
        assertEquals("A & B", line.text())
        assertEquals("甲与乙", line.translation)
        assertEquals("a and b", line.phonetic)
        assertEquals(listOf(1000, 1500), line.syllables.map { it.start })
        assertEquals(listOf(1500, 3000), line.syllables.map { it.end })
    }

    @Test fun metadataPronunciationRemainsWordSyncedToTheOriginalSyllables() {
        val xml = document(twoWords, """
            <transliterations><transliteration><text for="L1"><span>ha </span><span>lo</span></text></transliteration></transliterations>
        """)
        val line = displayedLine(xml)
        assertEquals(listOf("ha", "lo"), line.syllables.map { it.phonetic })
        assertEquals(listOf(1000, 1500), line.syllables.map { it.start })
        assertEquals(listOf(1500, 3000), line.syllables.map { it.end })
        assertNull(line.phonetic)
    }

    @Test fun unmatchedMetadataPronunciationIsKeptAsALineCaption() {
        val xml = document(twoWords, """
            <transliterations><transliteration><text for="L1">ha lo extra</text></transliteration></transliterations>
        """)
        val line = displayedLine(xml)
        assertEquals("ha lo extra", line.phonetic)
        assertEquals("Hello world", line.text())
        assertEquals(listOf(1000, 1500), line.syllables.map { it.start })
    }

    @Test fun structuredBackgroundVocalsKeepTheirOwnTranslationTimingAndAlignment() {
        val xml = document("""
            <p itunes:key="L1" ttm:agent="v2" begin="00:01.000" end="00:03.000">
              <span begin="00:01.000" end="00:03.000">Lead</span>
              <span ttm:role="x-bg" begin="00:01.200" end="00:02.000"><span begin="00:01.200" end="00:02.000">(Echo)</span></span>
            </p>
        """, """
            <ttm:agent xml:id="v1"/><ttm:agent xml:id="v2"/>
            <translations><translation><text for="L1">主唱<span ttm:role="x-bg">和声</span></text></translation></translations>
        """)
        val line = displayedLine(xml)
        assertEquals("Lead", line.text())
        assertEquals("主唱", line.translation)
        assertEquals(KaraokeAlignment.End, line.alignment)
        val background = requireNotNull(line.accompanimentLines).single()
        assertEquals("Echo", background.text())
        assertEquals("和声", background.translation)
        assertEquals(1200, background.start)
        assertEquals(2000, background.end)
        assertEquals(KaraokeAlignment.End, background.alignment)
    }

    @Test fun ordinaryParenthesizedTranslationIsNotReassignedToBackgroundVocals() {
        val line = displayedLine(document(twoWords, """
            <translations><translation><text for="L1">主译（补充说明）</text></translation></translations>
        """))
        assertEquals("主译（补充说明）", line.translation)
    }

    @Test fun namespaceAndMixedTextUseOnlyTheBodyAndRetainOriginalWordOrder() {
        val xml = """
            <tt:tt xmlns:tt="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xml:lang="en">
              <tt:head><tt:metadata><tt:p begin="00:00.000" end="00:00.500">not lyrics</tt:p></tt:metadata></tt:head>
              <tt:body><tt:div><tt:p begin="00:01.000" end="00:03.000">One <tt:span>two</tt:span> three<tt:span ttm:role="x-translation">一二三</tt:span></tt:p></tt:div></tt:body>
            </tt:tt>
        """.trimIndent()
        val result = mergeLyrics(listOf(LyricSourceData.AM(xml)))
        assertEquals(LyricSource.AM, result.source)
        val line = result.lyricLine.lines.single() as SyncedLine
        assertEquals("One two three", line.content)
        assertEquals("一二三", line.translation)
        assertEquals("en", line.languageTag)
    }

    @Test fun readableDownloadExportUsesTheSameUpstreamNamespaceAndWhitespaceHandling() {
        val xml = """
            <tt:tt xmlns:tt="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <tt:body><tt:div><tt:p begin="00:01.000" end="00:03.000"><tt:span begin="00:01.000" end="00:01.500">Hello  </tt:span><tt:span begin="00:01.500" end="00:03.000">world</tt:span><tt:span ttm:role="x-translation">你好世界</tt:span></tt:p></tt:div></tt:body>
            </tt:tt>
        """.trimIndent()
        assertEquals("[00:01.000]Hello world\n[00:01.000]你好世界",
            selectEmbeddedLyric(null, xml, null, embedOriginalTtml = false))
        assertEquals(xml, selectEmbeddedLyric(null, xml, null, embedOriginalTtml = true))
    }

    @Test fun emptyTtmlStillFallsBackToTheAvailableNeteaseLyrics() {
        val result = mergeLyrics(listOf(LyricSourceData.AM(document("")),
            LyricSourceData.NetEase(Lyric(lrc = Lyric.Lrc("[00:01.000]Fallback")))))
        assertEquals(LyricSource.NetEaseCloudMusic, result.source)
        assertEquals("Fallback", (result.lyricLine.lines.single() as SyncedLine).content)
    }

    private fun displayedLine(xml: String) = mergeLyrics(listOf(LyricSourceData.AM(xml)))
        .lyricLine.lines.single() as KaraokeLine.MainKaraokeLine

    private fun KaraokeLine.text() = syllables.joinToString("") { it.content }

    private fun document(body: String, metadata: String = "") = """
        <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:itunes="http://music.apple.com/lyric-ttml-internal">
          <head><metadata>$metadata</metadata></head><body><div>$body</div></body>
        </tt>
    """.trimIndent()

    private val twoWords = """<p itunes:key="L1" begin="00:01.000" end="00:03.000"><span begin="00:01.000" end="00:01.500">Hello </span><span begin="00:01.500" end="00:03.000">world</span></p>"""
}
