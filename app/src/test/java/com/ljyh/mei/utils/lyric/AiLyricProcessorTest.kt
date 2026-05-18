package com.ljyh.mei.utils.lyric

import org.junit.Assert.*
import org.junit.Test

/**
 * AiLyricProcessor 流水线本地逻辑测试
 *
 * 测试 determineWinnerLocal / isDuetLikely / isTranslationSerious 的算法行为，
 * 无需网络或 Hilt 依赖，可直接运行。
 */
class AiLyricProcessorTest {

    // ==================== Step 1: determineWinnerLocal ====================

    @Test
    fun bothHaveVerbatim_neteaseWins() {
        assertEquals("netease", determineWinnerLocal(makeYrc(), makeQrc(), null, null))
    }

    @Test
    fun onlyNeteaseHasVerbatim_neteaseWins() {
        assertEquals("netease", determineWinnerLocal(makeYrc(), null, null, null))
    }

    @Test
    fun onlyQqHasVerbatim_qqWins() {
        assertEquals("qq", determineWinnerLocal(null, makeQrc(), null, null))
    }

    @Test
    fun bothHaveLineLevel_neteaseWins() {
        assertEquals("netease", determineWinnerLocal(null, null, "lrc", "lrc"))
    }

    @Test
    fun onlyNeteaseHasLineLevel_neteaseWins() {
        assertEquals("netease", determineWinnerLocal(null, null, "lrc", null))
    }

    @Test
    fun onlyQqHasLineLevel_qqWins() {
        assertEquals("qq", determineWinnerLocal(null, null, null, "lrc"))
    }

    @Test
    fun noLyrics_returnsNull() {
        assertNull(determineWinnerLocal(null, null, null, null))
    }

    @Test
    fun neteaseHasVerbatim_qQHasLine_neteaseWins() {
        assertEquals("netease", determineWinnerLocal(makeYrc(), null, "netLine", "qqLine"))
    }

    // ==================== Step 2: isDuetLikely ====================

    @Test
    fun multiRoleDuet_returnsTrue() {
        val lrc = """
            [00:20.23]mizuki:
            [00:20.24]手を止めた一秒で変わるはずないと
            [00:24.19]気を緩めた後ろで
            [00:40.32]naNami：
            [00:40.33]このままーっの物語抱いて
            [00:58.27]All:
            [00:58.28]Let it shine!
        """.trimIndent()
        assertTrue(isDuetLikely(lrc))
    }

    @Test
    fun twoRoleDuet_returnsTrue() {
        val lrc = """
            [00:15.00]male:
            [00:15.10]你说爱像云
            [00:20.00]female:
            [00:20.10]要自在飘浮才美丽
        """.trimIndent()
        assertTrue(isDuetLikely(lrc))
    }

    @Test
    fun singleRoleMark_returnsFalse() {
        val lrc = """
            [00:15.00]male:
            [00:15.10]手を止めた一秒で
            [00:20.00]male:
            [00:20.10]気を緩めた後ろで
        """.trimIndent()
        assertFalse(isDuetLikely(lrc))
    }

    @Test
    fun noRoleMarks_returnsFalse() {
        val lrc = """
            [00:15.50]你说爱像云
            [00:19.20]要自在飘浮才美丽
            [00:23.40]我终于明白
        """.trimIndent()
        assertFalse(isDuetLikely(lrc))
    }

    @Test
    fun composerColonNotDuet_returnsFalse() {
        val lrc = """
            [00:00.85]Composer:
            [00:03.41]一歩ずつ踏み出した感情論
        """.trimIndent()
        assertFalse(isDuetLikely(lrc))
    }

    @Test
    fun normalLrcWithoutDuet_returnsFalse() {
        val lrc = """
            [00:15.50]你说爱像云
            [00:19.20]要自在飘浮才美丽
            [00:23.40]我终于明白
            [00:27.60]你给的承诺
            [00:31.80]像风一样轻盈
            [00:36.00]却飘向了远方
        """.trimIndent()
        assertFalse(isDuetLikely(lrc))
    }

