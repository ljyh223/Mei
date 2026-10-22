package com.ljyh.mei.playback.equalizer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tanh

/**
 * Peak limiter with a soft knee, inserted after the EQ/reverb chain.
 *
 * A fast attack gain envelope keeps transients under the threshold, a slow release avoids pumping,
 * and a tanh knee rounds off whatever still exceeds it. This replaces the hard clamp that used to
 * live inside the equalizer, so boosted bands no longer clip into a square wave.
 */
class SoftLimiterProcessor : BaseAudioProcessor() {
    @Volatile private var requestedEnabled = true
    @Volatile private var requestedThresholdDb = -0.5f
    private var appliedEnabled = true
    private var threshold = 0.94
    private var gain = 1.0
    private var releaseCoefficient = 0.0005

    fun configure(enabled: Boolean, thresholdDb: Float) {
        requestedEnabled = enabled
        requestedThresholdDb = thresholdDb
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        ) inputAudioFormat else AudioProcessor.AudioFormat.NOT_SET

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        resetState()
    }

    override fun onReset() {
        resetState()
    }

    private fun resetState() {
        gain = 1.0
        appliedEnabled = requestedEnabled
        threshold = dbToLinear(requestedThresholdDb)
        releaseCoefficient = releaseCoefficientFor(inputAudioFormat.sampleRate)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val channels = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate
        if (channels <= 0 || sampleRate <= 0) {
            val output = replaceOutputBuffer(inputBuffer.remaining())
            output.put(inputBuffer)
            output.flip()
            return
        }
        appliedEnabled = requestedEnabled
        threshold = dbToLinear(requestedThresholdDb)
        releaseCoefficient = releaseCoefficientFor(sampleRate)

        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val output = replaceOutputBuffer(inputBuffer.remaining()).order(ByteOrder.LITTLE_ENDIAN)
        val isFloat = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val frameBytes = channels * if (isFloat) 4 else 2
        while (inputBuffer.remaining() >= frameBytes) {
            var peak = 0.0
            val frame = DoubleArray(channels)
            for (channel in 0 until channels) {
                val sample = if (isFloat) inputBuffer.float.toDouble()
                else inputBuffer.short / 32768.0
                frame[channel] = sample
                peak = maxOf(peak, abs(sample))
            }
            if (appliedEnabled) {
                val target = limiterTargetGain(peak, threshold)
                gain = if (target < gain) target else gain + (1.0 - gain) * releaseCoefficient
                for (channel in 0 until channels) {
                    val limited = softKnee(frame[channel] * gain)
                    if (isFloat) output.putFloat(limited.toFloat())
                    else output.putShort((limited.coerceIn(-1.0, 32767.0 / 32768.0) * 32768.0)
                        .roundToInt().toShort())
                }
            } else {
                for (channel in 0 until channels) {
                    if (isFloat) output.putFloat(frame[channel].toFloat())
                    else output.putShort((frame[channel].coerceIn(-1.0, 32767.0 / 32768.0) * 32768.0)
                        .roundToInt().toShort())
                }
            }
        }
        while (inputBuffer.hasRemaining()) output.put(inputBuffer.get())
        output.flip()
    }

    private fun softKnee(sample: Double): Double = softKneeSample(sample, threshold)

    private fun dbToLinear(db: Float): Double = 10.0.pow(db / 20.0).coerceIn(0.05, 1.0)

    /** 40 ms release per sample; attack is instantaneous, which is fine for music peaks. */
    private fun releaseCoefficientFor(sampleRate: Int): Double =
        (1.0 - kotlin.math.exp(-1.0 / (0.04 * sampleRate.coerceAtLeast(1)))).coerceIn(1e-6, 1.0)
}

/** Pure limiter math, shared with the unit tests. */
internal fun softKneeSample(sample: Double, threshold: Double): Double {
    val magnitude = abs(sample)
    if (magnitude <= threshold) return sample
    val sign = if (sample < 0) -1.0 else 1.0
    val over = (magnitude - threshold) / (1.0 - threshold).coerceAtLeast(1e-6)
    val compressed = threshold + (1.0 - threshold) * tanh(over)
    return sign * compressed.coerceAtMost(1.0)
}

internal fun limiterTargetGain(peak: Double, threshold: Double): Double =
    if (peak > threshold && peak > 0.0) threshold / peak else 1.0
