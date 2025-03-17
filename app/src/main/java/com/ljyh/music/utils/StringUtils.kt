package com.ljyh.music.utils

import java.net.URLEncoder

object StringUtils {

    fun specialReplace(s: String): String {
        val replacements = listOf(
            "<" to "＜",
            ">" to "＞",
            "\\" to "＼",
            "/" to "／",
            ":" to "：",
            "?" to "",
            "*" to "＊",
            "\"" to "＂",
            "|" to "｜",
            "..." to " "
        )

        var mutableString = s
        for ((original, replacement) in replacements) {
            mutableString = mutableString.replace(original, replacement)
        }
        return mutableString
    }

    fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")
}