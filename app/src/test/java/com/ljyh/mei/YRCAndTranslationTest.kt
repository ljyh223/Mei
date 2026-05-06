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

    // ======================== Ruby text (furigana) tests ========================

    private val yrcWithRuby = """{"t":0,"c":[{"tx":"作词: "},{"tx":"wowaka"}]}
{"t":1000,"c":[{"tx":"作曲: "},{"tx":"wowaka"}]}
[40320,3790](40320,631,0)ゆ(40951,631,0)ら(41582,631,0)り(42213,631,0)ふ(42844,631,0)ら(43475,635,0)り
[52040,8070](52040,510,0)足(52550,840,0)元(53390,30,0)（(53420,30,0)あ(53450,30,0)し(53480,30,0)も(53510,30,0)と(53540,40,0)）(53580,3000,0)は(56580,2310,0)覚(58890,430,0)束(59320,0,0)（(59320,0,0)お(59320,0,0)ぼ(59320,0,0)つ(59320,0,0)か(59320,0,0)）(59320,380,0)な(59700,410,0)い
[61900,3720](61900,1040,0)ゆ(62940,360,0)ら(63300,510,0)ゆ(63810,540,0)ら(64350,1270,0)り"""

    private val ytlrcForRubySong = """[00:40.320]【摇摇晃晃】
[00:52.040]【没有注意脚下】
[01:01.900]【摇摇晃晃】"""

    private val translationLrcForRuby = """[by:_以太_]
[00:39.980]【摇摇晃晃】
[00:45.550]【飘飘荡荡】
[00:50.900]【没有注意脚下】"""

    @Test
    fun testYRCWithRubyTextParsesLines() {
        val result = YRCParser.parse(yrcWithRuby, null)
        val lines = result.lines as List<KaraokeLine>
        assertTrue("Should parse lines from YRC with ruby text, got ${lines.size}", lines.size >= 3)
    }

    @Test
    fun testYRCWithRubyTextContent() {
        val result = YRCParser.parse(yrcWithRuby, null)
        val lines = result.lines as List<KaraokeLine>

        // The "足元" line at start=52040 should contain furigana inline
        val rubyLine = lines.firstOrNull { it.start == 52040 }
        assertNotNull("Should have line at 52040ms", rubyLine)
        val text = rubyLine!!.syllables.joinToString("") { it.content }
        // With current implementation, ruby text appears inline: "足元（あしもと）は..."
        assertTrue("Ruby line should contain kanji '足元'", text.contains("足元"))
        // Ruby furigana characters also appear (known limitation)
        assertTrue("Ruby line contains furigana 'あしもと'", text.contains("あしもと"))
    }

    @Test
    fun testYRCWithRubyTextTranslationAssigned() {
        val result = YRCParser.parse(yrcWithRuby, translationLrcForRuby)
        val lines = result.lines as List<KaraokeLine>

        // Metadata at t=0 should NOT get a translation
        val metaLine = lines.firstOrNull { it.start == 0 }
        assertNotNull("Should have metadata line at t=0", metaLine)
        assertNull("Metadata line should not have translation", metaLine!!.translation)

        // The first lyric line at 40320 should get translation if matched
        val firstLyric = lines.firstOrNull { it.start == 40320 }
        assertNotNull("Should have lyric line at 40320", firstLyric)
    }

    @Test
    fun testYRCWithYtlrcTranslationAssigned() {
        // ytlrc format uses LRC syntax, directly passable as translation
        val result = YRCParser.parse(yrcWithRuby, ytlrcForRubySong)
        val lines = result.lines as List<KaraokeLine>

        val yuraLine = lines.firstOrNull { it.start == 40320 }
        assertNotNull("Should have line at 40320ms", yuraLine)
        assertEquals("【摇摇晃晃】", yuraLine!!.translation)

        val ashiLine = lines.firstOrNull { it.start == 52040 }
        assertNotNull("Should have line at 52040ms", ashiLine)
        assertEquals("【没有注意脚下】", ashiLine!!.translation)
    }

    @Test
    fun testYRCWithRubyTextZeroDurationSyllables() {
        // Furigana like "（おぼつか）" has 0ms duration syllables
        // These should still be parsed but have start == end (invisible for animation)
        val result = YRCParser.parse(yrcWithRuby, null)
        val lines = result.lines as List<KaraokeLine>

        val rubyLine = lines.first { it.start == 52040 }
        // "束" is followed by "（おぼつか）" with 0 duration
        // Find the zero-duration syllables
        val zeroDurSyllables = rubyLine.syllables.filter { it.start == it.end }
        assertTrue("Should have zero-duration furigana syllables", zeroDurSyllables.isNotEmpty())

        // Base kanji "足" should have proper timing
        val ashiSyllable = rubyLine.syllables.firstOrNull { it.content == "足" }
        assertNotNull("Should have '足' syllable", ashiSyllable)
        assertEquals("足 start time", 52040, ashiSyllable!!.start)
        assertTrue("足 should have duration > 0", ashiSyllable.end > ashiSyllable.start)
    }

    // ======================== Edge cases ========================

    @Test
    fun testYRCWithNullTranslation() {
        val result = YRCParser.parse(yrcRaw, null)
        val lines = result.lines as List<KaraokeLine>
        assertTrue("All lines should have null translation when none provided",
            lines.all { it.translation == null })
    }

    @Test
    fun testYRCWithEmptyTranslation() {
        val result = YRCParser.parse(yrcRaw, "")
        val lines = result.lines as List<KaraokeLine>
        assertTrue("All lines should have null translation when empty string provided",
            lines.all { it.translation == null })
    }

    @Test
    fun testYRCWithBlankTranslation() {
        val result = YRCParser.parse(yrcRaw, "   \n  ")
        val lines = result.lines as List<KaraokeLine>
        assertTrue("All lines should have null translation when whitespace only",
            lines.all { it.translation == null })
    }

    @Test
    fun testYRCWithOnlyHeaderTranslation() {
        // Translation with only [by:xxx] header and no actual translation lines
        val translationOnlyHeader = "[by:someone]"
        val result = YRCParser.parse(yrcRaw, translationOnlyHeader)
        val lines = result.lines as List<KaraokeLine>
        assertTrue("No line should get translation from header-only data",
            lines.all { it.translation == null })
    }

    @Test
    fun testYRCTranslationTimestampEdgeCases() {
        // Translation lines with exact boundary timestamps
        val preciseYrc = "[20100,900](20100,300,0)A(20400,300,0)B(20700,300,0)C"
        val preciseTrans = "[00:20.100]test A\n[00:20.700]test C\n[00:22.000]test D"

        val result = YRCParser.parse(preciseYrc, preciseTrans)
        val lines = result.lines as List<KaraokeLine>

        assertEquals("Should match translation at 20100", "test A", lines[0].translation)
    }

    @Test
    fun testYRCTranslationWithIgnoreKeywords() {
        // Translation with lines that should be ignored (copyright, etc.)
        val transWithIgnore = """[00:20.500]real translation
[00:22.500]Provided by record label
[00:24.500]another translation"""
        val shortYrc = "[20500,1700](20500,300,0)x(20800,300,0)y"

        val result = YRCParser.parse(shortYrc, transWithIgnore)
        val lines = result.lines as List<KaraokeLine>

        assertEquals("Should skip 'Provided by' line and match correct translation",
            "real translation", lines[0].translation)
    }

    @Test
    fun testYRCTranslationWithCommentLines() {
        // Translation with lines starting with // should be ignored
        val transWithComment = """[00:20.500]// This is a comment
[00:22.500]good translation"""
        val shortYrc = "[22500,1700](22500,300,0)x(22800,300,0)y"

        val result = YRCParser.parse(shortYrc, transWithComment)
        val lines = result.lines as List<KaraokeLine>

        assertEquals("Should match non-comment translation", "good translation", lines[0].translation)
    }
}