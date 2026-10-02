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
}
