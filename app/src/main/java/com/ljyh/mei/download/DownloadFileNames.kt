package com.ljyh.mei.download

import com.ljyh.mei.utils.StringUtils.specialReplace
import java.util.Locale

/** Names shared by the audio file and its lyric sidecar. Limits are UTF-8 bytes, not characters. */
internal object DownloadFileNames {
    private const val PLAYLIST_LIMIT = 120
    private const val TITLE_LIMIT = 100
    private const val ARTIST_LIMIT = 70
    private const val FILE_BASE_LIMIT = 180
    private const val ROOT = "Music/Mei/"

    private val reservedNames = setOf("CON", "PRN", "AUX", "NUL") +
        (1..9).flatMap { listOf("COM$it", "LPT$it") }
    private val audioExtensions = setOf("mp3", "flac", "aac", "ogg", "wav", "m4a", "opus", "mp4", "mpeg")

    fun playlistName(raw: String): String = safeSegment(raw, "未分类", PLAYLIST_LIMIT)

    fun songBaseName(title: String, artist: String): String {
        val safeTitle = safeSegment(title, "未命名歌曲", TITLE_LIMIT)
        val safeArtist = safeSegment(artist, "未知歌手", ARTIST_LIMIT)
        return safeSegment("$safeTitle - $safeArtist", "未命名歌曲", FILE_BASE_LIMIT)
    }

    fun audioExtension(fileType: String, url: String): String? {
        fun accepted(value: String): String? = value.trim().removePrefix(".")
            .lowercase(Locale.ROOT).takeIf { it in audioExtensions }

        val urlFileName = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
        val fromUrl = urlFileName.substringAfterLast('.', "")
        return accepted(fileType) ?: accepted(fromUrl)
    }

    fun isSafeRelativePath(path: String): Boolean {
        if (!path.startsWith(ROOT)) return false
        val segment = path.removePrefix(ROOT)
        return segment.isNotBlank() &&
            segment.trim() != "." && segment.trim() != ".." &&
            '/' !in segment && '\\' !in segment &&
            segment.codePoints().noneMatch(Character::isISOControl)
    }

    private fun safeSegment(raw: String, fallback: String, maxBytes: Int): String {
        val replaced = specialReplace(raw)
        val withoutControls = buildString(replaced.length) {
            var index = 0
            while (index < replaced.length) {
                val point = replaced.codePointAt(index)
                if (!Character.isISOControl(point)) appendCodePoint(point)
                index += Character.charCount(point)
            }
        }
        val normalized = withoutControls.trim().trimEnd('.', ' ')
            .takeUnless { it.isBlank() || it == "." || it == ".." } ?: fallback
        val limited = truncateUtf8(normalized, maxBytes).trimEnd('.', ' ')
        val safe = limited.ifBlank { fallback }
        return if (safe.substringBefore('.').uppercase(Locale.ROOT) in reservedNames) {
            "_${truncateUtf8(safe, maxBytes - 1).trimEnd('.', ' ')}"
        } else safe
    }

    private fun truncateUtf8(value: String, maxBytes: Int): String = buildString {
        var bytes = 0
        var index = 0
        while (index < value.length) {
            val point = value.codePointAt(index)
            val text = String(Character.toChars(point))
            val size = text.toByteArray(Charsets.UTF_8).size
            if (bytes + size > maxBytes) break
            append(text)
            bytes += size
            index += Character.charCount(point)
        }
    }
}
