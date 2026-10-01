package com.ljyh.mei.playback.equalizer

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class FilterType(val label: String, val usesGain: Boolean = true) {
    LOW_SHELF("低架"),
    PEAK("峰值"),
    HIGH_SHELF("高架"),
    LOW_PASS("低通", false),
    HIGH_PASS("高通", false),
}

data class EqFilter(
    val id: Int,
    val type: FilterType,
    val frequencyHz: Float,
    val gainDb: Float = 0f,
    val q: Float = 0.7f,
    val enabled: Boolean = true,
) {
    fun normalized() = copy(
        frequencyHz = frequencyHz.coerceIn(20f, 20_000f),
        gainDb = gainDb.coerceIn(-24f, 24f),
        q = q.coerceIn(0.1f, 12f),
    )
}

data class EqualizerProfile(
    val enabled: Boolean = false,
    val presetId: String = "flat",
    val inputGainDb: Float = 0f,
    val outputGainDb: Float = 0f,
    val filters: List<EqFilter> = emptyList(),
) {
    fun normalized() = copy(
        inputGainDb = inputGainDb.coerceIn(-24f, 12f),
        outputGainDb = outputGainDb.coerceIn(-24f, 12f),
        filters = filters.take(MAX_FILTERS).map(EqFilter::normalized).distinctBy(EqFilter::id),
    )

    companion object {
        const val MAX_FILTERS = 32
    }
}

/** A compact, versioned DataStore representation; malformed filters are ignored. */
object EqualizerProfileCodec {
    fun encode(profile: EqualizerProfile): String {
        val safe = profile.normalized()
        return buildString {
            append("v1;")
            append(if (safe.enabled) "1" else "0")
            append(';').append(safe.presetId.replace(";", ""))
            append(';').append(safe.inputGainDb)
            append(';').append(safe.outputGainDb)
            safe.filters.forEach { filter ->
                append(';').append(filter.id).append(',').append(filter.type.name)
                append(',').append(filter.frequencyHz).append(',').append(filter.gainDb)
                append(',').append(filter.q).append(',').append(if (filter.enabled) "1" else "0")
            }
        }
    }

    fun decode(raw: String?): EqualizerProfile? {
        if (raw == null) return null
        val parts = raw.split(';')
        if (parts.size < 5 || parts[0] != "v1") return null
        val filters = parts.drop(5).mapNotNull { encoded ->
            val fields = encoded.split(',')
            if (fields.size != 6) return@mapNotNull null
            val id = fields[0].toIntOrNull() ?: return@mapNotNull null
            val type = FilterType.entries.firstOrNull { it.name == fields[1] }
                ?: return@mapNotNull null
            val frequency = fields[2].toFloatOrNull() ?: return@mapNotNull null
            val gain = fields[3].toFloatOrNull() ?: return@mapNotNull null
            val q = fields[4].toFloatOrNull() ?: return@mapNotNull null
            if (!frequency.isFinite() || !gain.isFinite() || !q.isFinite()) return@mapNotNull null
            EqFilter(id, type, frequency, gain, q, fields[5] == "1")
        }
        val input = parts[3].toFloatOrNull()?.takeIf(Float::isFinite) ?: 0f
        val output = parts[4].toFloatOrNull()?.takeIf(Float::isFinite) ?: 0f
        return EqualizerProfile(parts[1] == "1", parts[2], input, output, filters).normalized()
    }
}

data class BiquadCoefficients(
    val b0: Double, val b1: Double, val b2: Double,
    val a1: Double, val a2: Double,
) {
    fun magnitudeDb(frequencyHz: Double, sampleRate: Int): Double {
        val omega = 2.0 * PI * frequencyHz / sampleRate
        val c1 = cos(omega)
        val s1 = sin(omega)
        val c2 = cos(2 * omega)
        val s2 = sin(2 * omega)
        val numerator = (b0 + b1 * c1 + b2 * c2).pow(2) + (b1 * s1 + b2 * s2).pow(2)
        val denominator = (1.0 + a1 * c1 + a2 * c2).pow(2) + (a1 * s1 + a2 * s2).pow(2)
        return 10.0 * log10(max(numerator, 1e-20) / max(denominator, 1e-20))
    }
}

