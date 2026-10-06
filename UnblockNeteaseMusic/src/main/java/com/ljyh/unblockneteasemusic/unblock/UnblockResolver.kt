package com.ljyh.unblockneteasemusic.unblock

import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.unblockneteasemusic.model.PlayableAudio
import com.ljyh.unblockneteasemusic.provider.MusicCatalogProvider
import com.ljyh.unblockneteasemusic.provider.PlayableAudioProvider
import kotlinx.coroutines.CancellationException

/** Matches an unavailable track against sources able to return a playable audio URL. */
class UnblockResolver(
    catalogs: List<MusicCatalogProvider>,
    audioProviders: List<PlayableAudioProvider>,
    private val matcher: TrackMatcher = DefaultTrackMatcher,
) {
    private val sources = catalogs.mapNotNull { catalog ->
        audioProviders.firstOrNull { it.sourceId == catalog.sourceId }
            ?.let { audio -> catalog to audio }
    }

    suspend fun resolve(target: MusicTrack): PlayableAudio? {
        for ((catalog, audio) in sources) {
            if (catalog.sourceId == target.id.source) continue
            val candidates = try {
                catalog.search(target.title, limit = 20)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                continue
            }
            for (candidate in candidates
                .map { it to matcher.score(target, it) }
                .filter { it.second >= matcher.minimumScore }
                .sortedByDescending { it.second }
                .map { it.first }) {
                try {
                    audio.resolve(candidate)?.let { return it }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Continue with another matching track or source.
                }
            }
        }
        return null
    }
}

interface TrackMatcher {
    val minimumScore: Double
    fun score(target: MusicTrack, candidate: MusicTrack): Double
}

object DefaultTrackMatcher : TrackMatcher {
    override val minimumScore: Double = 0.8

    override fun score(target: MusicTrack, candidate: MusicTrack): Double {
        val targetTitle = normalize(target.title)
        val candidateTitle = normalize(candidate.title)
        if (targetTitle.isEmpty() || targetTitle != candidateTitle) return 0.0

        val targetArtists = target.artists.map { normalize(it.name) }.filter(String::isNotEmpty).toSet()
        val candidateArtists = candidate.artists.map { normalize(it.name) }.filter(String::isNotEmpty).toSet()
        if (targetArtists.isNotEmpty() && targetArtists.intersect(candidateArtists).isEmpty()) return 0.0

        val durationDifference = kotlin.math.abs(target.durationMs - candidate.durationMs)
        if (target.durationMs > 0 && candidate.durationMs > 0 && durationDifference > 10_000) return 0.0

        return 1.0 - (durationDifference.coerceAtMost(10_000) / 100_000.0)
    }

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("[\\s\\p{Punct}]+"), "")
}
