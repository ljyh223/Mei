package com.ljyh.mei.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryFeaturePolicyTest {
    @Test
    fun lowBatteryOnlyRestrictsFeaturesWhenUnplugged() {
        assertFalse(canUseBatteryIntensiveFeatures(19, isPluggedIn = false, isPowerSaveMode = false))
        assertTrue(canUseBatteryIntensiveFeatures(20, isPluggedIn = false, isPowerSaveMode = false))
        assertTrue(canUseBatteryIntensiveFeatures(19, isPluggedIn = true, isPowerSaveMode = false))
    }

    @Test
    fun powerSaveModeRestrictsFeaturesEvenWhenPluggedIn() {
        assertFalse(canUseBatteryIntensiveFeatures(80, isPluggedIn = true, isPowerSaveMode = true))
    }

    @Test
    fun unknownBatteryLevelDoesNotOverrideExplicitSettings() {
        assertTrue(canUseBatteryIntensiveFeatures(null, isPluggedIn = false, isPowerSaveMode = false))
    }
}
