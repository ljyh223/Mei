package com.ljyh.music.data.model

import com.google.gson.annotations.SerializedName
import com.ljyh.music.data.model.LyricUtils.mergedLyric
import com.ljyh.music.ui.component.player.LyricLine
import com.ljyh.music.ui.component.player.LyricLineA
import com.ljyh.music.ui.component.player.LyricWord
import java.util.regex.Pattern
import kotlin.math.abs


data class Lyric(
    @SerializedName("code")
    val code: Int,
    @SerializedName("klyric")
    val klyric: Klyric,
    @SerializedName("lrc")
    val lrc: Lrc,
    @SerializedName("qfy")
    val qfy: Boolean,
    @SerializedName("romalrc")
    val romalrc: Romalrc,
    @SerializedName("sfy")
    val sfy: Boolean,
    @SerializedName("sgc")
    val sgc: Boolean,
    @SerializedName("tlyric")
    val tlyric: Tlyric?,
    @SerializedName("yrc")
    val yrc: Yrc?,
    @SerializedName("pureMusic")
    val pureMusic: Boolean?,
) {
    data class Klyric(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )

    data class Lrc(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )

    data class Romalrc(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )

    data class Tlyric(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )

    data class Yrc(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )
}


/**
 * SaltPlayerSource  Copyright (C) 2021  Moriafly
 * This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
 * This is free software, and you are welcome to redistribute it
 * under certain conditions; type `show c' for details.
 *
 * The hypothetical commands `show w' and `show c' should show the appropriate
 * parts of the General Public License.  Of course, your program's commands
 * might be different; for a GUI interface, you would use an "about box".
 *
 * You should also get your employer (if you work as a programmer) or school,
 * if any, to sign a "copyright disclaimer" for the program, if necessary.
 * For more information on this, and how to apply and follow the GNU GPL, see
 * <https://www.gnu.org/licenses/>.
 *
 * The GNU General Public License does not permit incorporating your program
 * into proprietary programs.  If your program is a subroutine library, you
 * may consider it more useful to permit linking proprietary applications with
 * the library.  If this is what you want to do, use the GNU Lesser General
 * Public License instead of this License.  But first, please read
 * <https://www.gnu.org/licenses/why-not-lgpl.html>.
 */
object LyricUtils {
    private val PATTERN_TIME = Pattern.compile("\\[(\\d\\d):(\\d\\d)\\.(\\d{1,6})]")

    // 行匹配
    private val PATTERN_LINE = Pattern.compile("((\\[\\d\\d:\\d\\d\\.\\d{1,6}])+)(.+)")

    private const val MINUTE_IN_MILLIS = 60_000L
    private const val SECOND_IN_MILLIS = 1000L
    fun parseLine(lyricsString: String): LyricLineA? {
        var lyrics = lyricsString
        // 如果为空
        if (lyrics.isEmpty()) return null
        lyrics = lyrics.trim { it <= ' ' }
        // [00:24.61]雪花飘 青山遇绝壁
        val lineMatcher = PATTERN_LINE.matcher(lyrics)
        if (!lineMatcher.matches()) {
            return null
        }
        val times = lineMatcher.group(1) ?: ""
        val text = lineMatcher.group(3) ?: ""
        if (text.first() == '[') return null
        // [00:17.65]
        val timeMatcher = PATTERN_TIME.matcher(times)
        if (timeMatcher.find()) {
            val min = (timeMatcher.group(1) ?: "").toLong()
            val sec = (timeMatcher.group(2) ?: "").toLong()
            val milString = timeMatcher.group(3) ?: ""
            var mil = milString.toLong()
            // 如果毫秒是两位数，需要乘以 10
            when (milString.length) {
                1 -> mil *= 100
                2 -> mil *= 10
                4 -> mil /= 10
                5 -> mil /= 100
                6 -> mil /= 1000
            }
            val time = min * MINUTE_IN_MILLIS + sec * SECOND_IN_MILLIS + mil
            return LyricLineA(
                lyric = text,
                time = time
            )
        }
        return null
    }

    fun mergedLyric(lyric: String, tlyric: String): String {
        // 去除末尾的空白字符
        val lyricT = lyric.trim()
        val tlyricT = tlyric.trim()

        // 使用 HashMap 存储翻译歌词，键为时间，值为对应的歌词
        val tlyricMap = mutableMapOf<String, String>()

        // 处理翻译歌词，每行按 "]" 分割，存储到 HashMap
        tlyricT.lines().forEach { line ->
            val parts = line.split("]", limit = 2)
            if (parts.size == 2) {
                var time = parts[0].removePrefix("[") // 提取时间
                if (countSpecificCharacter(time) > 1) {
                    time = replaceLastOccurrenceWithStringBuilder(time, ':', '.')
                }
                val text = parts[1] // 提取歌词
                tlyricMap[time] = text
            }
        }

        val merged = ArrayList<String>()

        // 处理主歌词
        lyricT.lines().forEach { line ->
            if (line.first() == '[' && line.last() == ']') {
                merged.add(line)
                return@forEach
            }
            val parts = line.split("]", limit = 2)
            if (parts.size == 2) {
                var time = parts[0].removePrefix("[") // 提取时间
                val text = parts[1] // 提取歌词

                if (countSpecificCharacter(time) > 1) {
                    time = replaceLastOccurrenceWithStringBuilder(time, ':', '.')
                }
                // 如果翻译歌词没有对应时间戳，直接添加原歌词
                if (time !in tlyricMap) {
                    merged.add("[$time]$text")
                } else {
                    // 否则，合并两行歌词
                    merged.add("[$time]$text")
                    merged.add("[$time]${tlyricMap[time]}")
                }
            }
        }
        return merged.joinToString("\n")
    }


