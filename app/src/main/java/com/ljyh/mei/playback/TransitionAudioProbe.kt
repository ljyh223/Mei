package com.ljyh.mei.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/** A pass-through PCM probe. It observes sound before the deck volume is applied. */
internal class TransitionAudioProbe : BaseAudioProcessor() {
    @Volatile private var measurements: List<Float> = emptyList()
    private var pendingPower = 0.0
    private var pendingFrames = 0
    private val history = ArrayDeque<Float>()

    val envelope: List<Float> get() = measurements

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) inputAudioFormat
        else AudioProcessor.AudioFormat.NOT_SET

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) = clear()
    override fun onReset() = clear()

    private fun clear() {
        pendingPower = 0.0
        pendingFrames = 0
        history.clear()
        measurements = emptyList()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        // Media3 also queues its shared EMPTY_BUFFER while draining the processing pipeline.
        // replaceOutputBuffer(0) would return that same instance, making put(buffer) illegal.
        if (!inputBuffer.hasRemaining()) return
        val sampleRate = inputAudioFormat.sampleRate
        val channels = inputAudioFormat.channelCount
        val source = inputBuffer.duplicate()
        val inputLimit = inputBuffer.limit()
        val size = source.remaining()
        val inspect = source.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = channels * 2
        if (sampleRate > 0 && frameBytes > 0) {
            val framesPerWindow = (sampleRate / 10).coerceAtLeast(1)
            while (inspect.remaining() >= frameBytes) {
                var power = 0.0
                repeat(channels) {
                    val sample = inspect.short.toDouble() / 32768.0
                    power += sample * sample
                }
                pendingPower += power / channels
                pendingFrames++
                if (pendingFrames >= framesPerWindow) {
                    history.addLast(sqrt(pendingPower / pendingFrames).toFloat())
                    if (history.size > 160) history.removeFirst()
                    measurements = history.toList()
                    pendingPower = 0.0
                    pendingFrames = 0
                }
            }
        }
        var output = replaceOutputBuffer(size)
        if (output === inputBuffer) {
            // BaseAudioProcessor may reuse its previous output as this input. Allocate a distinct
            // output before copying so ByteBuffer.put never receives the destination as source.
            output = replaceOutputBuffer(output.capacity() + 1)
        }
        output.put(source)
        output.flip()
        inputBuffer.limit(inputLimit)
        inputBuffer.position(inputLimit)
    }
}
