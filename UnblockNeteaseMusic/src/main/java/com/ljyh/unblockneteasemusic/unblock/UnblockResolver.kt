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
    private val onEvent: (UnblockEvent) -> Unit = {},
) {
    private val sources = catalogs.mapNotNull { catalog ->
        audioProviders.firstOrNull { it.sourceId == catalog.sourceId }
            ?.let { audio -> catalog to audio }
    }

    suspend fun resolve(target: MusicTrack): PlayableAudio? {
        fun emit(event: UnblockEvent) = onEvent(event.copy(targetId = target.id.value))
        val artistNames = target.artists.map { it.name }.filter(String::isNotBlank)
        val query = listOf(stripCoverTag(target.title), artistNames.take(2).joinToString(" / "))
            .filter(String::isNotBlank).joinToString(" - ")
        for ((catalog, audio) in sources) {
            if (catalog.sourceId == target.id.source) continue
            emit(UnblockEvent(UnblockStage.SEARCH_STARTED, catalog.sourceId))
            val candidates = try {
                catalog.search(query, limit = 5)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                emit(UnblockEvent(UnblockStage.SEARCH_FAILED, catalog.sourceId, errorType = error.javaClass.simpleName))
                continue
            }
            emit(UnblockEvent(UnblockStage.SEARCH_FINISHED, catalog.sourceId, count = candidates.size))
            val matches = candidates
                .map { it to matcher.score(target, it) }
                .filter { it.second >= matcher.minimumScore }
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first }
            emit(UnblockEvent(UnblockStage.MATCHED, catalog.sourceId, count = matches.size))
            for (candidate in matches) {
                try {
                    val playable = audio.resolve(candidate)
                    if (playable == null) {
                        emit(UnblockEvent(UnblockStage.URL_UNAVAILABLE, catalog.sourceId, candidateId = candidate.id.value))
                        continue
                    }
                    if (matcher.score(target, playable.track) < matcher.minimumScore) {
                        emit(UnblockEvent(UnblockStage.DURATION_REJECTED, catalog.sourceId, candidateId = candidate.id.value))
                        continue
                    }
                    emit(UnblockEvent(UnblockStage.SELECTED, catalog.sourceId, candidateId = candidate.id.value))
                    return playable
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    emit(UnblockEvent(
                        UnblockStage.URL_FAILED, catalog.sourceId,
                        candidateId = candidate.id.value, errorType = error.javaClass.simpleName,
                    ))
                }
            }
        }
        emit(UnblockEvent(UnblockStage.EXHAUSTED))
        return null
    }
}

enum class UnblockStage {
    SEARCH_STARTED, SEARCH_FINISHED, SEARCH_FAILED, MATCHED,
    URL_UNAVAILABLE, URL_FAILED, DURATION_REJECTED, SELECTED, EXHAUSTED,
}

/** Trace metadata only; credentials and signed playback URLs are never included. */
data class UnblockEvent(
    val stage: UnblockStage,
    val sourceId: String? = null,
    val count: Int? = null,
    val candidateId: String? = null,
    val errorType: String? = null,
    val targetId: String? = null,
)

interface TrackMatcher {
    val minimumScore: Double
    fun score(target: MusicTrack, candidate: MusicTrack): Double
}

object DefaultTrackMatcher : TrackMatcher {
    override val minimumScore: Double = 0.8

    override fun score(target: MusicTrack, candidate: MusicTrack): Double {
        val targetTitle = normalizeTitle(target.title)
        val candidateTitle = normalizeTitle(candidate.title)
        if (targetTitle.isEmpty() || targetTitle != candidateTitle) return 0.0

        val targetArtists = target.artists.map { normalize(it.name) }.filter(String::isNotEmpty).toSet()
        val candidateArtists = candidate.artists.map { normalize(it.name) }.filter(String::isNotEmpty).toSet()
        if (targetArtists.isNotEmpty() && targetArtists.intersect(candidateArtists).isEmpty()) return 0.0

        val durationDifference = kotlin.math.abs(target.durationMs - candidate.durationMs)
        if (target.durationMs > 0 && candidate.durationMs > 0 && durationDifference > 10_000) return 0.0

        return 1.0 - (durationDifference.coerceAtMost(10_000) / 100_000.0)
    }

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("[\\s\\p{P}\\p{S}]+"), "")

    private fun normalizeTitle(value: String): String = normalize(stripCoverTag(value))

}

private fun stripCoverTag(value: String): String = value.replace(
    Regex("[（(]\\s*(?:cover|翻自)[:：\\s][^）)]+[）)]", RegexOption.IGNORE_CASE), ""
).trim()
