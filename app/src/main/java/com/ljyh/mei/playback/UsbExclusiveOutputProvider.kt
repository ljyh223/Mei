package com.ljyh.mei.playback

import android.content.Context
import android.hardware.usb.UsbManager
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Clock
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import com.ljyh.mei.playback.usb.UsbNativeBridge
import com.ljyh.mei.playback.usb.UsbNativeOwnership
import com.ljyh.mei.playback.usb.UsbPcmAudioOutput
import com.ljyh.mei.playback.usb.UsbPcmTransport
import com.ljyh.mei.playback.usb.hasPcmOutput
import java.nio.ByteBuffer

internal fun AudioDeviceInfo.isUsbAudioOutput() =
    type == AudioDeviceInfo.TYPE_USB_HEADSET || type == AudioDeviceInfo.TYPE_USB_DEVICE

/** USB host permission + NeriPlayer native UAC transport, independent of AudioTrack routing. */
class UsbExclusiveOutputProvider(
    context: Context,
    private val onFailure: () -> Unit,
    private val enabled: () -> Boolean,
) : AudioOutputProvider {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val usbManager = context.getSystemService(UsbManager::class.java)
    private val delegate = AudioTrackAudioOutputProvider.Builder(context).build()
    private var sourceBits = 16
    private var currentOutput: AudioOutput? = null

    fun supportsBitPerfect(format: Format): Boolean {
        if (!enabled() || !UsbOutputState.store.nativeUsable()) return false
        if (!UsbNativeBridge.available) {
            reportFallback("无法加载 USB 独占驱动，使用普通输出")
            return false
        }
        sourceBits = when (format.pcmEncoding) {
            C.ENCODING_PCM_24BIT -> 24
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
            else -> 16
        }
        if (format.pcmEncoding == C.ENCODING_PCM_32BIT) {
            reportFallback("32 位整数歌曲暂不支持原生独占，使用普通输出")
            return false
        }
        if (format.channelCount !in 1..2 || format.sampleRate <= 0) {
            reportFallback("这首歌的格式不支持 USB 独占，使用普通输出")
            return false
        }
        val devices = usbManager.deviceList.values.filter { it.hasPcmOutput() }
        if (devices.size > 1) reportFallback("连接了多个 USB 音频设备，请只保留一个解码器")
        return devices.size == 1 && usbManager.hasPermission(devices.single())
    }

    override fun getFormatSupport(formatConfig: AudioOutputProvider.FormatConfig): AudioOutputProvider.FormatSupport {
        if (enabled()) {
            // Reject every compressed bypass/offload path. The USB transport accepts decoded PCM only.
            return AudioOutputProvider.FormatSupport.Builder().setFormatSupportLevel(
                if (formatConfig.format.sampleMimeType == MimeTypes.AUDIO_RAW) AudioOutputProvider.FORMAT_SUPPORTED_DIRECTLY
                else AudioOutputProvider.FORMAT_UNSUPPORTED
            ).build()
        }
        return delegate.getFormatSupport(formatConfig)
    }

    override fun getOutputConfig(formatConfig: AudioOutputProvider.FormatConfig): AudioOutputProvider.OutputConfig {
        val format = formatConfig.format
        val frameBytes = Util.getPcmFrameSize(format.pcmEncoding, format.channelCount)
        return AudioOutputProvider.OutputConfig.Builder()
            .setEncoding(format.pcmEncoding)
            .setSampleRate(format.sampleRate)
            .setChannelMask(Util.getAudioTrackChannelConfig(format.channelCount))
            .setBufferSize((format.sampleRate / 2).coerceAtLeast(1) * frameBytes)
            .setAudioAttributes(formatConfig.audioAttributes)
            .setAudioSessionId(formatConfig.audioSessionId)
            .build()
    }

    override fun getAudioOutput(config: AudioOutputProvider.OutputConfig): AudioOutput {
        val device = usbManager.deviceList.values.filter { it.hasPcmOutput() }.singleOrNull()
        if (!enabled() || !UsbOutputState.store.nativeUsable() || device == null || !usbManager.hasPermission(device)) {
            return delegate.getAudioOutput(config)
        }
        val owner = Any()
        if (!UsbNativeOwnership.acquire(owner)) throw AudioOutputProvider.InitializationException(
            IllegalStateException("Another decoder still owns the USB device"),
        )
        var connection: android.hardware.usb.UsbDeviceConnection? = null
        var handle = 0L
        val attempt = UsbOutputState.store.beginAttempt()
        try {
            connection = checkNotNull(usbManager.openDevice(device)) { "USB permission lost" }
            val formats = when (sourceBits) {
                16 -> listOf(16 to 2, 24 to 3, 24 to 4, 32 to 4)
                24 -> listOf(24 to 3, 24 to 4, 32 to 4)
                else -> listOf(32 to 4, 24 to 4, 24 to 3)
            }
            var outputBits = 0
            var outputBytes = 0
            for ((bits, bytes) in formats) {
                handle = UsbNativeBridge.nativeOpen(connection.fileDescriptor, config.sampleRate, 2, bits, bytes)
                if (handle != 0L) { outputBits = bits; outputBytes = bytes; break }
            }
            check(handle != 0L) { UsbNativeBridge.nativeLastOpenError() }
            check(UsbNativeBridge.nativeConfigurePlayerBufferDuration(handle, 600)) { "USB buffer configuration failed" }
            check(UsbNativeBridge.nativePreparePlayerPcm(handle, config.sampleRate, Integer.bitCount(config.channelMask), config.encoding)) {
                "USB PCM configuration failed"
            }
            val nativeHandle = handle
            val hardwareVolume = UsbNativeBridge.nativeHasHardwareVolume(nativeHandle)
            check(UsbNativeBridge.nativeSetPlayerBitPerfect(handle, hardwareVolume)) { "USB volume configuration failed" }
            fun systemVolume(): Float {
                val maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                return (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maximum).coerceIn(0f, 1f)
            }
            var lastVolume = -1f
            fun updateHardwareVolume(): Float {
                val volume = systemVolume()
                if (hardwareVolume && volume != lastVolume) {
                    check(UsbNativeBridge.nativeSetHardwareVolume(nativeHandle, volume)) { "USB hardware volume failed" }
                    lastVolume = volume
                }
                return if (hardwareVolume) 1f else volume
            }
            updateHardwareVolume()
            val nativeConnection = connection
            val transport = object : UsbPcmTransport {
                private var lastHealthCheck = 0L
                private var lastFailure: String? = null
                private val capacity = UsbNativeBridge.nativeGetPlayerPcmFreeBytes(nativeHandle) / (2 * outputBytes)
                override fun write(buffer: ByteBuffer) = UsbNativeBridge.nativeWritePlayerPcm(
                    nativeHandle, buffer, buffer.position(), buffer.remaining(), updateHardwareVolume(),
                )
                override fun queuedFrames() = UsbNativeBridge.nativeGetQueuedPlayerFrames(nativeHandle)
                override fun completedFrames() = UsbNativeBridge.nativeGetCompletedAudioFrames(nativeHandle)
                override fun capacityFrames() = capacity
                override fun play(): Boolean {
                    updateHardwareVolume()
                    return UsbNativeBridge.nativePlayPlayerPcm(nativeHandle)
                }
                override fun pause() = UsbNativeBridge.nativePausePlayerPcm(nativeHandle)
                override fun flush() = UsbNativeBridge.nativeFlushPlayerPcm(nativeHandle)
                override fun drain() = UsbNativeBridge.nativeDrainPlayerPcm(nativeHandle)
                override fun failure(): String? {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastHealthCheck >= 250) {
                        lastHealthCheck = now
                        val report = UsbNativeBridge.nativeRuntimeReport(nativeHandle)
                        if (report.contains("terminalFailure=true") || report.contains("transportFailed=true")) lastFailure = report
                    }
                    return lastFailure
                }
                override fun close() {
                    try { UsbNativeBridge.nativeClose(nativeHandle) } finally {
                        nativeConnection.close()
                        UsbNativeOwnership.release(owner)
                    }
                }
            }
            val detail = "${device.productName ?: "USB 解码器"} · ${config.sampleRate / 1000.0} kHz · $outputBits bit" +
                if (hardwareVolume) " · 硬件音量" else " · 软件音量"
            UsbOutputState.store.update(attempt, UsbOutputStatus(message = "USB 已就绪，等待播放 · $detail", deviceId = device.deviceId))
            val output = UsbPcmAudioOutput(
                transport = transport,
                rate = config.sampleRate,
                inputFrameBytes = Util.getPcmFrameSize(config.encoding, Integer.bitCount(config.channelMask)),
                sessionId = config.audioSessionId,
                nowMs = SystemClock::elapsedRealtime,
                onActiveChanged = { active ->
                    UsbOutputState.store.update(attempt, UsbOutputStatus(
                        active, if (active) "USB 原生独占输出 · $detail" else "USB 已就绪，等待播放 · $detail", device.deviceId,
                    ))
                },
                onFailure = { error -> fail(error, attempt) },
                onRelease = { UsbOutputState.store.finish(attempt) },
            )
            currentOutput = output
            return output
        } catch (error: Exception) {
            try { if (handle != 0L) UsbNativeBridge.nativeClose(handle) } finally {
                connection?.close()
                UsbNativeOwnership.release(owner)
            }
            fail(error.message ?: "USB open failed", attempt)
            // The service rebuilds the regular sink after the interfaces have been released.
            throw AudioOutputProvider.InitializationException(error)
        }
    }

    private fun fail(error: String, attempt: Any?) {
        Log.w("UsbExclusiveOutput", error)
        if (UsbOutputState.store.blockNative(attempt, "USB 独占启动或传输失败，已暂停；可关闭独占后重试")) onFailure()
    }

    fun reportFallback(message: String) = UsbOutputState.store.fallback(message)
    fun clearPreferredFormat(message: String = "") {
        currentOutput?.release()
        currentOutput = null
        if (message.isNotEmpty()) reportFallback(message)
    }
    override fun addListener(listener: AudioOutputProvider.Listener) = delegate.addListener(listener)
    override fun removeListener(listener: AudioOutputProvider.Listener) = delegate.removeListener(listener)
    override fun setClock(clock: Clock) = delegate.setClock(clock)
    override fun hasPendingReleases() = delegate.hasPendingReleases()
    override fun release() { clearPreferredFormat(); delegate.release() }
}
