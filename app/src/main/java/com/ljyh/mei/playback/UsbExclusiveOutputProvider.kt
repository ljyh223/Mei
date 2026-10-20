package com.ljyh.mei.playback

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.media.AudioRouting
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutput
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToLong

internal fun AudioDeviceInfo.isUsbAudioOutput() =
    type == AudioDeviceInfo.TYPE_USB_HEADSET || type == AudioDeviceInfo.TYPE_USB_DEVICE

/** Exact for PCM values with at most 24 significant bits. */
internal fun floatPcmToInt32(sample: Float): Int {
    if (!sample.isFinite()) return 0
    return (sample.toDouble() * 2_147_483_648.0).roundToLong()
        .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}

/** Android's USB bit-perfect mix port, with exact float-to-int32 conversion for 16/24-bit PCM. */
class UsbExclusiveOutputProvider(
    context: Context,
    private val mediaAttributes: android.media.AudioAttributes,
    private val enabled: () -> Boolean,
) : AudioOutputProvider {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val delegate = AudioTrackAudioOutputProvider.Builder(context).build()
    private var selectedDevice: AudioDeviceInfo? = null
    private var outputOwner: Any? = null
    private var statusAttempt: Any? = null

    override fun getFormatSupport(formatConfig: AudioOutputProvider.FormatConfig) =
        delegate.getFormatSupport(formatConfig)

    override fun getOutputConfig(formatConfig: AudioOutputProvider.FormatConfig) =
        delegate.getOutputConfig(formatConfig)

    fun supportsBitPerfect(format: Format): Boolean {
        if (!enabled() || Build.VERSION.SDK_INT < 34) return false
        if (format.channelCount != 2) {
            reportFallback("这首歌的声道格式不支持独占，使用普通输出")
            return false
        }
        val encoding = when (format.pcmEncoding) {
            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> AudioFormat.ENCODING_PCM_32BIT
            else -> AudioFormat.ENCODING_PCM_16BIT
        }
        val supported = findRoute(encoding, format.sampleRate, format.channelCount) != null
        if (!supported) reportFallback("这首歌的格式或解码器不支持独占，使用普通输出")
        return supported
    }

    @RequiresApi(34)
    private fun findRoute(encoding: Int, sampleRate: Int, channelCount: Int): Pair<AudioDeviceInfo, AudioMixerAttributes>? {
        for (device in audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter { it.isUsbAudioOutput() }) {
            val attributes = runCatching { audioManager.getSupportedMixerAttributes(device) }
                .onFailure { Log.w("UsbExclusiveOutput", "Cannot query USB mixer attributes", it) }
                .getOrNull()?.firstOrNull {
                    val format = it.format
                    val channels = Integer.bitCount(
                        if (format.channelMask != 0) format.channelMask else format.channelIndexMask
                    )
                    it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT &&
                        format.encoding == encoding && format.sampleRate == sampleRate && channels == channelCount
                }
            if (attributes != null) return device to attributes
        }
        return null
    }

    override fun getAudioOutput(config: AudioOutputProvider.OutputConfig): AudioOutput {
        val targetEncoding = when (config.encoding) {
            C.ENCODING_PCM_FLOAT -> AudioFormat.ENCODING_PCM_32BIT
            C.ENCODING_PCM_16BIT -> AudioFormat.ENCODING_PCM_16BIT
            else -> null
        }
        if (!enabled() || Build.VERSION.SDK_INT < 34 || targetEncoding == null) {
            clearPreferredFormat()
            return delegate.getAudioOutput(config)
        }

        val route = findRoute(targetEncoding, config.sampleRate, Integer.bitCount(config.channelMask))
        if (route == null) {
            clearPreferredFormat("这首歌的格式或解码器不支持独占，使用普通输出")
            return delegate.getAudioOutput(config)
        }
        val (device, supported) = route
        // A previous output may have selected a different USB device.
        clearPreferredFormat()
        val owner = UsbOutputState.mixerPreferences.acquire(device.id) {
            runCatching { audioManager.setPreferredMixerAttributes(mediaAttributes, device, supported) }
                .onFailure { Log.w("UsbExclusiveOutput", "Cannot set preferred USB mixer attributes", it) }
                .getOrDefault(false)
        }
        if (owner == null) {
            Log.w("UsbExclusiveOutput", "No bit-perfect route: device=${device.productName}, " +
                "encoding=$targetEncoding, rate=${config.sampleRate}, mask=${config.channelMask}")
            clearPreferredFormat("系统未接受 USB 独占请求，使用普通输出")
            return delegate.getAudioOutput(config)
        }

        selectedDevice = device
        outputOwner = owner
        val attempt = UsbOutputState.store.beginAttempt()
        statusAttempt = attempt
        UsbOutputState.store.update(attempt, UsbOutputStatus(
            message = "正在请求 USB 独占 · ${device.productName}", deviceId = device.id,
        ))
        var createdOutput: AudioTrackAudioOutput? = null
        try {
            val trackConfig = if (config.encoding == C.ENCODING_PCM_FLOAT) {
                config.buildUpon().setEncoding(C.ENCODING_PCM_32BIT).build()
            } else config
            val output = delegate.getAudioOutput(trackConfig)
            createdOutput = output
            check(output.audioTrack.setPreferredDevice(device)) { "USB routing request rejected" }
            val tracked = observeOutput(output, device, supported, attempt) {
                if (outputOwner === owner) clearPreferredFormat()
            }
            return if (config.encoding == C.ENCODING_PCM_FLOAT) FloatToInt32AudioOutput(tracked) else tracked
        } catch (error: Exception) {
            Log.w("UsbExclusiveOutput", "USB output failed; using ordinary output", error)
            createdOutput?.release()
            clearPreferredFormat("USB 独占启动失败，已用普通输出")
            return delegate.getAudioOutput(config)
        }
    }

    fun reportFallback(message: String) = UsbOutputState.store.fallback(message)

    fun clearPreferredFormat(message: String = "") {
        if (Build.VERSION.SDK_INT >= 34) {
            selectedDevice?.let { device ->
                UsbOutputState.mixerPreferences.release(device.id, outputOwner) {
                    runCatching { audioManager.clearPreferredMixerAttributes(mediaAttributes, device) }
                        .onFailure { Log.w("UsbExclusiveOutput", "Cannot clear USB mixer attributes", it) }
                }
            }
        }
        selectedDevice = null
        outputOwner = null
        UsbOutputState.store.finish(statusAttempt)
        statusAttempt = null
        if (message.isNotEmpty()) reportFallback(message)
    }

    @RequiresApi(34)
    private fun observeOutput(
        output: AudioTrackAudioOutput,
        device: AudioDeviceInfo,
        mixerAttributes: AudioMixerAttributes,
        attempt: Any?,
        onRelease: () -> Unit,
    ): AudioOutput {
        val handler = Handler(checkNotNull(Looper.myLooper()))
        var released = false
        var playing = false
        fun updateStatus() {
            if (released) return
            val routedDevice = output.audioTrack.routedDevice
            val preferred = runCatching {
                audioManager.getPreferredMixerAttributes(mediaAttributes, device) == mixerAttributes
            }.getOrDefault(false)
            val detail = "${device.productName} · ${mixerAttributes.format.sampleRate / 1000.0} kHz · " +
                if (mixerAttributes.format.encoding == AudioFormat.ENCODING_PCM_32BIT) "32-bit 容器" else "16 bit"
            val observation = UsbOutputObservation(playing, routedDevice?.id, preferred)
            UsbOutputState.store.update(attempt, observation.statusFor(device.id, detail))
        }
        val routingListener = AudioRouting.OnRoutingChangedListener { updateStatus() }
        val mixerListener = AudioManager.OnPreferredMixerAttributesChangedListener { attributes, changedDevice, _ ->
            if (attributes == mediaAttributes && changedDevice.id == device.id) updateStatus()
        }
        output.audioTrack.addOnRoutingChangedListener(routingListener, handler)
        val mixerListenerRegistered = runCatching {
            audioManager.addOnPreferredMixerAttributesChangedListener({ task -> handler.post(task) }, mixerListener)
        }.onFailure { Log.w("UsbExclusiveOutput", "Cannot observe USB mixer attributes", it) }.isSuccess
        updateStatus()
        return object : AudioOutput by output {
            override fun play() {
                output.play()
                playing = true
                updateStatus()
            }

            override fun pause() {
                output.pause()
                playing = false
                updateStatus()
            }

            override fun stop() {
                output.stop()
                playing = false
                updateStatus()
            }

            // Keep this sink on the USB device selected for its mixer profile.
            override fun setPreferredDevice(preferredDevice: AudioDeviceInfo?) {
                output.setPreferredDevice(device)
            }

            override fun release() {
                if (released) return
                released = true
                output.audioTrack.removeOnRoutingChangedListener(routingListener)
                if (mixerListenerRegistered) {
                    runCatching { audioManager.removeOnPreferredMixerAttributesChangedListener(mixerListener) }
                }
                try {
                    output.release()
                } finally {
                    onRelease()
                }
            }
        }
    }

    override fun addListener(listener: AudioOutputProvider.Listener) = delegate.addListener(listener)
    override fun removeListener(listener: AudioOutputProvider.Listener) = delegate.removeListener(listener)
    override fun setClock(clock: androidx.media3.common.util.Clock) = delegate.setClock(clock)
    override fun hasPendingReleases() = delegate.hasPendingReleases()
    override fun release() {
        clearPreferredFormat()
        delegate.release()
    }
}

