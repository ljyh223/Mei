package com.ljyh.mei.playback.transition

import kotlin.math.abs
import kotlin.math.max

internal data class TransitionPlan(val durationMs: Long, val incomingStartMs: Long = 0L)

/**
 * Builds a conservative plan from 100 ms RMS windows measured by the audio sinks.
 *
 * Returns null when a crossfade would sound forced: no measurements, a track that already ends
 * in silence, two dense tracks without a shared pulse, or incompatible tempos. Letting the track
 * end naturally is a better outcome than overlapping everything.
 */
internal object SmartTransitionPlanner {
    fun plan(outgoing: List<Float>, incoming: List<Float>, availableMs: Long): TransitionPlan? {
        if (availableMs < 2_500L) return null
        if (outgoing.size < 30 || incoming.size < 12) return null
        val tail = outgoing.takeLast(20)
        val outgoingLevel = tail.average().toFloat()
        // Already fading to silence: no crossfade needed.
        if (outgoingLevel < 0.008f) return null
        val incomingLevel = incoming.dropWhile { it < 0.012f }
        val introSilenceMs = ((incoming.size - incomingLevel.size) * 100L).coerceAtMost(2_000L)
        val incomingStart = (introSilenceMs - 200L).coerceAtLeast(0L)
        val introLevel = incomingLevel.take(20).average().toFloat()

        val outgoingBeat = estimateBeatMs(outgoing)
        val incomingBeat = estimateBeatMs(incoming)
        val matchedPulse = outgoingBeat != null && incomingBeat != null &&
            abs(outgoingBeat - incomingBeat) < max(outgoingBeat, incomingBeat) * 0.10f
        // Different tempos cannot be blended musically without time-stretching.
        if (outgoingBeat != null && incomingBeat != null && !matchedPulse) return null

        val duration = when {
            matchedPulse -> (16 * ((outgoingBeat + incomingBeat) / 2L)).coerceIn(3_500L, 8_000L)
            // Without a reliable pulse, only blend when at least one side is sparse.
            outgoingLevel > 0.12f && introLevel > 0.12f -> return null
            outgoingLevel < 0.03f -> 3_000L
            else -> 2_500L
        }
        return TransitionPlan(
            durationMs = duration.coerceAtMost((availableMs - 500L).coerceAtLeast(1_500L)),
            incomingStartMs = incomingStart,
        )
    }

    internal fun estimateBeatMs(values: List<Float>): Long? {
        if (values.size < 48) return null
        val sample = values.takeLast(120)
        val mean = sample.average().toFloat()
        val pulses = sample.indices.filter { i ->
            i > 0 && i < sample.lastIndex && sample[i] > mean * 1.18f &&
                sample[i] > sample[i - 1] && sample[i] >= sample[i + 1]
        }
        if (pulses.size < 5) return null
        val intervals = pulses.zipWithNext { a, b -> (b - a) * 100L }
            .filter { it in 300L..900L }
        if (intervals.size < 4) return null
        val median = intervals.sorted()[intervals.size / 2]
        val matching = intervals.count { abs(it - median) < median * 0.16f }
        return median.takeIf { matching >= intervals.size * 0.6f }
    }
}