    @Test
    fun embeddedBracketRoles_returnsTrue() {
        val lrc = """
            [03:03.930]【クロハ】これが运命なんだよ
            [03:07.000]【ヒビヤ】あぶないっヒヨリ！
            [03:08.150]【ヒヨリ】ヒビヤァっ
            [03:10.200]【榎本貴音】遥っ、死じゃやだよ
            [03:14.100]【カノ】やぁ、おはよう
        """.trimIndent()
        assertTrue(isDuetLikely(lrc))
    }

    @Test
    fun embeddedColonRoles_returnsTrue() {
        val lrc = """
            [03:28.650]マリー：痛いくらいに现实は
            [03:40.300]カノ：选んだ今日は平凡で
            [04:11.180]合：进もう」
        """.trimIndent()
        assertTrue(isDuetLikely(lrc))
    }

    @Test
    fun singleBracketRole_returnsFalse() {
        val lrc = """
            [03:03.930]【クロハ】これが运命なんだよ
            [03:07.000]【クロハ】あぶないっ！
            [03:08.150]【クロハ】ヒビヤァっ
        """.trimIndent()
        assertFalse(isDuetLikely(lrc))
    }

    // ==================== Step 3: isTranslationSerious ====================

    @Test
    fun translationIsNull_serious() {
        assertTrue(isTranslationSerious(makeSimpleLrc(), null))
    }

    @Test
    fun translationIsEmpty_serious() {
        assertTrue(isTranslationSerious(makeSimpleLrc(), ""))
    }

    @Test
    fun translationLinesLessThan50percent_serious() {
        val lrc = """
            [00:01.00]line1
            [00:02.00]line2
            [00:03.00]line3
            [00:04.00]line4
            [00:05.00]line5
        """.trimIndent()
        val trans = """
            [00:01.00]t1
            [00:02.00]t2
        """.trimIndent()
        assertTrue(isTranslationSerious(lrc, trans))
    }

    @Test
    fun translationLinesAbove50percent_notSerious() {
        val lrc = """
            [00:01.00]line1
            [00:02.00]line2
            [00:03.00]line3
            [00:04.00]line4
            [00:05.00]line5
        """.trimIndent()
        val trans = """
            [00:01.00]t1
            [00:02.00]t2
            [00:03.00]t3
        """.trimIndent()
        assertFalse(isTranslationSerious(lrc, trans))
    }

    @Test
    fun completeTranslation_notSerious() {
        val lrc = makeSimpleLrc()
        assertFalse(isTranslationSerious(lrc, makeSimpleLrc()))
    }

    @Test
    fun emptyLinesFiltered_correctCount() {
        val lrc = """
            [00:01.00]line1
            [00:02.00]
            [00:03.00]line3
        """.trimIndent()
        val trans = """
            [00:01.00]t1
        """.trimIndent()
        assertFalse(isTranslationSerious(lrc, trans))
    }

    // ==================== 综合场景 ====================

    @Test
    fun neteaseVerbatim_qqVerbatim_noDuet_noTranslation_winnerNetease() {
        assertEquals("netease", determineWinnerLocal(makeYrc(), makeQrc(), null, null))
        assertFalse(isDuetLikely(""))
        assertTrue(isTranslationSerious(makeSimpleLrc(), null))
    }

    @Test
    fun neteaseLine_qqLine_hasDuet_noTranslation_winnerNetease() {
        assertEquals("netease", determineWinnerLocal(null, null, "netease lrc", "qq lrc"))

        val duetLrc = """
            [00:20.23]mizuki:
            [00:20.24]手を止めた
            [00:40.32]naNami：
            [00:40.33]このままーっの
        """.trimIndent()
        assertTrue(isDuetLikely(duetLrc))
        assertTrue(isTranslationSerious(makeSimpleLrc(), null))
    }