private class FloatToInt32AudioOutput(private val output: AudioOutput) : AudioOutput by output {
    private var sourceBuffer: ByteBuffer? = null
    private var convertedBuffer: ByteBuffer? = null
    private var scratch = ByteBuffer.allocateDirect(0).order(ByteOrder.LITTLE_ENDIAN)
    private var sourceStart = 0

    override fun write(buffer: ByteBuffer, encodedAccessUnitCount: Int, presentationTimeUs: Long): Boolean {
        if (sourceBuffer !== buffer || convertedBuffer == null) {
            require(buffer.remaining() % 4 == 0) { "Unaligned float PCM buffer" }
            sourceBuffer = buffer
            sourceStart = buffer.position()
            val input = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            if (scratch.capacity() < input.remaining()) {
                scratch = ByteBuffer.allocateDirect(input.remaining()).order(ByteOrder.LITTLE_ENDIAN)
            }
            convertedBuffer = scratch.apply {
                clear()
                while (input.hasRemaining()) {
                    putInt(floatPcmToInt32(input.float))
                }
                flip()
            }
        }
        val converted = checkNotNull(convertedBuffer)
        val complete = output.write(converted, encodedAccessUnitCount, presentationTimeUs)
        check(converted.position() % 4 == 0) { "Unaligned int32 PCM write" }
        buffer.position(sourceStart + converted.position())
        if (complete) {
            sourceBuffer = null
            convertedBuffer = null
        }
        return complete
    }

    override fun flush() {
        sourceBuffer = null
        convertedBuffer = null
        output.flush()
    }
}
