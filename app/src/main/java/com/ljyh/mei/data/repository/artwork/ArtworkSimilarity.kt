package com.ljyh.mei.data.repository.artwork

import com.ljyh.mei.data.repository.AppleAlbumCandidate
import com.ljyh.mei.data.repository.normalized
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/** A 64-bit perceptual hash for 32x32 luminance samples. */
internal object ArtworkSimilarity {
    private const val Side = 32
    private const val Frequencies = 8
    private val cosine = Array(Frequencies) { frequency ->
        DoubleArray(Side) { position -> cos((2 * position + 1) * frequency * PI / (2 * Side)) }
    }

    fun hash(luminance: DoubleArray): Long {
        require(luminance.size == Side * Side)
        val average = luminance.average()
        val horizontal = Array(Side) { DoubleArray(Frequencies) }
        for (y in 0 until Side) for (u in 0 until Frequencies) {
            var sum = 0.0
            for (x in 0 until Side) sum += (luminance[y * Side + x] - average) * cosine[u][x]
            horizontal[y][u] = sum
        }
        val coefficients = DoubleArray(Frequencies * Frequencies)
        for (v in 0 until Frequencies) for (u in 0 until Frequencies) {
            var sum = 0.0
            for (y in 0 until Side) sum += horizontal[y][u] * cosine[v][y]
            coefficients[v * Frequencies + u] = sum
        }
        // The DC term mainly captures overall brightness, so exclude it from the median.
        val median = coefficients.drop(1).sorted()[31]
        val epsilon = coefficients.drop(1).maxOf { abs(it) } * 1e-7
        var bits = 0L
        for (i in coefficients.indices) {
            if (i != 0 && coefficients[i] > median + epsilon) bits = bits or (1L shl i)
        }
        return bits
    }

    fun distance(first: Long, second: Long): Int = java.lang.Long.bitCount(first xor second)
}

internal fun artistMatchesForArtwork(candidate: String, expected: String): Boolean {
    val actual = normalized(candidate)
    val target = normalized(expected)
    return actual.isNotBlank() && target.isNotBlank() &&
        (actual.startsWith(target) || target.startsWith(actual))
}

internal fun rankedArtworkMatches(
    candidates: List<AppleAlbumCandidate>, artist: String, sourceHash: Long,
    hashes: Map<String, Long>, maxDistance: Int = 8,
): List<AppleAlbumCandidate> {
    if (normalized(artist).isBlank()) return emptyList()
    return candidates.asSequence()
        .filter { candidate -> artistMatchesForArtwork(candidate.artist, artist) }
        .mapNotNull { candidate ->
            hashes[candidate.id]?.let { hash -> candidate to ArtworkSimilarity.distance(sourceHash, hash) }
        }
        .filter { (_, distance) -> distance <= maxDistance }
        .sortedBy { (_, distance) -> distance }
        .map { (candidate, _) -> candidate }
        .toList()
}
