package com.ljyh.mei.playback.usb

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.*
import org.junit.Test

class UsbPlaybackPreferencesTest {
    @Test fun absentSettingsUseStableProfileAndNoOverlay() {
        val settings = emptyPreferences().usbPlaybackPreferences()
        assertEquals(UsbBufferProfile.Stable, settings.profile)
        assertEquals(500, settings.bufferMs(true))
        assertEquals(1500, settings.bufferMs(false))
        assertFalse(settings.floatingKeepAlive)
        assertTrue(settings.sampleRateCompatibility && settings.bitDepthCompatibility && settings.channelCompatibility)
    }
    @Test fun invalidStoredValuesAreNormalizedAndProfileFallsBack() {
        val settings = preferencesOf(UsbBufferProfileKey to "missing", UsbForegroundBufferKey to -1,
            UsbBackgroundBufferKey to 9000).usbPlaybackPreferences()
        assertEquals(100, settings.bufferMs(true))
        assertEquals(3000, settings.bufferMs(false))
        assertTrue(settings.customized)
    }
    @Test fun profileSeedsOnlyMissingDurationsAndCustomDurationsSurviveReload() {
        val settings = preferencesOf(UsbBufferProfileKey to "LowLatency", UsbBackgroundBufferKey to 1250).usbPlaybackPreferences()
        assertEquals(100, settings.bufferMs(true))
        assertEquals(1250, settings.bufferMs(false))
        assertTrue(settings.customized)
        assertEquals(250, normalizeForegroundBuffer(299))
    }
    @Test fun changesToBuffersAndKeepaliveDoNotRequireNewFormatSession() {
        val old = UsbPlaybackPreferences()
        assertEquals(old.formatPolicy(), old.copy(foregroundBufferMs = 100, floatingKeepAlive = true).formatPolicy())
        assertNotEquals(old.formatPolicy(), old.copy(sampleRateCompatibility = false).formatPolicy())
    }
    @Test fun descriptorTuplesAreNotCombinedIntoUnsupportedFormats() {
        val formats = parseUsbOutputFormats("16,2,2,44100,44100,0\n24,3,2,96000,96000,0")
        val targets = usbOutputTargets(formats, 44100, 24, 2, UsbPlaybackPreferences())
        assertEquals(UsbOutputTarget(44100,16,2,2), targets.first())
        assertFalse(targets.contains(UsbOutputTarget(44100,24,3,2)))
    }
    @Test fun strictRateAndBitsRejectUnsupportedSource() {
        val formats = parseUsbOutputFormats("16,2,2,48000,48000,0")
        assertTrue(usbOutputTargets(formats,44100,24,2,UsbPlaybackPreferences(sampleRateCompatibility=false)).isEmpty())
        assertTrue(usbOutputTargets(formats,48000,24,2,UsbPlaybackPreferences(bitDepthCompatibility=false)).isEmpty())
    }
    @Test fun channelCompatibilityAllowsMonoStereoAndMultichannelDacsOnlyWhenEnabled() {
        val formats = parseUsbOutputFormats("16,2,2,48000,48000,0\n16,2,8,48000,48000,0")
        assertEquals(2, usbOutputTargets(formats,48000,16,1,UsbPlaybackPreferences()).first().channels)
        assertTrue(usbOutputTargets(formats,48000,16,1,UsbPlaybackPreferences(channelCompatibility=false)).isEmpty())
        assertEquals(8, usbOutputTargets(formats.drop(1),48000,16,2,UsbPlaybackPreferences()).single().channels)
    }
    @Test fun strictMonoOutputIsPreserved() {
        val formats = parseUsbOutputFormats("16,2,1,48000,48000,0\n16,2,2,48000,48000,0")
        assertEquals(1,usbOutputTargets(formats,48000,16,1,UsbPlaybackPreferences(channelCompatibility=false)).single().channels)
    }
    @Test fun compatibilityPrefersSameClockFamilyAndNonReducingBitDepth() {
        val formats = parseUsbOutputFormats("16,2,2,48000,48000,0\n24,3,2,88200,88200,0\n32,4,2,88200,88200,0")
        assertEquals(UsbOutputTarget(88200,24,3,2),usbOutputTargets(formats,44100,24,2,UsbPlaybackPreferences()).first())
    }
    @Test fun continuousAndSteppedRangesChooseOnlyValidRates() {
        val stepped = UsbOutputFormat(24,3,2,44100,96000,44100)
        assertEquals(88200,stepped.nearestRate(96000))
        val continuous = UsbOutputFormat(16,2,2,32000,96000,0)
        assertEquals(48000,continuous.nearestRate(48000))
        assertEquals(96000,continuous.nearestRate(192000))
    }
    @Test fun malformedCapabilityReportsCannotCreateTargets() {
        assertTrue(parseUsbOutputFormats("16,garbage,2,2,48000,48000,0\n24,2,2,48000,48000,0\n16,2,99,0,0,0\n16,2,2,48000,44100,0").isEmpty())
    }
    @Test fun unreadableClockRangeTriesExactSourceWithoutInventingFallbackRates() {
        val targets = usbOutputTargets(parseUsbOutputFormats("16,2,2,0,0,0"),44100,16,2,UsbPlaybackPreferences())
        assertEquals(listOf(UsbOutputTarget(44100,16,2,2)),targets)
    }
    @Test fun foregroundSwitchDrainsExcessBeforeAcceptingMorePcm() {
        assertEquals(0,usbWritableBytes(4000, 1200, 500,1000,1000,4))
        assertEquals(400,usbWritableBytes(4000,400,500,1000,1000,4))
        assertEquals(4000,usbWritableBytes(4000,400,1500,1000,1000,4))
    }
    @Test fun bufferWatermarkConvertsOutputFramesToAlignedInputBytes() {
        assertEquals(440,usbWritableBytes(10000,0,120,44100,48000,4))
        assertEquals(0,usbWritableBytes(10000,120,120,44100,48000,4))
        assertEquals(396,usbWritableBytes(399,0,10000,44100,48000,4))
    }
    @Test fun keepaliveRequiresEveryRuntimeCondition() {
        assertTrue(shouldShowUsbKeepAlive(true,false,true,true,true))
        assertFalse(shouldShowUsbKeepAlive(false,false,true,true,true))
        assertFalse(shouldShowUsbKeepAlive(true,true,true,true,true))
        assertFalse(shouldShowUsbKeepAlive(true,false,false,true,true))
        assertFalse(shouldShowUsbKeepAlive(true,false,true,false,true))
        assertFalse(shouldShowUsbKeepAlive(true,false,true,true,false))
    }
}
