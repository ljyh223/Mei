package com.ljyh.mei.playback

import org.junit.Assert.assertEquals
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
}
