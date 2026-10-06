package com.ljyh.mei.ui.component.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ljyh.mei.utils.power.canUseBatteryIntensiveFeatures

@Composable
fun rememberBatteryIntensiveFeaturesAllowed(): Boolean {
    val context = LocalContext.current.applicationContext
    var allowed by remember(context) { mutableStateOf(context.canUseBatteryIntensiveFeatures()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                allowed = context.canUseBatteryIntensiveFeatures()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        allowed = context.canUseBatteryIntensiveFeatures()
        onDispose { context.unregisterReceiver(receiver) }
    }

    return allowed
}
