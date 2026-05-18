package com.ljyh.mei.utils.lyric

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import org.junit.Assert.*
import org.junit.Test

class YRCParserTest {

    private val yrcLyrics = """
        {"t":0,"c":[{"tx":"作词: "},{"tx":"Hiroyuki Sawano"}]}
        {"t":1000,"c":[{"tx":"作曲: "},{"tx":"Hiroyuki Sawano"}]}
        [10000,10630](10000,5315,0)mizuki(15315,5315,0):
        [20630,3720](20630,230,0)手(20860,120,0)を
        [24510,5400](24510,250,0)気(24760,140,0)を
        [30630,3460](30630,400,0)君(31030,270,0)は
        [40650,30](40650,15,0)naNami(40665,15,0)：
        [40680,4640](40680,80,0)こ(40760,100,0)の
        [57050,1510](57050,750,0)All(57800,760,0):
        [58560,1100](58560,275,0)Let (58835,275,0)it (59110,275,0)shine
        [100640,0](100640,0,0)Yosh(100640,0,0):
        [100640,3690](100640,340,0)Wasn't (100980,500,0)feeling
    """.trimIndent()

    @Test
    fun yrcParsedLines_areMonotonic() {
        val result = YRCParser.parse(yrcLyrics, null)

        println("Parsed ${result.lines.size} lines:")
        var lastStart = -1
        for ((i, line) in result.lines.withIndex()) {
            val kara = line as? KaraokeLine ?: continue
            val text = kara.syllables.joinToString("") { it.content }
            println("  L$i: start=${kara.start} align=${kara.alignment} text=${text.take(60)}")

            if (lastStart >= 0) {
                assertTrue("Line $i time went backwards: $lastStart → ${kara.start}", kara.start >= lastStart)
            }
            lastStart = kara.start
        }
    }

    @Test
    fun roleMarkerLines_arePresent() {
        val result = YRCParser.parse(yrcLyrics, null)
        val markers = result.lines.filter { line ->
            (line as? KaraokeLine)?.syllables?.joinToString("") { it.content }?.trim()?.let {
                it.matches(Regex("""^[A-Za-z0-9]+[:：]\s*$"""))
            } ?: false
        }
        println("Role marker lines: ${markers.size}")
        markers.forEach {
            val k = it as KaraokeLine
            println("  start=${k.start} text=${k.syllables.joinToString("") { it.content }} alignment=${k.alignment}")
        }
        assertTrue("Should have role marker lines", markers.isNotEmpty())
    }

    @Test
    fun allAlignments_areUnspecified() {
        val result = YRCParser.parse(yrcLyrics, null)
        result.lines.forEach { line ->
            if (line is KaraokeLine.MainKaraokeLine) {
                assertEquals("Line at ${line.start} should be Unspecified", KaraokeAlignment.Unspecified, line.alignment)
            }
        }
    }

    @Test
    fun duplicateTimestamps_Yosh() {
        val result = YRCParser.parse(yrcLyrics, null)
        val yoshLines = result.lines.filter {
            val text = (it as? KaraokeLine)?.syllables?.joinToString("") { it.content } ?: ""
            text.contains("Yosh") || text.contains("Wasn't")
        }
        println("Lines near Yosh:")
        yoshLines.forEach {
            val k = it as KaraokeLine
            println("  start=${k.start} text=${k.syllables.joinToString("") { it.content }}")
        }
        val starts = yoshLines.map { (it as KaraokeLine).start }
        for (i in 1 until starts.size) {
            assertTrue("Yosh lines not monotonic", starts[i] >= starts[i - 1])
        }
    }
}
