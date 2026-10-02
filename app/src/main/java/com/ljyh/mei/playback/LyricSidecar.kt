package com.ljyh.mei.playback

import com.ljyh.mei.utils.StringUtils.specialReplace

internal fun lyricSidecarFileName(title: String, artist: String, lyric: String): String {
    val extension = if (lyric.trimStart().let { it.startsWith("<?xml") || it.startsWith("<tt") }) {
        "ttml"
    } else {
        "lrc"
    }
    return "${specialReplace("$title - $artist")}.$extension"
}
