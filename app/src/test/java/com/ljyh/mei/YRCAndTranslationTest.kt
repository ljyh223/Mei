package com.ljyh.mei

import com.ljyh.mei.utils.lyric.TranslationHelper
import com.ljyh.mei.utils.lyric.YRCParser
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import org.junit.Assert.*
import org.junit.Test

class YRCAndTranslationTest {

    private val yrcRaw = """{"t":0,"c":[{"tx":"作词: "},{"tx":"宇多田ヒカル","li":"http://p1.music.126.net/KuLDlZwVsGOCEHiCw___4Q==/109951170267901684.jpg","or":"orpheus://nm/artist/home?id=18122&type=artist"}]}
{"t":1000,"c":[{"tx":"编曲: "},{"tx":"宇多田ヒカル"},{"tx":"/"},{"tx":"A. G. Cook"}]}
{"t":2000,"c":[{"tx":"制作人: "},{"tx":"宇多田ヒカル"},{"tx":"/"},{"tx":"A. G. Cook"}]}
{"t":3000,"c":[{"tx":"作曲: "},{"tx":"宇多田ヒカル"}]}
[20570,1900](20570,360,0)初(20930,150,0)め(21080,120,0)て(21200,170,0)の(21370,60,0)ル(21430,190,0)ー(21620,150,0)ブ(21770,170,0)ル(21940,530,0)は
[22760,2130](22760,210,0)な(22970,160,0)ん(23130,70,0)て(23200,140,0)こ(23340,150,0)と(23490,120,0)は(23610,140,0)な(23750,240,0)か(23990,60,0)っ(24050,350,0)た(24400,490,0)わ
[24920,1650](24920,440,0)私(25360,130,0)だ(25490,140,0)け(25630,140,0)の(25770,140,0)モ(25910,130,0)ナ(26040,160,0)リ(26200,370,0)ザ"""

    private val translationLrc = """[00:20.542]第一次去卢浮宫时
[00:22.559]并没有什么特别的感觉
[00:24.765]因为独属于我的蒙娜丽莎
[00:26.713]我早已遇见
[00:29.125]初次遇见你的那天
[00:30.973]齿轮开始转动
[00:33.327]无法停止那将要失去什么的预感"""

    @Test
    fun testYRCJsonVerbatimLinesParsed() {
        val result = YRCParser.parse(yrcRaw, null)
        val lines = result.lines as List<KaraokeLine>
        assertTrue("Should parse at least 1 line, got ${lines.size}", lines.size >= 1)
        val hasMetadata = lines.any {
            it.syllables.any { s -> s.content.startsWith("作") || s.content.startsWith("編") }
        }
        assertTrue("Should have JSON metadata lines", hasMetadata)
    }

    @Test
    fun testYRCJsonVerbatimLineTiming() {
        val result = YRCParser.parse(yrcRaw, null)
        val lines = result.lines as List<KaraokeLine>

        val line0 = lines.firstOrNull { it.start == 0 }
        assertNotNull("Should have a line at t=0", line0)
        val text0 = line0!!.syllables.joinToString("") { it.content }
        assertTrue("First line should contain '作词'", text0.contains("作词"))

        val line1000 = lines.firstOrNull { it.start == 1000 }
        assertNotNull("Should have a line at t=1000", line1000)
        val text1000 = line1000!!.syllables.joinToString("") { it.content }
        assertTrue("Line at t=1000 should contain '编曲'", text1000.contains("编曲"))
    }

    @Test
    fun testYRCMainLinesParsed() {
        val result = YRCParser.parse(yrcRaw, null)
        val lines = result.lines as List<KaraokeLine>

        val yrcStartLine = lines.firstOrNull { it.start == 20570 }
        assertNotNull("Should have YRC line starting at 20570ms", yrcStartLine)

        val text = yrcStartLine!!.syllables.joinToString("") { it.content }
        assertTrue("YRC line should contain '初'", text.contains("初"))
        assertEquals("First syllable should be '初'", "初", yrcStartLine.syllables.first().content)
        assertEquals("First syllable start should be 20570", 20570, yrcStartLine.syllables.first().start)
    }

    @Test
    fun testYRCSyllableTimings() {
        val result = YRCParser.parse(yrcRaw, null)
        val lines = result.lines as List<KaraokeLine>

        val yrcLine = lines.first { it.start == 20570 }
        assertEquals("First syllable '初' start", 20570, yrcLine.syllables[0].start)
        assertEquals("Second syllable 'め' start", 20930, yrcLine.syllables[1].start)
    }

    @Test
    fun testTranslationNotAssignedToMetadata() {
        val result = YRCParser.parse(yrcRaw, translationLrc)
        val lines = result.lines as List<KaraokeLine>

        val metadataLines = lines.filter {
            it.syllables.any { s ->
                s.content.contains("作词") || s.content.contains("编曲") ||
                s.content.contains("制作人") || s.content.contains("作曲")
            }
        }

        for (metaLine in metadataLines) {
            assertNull(
                "Metadata line '${metaLine.syllables.joinToString("") { it.content }}' should not have translation, got: ${metaLine.translation}",
                metaLine.translation
            )
        }
    }

    @Test
    fun testTranslationAssignedToMainLyrics() {
        val result = YRCParser.parse(yrcRaw, translationLrc)
        val lines = result.lines as List<KaraokeLine>

        val line20570 = lines.firstOrNull { it.start == 20570 }
        assertNotNull("Should have line at 20570ms", line20570)
        assertNotNull("Line at 20570 should have translation", line20570!!.translation)
        assertEquals("第一次去卢浮宫时", line20570.translation)
    }

    @Test
    fun testEmptyYRCReturnsNoLines() {
        val result = YRCParser.parse("", null)
        val lines = result.lines as List<KaraokeLine>
        assertTrue("Empty YRC should return empty list", lines.isEmpty())
    }

    @Test
    fun testPureYRCWithoutJson() {
        val pureQrc = "[1000,2000](1000,500,0)テ(1500,500,0)ス(2000,500,0)ト"
        val result = YRCParser.parse(pureQrc, null)
        val lines = result.lines as List<KaraokeLine>

        assertEquals(1, lines.size)
        val text = lines[0].syllables.joinToString("") { it.content }
        assertEquals("テスト", text)
        assertEquals(1000, lines[0].start)
    }

    @Test
    fun testTranslationHelperIntervalMatching() {
        val yrcOnly = "[20570,1900](20570,360,0)初(20930,150,0)め\n[22760,2130](22760,210,0)な(22970,160,0)ん"
        val result = YRCParser.parse(yrcOnly, translationLrc)
        val lines = result.lines as List<KaraokeLine>

        val first = lines.first { it.start == 20570 }
        assertEquals("第一次去卢浮宫时", first.translation)

        val second = lines.first { it.start == 22760 }
        assertEquals("并没有什么特别的感觉", second.translation)
    }
}