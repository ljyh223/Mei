package com.ljyh.mei.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricSidecarTest {
    @Test
    fun lrcUsesAudioBaseNameAndSanitizesPathCharacters() {
        assertEquals(
            "A／B - Artist.lrc",
            lyricSidecarFileName("A/B", "Artist", "[00:01.00]line"),
        )
    }

    @Test
    fun originalTtmlGetsTtmlExtension() {
        assertEquals(
            "Title - Artist.ttml",
            lyricSidecarFileName("Title", "Artist", "<?xml version=\"1.0\"?><tt></tt>"),
        )
    }

    @Test
    fun android10MusicTreeTargetsTheAudioPlaylistFolder() {
        assertEquals(
            listOf("Mei", "Favorites"),
            Android10LyricTree.directorySegments("Music/Mei/Favorites", "primary:Music"),
        )
        assertEquals(
            listOf("Favorites"),
            Android10LyricTree.directorySegments("Music/Mei/Favorites", "primary:Music/Mei"),
        )
        assertNull(Android10LyricTree.directorySegments("Music/Mei/Favorites", "primary:Download"))
        assertNull(Android10LyricTree.directorySegments("Music/Other/Favorites", "primary:Music"))
        assertNull(Android10LyricTree.directorySegments("Music/Mei/../Other", "primary:Music"))
    }
}
