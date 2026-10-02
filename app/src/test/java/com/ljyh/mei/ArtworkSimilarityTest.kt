package com.ljyh.mei.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkSimilarityTest {
    @Test
    fun hashIgnoresUniformBrightnessChanges() {
        val source = DoubleArray(32 * 32) { index ->
            val x = index % 32
            val y = index / 32
            (x * x + y * 3).toDouble()
        }
        val brightened = source.map { it + 20.0 }.toDoubleArray()
        assertEquals(0, ArtworkSimilarity.distance(
            ArtworkSimilarity.hash(source), ArtworkSimilarity.hash(brightened)
        ))
    }

    @Test
    fun ranksCloseArtworkButRequiresArtistAgreement() {
        val source = DoubleArray(32 * 32) { index ->
            if (index % 32 < 16) (index / 32).toDouble() else 200.0
        }
        val other = DoubleArray(32 * 32) { index ->
            if (index / 32 < 16) (index % 32).toDouble() else 200.0
        }
        val sourceHash = ArtworkSimilarity.hash(source)
        val differentHash = ArtworkSimilarity.hash(other)
        assertTrue(ArtworkSimilarity.distance(sourceHash, differentHash) > 8)
        val candidates = listOf(
            AppleAlbumCandidate("wrong", "Album Deluxe", "Other Artist"),
            AppleAlbumCandidate("right", "Album Deluxe", "Artist"),
            AppleAlbumCandidate("different", "Another Album", "Artist"),
        )
        assertEquals(listOf("right"), rankedArtworkMatches(
            candidates, "Artist", sourceHash,
            mapOf("wrong" to sourceHash, "right" to sourceHash, "different" to differentHash)
        ).map { it.id })
    }
}
