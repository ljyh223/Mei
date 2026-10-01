package com.ljyh.mei.playback.equalizer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.roundToInt

/** Applies the same biquad cascade displayed by the editor to interleaved PCM16 audio. */
class ParametricEqualizerProcessor : BaseAudioProcessor() {
    @Volatile private var requestedProfile = EqualizerProfile()
    private var appliedProfile: EqualizerProfile? = null
    private var current: FilterCascade? = null
    private var previous: FilterCascade? = null
    private var fadeFramesRemaining = 0
    private var fadeFramesTotal = 1

    fun setProfile(profile: EqualizerProfile) {
        requestedProfile = profile.normalized()
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) inputAudioFormat
        else AudioProcessor.AudioFormat.NOT_SET
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        current = null
        previous = null
        appliedProfile = null
        fadeFramesRemaining = 0
    }

    override fun onReset() {
        current = null
        previous = null
        appliedProfile = null
        fadeFramesRemaining = 0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val profile = requestedProfile
        val channels = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate
        if (channels <= 0 || sampleRate <= 0) {
            val output = replaceOutputBuffer(inputBuffer.remaining())
            output.put(inputBuffer)
            output.flip()
            return
        }
        if (profile != appliedProfile) {
            previous = current
            current = FilterCascade(profile, channels, sampleRate)
            appliedProfile = profile
            fadeFramesTotal = (sampleRate / 100).coerceAtLeast(1) // 10 ms
            fadeFramesRemaining = if (previous == null) 0 else fadeFramesTotal
        }

        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val output = replaceOutputBuffer(inputBuffer.remaining()).order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = channels * 2
        while (inputBuffer.remaining() >= frameBytes) {
            val blend = if (fadeFramesRemaining > 0) 1.0 - fadeFramesRemaining.toDouble() / fadeFramesTotal else 1.0
            repeat(channels) { channel ->
                val sample = inputBuffer.short.toDouble() / 32768.0
                val processed = current?.process(sample, channel) ?: sample
                val mixed = if (fadeFramesRemaining > 0) {
                    val old = previous?.process(sample, channel) ?: sample
                    old * (1.0 - blend) + processed * blend
                } else processed
                // Saturation is the last safety net. Input gain is user controlled and no automatic
                // limiter is inserted, so the editor recommends negative headroom for boosts.
                output.putShort((mixed.coerceIn(-1.0, 32767.0 / 32768.0) * 32768.0).roundToInt().toShort())
            }
            if (fadeFramesRemaining > 0 && --fadeFramesRemaining == 0) previous = null
        }
        // The audio sink supplies whole PCM frames. Keep any unexpected trailing bytes intact.
        while (inputBuffer.hasRemaining()) output.put(inputBuffer.get())
        output.flip()
    }

    private class FilterCascade(profile: EqualizerProfile, channels: Int, sampleRate: Int) {
        private val inputGain = 10.0.pow(profile.inputGainDb / 20.0)
        private val outputGain = 10.0.pow(profile.outputGainDb / 20.0)
        private val enabled = profile.enabled
        private val stages = if (enabled) profile.filters.filter(EqFilter::enabled).map { filter ->
            BiquadStage(BiquadDesign.coefficients(filter, sampleRate), channels)
        } else emptyList()

        fun process(sample: Double, channel: Int): Double {
            if (!enabled) return sample
            var value = sample * inputGain
            stages.forEach { value = it.process(value, channel) }
            return value * outputGain
        }
    }

    private class BiquadStage(private val c: BiquadCoefficients, channels: Int) {
        private val x1 = DoubleArray(channels)
        private val x2 = DoubleArray(channels)
        private val y1 = DoubleArray(channels)
        private val y2 = DoubleArray(channels)

        fun process(input: Double, channel: Int): Double {
            val output = c.b0 * input + c.b1 * x1[channel] + c.b2 * x2[channel] -
                c.a1 * y1[channel] - c.a2 * y2[channel]
            x2[channel] = x1[channel]
            x1[channel] = input
            y2[channel] = y1[channel]
            y1[channel] = if (output.isFinite()) output else 0.0
            return y1[channel]
        }
    }
}
