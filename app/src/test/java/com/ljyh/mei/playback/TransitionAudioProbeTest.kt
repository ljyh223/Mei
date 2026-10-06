package com.ljyh.mei.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TransitionAudioProbeTest {
    @Test
    fun emptyPipelineInputProducesNoOutput() {
        val processor = TransitionAudioProbe()
        processor.configure(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)

        processor.queueInput(AudioProcessor.EMPTY_BUFFER)

        assertFalse(processor.getOutput().hasRemaining())
    }

    @Test
    fun reusingPreviousOutputAsInputDoesNotCopyBufferIntoItself() {
        val processor = TransitionAudioProbe()
        processor.configure(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)

        val samples = byteArrayOf(1, 0, 2, 0, 3, 0, 4, 0)
        val input = ByteBuffer.allocateDirect(samples.size).order(ByteOrder.nativeOrder())
        input.put(samples)
        input.flip()

        processor.queueInput(input)
        val firstOutput = processor.getOutput()
        processor.queueInput(firstOutput)
        val secondOutput = processor.getOutput()
        val actual = ByteArray(secondOutput.remaining())
        secondOutput.get(actual)

        assertArrayEquals(samples, actual)
    }
}