    @Test
    fun qqVerbatim_neteaseLineOnlyWithDuet_qqWins_duetDetectable() {
        assertEquals("qq", determineWinnerLocal(null, makeQrc(), "netLrc", null))

        val duetLrc = """
            [00:20.23]mizuki:
            [00:20.24]手を止めた
            [00:40.32]naNami：
            [00:40.33]このままーっの
        """.trimIndent()
        assertTrue(isDuetLikely(duetLrc))
    }

    // ==================== Helpers (mirror the local logic) ====================

    private fun determineWinnerLocal(
        netVerbatim: String?, qqVerbatim: String?,
        netLine: String?, qqLine: String?
    ): String? {
        val hasNetVerb = netVerbatim != null
        val hasQqVerb = qqVerbatim != null

        if (hasNetVerb || hasQqVerb) {
            return if (hasNetVerb) "netease" else "qq"
        }

        val hasNetLine = netLine != null
        val hasQqLine = qqLine != null

        if (hasNetLine || hasQqLine) {
            return when {
                hasNetLine && hasQqLine -> "netease"
                hasNetLine -> "netease"
                else -> "qq"
            }
        }

        return null
    }

    private fun isDuetLikely(lrc: String): Boolean {
        if (hasStandaloneRoles(lrc)) return true
        if (hasRolePrefixes(lrc)) return true
        return false
    }

    private fun hasStandaloneRoles(lrc: String): Boolean {
        val lines = lrc.lines()
        val markRegex = Regex("""^\[(\d+):(\d+(?:\.\d+)?)\]\s*([A-Za-z0-9]+)[:：]\s*$""")
        val roles = mutableSetOf<String>()
        for (i in lines.indices) {
            val match = markRegex.find(lines[i].trim()) ?: continue
            val role = match.groupValues[3]
            val min = match.groupValues[1].toInt()
            val sec = match.groupValues[2].toDouble()
            val tsMs = min * 60 * 1000 + (sec * 1000).toLong()
            if (i + 1 < lines.size) {
                val nextMatch = Regex("""^\[(\d+):(\d+(?:\.\d+)?)\]""").find(lines[i + 1].trim())
                if (nextMatch != null) {
                    val nextMin = nextMatch.groupValues[1].toInt()
                    val nextSec = nextMatch.groupValues[2].toDouble()
                    val nextTs = nextMin * 60 * 1000 + (nextSec * 1000).toLong()
                    if (nextTs - tsMs <= 1000) roles.add(role.lowercase())
                }
            }
        }
        return roles.size >= 2
    }

    private fun hasRolePrefixes(lrc: String): Boolean {
        val roleRegex = Regex("""^\[\d+:\d+(?:\.\d+)?\](?:【([^】]+)】|(\S{1,8})[：:])\s*""")
        val roles = mutableSetOf<String>()
        for (line in lrc.lines()) {
            val match = roleRegex.find(line.trim()) ?: continue
            val role = (match.groupValues[1].ifBlank { match.groupValues[2] }).trim()
            if (role.isNotBlank()) roles.add(role.lowercase())
        }
        return roles.size >= 2
    }

    private fun isTranslationSerious(lrc: String, translation: String?): Boolean {
        if (translation.isNullOrBlank()) return true
        val lrcCount = countLrcLines(lrc)
        val transCount = countLrcLines(translation)
        return transCount < lrcCount * 0.5
    }

    private fun countLrcLines(text: String): Int {
        return text.lines().count { line ->
            val trimmed = line.trim()
            trimmed.matches(Regex("""^\[\d+:\d+(\.\d+)?\].+"""))
        }
    }

    private fun makeYrc() = """{"t":0,"c":[{"tx":"test"}]}"""
    private fun makeQrc() = "[1000,2000]test(1000,1000)"

    private fun makeSimpleLrc() = """
        [00:01.00]line1
        [00:02.00]line2
        [00:03.00]line3
    """.trimIndent()
}
