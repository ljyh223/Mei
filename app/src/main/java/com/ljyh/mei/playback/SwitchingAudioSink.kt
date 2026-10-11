package com.ljyh.mei.playback

import android.media.AudioDeviceInfo
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.Format
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.Clock
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioOffloadSupport
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioSink
import java.nio.ByteBuffer

/** Selects the regular DSP sink or the USB sink at each decoder configuration. */
class SwitchingAudioSink(
    private val regular: AudioSink,
    private val usb: AudioSink,
    private val usbProvider: UsbExclusiveOutputProvider,
    private val useUsb: () -> Boolean,
) : AudioSink {
    private var current: AudioSink = regular

    override fun setListener(listener: AudioSink.Listener) {
        regular.setListener(listener)
        usb.setListener(listener)
    }

    override fun setPlayerId(playerId: PlayerId?) {
        regular.setPlayerId(playerId)
        usb.setPlayerId(playerId)
    }

    override fun setClock(clock: Clock) {
        regular.setClock(clock)
        usb.setClock(clock)
    }

    override fun supportsFormat(format: Format) = selected().supportsFormat(format)
    override fun getFormatSupport(format: Format) = selected().getFormatSupport(format)
    override fun getFormatOffloadSupport(format: Format): AudioOffloadSupport =
        selected().getFormatOffloadSupport(format)

    override fun getCurrentPositionUs(sourceEnded: Boolean) = current.getCurrentPositionUs(sourceEnded)

    override fun configure(format: Format, specifiedBufferSize: Int, outputChannels: IntArray?) {
        switchIfNeeded(format)
        current.configure(format, specifiedBufferSize, outputChannels)
    }

    override fun configure(audioSinkConfig: AudioSink.AudioSinkConfig) {
        switchIfNeeded(audioSinkConfig.format)
        current.configure(audioSinkConfig)
    }

    private fun selected() = if (useUsb()) usb else regular

    private fun switchIfNeeded(format: Format) {
        val needsStereoDownmix = useUsb() && format.channelCount > 2
        val canUseUsb = useUsb() && !needsStereoDownmix && usbProvider.supportsBitPerfect(format)
        val fallbackStatus = UsbOutputState.status.value
        val next = if (canUseUsb) usb else regular
        if (next !== current) {
            current.reset()
            if (current === usb) usbProvider.clearPreferredFormat()
            current = next
        }
        if (needsStereoDownmix) {
            UsbOutputState.status.value = UsbOutputStatus(message = "多声道歌曲已转为立体声，使用普通输出")
        } else if (useUsb() && !canUseUsb) {
            UsbOutputState.status.value = fallbackStatus
        }
    }

    override fun play() = current.play()
    override fun handleDiscontinuity() = current.handleDiscontinuity()
    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int) =
        current.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
    override fun playToEndOfStream() = current.playToEndOfStream()
    override fun isEnded() = current.isEnded
    override fun hasPendingData() = current.hasPendingData()
    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        regular.playbackParameters = playbackParameters
        usb.playbackParameters = PlaybackParameters.DEFAULT
    }
    override fun getPlaybackParameters(): PlaybackParameters = current.playbackParameters
    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) {
        regular.skipSilenceEnabled = skipSilenceEnabled
        usb.skipSilenceEnabled = false
    }
    override fun getSkipSilenceEnabled() = current.skipSilenceEnabled
    override fun setAudioAttributes(audioAttributes: AudioAttributes) {
        regular.setAudioAttributes(audioAttributes)
        usb.setAudioAttributes(audioAttributes)
    }
    override fun getAudioAttributes(): AudioAttributes? = current.audioAttributes
    override fun getAudioCapabilities(): AudioCapabilities? = current.audioCapabilities
    override fun setAudioSessionId(audioSessionId: Int) {
        regular.setAudioSessionId(audioSessionId)
        usb.setAudioSessionId(audioSessionId)
    }
    override fun setAuxEffectInfo(auxEffectInfo: AuxEffectInfo) {
        regular.setAuxEffectInfo(auxEffectInfo)
        usb.setAuxEffectInfo(auxEffectInfo)
    }
    override fun setPreferredDevice(audioDeviceInfo: AudioDeviceInfo?) {
        regular.setPreferredDevice(audioDeviceInfo)
        usb.setPreferredDevice(audioDeviceInfo)
    }
    override fun setVirtualDeviceId(virtualDeviceId: Int) {
        regular.setVirtualDeviceId(virtualDeviceId)
        usb.setVirtualDeviceId(virtualDeviceId)
    }
    override fun setOutputStreamOffsetUs(outputStreamOffsetUs: Long) {
        regular.setOutputStreamOffsetUs(outputStreamOffsetUs)
        usb.setOutputStreamOffsetUs(outputStreamOffsetUs)
    }
    override fun getAudioTrackBufferSizeUs() = current.audioTrackBufferSizeUs
    override fun enableTunnelingV21() = current.enableTunnelingV21()
    override fun disableTunneling() = current.disableTunneling()
    override fun setOffloadMode(offloadMode: Int) {
        regular.setOffloadMode(offloadMode)
        usb.setOffloadMode(AudioSink.OFFLOAD_MODE_DISABLED)
    }
    override fun setOffloadDelayPadding(delayInFrames: Int, paddingInFrames: Int) {
        regular.setOffloadDelayPadding(delayInFrames, paddingInFrames)
        usb.setOffloadDelayPadding(delayInFrames, paddingInFrames)
    }
    override fun setAudioOutputProvider(audioOutputProvider: AudioOutputProvider) =
        regular.setAudioOutputProvider(audioOutputProvider)
    override fun setVolume(volume: Float) {
        regular.setVolume(volume)
        usb.setVolume(1f)
    }
    override fun pause() = current.pause()
    override fun flush() = current.flush()
    override fun reset() {
        regular.reset()
        usb.reset()
        usbProvider.clearPreferredFormat()
    }
    override fun release() {
        regular.release()
        usb.release()
        usbProvider.clearPreferredFormat()
    }
}
