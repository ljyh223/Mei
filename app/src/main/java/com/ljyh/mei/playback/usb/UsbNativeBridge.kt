package com.ljyh.mei.playback.usb

import androidx.annotation.Keep
import java.nio.ByteBuffer

/** JNI signatures for the attributed Rust USB audio driver. */
@Keep
internal object UsbNativeBridge {
    val available: Boolean by lazy {
        try {
            System.loadLibrary("mei_usb")
            true
        } catch (_: LinkageError) { false }
    }
    @JvmStatic external fun nativeOutputFormats(fd: Int): String
    @JvmStatic external fun nativeOpen(fd: Int, sampleRate: Int, channelCount: Int, bitsPerSample: Int, subslotBytes: Int): Long
    @JvmStatic external fun nativePreparePlayerPcm(handle: Long, inputSampleRate: Int, inputChannelCount: Int, inputEncoding: Int): Boolean
    @JvmStatic external fun nativeWritePlayerPcm(handle: Long, buffer: ByteBuffer, offset: Int, size: Int, volume: Float): Int
    @JvmStatic external fun nativeDrainPlayerPcm(handle: Long): Boolean
    @JvmStatic external fun nativePlayPlayerPcm(handle: Long): Boolean
    @JvmStatic external fun nativePausePlayerPcm(handle: Long): Boolean
    @JvmStatic external fun nativeFlushPlayerPcm(handle: Long): Boolean
    @JvmStatic external fun nativeConfigurePlayerBufferDuration(handle: Long, durationMs: Int): Boolean
    @JvmStatic external fun nativeHasHardwareVolume(handle: Long): Boolean
    @JvmStatic external fun nativeSetHardwareVolume(handle: Long, fraction: Float): Boolean
    @JvmStatic external fun nativeSetPlayerBitPerfect(handle: Long, enabled: Boolean): Boolean
    @JvmStatic external fun nativeGetCompletedAudioFrames(handle: Long): Long
    @JvmStatic external fun nativeGetQueuedPlayerFrames(handle: Long): Long
    @JvmStatic external fun nativeGetPlayerPcmFreeBytes(handle: Long): Long
    @JvmStatic external fun nativeMarkDeviceDetached(handle: Long)
    @JvmStatic external fun nativeClose(handle: Long)
    @JvmStatic external fun nativeRuntimeReport(handle: Long): String
    @JvmStatic external fun nativeLastOpenError(): String
}
