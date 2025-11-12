package com.ljyh.mei.utils.lyric

import android.util.Log
import com.ljyh.mei.ui.component.player.component.LyricData
import com.ljyh.mei.ui.component.player.component.LyricSource
import com.ljyh.mei.ui.component.player.component.LyricSourceData
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.UncheckedSyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.LrcParser

fun createDefaultLyricData(message: String): LyricData {
    return LyricData(
        isVerbatim = false,
        lyricLine = SyncedLyrics(lines = listOf())
//        lyricLine = LrcParser.parse("[00:00.00]${message}")
    )
}

fun mergeLyrics(sources: List<LyricSourceData>): LyricData {
//    Log.d("LyricUtils", "mergeLyrics: $sources")
    // 找到 NetEase 的逐字歌词（yrc + tlyric）
    val neteaseSource = sources.filterIsInstance<LyricSourceData.NetEase>().firstOrNull()
    if (neteaseSource != null) {
        val n = neteaseSource.lyric
        if (n.yrc != null && n.tlyric != null) {
            Log.d("LyricUtils", "yrc and tlyric")
            // NetEase 逐字（最高优先）
            return LyricData(
                isVerbatim = true,
                source = LyricSource.NetEaseCloudMusic,
                lyricLine = YRCParser.parse(n.yrc.lyric, n.tlyric.lyric)
            )
        }
    }

    // 如果 NetEase 没有逐字，再看 QQ 的逐字（qq.lyric + qq.trans）
    val qqSource = sources.filterIsInstance<LyricSourceData.QQMusic>().firstOrNull()
    if (qqSource != null) {
        val q = qqSource.lyric
        if (q.lyric.isNotBlank() && q.trans.isNotBlank()) {
            Log.d("LyricUtils", "qq.lyric and qq.trans")
            return LyricData(
                isVerbatim = true,
                source = LyricSource.QQMusic,
                lyricLine = QRCParser.parse(q.lyric, q.trans)
            )
        }
        // 有些情况下 QQ 有逐字但没有 trans（视为非完整逐字），上面判断要求 trans 存在
    }

    // ===== 没有任何逐字歌词，按源优先回退 =====
    // 1) NetEase 非逐字（LRC）
    if (neteaseSource != null ) {
        val n = neteaseSource.lyric
        if (n.lrc.lyric.isNotBlank() &&  n.tlyric?.lyric!= null) {
            Log.d("LyricUtils", "NetEase LRC")
            return LyricData(
                isVerbatim = false,
                source = LyricSource.NetEaseCloudMusic,
                lyricLine = LRCParser.parse(n.lrc.lyric, n.tlyric.lyric)
            )
        }
    }

    // 2) QQ 非逐字（QRC 没有 trans，或只有 lyric）
    if (qqSource != null) {
        val q = qqSource.lyric
        if (q.lyric.isNotBlank()) {
            // 如果有 trans 也可以用，但既然已经判定过逐字缺失，这里当作非逐字解析
            Log.d("LyricUtils", "QQ LRC")
            return LyricData(
                isVerbatim = false,
                source = LyricSource.QQMusic,
                lyricLine = QRCParser.parse(q.lyric, q.trans) // trans 可能为空
            )
        }
    }

    // 都没有，返回默认
    Log.d("LyricUtils", "no lyric")
    return createDefaultLyricData("暂无歌词")
}


fun String.parseAsTime(): Int {
    fun parseSecondsAndMillis(part: String): Int {
        val timeParts = part.split('.', limit = 2)
        val seconds = timeParts[0].toIntOrNull()?.times(1000) ?: 0

        // If there's no millisecond part, return seconds only.
        if (timeParts.size == 1) return seconds

        val millisStr = timeParts[1]
        // Pad the string to 3 digits on the right with '0'.
        // "4" -> "400", "45" -> "450", "456" -> "456"
        // Then take the first 3 characters to handle overly long inputs like "4567".
        val normalizedMillisStr = millisStr.padEnd(3, '0').substring(0, 3)
        val millis = normalizedMillisStr.toIntOrNull() ?: 0

        return seconds + millis
    }

    return try {
        val parts = this.split(":")
        when (parts.size) {
            3 -> { // Format: HH:MM:SS.ms
                val hours = parts[0].toIntOrNull()?.times(3600 * 1000) ?: 0
                val minutes = parts[1].toIntOrNull()?.times(60 * 1000) ?: 0
                val secondsAndMillis = parseSecondsAndMillis(parts[2])
                hours + minutes + secondsAndMillis
            }
            2 -> { // Format: MM:SS.ms
                val minutes = parts[0].toIntOrNull()?.times(60 * 1000) ?: 0
                val secondsAndMillis = parseSecondsAndMillis(parts[1])
                minutes + secondsAndMillis
            }
            1 -> { // Format: SS.ms
                parseSecondsAndMillis(parts[0])
            }
            else -> 0
        }
    } catch (_: Exception) {
        0
    }
}
