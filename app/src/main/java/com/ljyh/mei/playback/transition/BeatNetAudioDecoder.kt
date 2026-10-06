package com.ljyh.mei.playback.transition

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.ljyh.mei.constants.UserAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class BeatNetPcm(val samples: FloatArray, val sampleRate: Int)

/** Decodes only the requested audio window on a background dispatcher. */
internal class BeatNetAudioDecoder(private val context: Context) {
    suspend fun decode(uri: Uri, startMs: Long, windowMs: Long = 32_000L): BeatNetPcm =
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            var decoder: MediaCodec? = null
            try {
                if (uri.scheme == "http" || uri.scheme == "https") {
                    extractor.setDataSource(uri.toString(), mapOf("User-Agent" to UserAgent))
                } else {
                    extractor.setDataSource(context, uri, emptyMap())
                }
                val track = (0 until extractor.trackCount).firstOrNull { i ->
                    extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                } ?: error("No audio track")
                val format = extractor.getTrackFormat(track)
                val mime = requireNotNull(format.getString(MediaFormat.KEY_MIME))
                extractor.selectTrack(track)
                val startUs = startMs * 1_000L
                val endUs = (startMs + windowMs) * 1_000L
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                decoder = MediaCodec.createDecoderByType(mime).apply {
                    configure(format, null, null, 0)
                    start()
                }
                val codec = decoder
                val outputInfo = MediaCodec.BufferInfo()
                var sampleRate = format.intOr(MediaFormat.KEY_SAMPLE_RATE, 44_100)
                var channels = format.intOr(MediaFormat.KEY_CHANNEL_COUNT, 2)
                var encoding = AudioFormat.ENCODING_PCM_16BIT
                val chunks = ArrayList<FloatArray>()
                var total = 0
                var inputEnded = false
                var outputEnded = false
                var emptyPolls = 0
                while (!outputEnded && emptyPolls < 500) {
                    currentCoroutineContext().ensureActive()
                    if (!inputEnded) {
                        val index = codec.dequeueInputBuffer(10_000)
                        if (index >= 0) {
                            val buffer = requireNotNull(codec.getInputBuffer(index))
                            val timestamp = extractor.sampleTime
                            val bytes = if (timestamp < 0 || timestamp >= endUs) -1
                                else extractor.readSampleData(buffer, 0)
                            if (bytes < 0) {
                                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnded = true
                            } else {
                                codec.queueInputBuffer(index, 0, bytes, timestamp, 0)
                                extractor.advance()
                            }
                        }
                    }
                    when (val index = codec.dequeueOutputBuffer(outputInfo, 10_000)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val decodedFormat = codec.outputFormat
                            sampleRate = decodedFormat.intOr(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                            channels = decodedFormat.intOr(MediaFormat.KEY_CHANNEL_COUNT, channels)
                            encoding = decodedFormat.intOr(MediaFormat.KEY_PCM_ENCODING, encoding)
                            emptyPolls = 0
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> emptyPolls++
                        else -> if (index >= 0) {
                            emptyPolls = 0
                            codec.getOutputBuffer(index)?.let { output ->
                                val chunk = readMono(output, outputInfo, channels, sampleRate,
                                    encoding, startUs, endUs)
                                if (chunk.isNotEmpty()) { chunks.add(chunk); total += chunk.size }
                            }
                            outputEnded = outputInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(index, false)
                        }
                    }
                }
                check(outputEnded && total > 0) { "Audio analysis window could not be decoded" }
                val samples = FloatArray(total)
                var cursor = 0
                for (chunk in chunks) {
                    chunk.copyInto(samples, cursor)
                    cursor += chunk.size
                }
                BeatNetPcm(samples, sampleRate)
            } finally {
                runCatching { decoder?.stop() }
                decoder?.release()
                extractor.release()
            }
        }

    private fun readMono(
        output: ByteBuffer,
        info: MediaCodec.BufferInfo,
        channels: Int,
        sampleRate: Int,
        encoding: Int,
        startUs: Long,
        endUs: Long,
    ): FloatArray {
        val sampleBytes = when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT, AudioFormat.ENCODING_PCM_32BIT -> 4
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_16BIT -> 2
            else -> error("Unsupported PCM format $encoding")
        }
        val count = info.size / (sampleBytes * channels.coerceAtLeast(1))
        if (count == 0) return FloatArray(0)
        val first = (((startUs - info.presentationTimeUs) * sampleRate) / 1_000_000L)
            .toInt().coerceIn(0, count)
        val last = (((endUs - info.presentationTimeUs) * sampleRate) / 1_000_000L)
            .toInt().coerceIn(first, count)
        val source = output.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        source.position(info.offset + first * sampleBytes * channels)
        val mono = FloatArray(last - first)
        for (frame in mono.indices) {
            var sum = 0f
            repeat(channels) { sum += source.readSample(encoding) }
            mono[frame] = sum / channels
        }
        return mono
    }

    private fun ByteBuffer.readSample(encoding: Int): Float = when (encoding) {
        AudioFormat.ENCODING_PCM_FLOAT -> float.takeIf(Float::isFinite) ?: 0f
        AudioFormat.ENCODING_PCM_32BIT -> int / 2_147_483_648f
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
            val low = get().toInt() and 0xff
            val middle = (get().toInt() and 0xff) shl 8
            val high = get().toInt() shl 16
            (low or middle or high) / 8_388_608f
        }
        AudioFormat.ENCODING_PCM_8BIT -> ((get().toInt() and 0xff) - 128) / 128f
        else -> short / 32768f
    }

    private fun MediaFormat.intOr(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback
}
