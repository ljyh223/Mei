package com.ljyh.mei.playback

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The observed output, separate from the user's requested setting. */
data class UsbOutputStatus(
    val active: Boolean = false,
    val message: String = "",
    val deviceId: Int? = null,
)

internal data class UsbOutputObservation(
    val playing: Boolean,
    val routedDeviceId: Int?,
    val preferredMixer: Boolean,
) {
    fun statusFor(deviceId: Int, detail: String): UsbOutputStatus {
        val active = playing && routedDeviceId == deviceId && preferredMixer
        return UsbOutputStatus(
            active = active,
            message = when {
                !preferredMixer -> "USB 独占配置已失效，重新播放后重试"
                !playing -> "USB 已就绪，等待播放 · $detail"
                active -> "USB 独占输出 · $detail"
                routedDeviceId != null -> "音频未路由到 USB，使用普通输出"
                else -> "已请求 USB 独占，等待音频路由 · $detail"
            },
            deviceId = deviceId,
        )
    }
}

object UsbOutputState {
    internal val store = UsbOutputStatusStore()
    internal val mixerPreferences = UsbMixerPreferenceStore()
    val status = store.status
}

/** Serializes service/device events and playback-thread callbacks from both player decks. */
internal class UsbOutputStatusStore {
    private val mutableStatus = MutableStateFlow(UsbOutputStatus())
    val status = mutableStatus.asStateFlow()
    private var enabled = false
    private var supported = false
    private var devices: Map<Int, String> = emptyMap()
    private var attempt: Any? = null

    @Synchronized
    fun updateEnvironment(enabled: Boolean, supported: Boolean, devices: Map<Int, String>) {
        if (this.enabled == enabled && this.supported == supported && this.devices == devices) return
        this.enabled = enabled
        this.supported = supported
        this.devices = devices.toMap()
        attempt = null
        mutableStatus.value = idleStatus()
    }

    @Synchronized
    fun beginAttempt(): Any? {
        if (!enabled || !supported || devices.isEmpty()) return null
        return Any().also { attempt = it }
    }

    @Synchronized
    fun update(attempt: Any?, status: UsbOutputStatus) {
        if (attempt != null && this.attempt === attempt &&
            (status.deviceId == null || status.deviceId in devices)
        ) mutableStatus.value = status
    }

    @Synchronized
    fun finish(attempt: Any?) {
        if (attempt != null && this.attempt === attempt) {
            this.attempt = null
            mutableStatus.value = idleStatus()
        }
    }

    fun fallback(message: String) {
        val owner = beginAttempt()
        update(owner, UsbOutputStatus(message = message))
    }

    private fun idleStatus() = UsbOutputStatus(message = when {
        !enabled -> ""
        !supported -> "USB 独占需要 Android 14 或更高版本，使用普通输出"
        devices.isEmpty() -> "未连接 USB 解码器，使用普通输出"
        else -> "已连接 ${devices.values.first()}，播放时尝试独占输出"
    })
}

internal data class UsbDeviceChange(val reconfigure: Boolean, val pause: Boolean)

/** Initial callback snapshots and unrelated device changes must not restart playback. */
internal fun usbDeviceChange(
    previous: Set<Int>,
    current: Set<Int>,
    enabled: Boolean,
    selectedDeviceId: Int?,
): UsbDeviceChange {
    if (!enabled || previous == current) return UsbDeviceChange(reconfigure = false, pause = false)
    val removed = previous - current
    return UsbDeviceChange(
        reconfigure = true,
        pause = removed.isNotEmpty() && (selectedDeviceId == null || selectedDeviceId in removed || current.isEmpty()),
    )
}

/** A detached USB track can fail before the device callback's debounce completes. */
internal fun shouldReconfigureUsbPlayback(playbackState: Int, errorCode: Int?): Boolean =
    playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY ||
        (playbackState == Player.STATE_IDLE &&
            (errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED))

/** Android mixer preferences belong to the UID, so the two decks must share ownership. */
internal class UsbMixerPreferenceStore {
    private val owners = mutableMapOf<Int, Any>()

    @Synchronized
    fun acquire(deviceId: Int, configure: () -> Boolean): Any? {
        if (!configure()) return null
        return Any().also { owners[deviceId] = it }
    }

    @Synchronized
    fun release(deviceId: Int, owner: Any?, clear: () -> Unit) {
        if (owner != null && owners[deviceId] === owner) {
            try {
                clear()
            } finally {
                owners.remove(deviceId)
            }
        }
    }
}
