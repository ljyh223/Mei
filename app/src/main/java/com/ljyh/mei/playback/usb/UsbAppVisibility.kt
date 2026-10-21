package com.ljyh.mei.playback.usb

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process visibility without binding the audio service to one Activity instance. */
object UsbAppVisibility : Application.ActivityLifecycleCallbacks {
    private val handler = Handler(Looper.getMainLooper())
    private val mutableForeground = MutableStateFlow(false)
    val foreground = mutableForeground.asStateFlow()
    private var started = 0
    private val markBackground = Runnable { if (started == 0) mutableForeground.value = false }
    override fun onActivityStarted(activity: Activity) {
        handler.removeCallbacks(markBackground)
        started++
        mutableForeground.value = true
    }
    override fun onActivityStopped(activity: Activity) {
        started = (started - 1).coerceAtLeast(0)
        if (started == 0) handler.postDelayed(markBackground, 700) // Avoid a buffer switch during rotation.
    }
    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