/** RBJ audio EQ cookbook coefficients. Shared by playback and graph rendering. */
object BiquadDesign {
    fun coefficients(filter: EqFilter, sampleRate: Int): BiquadCoefficients {
        val safe = filter.normalized()
        val frequency = min(safe.frequencyHz.toDouble(), sampleRate * 0.45)
        val omega = 2.0 * PI * frequency / sampleRate
        val cosine = cos(omega)
        val sine = sin(omega)
        val q = safe.q.toDouble()
        val alpha = sine / (2.0 * q)
        val amplitude = 10.0.pow(safe.gainDb / 40.0)
        val sqrtAmplitude = sqrt(amplitude)

        val values = when (safe.type) {
            FilterType.PEAK -> doubleArrayOf(
                1 + alpha * amplitude, -2 * cosine, 1 - alpha * amplitude,
                1 + alpha / amplitude, -2 * cosine, 1 - alpha / amplitude,
            )
            FilterType.LOW_SHELF -> doubleArrayOf(
                amplitude * ((amplitude + 1) - (amplitude - 1) * cosine + 2 * sqrtAmplitude * alpha),
                2 * amplitude * ((amplitude - 1) - (amplitude + 1) * cosine),
                amplitude * ((amplitude + 1) - (amplitude - 1) * cosine - 2 * sqrtAmplitude * alpha),
                (amplitude + 1) + (amplitude - 1) * cosine + 2 * sqrtAmplitude * alpha,
                -2 * ((amplitude - 1) + (amplitude + 1) * cosine),
                (amplitude + 1) + (amplitude - 1) * cosine - 2 * sqrtAmplitude * alpha,
            )
            FilterType.HIGH_SHELF -> doubleArrayOf(
                amplitude * ((amplitude + 1) + (amplitude - 1) * cosine + 2 * sqrtAmplitude * alpha),
                -2 * amplitude * ((amplitude - 1) + (amplitude + 1) * cosine),
                amplitude * ((amplitude + 1) + (amplitude - 1) * cosine - 2 * sqrtAmplitude * alpha),
                (amplitude + 1) - (amplitude - 1) * cosine + 2 * sqrtAmplitude * alpha,
                2 * ((amplitude - 1) - (amplitude + 1) * cosine),
                (amplitude + 1) - (amplitude - 1) * cosine - 2 * sqrtAmplitude * alpha,
            )
            FilterType.LOW_PASS -> doubleArrayOf(
                (1 - cosine) / 2, 1 - cosine, (1 - cosine) / 2,
                1 + alpha, -2 * cosine, 1 - alpha,
            )
            FilterType.HIGH_PASS -> doubleArrayOf(
                (1 + cosine) / 2, -(1 + cosine), (1 + cosine) / 2,
                1 + alpha, -2 * cosine, 1 - alpha,
            )
        }
        val a0 = values[3]
        return BiquadCoefficients(values[0] / a0, values[1] / a0, values[2] / a0, values[4] / a0, values[5] / a0)
    }

    fun responseDb(profile: EqualizerProfile, frequencyHz: Double, sampleRate: Int = 48_000): Double {
        if (!profile.enabled) return 0.0
        return profile.inputGainDb + profile.outputGainDb + profile.filters.asSequence()
            .filter(EqFilter::enabled)
            .sumOf { coefficients(it, sampleRate).magnitudeDb(frequencyHz, sampleRate) }
    }

    fun sampleResponse(profile: EqualizerProfile, points: Int = 221, sampleRate: Int = 48_000): DoubleArray {
        val stages = profile.filters.filter(EqFilter::enabled).map { coefficients(it, sampleRate) }
        return DoubleArray(points) { index ->
            val frequency = 20.0 * 1_000.0.pow(index.toDouble() / (points - 1).coerceAtLeast(1))
            profile.inputGainDb + profile.outputGainDb +
                stages.sumOf { it.magnitudeDb(frequency, sampleRate) }
        }
    }
}
