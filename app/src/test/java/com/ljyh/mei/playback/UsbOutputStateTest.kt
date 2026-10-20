package com.ljyh.mei.playback

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbOutputStateTest {
    private val devices = mapOf(7 to "DAC")
    private val active = UsbOutputStatus(active = true, message = "USB 独占输出", deviceId = 7)

    @Test
    fun disconnectedDeviceCannotBeReportedActiveByLateCallbacks() {
        val store = connectedStore()
        val output = store.beginAttempt()
        store.update(output, active)
        store.updateEnvironment(true, true, emptyMap())
        store.update(output, active)
        store.finish(output)
        assertFalse(store.status.value.active)
        assertTrue(store.status.value.message.contains("未连接"))
        assertNull(store.status.value.deviceId)
        assertNull(store.beginAttempt())
    }

    @Test
    fun oldOutputReleaseCannotClearNewOutputStatus() {
        val store = connectedStore()
        val oldOutput = store.beginAttempt()
        val newOutput = store.beginAttempt()
        store.update(newOutput, active)
        store.finish(oldOutput)
        store.update(oldOutput, UsbOutputStatus(message = "旧音轨"))
        assertEquals(active, store.status.value)
    }

    @Test
    fun standbyOutputWithoutAttemptCannotClearActiveOutput() {
        val store = connectedStore()
        store.update(store.beginAttempt(), active)
        store.finish(null)
        assertEquals(active, store.status.value)
    }

    @Test
    fun repeatedDeviceSnapshotPreservesCurrentOutput() {
        val store = connectedStore()
        val output = store.beginAttempt()
        store.update(output, active)
        store.updateEnvironment(true, true, devices)
        assertEquals(active, store.status.value)
        store.finish(output)
        assertFalse(store.status.value.active)
        assertTrue(store.status.value.message.contains("已连接 DAC"))
    }

    @Test
    fun reconnectWaitsForNewOutputAndRejectsPreviousAttempt() {
        val store = connectedStore()
        val oldOutput = store.beginAttempt()
        store.updateEnvironment(true, true, emptyMap())
        store.updateEnvironment(true, true, devices)
        store.update(oldOutput, active)
        assertFalse(store.status.value.active)
        assertNotNull(store.beginAttempt())
    }

    @Test
    fun disablingAndStoppingServiceRejectLateOutputUpdates() {
        val store = connectedStore()
        val output = store.beginAttempt()
        store.updateEnvironment(false, true, emptyMap())
        store.update(output, active)
        store.fallback("不应该显示的回退状态")
        store.finish(output)
        assertEquals(UsbOutputStatus(), store.status.value)
    }

    @Test
    fun unsupportedAndroidVersionShowsReasonEvenWithoutPlayback() {
        val store = UsbOutputStatusStore()
        store.updateEnvironment(true, false, devices)
        assertTrue(store.status.value.message.contains("Android 14"))
        assertNull(store.beginAttempt())
    }

    @Test
    fun fallbackSurvivesReleaseOfPreviousUsbOutput() {
        val store = connectedStore()
        val output = store.beginAttempt()
        store.fallback("格式不支持独占，使用普通输出")
        store.finish(output)
        assertEquals("格式不支持独占，使用普通输出", store.status.value.message)
        assertFalse(store.status.value.active)
    }

    private fun connectedStore() = UsbOutputStatusStore().apply {
        updateEnvironment(true, true, devices)
    }
}

class UsbDeviceChangeTest {
    @Test
    fun initialSnapshotAndUnrelatedDeviceEventsDoNotRestartPlayback() {
        assertEquals(UsbDeviceChange(false, false), usbDeviceChange(setOf(7), setOf(7), true, 7))
        assertEquals(UsbDeviceChange(false, false), usbDeviceChange(emptySet(), emptySet(), true, null))
    }

    @Test
    fun pluggingInRequestsReconfigurationWithoutPausing() {
        assertEquals(UsbDeviceChange(true, false), usbDeviceChange(emptySet(), setOf(7), true, null))
    }

    @Test
    fun removingSelectedDevicePausesEvenWithAnotherUsbDeviceConnected() {
        assertEquals(UsbDeviceChange(true, true), usbDeviceChange(setOf(7, 8), setOf(8), true, 7))
    }

    @Test
    fun removingLastDevicePausesEvenWhenExclusiveNegotiationFailed() {
        assertEquals(UsbDeviceChange(true, true), usbDeviceChange(setOf(7), emptySet(), true, null))
    }

    @Test
    fun removingOtherDeviceDoesNotPauseActiveUsbDevice() {
        assertEquals(UsbDeviceChange(true, false), usbDeviceChange(setOf(7, 8), setOf(7), true, 7))
    }

    @Test
    fun ordinaryOutputIgnoresUsbDeviceChanges() {
        assertEquals(UsbDeviceChange(false, false), usbDeviceChange(setOf(7), emptySet(), false, 7))
    }

