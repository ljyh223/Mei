package com.ljyh.mei.playback.transition

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sqrt

internal data class BeatNetRhythm(
    val bpm: Double,
    val confidence: Double,
    val beatsMs: LongArray,
    val downbeatsMs: LongArray,
)

internal data class BeatNetTransitionPlan(
    val outgoingStartMs: Long,
    val incomingStartMs: Long,
    val durationMs: Long,
    val incomingRate: Float,
    val confidence: Double,
)

/** Decodes beat/downbeat activations and selects phrase boundaries for a pair. */
internal object BeatNetRhythmPlanner {
    fun decode(activations: FloatArray, regionStartMs: Long): BeatNetRhythm? {
        if (activations.size != 3_200) return null
        val beat = FloatArray(1_600) { activations[2 * it] }
        val bar = FloatArray(1_600) { activations[2 * it + 1] }
        val average = beat.average().toFloat()
        val scores = (12..55).associateWith { lag ->
            var numerator = 0.0
            var firstPower = 0.0
            var secondPower = 0.0
            for (i in lag until beat.size) {
                val a = (beat[i] - average).toDouble()
                val b = (beat[i - lag] - average).toDouble()
                numerator += a * b
                firstPower += a * a
                secondPower += b * b
            }
            (numerator / max(sqrt(firstPower * secondPower), 1e-9)).toFloat()
        }
        var period = scores.maxByOrNull { it.value }?.key ?: return null
        val half = period / 2
        val double = period * 2
        if (3_000.0 / period > 180 && double in scores &&
            scores.getValue(double) >= scores.getValue(period) * 0.9f
        ) period = double
        else if (3_000.0 / period < 75 && half in scores &&
            scores.getValue(half) >= scores.getValue(period) * 0.9f
        ) period = half
        val phase = (0 until period).maxByOrNull { offset ->
            var score = 0f
            for (frame in offset until beat.size step period) score += beat[frame]
            score
        } ?: return null
        val frames = ArrayList<Int>()
        for (center in phase until beat.size step period) {
            val nearby = (center - 2).coerceAtLeast(0)..(center + 2).coerceAtMost(beat.lastIndex)
            val peak = nearby.maxByOrNull(beat::get) ?: center
            if (frames.lastOrNull() != peak) frames.add(peak)
        }
        if (frames.size < 5) return null
        val barPhase = (0..3).maxByOrNull { offset ->
            var score = 0f
            for (i in offset until frames.size step 4) score += bar[frames[i]]
            score
        } ?: 0
        val pulseLevel = frames.map { beat[it] }.average().toFloat()
        val contrast = ((pulseLevel - average) / max(1f - average, 0.01f)).coerceAtLeast(0f)
        val confidence = (contrast * 0.65f + scores.getValue(period).coerceAtLeast(0f) * 0.35f)
            .coerceIn(0f, 1f).toDouble()
        fun timestamp(frame: Int) = regionStartMs + frame * 20L
        return BeatNetRhythm(
            bpm = 3_000.0 / period,
            confidence = confidence,
            beatsMs = frames.map(::timestamp).toLongArray(),
            downbeatsMs = frames.filterIndexed { i, _ -> i % 4 == barPhase }.map(::timestamp).toLongArray(),
        )
    }

    fun plan(outgoing: BeatNetRhythm, incoming: BeatNetRhythm, durationMs: Long): BeatNetTransitionPlan? {
        val confidence = min(outgoing.confidence, incoming.confidence)
        if (confidence < 0.08 || outgoing.beatsMs.isEmpty() || incoming.beatsMs.isEmpty()) return null
        val candidateBpm = listOf(incoming.bpm / 2, incoming.bpm, incoming.bpm * 2)
            .minByOrNull { abs(it - outgoing.bpm) } ?: return null
        if (candidateBpm <= 0 || !candidateBpm.isFinite() || !outgoing.bpm.isFinite()) return null
        val ratio = outgoing.bpm / candidateBpm
        val rate = if (abs(ratio - 1) <= 0.06) ratio.toFloat() else 1f
        val barMs = (4 * 60_000.0 / outgoing.bpm).roundToLong()
        val overlapMs = (4 * barMs).coerceIn(4_000L, 16_000L)
        val wantedStart = (durationMs - overlapMs).coerceAtLeast(0L)
        val outgoingMarkers = outgoing.downbeatsMs.takeIf { it.isNotEmpty() } ?: outgoing.beatsMs
        val outgoingStart = outgoingMarkers.minByOrNull { abs(it - wantedStart) } ?: return null
        val incomingMarkers = incoming.downbeatsMs.takeIf { it.isNotEmpty() } ?: incoming.beatsMs
        val incomingStart = incomingMarkers.firstOrNull { it in 0L..8_000L } ?: 0L
        if (outgoingStart >= durationMs - 2_000L) return null
        return BeatNetTransitionPlan(
            outgoingStartMs = outgoingStart,
            incomingStartMs = incomingStart,
            durationMs = (durationMs - outgoingStart).coerceAtMost(18_000L),
            incomingRate = rate,
            confidence = confidence,
        )
    }
}
