package com.ljyh.mei.utils.power

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager

private const val LowBatteryThresholdPercent = 20

internal fun canUseBatteryIntensiveFeatures(
    batteryPercent: Int?,
    isPluggedIn: Boolean,
    isPowerSaveMode: Boolean,
): Boolean = !isPowerSaveMode && (
    isPluggedIn || batteryPercent == null || batteryPercent >= LowBatteryThresholdPercent
)

fun Context.canUseBatteryIntensiveFeatures(): Boolean {
    val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else null
    val isPluggedIn = (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
    val isPowerSaveMode = getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    return canUseBatteryIntensiveFeatures(batteryPercent, isPluggedIn, isPowerSaveMode)
}
