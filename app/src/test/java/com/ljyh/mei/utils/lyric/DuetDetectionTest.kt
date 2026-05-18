package com.ljyh.mei.utils.lyric

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * 本地对唱检测集成测试 — 使用 temp/ 中完整的真实歌词 JSON 数据
 *
 * 直接内联算法逻辑，不依赖 AiLyricProcessor 实例。
 */
class DuetDetectionTest {

    private val gson = Gson()

    private fun loadLrc(filename: String): String {
        val projectRoot = File(System.getProperty("user.dir")).parentFile
        val json = File(projectRoot, "temp/$filename").readText()
        val obj = gson.fromJson(json, JsonObject::class.java)
        return obj.getAsJsonObject("lrc").get("lyric").asString
    }

    // ==================== 本地对唱检测算法（镜像 AiLyricProcessor） ====================

    private fun isDuetLikely(lrc: String): Boolean {
        return hasStandaloneRoles(lrc) || hasRolePrefixes(lrc)
    }

    private fun hasStandaloneRoles(lrc: String): Boolean {
        val lines = lrc.lines()
        val markRegex = Regex("""^\[(\d+):(\d+(?:\.\d+)?)\]\s*([A-Za-z0-9]+)[:：]\s*$""")
        val roles = mutableSetOf<String>()
        for (i in lines.indices) {
            val match = markRegex.find(lines[i].trim()) ?: continue
            val role = match.groupValues[3]
            val tsMs = match.groupValues[1].toInt() * 60 * 1000 + (match.groupValues[2].toDouble() * 1000).toLong()
            if (i + 1 < lines.size) {
                val nextMatch = Regex("""^\[(\d+):(\d+(?:\.\d+)?)\]""").find(lines[i + 1].trim())
                if (nextMatch != null) {
                    val nextTs = nextMatch.groupValues[1].toInt() * 60 * 1000 + (nextMatch.groupValues[2].toDouble() * 1000).toLong()
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

    private data class DuetSegment(val role: String, val startLine: Int, val endLine: Int)

    private fun detectDuetLocally(lrc: String): List<DuetSegment>? {
        if (hasStandaloneRoles(lrc)) {
            val r = detectStandaloneSegments(lrc)
            if (r != null) return r
        }
        if (hasRolePrefixes(lrc)) {
            val r = detectPrefixSegments(lrc)
            if (r != null) return r
        }
        return null
    }

    private fun detectStandaloneSegments(lrc: String): List<DuetSegment>? {
        val lines = lrc.lines()
        val markRegex = Regex("""^\[(\d+):(\d+(?:\.\d+)?)\]\s*([A-Za-z0-9]+)[:：]\s*$""")
        val segments = mutableListOf<DuetSegment>()
        var curRole: String? = null
        var curStart = -1
        for (i in lines.indices) {
            val m = markRegex.find(lines[i].trim())
            if (m != null) {
                val role = m.groupValues[3]
                val tsMs = m.groupValues[1].toInt() * 60 * 1000 + (m.groupValues[2].toDouble() * 1000).toLong()
                val nextIsLyric = i + 1 < lines.size &&
                    Regex("""^\[(\d+):(\d+(?:\.\d+)?)\]""").find(lines[i + 1].trim())?.let {
                        val nt = it.groupValues[1].toInt() * 60 * 1000 + (it.groupValues[2].toDouble() * 1000).toLong()
                        nt - tsMs in 1..1000
                    } ?: false
                if (nextIsLyric && role.length in 1..15 && role.any { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' }) {
                    if (curRole != null && curStart >= 0) segments.add(DuetSegment(curRole, curStart, i))
                    curRole = role
                    curStart = i + 1
                }
            }
        }
        if (curRole != null && curStart >= 0) segments.add(DuetSegment(curRole, curStart, lines.size))
        return if (segments.size >= 2) segments else null
    }

    private fun detectPrefixSegments(lrc: String): List<DuetSegment>? {
        val roleRegex = Regex("""^\[\d+:\d+(?:\.\d+)?\](?:【([^】]+)】|(\S{1,8})[：:])\s*""")
        val segments = mutableListOf<DuetSegment>()
        var curRole: String? = null
        var curStart = -1
        for ((i, line) in lrc.lines().withIndex()) {
            val m = roleRegex.find(line.trim())
            val role = m?.let { (it.groupValues[1].ifBlank { it.groupValues[2] }).trim().takeIf { r -> r.isNotBlank() } }
            if (role != null) {
                if (curRole != null && curRole != role.lowercase() && curStart >= 0) {
                    segments.add(DuetSegment(curRole, curStart, i))
                }
                if (curRole == null || curRole != role.lowercase()) curStart = i
                curRole = role.lowercase()
            }
        }
        if (curRole != null && curStart >= 0) segments.add(DuetSegment(curRole, curStart, lrc.lines().size))
        return if (segments.size >= 2) segments else null
    }

    // ==================== test_lyric_1.json ====================

    @Test
    fun test1_isDuetLikely() {
        val lrc = loadLrc("test_lyric_1.json")
        assertTrue("【Name】嵌入式应检测对唱", isDuetLikely(lrc))
    }

    @Test
    fun test1_segments_ge2() {
        val lrc = loadLrc("test_lyric_1.json")
        val duet = detectDuetLocally(lrc)
        assertNotNull("应检测到 segment", duet)
        assertTrue("应≥2, 实际=${duet?.size}", (duet?.size ?: 0) >= 2)
    }

    @Test
    fun test1_segments_print() {
        val lrc = loadLrc("test_lyric_1.json")
        val duet = detectDuetLocally(lrc)!!
        println("\n=== test_1 segments ===")
        for (seg in duet) println("  ${seg.role}: [${seg.startLine}, ${seg.endLine})")
        for (i in 1 until duet.size) {
            assertNotEquals("相邻角色不应相同: ${duet[i-1].role} vs ${duet[i].role}", duet[i-1].role, duet[i].role)
        }
    }

    // ==================== test_lyric_2.json ====================

    @Test
    fun test2_isDuetLikely() {
        val lrc = loadLrc("test_lyric_2.json")
        assertTrue("Name: 独立行应检测对唱", isDuetLikely(lrc))
    }

    @Test
    fun test2_segments_ge2() {
        val lrc = loadLrc("test_lyric_2.json")
        val duet = detectDuetLocally(lrc)
        assertNotNull("应检测到 segment", duet)
        assertTrue("应≥2, 实际=${duet?.size}", (duet?.size ?: 0) >= 2)
    }

    @Test
    fun test2_segments_print() {
        val lrc = loadLrc("test_lyric_2.json")
        val duet = detectDuetLocally(lrc)!!
        println("\n=== test_2 segments ===")
        for (seg in duet) println("  ${seg.role}: [${seg.startLine}, ${seg.endLine})")
        // 标记行独占一行，所以前一段 end 与下一段 start 间隔 1
        for (i in 1 until duet.size) {
            assertTrue("间隔应为1(标记行): end=${duet[i-1].endLine} start=${duet[i].startLine}",
                duet[i].startLine - duet[i - 1].endLine == 1)
        }
    }
}
