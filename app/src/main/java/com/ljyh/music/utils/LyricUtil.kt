package com.ljyh.music.utils

import com.ljyh.music.data.model.Lyric
import com.ljyh.music.data.model.LyricLine

object LyricUtil {
    fun getLyric(lyric: Lyric): String {
        val _lyric = lyric.lrc.lyric
        if (lyric.pureMusic == null || lyric.pureMusic == false) {
            return mergedLyric(_lyric, lyric.tlyric.lyric)
        }

        return _lyric
    }

    private fun mergedLyric(lyric: String, tlyric: String): String {
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


    fun findShowLine(lyrics: List<LyricLine>, time: Long): Int {
        return lyrics.indexOfLast { lyric ->
            lyric.time <= time
        }
    }

}


