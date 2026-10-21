package com.ljyh.mei.playback.usb

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.media3.common.Player
import com.ljyh.mei.playback.UsbOutputState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object UsbKeepAliveState {
    internal val mutableMessage = MutableStateFlow("仅在 USB 独占后台播放时运行")
    val message = mutableMessage.asStateFlow()
}

/** Owned by the media service, independent of desktop lyrics and removed on pause/USB release. */
internal class UsbFloatingKeepAlive(
    private val context: Context,
    private val player: Player,
    private val enabled: () -> Boolean,
) : AutoCloseable {
    private var windowContext: Context? = null
    private var manager: WindowManager? = null
    private var window: View? = null
    private lateinit var scope: CoroutineScope
    private var observer: Job? = null
    private var permissionWatcher: Job? = null
    private var failed = false
    private var closed = false
    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = update()
    }
    fun start(scope: CoroutineScope) {
        this.scope = scope
        player.addListener(listener)
        observer = scope.launch {
            combine(UsbAppVisibility.foreground, UsbOutputState.status) { _, _ -> Unit }.collect { update() }
        }
    }
    fun update() {
        if (closed) return
        val permitted = Settings.canDrawOverlays(context)
        val show = shouldShowUsbKeepAlive(enabled(), UsbAppVisibility.foreground.value, player.isPlaying,
            UsbOutputState.status.value.active, permitted)
        if (!show) {
            removeWindow()
            failed = false
            UsbKeepAliveState.mutableMessage.value = when {
                !enabled() -> "关闭"
                !permitted -> "需要允许显示在其他应用上层"
                else -> "已开启，等待 USB 独占后台播放"
            }
            return
        }
        if (window == null && !failed) {
            try {
                val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
                    ?: error("Display unavailable")
                val visualContext = context.createDisplayContext(display)
                    .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
                val wm = visualContext.getSystemService(WindowManager::class.java)
                val view = View(visualContext).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
                val params = WindowManager.LayoutParams(1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT).apply {
                    gravity = Gravity.TOP or Gravity.START
                    title = "Mei USB keepalive"
                }
                wm.addView(view, params)
                windowContext = visualContext
                manager = wm
                window = view
                permissionWatcher = scope.launch {
                    // Permission may be revoked from system settings while the Activity is absent.
                    while (isActive) { delay(1000); update() }
                }
            } catch (_: Exception) {
                failed = true
            }
        }
        UsbKeepAliveState.mutableMessage.value = if (window != null) "正在辅助 USB 后台播放" else "悬浮保活启动失败，请检查权限后重新开启"
    }
    private fun removeWindow() {
        permissionWatcher?.cancel()
        permissionWatcher = null
        window?.let { view -> runCatching { manager?.removeViewImmediate(view) } }
        window = null
        manager = null
        windowContext = null
    }
    override fun close() {
        closed = true
        observer?.cancel()
        permissionWatcher?.cancel()
        player.removeListener(listener)
        removeWindow()
        UsbKeepAliveState.mutableMessage.value = "播放服务未运行"
    }
}
