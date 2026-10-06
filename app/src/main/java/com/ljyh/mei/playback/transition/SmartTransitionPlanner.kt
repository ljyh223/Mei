package com.ljyh.mei.playback.transition

import kotlin.math.abs
import kotlin.math.max

internal data class TransitionPlan(val durationMs: Long, val incomingStartMs: Long = 0L)

/** Builds a conservative plan from 100 ms RMS windows measured by the audio sinks. */
internal object SmartTransitionPlanner {
    fun plan(outgoing: List<Float>, incoming: List<Float>, availableMs: Long): TransitionPlan {
        if (outgoing.size < 30 || incoming.size < 12) return TransitionPlan(6_000L)
        val outgoingLevel = outgoing.takeLast(30).average().toFloat()
        val incomingLevel = incoming.dropWhile { it < 0.012f }
        val introSilenceMs = ((incoming.size - incomingLevel.size) * 100L).coerceAtMost(2_000L)
        val incomingStart = (introSilenceMs - 200L).coerceAtLeast(0L)
        val introLevel = incomingLevel.take(20).average().toFloat()

        // A quiet outro should hand over promptly; dense material needs a longer overlap.
        var duration = when {
            outgoingLevel < 0.025f -> 3_500L
            outgoingLevel > 0.17f && introLevel > 0.17f -> 8_000L
            else -> 6_000L
        }
        val outgoingBeat = estimateBeatMs(outgoing)
        val incomingBeat = estimateBeatMs(incoming)
        if (outgoingBeat != null && incomingBeat != null &&
            abs(outgoingBeat - incomingBeat) < max(outgoingBeat, incomingBeat) * 0.12f
        ) {
            // End after four bars when both songs have a compatible pulse.
            duration = (16 * ((outgoingBeat + incomingBeat) / 2L)).coerceIn(4_000L, 10_000L)
        }
        return TransitionPlan(duration.coerceAtMost((availableMs - 500L).coerceAtLeast(2_000L)), incomingStart)
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
