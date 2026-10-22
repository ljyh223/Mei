package com.ljyh.mei.playback.equalizer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/** Exercises the real AudioProcessor I/O paths used inside the playback chain. */
class DspProcessorIoTest {

    private fun format(encoding: Int) = AudioProcessor.AudioFormat(48_000, 2, encoding)

    private fun buffer16(frames: Int, sample: (Int) -> Short): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(frames * 2 * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames) { index ->
            val value = sample(index)
            buffer.putShort(value)
            buffer.putShort(value)
        }
        buffer.flip()
        return buffer
    }

    @Test
    fun reverbPassthroughWhenDisabled() {
        val processor = FreeverbProcessor()
        processor.configure(format(C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.setProfile(ReverbProfile.Off)
        processor.queueInput(buffer16(64) { 12_345 })
        val output = processor.getOutput().order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(12_345, output.getShort(0).toInt())
        assertEquals(12_345, output.getShort(output.limit() - 2).toInt())
    }

    @Test
    fun reverbProducesTailAfterImpulse() {
        val processor = FreeverbProcessor()
        processor.configure(format(C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.setProfile(ReverbProfile.Hall)
        val frames = 9_600 // 200 ms
        processor.queueInput(buffer16(frames) { index -> if (index == 0) 32_000 else 0 })
        val output = processor.getOutput().order(ByteOrder.LITTLE_ENDIAN)
        var tailEnergy = 0.0
        for (index in 4_800 until frames) {
            val value = output.getShort(index * 4).toDouble()
            tailEnergy += value * value
            assertTrue(value.isFinite())
        }
        assertTrue("reverb tail should be audible, energy=$tailEnergy", tailEnergy > 1.0)
    }

    @Test
    fun limiterCapsLoudPcmSamples() {
        val processor = SoftLimiterProcessor()
        processor.configure(format(C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.configure(enabled = true, thresholdDb = -6f)
        processor.queueInput(buffer16(256) { 32_767 })
        val output = processor.getOutput().order(ByteOrder.LITTLE_ENDIAN)
        val last = abs(output.getShort(output.limit() - 4).toInt()) / 32_768.0
        assertTrue("peak $last should stay near -6 dBFS", last <= 0.52)
        assertTrue("limiter must not mute", last > 0.35)
    }

    @Test
    fun limiterPassesQuietPcmSamples() {
        val processor = SoftLimiterProcessor()
        processor.configure(format(C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.configure(enabled = true, thresholdDb = -1f)
        processor.queueInput(buffer16(64) { 3_000 })
        val output = processor.getOutput().order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(3_000, output.getShort(output.limit() - 2).toInt())
    }

    @Test
    fun processorsAcceptFloatPcm() {
        val reverb = FreeverbProcessor()
        reverb.configure(format(C.ENCODING_PCM_FLOAT))
        reverb.flush()
        reverb.setProfile(ReverbProfile.Off)
        val floatInput = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.LITTLE_ENDIAN)
        repeat(4) { floatInput.putFloat(0.25f); floatInput.putFloat(-0.25f) }
        floatInput.flip()
        reverb.queueInput(floatInput)
        val floatOutput = reverb.getOutput().order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0.25f, floatOutput.getFloat(0), 1e-6f)

        val limiter = SoftLimiterProcessor()
        limiter.configure(format(C.ENCODING_PCM_FLOAT))
        limiter.flush()
        limiter.configure(enabled = true, thresholdDb = -6f)
        val loud = ByteBuffer.allocateDirect(4 * 4).order(ByteOrder.LITTLE_ENDIAN)
        repeat(2) { loud.putFloat(2f); loud.putFloat(-2f) }
        loud.flip()
        limiter.queueInput(loud)
        val limited = limiter.getOutput().order(ByteOrder.LITTLE_ENDIAN)
        assertTrue(abs(limited.getFloat(0)) <= 0.52f)
    }
}