    @Test
    fun replacingUsbDevicePausesBeforeReconfiguring() {
        assertEquals(UsbDeviceChange(true, true), usbDeviceChange(setOf(7), setOf(8), true, 7))
    }

    @Test
    fun unknownOrdinaryRoutePausesConservativelyWhenUsbDeviceIsRemoved() {
        assertEquals(UsbDeviceChange(true, true), usbDeviceChange(setOf(7, 8), setOf(8), true, null))
    }
}

class UsbMixerPreferenceStoreTest {
    @Test
    fun oldDeckCannotClearMixerAttributesSelectedByNewDeck() {
        val preferences = UsbMixerPreferenceStore()
        val oldDeck = preferences.acquire(7) { true }
        val newDeck = preferences.acquire(7) { true }
        var clearCount = 0
        preferences.release(7, oldDeck) { clearCount++ }
        assertEquals(0, clearCount)
        preferences.release(7, newDeck) { clearCount++ }
        preferences.release(7, newDeck) { clearCount++ }
        assertEquals(1, clearCount)
    }

    @Test
    fun differentDevicesHaveIndependentOwnership() {
        val preferences = UsbMixerPreferenceStore()
        val first = preferences.acquire(7) { true }
        val second = preferences.acquire(8) { true }
        var clearedDevice = 0
        preferences.release(7, second) { clearedDevice = 7 }
        assertEquals(0, clearedDevice)
        preferences.release(8, second) { clearedDevice = 8 }
        assertEquals(8, clearedDevice)
        preferences.release(7, first) { clearedDevice = 7 }
        assertEquals(7, clearedDevice)
    }

    @Test
    fun rejectedMixerRequestDoesNotTakeOwnershipFromExistingOutput() {
        val preferences = UsbMixerPreferenceStore()
        val old = preferences.acquire(7) { true }
        assertNull(preferences.acquire(7) { false })
        var cleared = false
        preferences.release(7, old) { cleared = true }
        assertTrue(cleared)
    }
}

class UsbOutputObservationTest {
    @Test
    fun mixerRequestWithoutUsbRouteIsStillPending() {
        val status = UsbOutputObservation(true, null, true).statusFor(7, "DAC")
        assertFalse(status.active)
        assertTrue(status.message.contains("等待音频路由"))
    }

    @Test
    fun playingUsbRouteWithPreferredMixerIsActive() {
        val status = UsbOutputObservation(true, 7, true).statusFor(7, "DAC · 44.1 kHz")
        assertTrue(status.active)
        assertTrue(status.message.contains("44.1 kHz"))
        assertEquals(7, status.deviceId)
    }

    @Test
    fun mixerPreferenceRevocationClearsActiveState() {
        val status = UsbOutputObservation(true, 7, false).statusFor(7, "DAC")
        assertFalse(status.active)
        assertTrue(status.message.contains("配置已失效"))
    }

    @Test
    fun routingToAnotherDeviceShowsOrdinaryOutput() {
        val status = UsbOutputObservation(true, 8, true).statusFor(7, "DAC")
        assertFalse(status.active)
        assertTrue(status.message.contains("使用普通输出"))
    }

    @Test
    fun pausedOutputIsReadyEvenIfAndroidKeepsItsPreviousRoute() {
        val status = UsbOutputObservation(false, 7, true).statusFor(7, "DAC")
        assertFalse(status.active)
        assertTrue(status.message.contains("等待播放"))
    }
}

class UsbPlaybackReconfigurationTest {
    @Test
    fun bufferedOrPreparingTrackCanSwitchOutputs() {
        assertTrue(shouldReconfigureUsbPlayback(Player.STATE_READY, null))
        assertTrue(shouldReconfigureUsbPlayback(Player.STATE_BUFFERING, null))
    }

    @Test
    fun stoppedOrFinishedQueueIsNotPreparedByDeviceEvents() {
        assertFalse(shouldReconfigureUsbPlayback(Player.STATE_IDLE, null))
        assertFalse(shouldReconfigureUsbPlayback(Player.STATE_ENDED, null))
    }

    @Test
    fun usbTrackFailureBeforeDeviceEventCanRecover() {
        assertTrue(shouldReconfigureUsbPlayback(Player.STATE_IDLE, PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED))
        assertTrue(shouldReconfigureUsbPlayback(Player.STATE_IDLE, PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED))
    }

    @Test
    fun sourceAndDecoderFailuresAreNotRetriedByUsbEvents() {
        assertFalse(shouldReconfigureUsbPlayback(Player.STATE_IDLE, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        assertFalse(shouldReconfigureUsbPlayback(Player.STATE_IDLE, PlaybackException.ERROR_CODE_DECODING_FAILED))
    }
}