    private fun countSpecificCharacter(str: String): Int {
        return str.count { it == ':' }
    }

    private fun replaceLastOccurrenceWithStringBuilder(
        str: String,
        target: Char,
        replacement: Char
    ): String {
        val lastIndex = str.lastIndexOf(target)
        return if (lastIndex != -1) {
            StringBuilder(str).apply { setCharAt(lastIndex, replacement) }.toString()
        } else {
            str
        }
    }


}

fun Lyric.parse(): MutableList<LyricLine> {
    val lines = mutableListOf<LyricLine>()
    lrc.lyric.split("\n")
        .filter {
            it.matches(Regex("\\[\\d+:\\d+.\\d+].+"))
        }.forEach {
            LyricUtils.parseLine(it)?.let { e ->
                LyricLine(
                    e.lyric,
                    e.time,
                    0L,
                    emptyList(),
                )
            }
        }

    // 将翻译添加到歌词中
    tlyric?.lyric?.split("\n")?.filter {
        it.matches(Regex("\\[\\d+:\\d+.\\d+].+"))
    }?.forEach {
        LyricUtils.parseLine(it)?.let { e ->
            lines.find { lyric -> lyric.startTimeMs == e.time }?.translation =
                e.lyric
        }
    }
    return lines
}

fun Lyric.parseString(): String {
    if ((pureMusic == null || pureMusic == false) && tlyric != null) {
        return mergedLyric(lrc.lyric, tlyric.lyric)
    }

    return lrc.lyric
}

fun Lyric.parseYrc(): List<LyricLine> {

    if (yrc == null) {
        val lines = mutableListOf<LyricLine>()
        lrc.lyric.split("\n")
            .filter {
                it.matches(Regex("\\[\\d+:\\d+.\\d+].+"))
            }.forEach {
                LyricUtils.parseLine(it)?.let { e ->
                    lines.add(
                        LyricLine(
                            e.lyric,
                            e.time,
                            0L,
                            emptyList(),
                        )
                    )
                }
            }

        // 将翻译添加到歌词中
        tlyric?.lyric?.split("\n")?.filter {
            it.matches(Regex("\\[\\d+:\\d+.\\d+].+"))
        }?.forEach {
            LyricUtils.parseLine(it)?.let { e ->
                lines.find { lyric -> lyric.startTimeMs == e.time }?.translation =
                    e.lyric
            }
        }
        return lines
    }

    val regex = "\\[(\\d+),(\\d+)\\](.*)".toRegex()
    val wordRegex = "\\((\\d+),(\\d+),\\d+\\)([^\\(\\)]*)".toRegex()

    // ✅ 解析逐字歌词
    val mYrc = yrc.lyric.lineSequence()
        .mapNotNull { line ->
            val match = regex.matchEntire(line) ?: return@mapNotNull null
            val (lineStart, lineDuration, wordsText) = match.destructured

            val words = wordRegex.findAll(wordsText)
                .map { wordMatch ->
                    val (wordStart, wordDuration, wordText) = wordMatch.destructured
                    LyricWord(
                        startTimeMs = wordStart.toLong(),
                        durationMs = wordDuration.toLong(),
                        text = wordText
                    )
                }
                .toList()

            LyricLine(
                lyric = words.joinToString("") { it.text },
                startTimeMs = lineStart.toLong(),
                durationMs = lineDuration.toLong(),
                words = words
            )
        }
        .toList()

    // ✅ 解析翻译歌词
    val translations = tlyric?.lyric?.lineSequence()
        ?.mapNotNull { line -> LyricUtils.parseLine(line) }
        ?.toList() ?: emptyList()
    if (translations.isEmpty()) return mYrc
    println("有翻译歌词")
    // ✅ 用双指针优化匹配翻译歌词（O(N) 复杂度）
    var j = 0 // 翻译歌词的索引
    for (i in mYrc.indices) {
        val lyricLine = mYrc[i]

        while (j < translations.size - 1 &&
            abs(translations[j].time - lyricLine.startTimeMs) > abs(translations[j + 1].time - lyricLine.startTimeMs)
        ) {
            j++
        }

        if (j < translations.size) {
            lyricLine.translation = translations[j].lyric
        }
    }
    println(mYrc)
    return mYrc
}
