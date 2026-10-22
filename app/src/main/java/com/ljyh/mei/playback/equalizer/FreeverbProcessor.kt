package com.ljyh.mei.playback.equalizer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Schroeder/Freeverb room reverb for interleaved PCM16.
 *
 * Eight parallel comb filters feed four series allpass filters per channel, with the classic
 * Freeverb tunings scaled to the stream sample rate. This is the same topology BASS_FX's
 * "V3 reverb" uses, so the room size / damping / width / wet / dry vocabulary lines up.
 */
class FreeverbProcessor : BaseAudioProcessor() {
    @Volatile private var requested = ReverbProfile.Off
    private var applied: ReverbProfile? = null
    private var core: FreeverbCore? = null

    fun setProfile(profile: ReverbProfile) {
        requested = profile.normalized()
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) inputAudioFormat
        else AudioProcessor.AudioFormat.NOT_SET

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        core = null
        applied = null
    }

    override fun onReset() {
        core = null
        applied = null
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
        val profile = requested
        if (profile != applied) {
            core = FreeverbCore(profile, channels, sampleRate)
            applied = profile
        }

        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val output = replaceOutputBuffer(inputBuffer.remaining()).order(ByteOrder.LITTLE_ENDIAN)
        val frame = FloatArray(channels)
        val frameBytes = channels * 2
        val engine = core
        while (inputBuffer.remaining() >= frameBytes) {
            for (channel in 0 until channels) {
                frame[channel] = inputBuffer.short / 32768f
            }
            engine?.process(frame)
            for (channel in 0 until channels) {
                val value = frame[channel].coerceIn(-1f, 32767f / 32768f)
                output.putShort((value * 32768f).roundToInt().toShort())
            }
        }
        while (inputBuffer.hasRemaining()) output.put(inputBuffer.get())
        output.flip()
    }
}

/** Testable DSP core: one frame in, one damped frame out. */
internal class FreeverbCore(
    profile: ReverbProfile,
    private val channels: Int,
    sampleRate: Int,
) {
    private val safe = profile.normalized()
    private val enabled = safe.enabled
    private val dry = safe.dry
    private val wet1: Float
    private val wet2: Float
    private val feedback = 0.7f + safe.roomSize * 0.28f
    private val damp1 = safe.damping * 0.4f
    private val damp2 = 1f - damp1

    private val combs = Array(channels) { channel -> COMB_TUNING.map { tuning ->
        Comb(scaled(tuning, channel, sampleRate), feedback, damp1, damp2)
    } }
    private val allpasses = Array(channels) { channel -> ALLPASS_TUNING.map { tuning ->
        AllPass(scaled(tuning, channel, sampleRate))
    } }
    private val preDelay: Array<FloatArray>
    private var preDelayIndex = 0

    init {
        val half = safe.width * 0.5f + 0.5f
        val inverse = (1f - safe.width) * 0.5f
        wet1 = safe.wet * half
        wet2 = safe.wet * inverse
        val delaySamples = (safe.preDelayMs * sampleRate / 1000f).toInt().coerceIn(0, sampleRate)
        preDelay = Array(channels) { FloatArray(delaySamples) }
    }

    fun process(frame: FloatArray) {
        if (!enabled) return
        val dryFrames = FloatArray(channels) { frame[it] }
        val wetFrames = FloatArray(channels)
        for (channel in 0 until channels) {
            val delayed = readPreDelay(channel, dryFrames[channel])
            wetFrames[channel] = combs[channel].fold(0f) { sum, comb -> sum + comb.process(delayed * INPUT_GAIN) }
            allpasses[channel].forEach { wetFrames[channel] = it.process(wetFrames[channel]) }
        }
        if (preDelay[0].isNotEmpty()) preDelayIndex = (preDelayIndex + 1) % preDelay[0].size
        // Width mixes the opposite channel into the tail; mono ignores it.
        val opposite = if (channels > 1) {
            FloatArray(channels) { wetFrames[(it + 1) % channels] }
        } else FloatArray(channels)
        for (channel in 0 until channels) {
            frame[channel] = dryFrames[channel] * dry + wetFrames[channel] * wet1 + opposite[channel] * wet2
        }
    }

    private fun readPreDelay(channel: Int, input: Float): Float {
        val buffer = preDelay[channel]
        if (buffer.isEmpty()) return input
        val delayed = buffer[preDelayIndex]
        buffer[preDelayIndex] = input
        return delayed
    }

    private fun scaled(tuning: Int, channel: Int, sampleRate: Int): Int {
        val spread = if (channel % 2 == 1) STEREO_SPREAD else 0
        return ((tuning + spread) * sampleRate / 44_100f).toInt().coerceAtLeast(8)
    }

    private class Comb(
        size: Int,
        private val feedback: Float,
        private val damp1: Float,
        private val damp2: Float,
    ) {
        private val buffer = FloatArray(size)
        private var index = 0
        private var store = 0f

        fun process(input: Float): Float {
            val output = buffer[index]
            store = output * damp2 + store * damp1
            buffer[index] = input + store * feedback
            if (++index >= buffer.size) index = 0
            return output
        }
    }

    private class AllPass(size: Int) {
        private val buffer = FloatArray(size)
        private var index = 0

        fun process(input: Float): Float {
            val buffered = buffer[index]
            buffer[index] = input + buffered * 0.5f
            if (++index >= buffer.size) index = 0
            return buffered - input
        }
    }

    private companion object {
        val COMB_TUNING = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
        val ALLPASS_TUNING = intArrayOf(556, 441, 341, 225)
        const val STEREO_SPREAD = 23
        const val INPUT_GAIN = 0.015f
    }
}
