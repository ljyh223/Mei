package com.ljyh.mei.playback.source.lx

import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.extractor.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.EOFException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Reads bounded ranges, including MP4 moov/Xing/FLAC metadata, rather than downloading the song. */
@Singleton
class AudioUrlProbe @Inject constructor() {
    private val client = OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS).callTimeout(4, TimeUnit.SECONDS).build()

    suspend fun durationMs(url: String): Long? = runInterruptible(Dispatchers.IO) {
        val reader = RangeReader(url)
        val extractors = DefaultExtractorsFactory().setMp3ExtractorFlags(
            androidx.media3.extractor.mp3.Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING
        ).createExtractors()
        try {
            val extractor = extractors.firstOrNull {
                reader.position = 0
                val input = DefaultExtractorInput(reader, 0, reader.length)
                try { it.sniff(input) } catch (_: Exception) { false }
            } ?: return@runInterruptible null
            reader.position = 0
            var input = DefaultExtractorInput(reader, 0, reader.length)
            var durationUs = C.TIME_UNSET
            var hasAudio = false
            extractor.init(object : ExtractorOutput {
                override fun track(id: Int, type: Int): TrackOutput {
                    if (type == C.TRACK_TYPE_AUDIO) hasAudio = true
                    return DiscardingTrackOutput()
                }
                override fun endTracks() = Unit
                override fun seekMap(seekMap: SeekMap) { durationUs = seekMap.durationUs }
            })
            val seek = PositionHolder()
            var steps = 0
            while (durationUs == C.TIME_UNSET && steps++ < 1024 && !Thread.currentThread().isInterrupted) {
                when (extractor.read(input, seek)) {
                    Extractor.RESULT_END_OF_INPUT -> break
                    Extractor.RESULT_SEEK -> {
                        reader.position = seek.position
                        input = DefaultExtractorInput(reader, seek.position, reader.length)
                    }
                }
            }
            (durationUs.takeIf { it > 0 && hasAudio }?.div(1000))
        } catch (_: Exception) { null }
        finally { extractors.forEach(Extractor::release) }
    }

    private inner class RangeReader(private val url: String) : DataReader {
        var position = 0L
        var length = C.LENGTH_UNSET.toLong()
            private set
        private var start = -1L
        private var chunk = ByteArray(0)
        private var budget = 2 * 1024 * 1024
        private val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (Thread.currentThread().isInterrupted || System.nanoTime() > deadline) throw EOFException()
            if (this.length >= 0 && position >= this.length) return C.RESULT_END_OF_INPUT
            if (position < start || position >= start + chunk.size) {
                if (budget <= 0) throw EOFException()
                val size = minOf(budget, 128 * 1024)
                client.newCall(Request.Builder().url(url).header("Range", "bytes=$position-${position + size - 1}")
                    .header("Accept-Encoding", "identity").build()).execute().use { response ->
                    require(response.isSuccessful && (position == 0L || response.code == 206))
                    val contentType = response.header("Content-Type").orEmpty().lowercase()
                    require(!contentType.contains("json") && !contentType.contains("html") && !contentType.startsWith("text/"))
                    val contentRange = response.header("Content-Range")
                    if (response.code == 206) {
                        val match = Regex("bytes (\\d+)-\\d+/(\\d+|\\*)").matchEntire(contentRange.orEmpty())
                        require(match != null && match.groupValues[1].toLong() == position)
                        this.length = match.groupValues[2].toLongOrNull() ?: this.length
                    } else if (position == 0L) this.length = response.body.contentLength()
                    start = position
                    chunk = response.body.byteStream().readLimited(size)
                    budget -= chunk.size
                }
                if (chunk.isEmpty()) return C.RESULT_END_OF_INPUT
            }
            val count = minOf(length, (start + chunk.size - position).toInt())
            chunk.copyInto(buffer, offset, (position - start).toInt(), (position - start).toInt() + count)
            position += count
            return count
        }
    }
}
