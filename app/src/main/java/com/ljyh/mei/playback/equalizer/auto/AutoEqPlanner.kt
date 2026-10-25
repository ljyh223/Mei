package com.ljyh.mei.playback.equalizer.auto

import com.ljyh.mei.playback.equalizer.EqualizerProfile
import com.ljyh.mei.playback.equalizer.EqFilter
import com.ljyh.mei.playback.equalizer.FilterType
import com.ljyh.mei.playback.equalizer.NeteaseEqualizerPresets
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Turns NetEase's per-song analysis into an EQ profile.
 *
 * Two layers:
 *  - an official 10-band curve picked from the emotion tag and coarse descriptors;
 *  - small trims on the same 10 bands from the energy ratios and loudness.
 *
 * Every gain is capped at ±12 dB (the range NetEase's own engine accepts) and the input gain is
 * lowered when the curve boosts a lot, so the result does not rely on the limiter.
 */
object AutoEqPlanner {
    const val MAX_TRIM_DB = 12f
    private const val AUTO_PRESET_ID = "auto"

    fun plan(features: TrackAudioFeatures, intensity: Float): EqualizerProfile {
        val base = NeteaseEqualizerPresets.byId(basePresetId(features))
            ?: NeteaseEqualizerPresets.byId("pop")
            ?: return EqualizerProfile()
        return plan(base, features, intensity)
    }

    internal fun plan(
        base: NeteaseEqualizerPresets.Definition,
        features: TrackAudioFeatures,
        intensity: Float,
    ): EqualizerProfile {
        val k = intensity.coerceIn(0f, 1f)
        val gains = base.gains.map { it * k }.toMutableList()
        var inputGain = 0f

        fun trim(frequency: Float, gain: Float) {
            if (k <= 0f || gain == 0f) return
            val index = nearestBand(frequency)
            gains[index] += gain
        }

        val bass = features.bassEnergyRatio
        val vocal = features.vocalEnergyRatio
        val drum = features.drumEnergyRatio

        if (bass >= 0.45f) {
            val cut = -min(4.5f, (bass - 0.45f) * 20f) * k
            trim(63f, cut)
            trim(125f, cut * 0.6f)
        } else if (bass > 0f && bass <= 0.18f) {
            trim(63f, min(2f, (0.18f - bass) * 14f) * k)
        }
        if (vocal >= 0.32f) {
            trim(2_000f, -min(2.5f, (vocal - 0.32f) * 12f) * k)
        } else if (vocal in 0.001f..0.08f && features.vocalTimeRatio >= 0.3f) {
            trim(2_000f, min(1.5f, features.vocalTimeRatio * 2f) * k)
        }
        if (drum >= 0.25f) {
            val cut = -min(2.5f, (drum - 0.25f) * 12f) * k
            trim(4_000f, cut)
            trim(8_000f, cut * 0.6f)
        }
        if (features.loudnessLufs >= -6f) inputGain -= 1.5f * k
        if (features.dynamicRange > 0f && features.dynamicRange <= 7f) {
            trim(16_000f, -1f * k)
        }

        val capped = gains.map { it.coerceIn(-NeteaseEqualizerPresets.MAX_GAIN_DB, NeteaseEqualizerPresets.MAX_GAIN_DB) }
        val maxBoost = max(0f, capped.max())
        // Leave headroom for the largest boost, but ignore the first 2 dB so normal presets play at
        // full level.
        inputGain = min(inputGain, -max(0f, maxBoost - 2f))

        return EqualizerProfile(
            enabled = true,
            presetId = AUTO_PRESET_ID,
            inputGainDb = inputGain.coerceIn(-24f, 12f),
            filters = capped.mapIndexed { index, gain ->
                EqFilter(
                    id = index + 1,
                    type = FilterType.PEAK,
                    frequencyHz = NeteaseEqualizerPresets.bandFrequencies[index],
                    gainDb = gain,
                    q = NeteaseEqualizerPresets.BAND_Q,
                )
            },
        ).normalized()
    }

    /** Emotion first; when there is no usable tag, fall back to coarse descriptor clusters. */
    internal fun basePresetId(features: TrackAudioFeatures): String {
        val emotion = features.emotionLabel
        fun has(vararg words: String) = words.any { emotion.contains(it) }
        return when {
            has("治愈", "温暖", "舒缓", "安静", "轻松", "温柔") -> "soft"
            has("悲伤", "忧郁", "失落", "孤独") -> "ballad"
            has("激昂", "热血", "燃", "兴奋", "快乐", "欢快") -> "rock"
            has("电子", "舞曲", "动感", "节奏") -> "electronic"
            features.bassEnergyRatio >= 0.5f && features.bpm >= 110f -> "hiphop"
            features.vocalTimeRatio >= 0.5f && features.drumEnergyRatio <= 0.12f -> "ballad"
            features.dynamicRange >= 14f -> "classical"
            else -> "pop"
        }
    }

    private fun nearestBand(frequency: Float): Int {
        var best = 0
        var bestDistance = Float.MAX_VALUE
        NeteaseEqualizerPresets.bandFrequencies.forEachIndexed { index, band ->
            val distance = abs(ln(band / frequency))
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
            }
        }
        return best
    }
}
