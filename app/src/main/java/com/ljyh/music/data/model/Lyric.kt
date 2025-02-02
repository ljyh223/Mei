package com.ljyh.music.data.model
import com.google.gson.annotations.SerializedName
import java.util.regex.Pattern


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
    val tlyric: Tlyric,
    @SerializedName("pureMusic")
    val pureMusic: Boolean?


) {

    companion object {
        const val LYRICS_NOT_FOUND = "LYRICS_NOT_FOUND"
    }
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
    fun parseLine(lyricsString: String): LyricLine? {
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
        if(text.first()=='[') return null
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
            return LyricLine(time, text, null)
        }
        return null
    }
}

fun Lyric.parse(): List<LyricLine> {
    val lines = mutableListOf<LyricLine>()
    lrc.lyric.split("\n")
        .filter {
            it.matches(Regex("\\[\\d+:\\d+.\\d+].+"))
        }.forEach {
            LyricUtils.parseLine(it)?.let { e ->
                lines.add(e)
            }
        }

    // 将翻译添加到歌词中
    tlyric?.lyric?.split("\n")?.filter {
        it.matches(Regex("\\[\\d+:\\d+.\\d+].+"))
    }?.forEach {
        LyricUtils.parseLine(it)?.let { e ->
            lines.find { lyric -> lyric.time == e.time }?.translation =
                e.lyric
        }
    }
    return lines
}

data class LyricLine(
    val time: Long,
    val lyric: String,
    var translation: String?
)