package com.ljyh.mei.playback

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.MutableStateFlow

/** The actual output, rather than the requested setting. */
data class UsbOutputStatus(val active: Boolean = false, val message: String = "")

object UsbOutputState {
    val status = MutableStateFlow(UsbOutputStatus())
}

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

    override fun getFormatSupport(formatConfig: AudioOutputProvider.FormatConfig) =
        delegate.getFormatSupport(formatConfig)

    override fun getOutputConfig(formatConfig: AudioOutputProvider.FormatConfig) =
        delegate.getOutputConfig(formatConfig)

    fun supportsBitPerfect(format: Format): Boolean {
        if (!enabled() || Build.VERSION.SDK_INT < 34 || format.channelCount != 2) return false
        val encoding = when (format.pcmEncoding) {
            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> AudioFormat.ENCODING_PCM_32BIT
            else -> AudioFormat.ENCODING_PCM_16BIT
        }
        val device = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_DEVICE
        }
        if (device == null) {
            UsbOutputState.status.value = UsbOutputStatus(message = "未连接 USB 解码器，已用普通输出")
            return false
        }
        val supportedAttributes = runCatching { audioManager.getSupportedMixerAttributes(device) }
            .onFailure { Log.w("UsbExclusiveOutput", "Cannot query USB mixer attributes", it) }
            .getOrNull()
        val supported = supportedAttributes?.any { attributes ->
            val mixerFormat = attributes.format
            val mixerChannels = Integer.bitCount(
                if (mixerFormat.channelMask != 0) mixerFormat.channelMask else mixerFormat.channelIndexMask
            )
            attributes.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT &&
                mixerFormat.encoding == encoding &&
                mixerFormat.sampleRate == format.sampleRate &&
                mixerChannels == format.channelCount
        } == true
        if (!supported) {
            UsbOutputState.status.value = UsbOutputStatus(message = "这首歌的格式不支持独占，已用普通输出")
        }
        return supported
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

        val device = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_DEVICE
        }
        if (device == null) {
            clearPreferredFormat("未连接 USB 解码器，已用普通输出")
            return delegate.getAudioOutput(config)
        }
        val supported = runCatching { audioManager.getSupportedMixerAttributes(device) }
            .onFailure { Log.w("UsbExclusiveOutput", "Cannot query USB mixer attributes", it) }
            .getOrNull()?.firstOrNull { attributes ->
            val format = attributes.format
            val outputChannels = Integer.bitCount(config.channelMask)
            val mixerChannels = Integer.bitCount(
                if (format.channelMask != 0) format.channelMask else format.channelIndexMask
            )
            attributes.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT &&
                format.encoding == targetEncoding &&
                format.sampleRate == config.sampleRate &&
                mixerChannels == outputChannels
        }
        val selected = supported != null && runCatching {
            audioManager.setPreferredMixerAttributes(mediaAttributes, device, supported)
        }.onFailure { Log.w("UsbExclusiveOutput", "Cannot set preferred USB mixer attributes", it) }
            .getOrDefault(false)
        if (!selected) {
            Log.w("UsbExclusiveOutput", "No bit-perfect route: device=${device.productName}, " +
                "encoding=$targetEncoding, rate=${config.sampleRate}, mask=${config.channelMask}, " +
                "profileFound=${supported != null}")
            clearPreferredFormat("这首歌的格式不支持独占，已用普通输出")
            return delegate.getAudioOutput(config)
        }

        try {
            selectedDevice = device
            val trackConfig = if (config.encoding == C.ENCODING_PCM_FLOAT) {
                config.buildUpon().setEncoding(C.ENCODING_PCM_32BIT).build()
            } else config
            val output = delegate.getAudioOutput(trackConfig)
            output.setPreferredDevice(device)
            UsbOutputState.status.value = UsbOutputStatus(
                active = true,
                message = "已请求 USB 独占 · ${config.sampleRate / 1000.0} kHz · ${if (targetEncoding == AudioFormat.ENCODING_PCM_32BIT) "32-bit 容器" else "16 bit"}",
            )
            return if (config.encoding == C.ENCODING_PCM_FLOAT) FloatToInt32AudioOutput(output) else output
        } catch (error: Exception) {
            Log.w("UsbExclusiveOutput", "USB output failed; using ordinary output", error)
            clearPreferredFormat("USB 独占启动失败，已用普通输出")
            return delegate.getAudioOutput(config)
        }
    }

    fun clearPreferredFormat(message: String = "") {
        selectedDevice?.let { device ->
            runCatching { audioManager.clearPreferredMixerAttributes(mediaAttributes, device) }
        }
        selectedDevice = null
        UsbOutputState.status.value = UsbOutputStatus(message = message)
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
