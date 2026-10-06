package com.ljyh.mei.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppleAlbumMatchingTest {
    @Test
    fun `matches normalized album and artist using the first eligible candidate`() {
        val candidates = listOf(
            AppleAlbumCandidate("wrong", "Same Album", "Another Singer"),
            AppleAlbumCandidate("right", "Ｓａｍｅ Album！", "Artist & Friends"),
            AppleAlbumCandidate("later", "Same Album", "Artist")
        )
        assertEquals("right", matchAppleAlbum(candidates, "Same Album", "Artist")?.id)
    }

    @Test
    fun `does not match missing or partial album names`() {
        val candidates = listOf(AppleAlbumCandidate("one", "An Album Deluxe", "Artist"))
        assertNull(matchAppleAlbum(candidates, "An Album", "Artist"))
        assertNull(matchAppleAlbum(candidates, "An Album Deluxe", ""))
    }

    @Test
    fun `formats Apple artwork template for candidate thumbnail`() {
        assertEquals(
            "https://example.com/240x240bb.jpg",
            formatAppleArtworkUrl("https://example.com/{w}x{h}bb.{f}", 240)
        )
        assertNull(formatAppleArtworkUrl("http://example.com/{w}x{h}bb.{f}", 240))
    }
}
