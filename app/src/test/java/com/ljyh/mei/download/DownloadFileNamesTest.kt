package com.ljyh.mei.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFileNamesTest {
    @Test
    fun playlistNamesCannotBecomePathSegments() {
        assertEquals("AC／DC", DownloadFileNames.playlistName(" AC/DC "))
        assertEquals("未分类", DownloadFileNames.playlistName(" . "))
        assertEquals("未分类", DownloadFileNames.playlistName("..."))
        assertEquals("A／B", DownloadFileNames.playlistName(" A\u0000/\nB. "))
        assertEquals("_CON", DownloadFileNames.playlistName("CON"))
        assertEquals("_CON.txt", DownloadFileNames.playlistName("CON.txt"))
    }

    @Test
    fun audioAndLyricShareASafeBoundedBaseName() {
        val base = DownloadFileNames.songBaseName("A/B\u0000", "Artist")
        assertEquals("A／B - Artist", base)
        assertEquals("$base.lrc", lyricSidecarFileName("A/B\u0000", "Artist", "[00:01]line"))
        assertEquals("未命名歌曲 - 未知歌手", DownloadFileNames.songBaseName("..", ""))

        val longBase = DownloadFileNames.songBaseName("🎵".repeat(100), "歌手".repeat(100))
        assertTrue(longBase.toByteArray(Charsets.UTF_8).size <= 180)
        assertFalse(longBase.contains('/'))
        assertFalse(longBase.contains('\uFFFD'))
    }

    @Test
    fun extensionMustBeARecognizedAudioType() {
        assertEquals("flac", DownloadFileNames.audioExtension(".FLAC", "https://example.com/a"))
        assertEquals("mp3", DownloadFileNames.audioExtension("../evil", "https://example.com/a.mp3?token=1"))
        assertNull(DownloadFileNames.audioExtension("../../x", "https://example.com/a.exe"))
        assertNull(DownloadFileNames.audioExtension("", "https://example.com/a"))
    }

    @Test
    fun lyricDirectoryMustStayUnderMusicMei() {
        assertTrue(DownloadFileNames.isSafeRelativePath("Music/Mei/AC／DC"))
        assertTrue(DownloadFileNames.isSafeRelativePath("Music/Mei/Older name."))
        assertFalse(DownloadFileNames.isSafeRelativePath("Music/Mei/../Other"))
        assertFalse(DownloadFileNames.isSafeRelativePath("Music/Mei/A/B"))
        assertFalse(DownloadFileNames.isSafeRelativePath("Music/Mei/A\u0000B"))
        assertFalse(DownloadFileNames.isSafeRelativePath("Download/Mei/Album"))
    }
}
