package com.ljyh.mei.playback.usb

import android.media.AudioDeviceInfo
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.audio.AudioOutput
import java.nio.ByteBuffer
import kotlin.math.min

internal interface UsbPcmTransport {
    fun write(buffer: ByteBuffer): Int
    fun queuedFrames(): Long
    fun completedFrames(): Long
    fun capacityFrames(): Long
    fun play(): Boolean
    fun pause(): Boolean
    fun flush(): Boolean
    fun drain(): Boolean
    fun failure(): String?
    fun close()
}

/** No AudioTrack is created while libusb owns the USB interfaces. */
internal class UsbPcmAudioOutput(
    private val transport: UsbPcmTransport,
    private val rate: Int,
    private val inputFrameBytes: Int,
    private val sessionId: Int,
    private val nowMs: () -> Long,
    private val onActiveChanged: (Boolean) -> Unit,
    private val onFailure: (String) -> Unit,
    private val onRelease: () -> Unit,
    private val outputRate: Int = rate,
    private val prerollMs: () -> Int = { 300 },
) : AudioOutput {
    private val listeners = linkedSetOf<AudioOutput.Listener>()
    private var playing = false
    private var started = false
    private var resuming = false
    private var draining = false
    private var closed = false
    private var failed = false
    private var writeAttempted = false
    private var acceptedFrames = 0L
    private var lastCompleted = 0L
    private var lastProgressMs = 0L
    private var advancingReported = false

    override fun write(buffer: ByteBuffer, encodedAccessUnitCount: Int, presentationTimeUs: Long): Boolean {
        if (closed || failed) return false
        require(buffer.remaining() % inputFrameBytes == 0) { "Unaligned USB PCM" }
        val remaining = buffer.remaining()
        if (remaining == 0) return true
        if (draining) {
            draining = false
            advancingReported = false
        }
        writeAttempted = true
        val written = try { transport.write(buffer) } catch (error: Exception) {
            fail(error.message ?: "USB PCM write failed")
            return false
        }
        if (written < 0 || written > remaining || written % inputFrameBytes != 0) {
            fail("USB PCM write failed: $written")
            return false
        }
        buffer.position(buffer.position() + written)
        acceptedFrames += written / inputFrameBytes
        startIfReady()
        checkProgress()
        return !buffer.hasRemaining()
    }

    override fun play() {
        if (closed || failed || playing) return
        playing = true
        lastProgressMs = nowMs()
        startIfReady()
    }

    private fun startIfReady() {
        if (!playing || started || closed || failed) return
        val queued = transport.queuedFrames()
        val threshold = min(outputRate * prerollMs().toLong() / 1000, (transport.capacityFrames() * 3 / 4).coerceAtLeast(1))
        if (queued <= 0 || (!draining && !resuming && queued < threshold)) return
        val startedSuccessfully = try { transport.play() } catch (_: Exception) { false }
        if (!startedSuccessfully) {
            fail("USB transport start failed")
            return
        }
        started = true
        resuming = false
        lastProgressMs = nowMs()
    }

    override fun pause() {
        if (closed) return
        playing = false
        if (started && !runCatching { transport.pause() }.getOrDefault(false)) fail("USB transport pause failed")
        resuming = resuming || started
        started = false
        advancingReported = false
        onActiveChanged(false)
    }

    override fun stop() {
        if (closed) return
        // AudioOutput.stop means drain, not discard. Short songs must start below the preroll threshold.
        draining = true
        runCatching { transport.drain() }.onFailure { fail("USB drain failed") }
        startIfReady()
    }

    override fun flush() {
        if (closed) return
        if (!runCatching { transport.flush() }.getOrDefault(false)) fail("USB transport flush failed")
        started = false
        resuming = false
        draining = false
        writeAttempted = false
        acceptedFrames = 0
        lastCompleted = 0
        advancingReported = false
        onActiveChanged(false)
    }

    override fun getPositionUs(): Long {
        if (closed) return lastCompleted * 1_000_000 / outputRate
        checkProgress()
        return lastCompleted * 1_000_000 / outputRate
    }

    private fun checkProgress() {
        if (closed || failed) return
        val completed = transport.completedFrames().coerceIn(0, acceptedOutputFrames())
        if (completed > lastCompleted) {
            lastCompleted = completed
            lastProgressMs = nowMs()
            if (playing && !advancingReported) {
                advancingReported = true
                onActiveChanged(true)
                listeners.toList().forEach { it.onPositionAdvancing(System.currentTimeMillis()) }
            }
        }
        if (!playing) return
        val error = transport.failure()
        val queued = transport.queuedFrames()
        if (error != null) fail(error)
        else if (((started && queued > 0) || (!started && queued == 0L && writeAttempted)) &&
            nowMs() - lastProgressMs >= 3_000
        ) fail("USB transfer made no progress for 3000 ms")
        if (draining && lastCompleted >= acceptedOutputFrames()) onActiveChanged(false)
    }

    private fun acceptedOutputFrames() = acceptedFrames * outputRate / rate

    private fun fail(reason: String) {
        if (failed || closed) return
        failed = true
        onActiveChanged(false)
        onFailure(reason)
    }

    override fun release() {
        if (closed) return
        closed = true
        try { transport.close() } finally {
            onRelease()
            listeners.toList().forEach { it.onReleased() }
            listeners.clear()
        }
    }

    override fun getAudioSessionId() = sessionId
    override fun getSampleRate() = rate
    override fun getBufferSizeInFrames() = transport.capacityFrames() * rate / outputRate
    override fun isOffloadedPlayback() = false
    // Recovery is owned by the service; do not let DefaultAudioSink reopen the same failing route forever.
    override fun isStalled() = false
    override fun getPlaybackParameters(): PlaybackParameters = PlaybackParameters.DEFAULT
    override fun setPlaybackParameters(playbackParams: PlaybackParameters) = Unit
    override fun setVolume(volume: Float) = Unit
    override fun attachAuxEffect(effectId: Int) = Unit
    override fun setAuxEffectSendLevel(level: Float) = Unit
    override fun setOffloadDelayPadding(delayInFrames: Int, paddingInFrames: Int) = Unit
    override fun setOffloadEndOfStream() = Unit
    override fun setPreferredDevice(preferredDevice: AudioDeviceInfo?) = Unit
    override fun addListener(listener: AudioOutput.Listener) { listeners += listener }
    override fun removeListener(listener: AudioOutput.Listener) { listeners -= listener }
}
