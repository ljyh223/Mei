package com.ljyh.mei.playback.transition

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Log spectral bands and positive spectral changes expected by BeatNet's model 1. */
internal class BeatNetFeatures {
    private data class Band(val bins: IntArray, val weights: FloatArray)
    private val window = DoubleArray(WINDOW_SIZE) { i -> 0.5 - 0.5 * cos(2 * PI * i / WINDOW_SIZE) }
    private val bands = createBands()

    fun extract(samples: FloatArray, sampleRate: Int): FloatArray {
        require(sampleRate > 0)
        val result = FloatArray(FRAMES * FEATURES)
        val previous = FloatArray(BANDS)
        val real = DoubleArray(FFT_SIZE)
        val imaginary = DoubleArray(FFT_SIZE)
        val magnitude = DoubleArray(FFT_SIZE / 2)
        for (frame in 0 until FRAMES) {
            real.fill(0.0)
            imaginary.fill(0.0)
            val center = frame * HOP
            for (point in 0 until WINDOW_SIZE) {
                val outputIndex = center + point - WINDOW_SIZE / 2
                if (outputIndex !in 0 until SAMPLE_RATE * 32) continue
                val source = outputIndex.toDouble() * sampleRate / SAMPLE_RATE
                val left = floor(source).toInt()
                if (left !in samples.indices) continue
                val right = min(left + 1, samples.lastIndex)
                val fraction = source - left
                real[point] = (samples[left] * (1 - fraction) + samples[right] * fraction) * window[point]
            }
            fft(real, imaginary)
            for (bin in magnitude.indices) {
                magnitude[bin] = sqrt(real[bin] * real[bin] + imaginary[bin] * imaginary[bin]) * 0.5
            }
            val offset = frame * FEATURES
            for (band in bands.indices) {
                val filter = bands[band]
                var value = 0.0
                for (i in filter.bins.indices) value += magnitude[filter.bins[i]] * filter.weights[i]
                val compressed = log10(1 + value).toFloat()
                result[offset + band] = compressed
                result[offset + BANDS + band] = max(0f, compressed - previous[band])
                previous[band] = compressed
            }
        }
        return result
    }

    private fun createBands(): List<Band> {
        val nativeBinHz = SAMPLE_RATE.toDouble() / (705 * 2)
        val firstExponent = floor(log2(30.0 / 440.0) * 24).toInt()
        val lastExponent = ceil(log2(17_000.0 / 440.0) * 24).toInt()
        val centers = ArrayList<Int>()
        for (exponent in firstExponent until lastExponent) {
            val hz = 440.0 * 2.0.pow(exponent / 24.0)
            if (hz !in 30.0..17_000.0) continue
            val nativeBin = (hz / nativeBinHz).roundToInt().coerceIn(1, 704)
            if (centers.lastOrNull() != nativeBin) centers.add(nativeBin)
        }
        val fftBinHz = SAMPLE_RATE.toDouble() / FFT_SIZE
        val filters = ArrayList<Band>()
        for (i in 0 until centers.size - 2) {
            val left = (centers[i] * nativeBinHz / fftBinHz).roundToInt()
            val center = (centers[i + 1] * nativeBinHz / fftBinHz).roundToInt()
            val right = (centers[i + 2] * nativeBinHz / fftBinHz).roundToInt()
            val bins = ArrayList<Int>()
            val weights = ArrayList<Float>()
            for (bin in left.coerceAtLeast(0) until right.coerceAtMost(FFT_SIZE / 2)) {
                val weight = if (bin < center) (bin - left).toFloat() / max(center - left, 1)
                else (right - bin).toFloat() / max(right - center, 1)
                if (weight > 0f) { bins.add(bin); weights.add(weight) }
            }
            val sum = weights.sum().coerceAtLeast(Float.MIN_VALUE)
            filters.add(Band(bins.toIntArray(), weights.map { it / sum }.toFloatArray()))
        }
        require(filters.size == BANDS) { "BeatNet feature bank has ${filters.size} bands" }
        return filters
    }

    private fun fft(real: DoubleArray, imaginary: DoubleArray) {
        var reverse = 0
        for (index in 1 until FFT_SIZE) {
            var bit = FFT_SIZE shr 1
            while (reverse and bit != 0) {
                reverse = reverse xor bit
                bit = bit shr 1
            }
            reverse = reverse xor bit
            if (index < reverse) {
                val a = real[index]; real[index] = real[reverse]; real[reverse] = a
                val b = imaginary[index]; imaginary[index] = imaginary[reverse]; imaginary[reverse] = b
            }
        }
        var groupSize = 2
        while (groupSize <= FFT_SIZE) {
            val angle = -2 * PI / groupSize
            val cosine = cos(angle)
            val sine = sin(angle)
            for (start in 0 until FFT_SIZE step groupSize) {
                var twiddleReal = 1.0
                var twiddleImaginary = 0.0
                for (offset in 0 until groupSize / 2) {
                    val a = start + offset
                    val b = a + groupSize / 2
                    val otherReal = real[b] * twiddleReal - imaginary[b] * twiddleImaginary
                    val otherImaginary = real[b] * twiddleImaginary + imaginary[b] * twiddleReal
                    real[b] = real[a] - otherReal
                    imaginary[b] = imaginary[a] - otherImaginary
                    real[a] += otherReal
                    imaginary[a] += otherImaginary
                    val nextReal = twiddleReal * cosine - twiddleImaginary * sine
                    twiddleImaginary = twiddleReal * sine + twiddleImaginary * cosine
                    twiddleReal = nextReal
                }
            }
            groupSize = groupSize shl 1
        }
    }

    private fun log2(value: Double) = ln(value) / ln(2.0)

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val WINDOW_SIZE = 1_411
        const val HOP = 441
        const val FFT_SIZE = 2_048
        const val FRAMES = 1_600
        const val BANDS = 136
        const val FEATURES = 272
    }
}
