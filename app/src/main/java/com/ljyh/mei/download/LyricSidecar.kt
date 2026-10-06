package com.ljyh.mei.download

internal fun lyricSidecarFileName(title: String, artist: String, lyric: String): String {
    val extension = if (lyric.trimStart().let { it.startsWith("<?xml") || it.startsWith("<tt") }) {
        "ttml"
    } else {
        "lrc"
    }
    return "${DownloadFileNames.songBaseName(title, artist)}.$extension"
}
