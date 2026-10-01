package com.ljyh.mei.playback

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.datastore.preferences.core.edit
import androidx.media3.common.Player
import com.ljyh.mei.MainActivity
import com.ljyh.mei.constants.DesktopLyricsEnabledKey
import com.ljyh.mei.constants.DesktopLyricsXKey
import com.ljyh.mei.constants.DesktopLyricsYKey
import com.ljyh.mei.extensions.currentMetadata
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.lyric.LyricManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt

/** The overlay lives with the media service, so lyrics continue after the activity closes. */
internal class DesktopLyricsController(
    private val context: Context,
    private val player: Player,
    private val lyricManager: LyricManager,
    private val scope: CoroutineScope,
) {
    private val windowContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    } else {
        context
    }
    private val windowManager = windowContext.getSystemService(WindowManager::class.java)
    private var window: LinearLayout? = null
    private var primaryText: TextView? = null
    private var translationText: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var ticker: Job? = null
    private var currentSongId: String? = null
    private var lastLine: DesktopLyricLine? = null
    private var savedX: Int? = null
    private var savedY: Int? = null

    fun start() {
        scope.launch {
            val preferences = context.dataStore.data
            preferences.map { it[DesktopLyricsEnabledKey] ?: false }
                .distinctUntilChanged()
                .collect { enabled ->
                    ticker?.cancel()
                    ticker = null
                    if (enabled) {
                        val snapshot = preferences.first()
                        savedX = snapshot[DesktopLyricsXKey]
                        savedY = snapshot[DesktopLyricsYKey]
                        ticker = launch {
                            while (true) {
                                refresh()
                                delay(180)
                            }
                        }
                    } else {
                        hide()
                    }
                }
        }
    }

    fun close() {
        ticker?.cancel()
        ticker = null
        hide()
    }

    private fun refresh() {
        if (!Settings.canDrawOverlays(context) || !player.playWhenReady ||
            player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED
        ) {
            hide()
            return
        }

        val metadata = player.currentMetadata ?: run {
            hide()
            return
        }
        val songId = metadata.id.toString()
        if (currentSongId != songId) {
            currentSongId = songId
            lyricManager.loadLyrics(metadata)
            lastLine = null
        }

        val line = lyricManager.lyricData.value.desktopLineAt(player.currentPosition)
        if (line == null) {
            hide()
            return
        }
        if (window == null) show()
        if (line != lastLine) {
            primaryText?.text = line.text
            translationText?.apply {
                text = line.translation.orEmpty()
                visibility = if (line.translation == null) View.GONE else View.VISIBLE
            }
            lastLine = line
        }
        updateBounds()
    }

    private fun show() {
        val content = makeWindow()
        lastLine = null
        val bounds = screenBounds()
        val width = (bounds.width() - dp(32)).coerceAtMost(dp(460)).coerceAtLeast(dp(180))
        val layout = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX ?: (bounds.width() - width) / 2
            y = savedY ?: (bounds.height() * 0.18f).roundToInt()
        }

        try {
            windowManager.addView(content, layout)
            window = content
            params = layout
        } catch (error: RuntimeException) {
            Timber.w(error, "Could not show desktop lyrics overlay")
        }
    }

    private fun hide() {
        window?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (error: RuntimeException) {
                Timber.w(error, "Could not remove desktop lyrics overlay")
            }
        }
        window = null
        primaryText = null
        translationText = null
        params = null
        lastLine = null
    }

    private fun updateBounds(force: Boolean = false) {
        val view = window ?: return
        val layout = params ?: return
        val bounds = screenBounds()
        val width = (bounds.width() - dp(32)).coerceAtMost(dp(460)).coerceAtLeast(dp(180))
        val x = layout.x.coerceIn(0, (bounds.width() - width).coerceAtLeast(0))
        val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).top
        } else {
            dp(24)
        }
        val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).bottom
        } else {
            dp(24)
        }
        val maxY = (bounds.height() - bottomInset - view.height).coerceAtLeast(topInset)
        val y = layout.y.coerceIn(topInset, maxY)
        if (!force && layout.width == width && layout.x == x && layout.y == y) return
        layout.width = width
        layout.x = x
        layout.y = y
        try {
            windowManager.updateViewLayout(view, layout)
        } catch (error: RuntimeException) {
            Timber.w(error, "Could not update desktop lyrics overlay")
            hide()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeWindow(): LinearLayout {
        val background = GradientDrawable().apply {
            setColor(Color.argb(230, 27, 27, 31))
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), Color.argb(40, 255, 255, 255))
        }
        val main = lyricText(18f, Color.WHITE).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val translation = lyricText(13f, Color.rgb(206, 206, 211))
        primaryText = main
        translationText = translation

        return LinearLayout(windowContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(12), dp(20), dp(12))
            minimumHeight = dp(56)
            this.background = background
            contentDescription = "桌面歌词，拖动可移动，点击返回 Mei"
            addView(main, LinearLayout.LayoutParams(-1, -2))
            addView(translation, LinearLayout.LayoutParams(-1, -2))
            setOnClickListener {
                context.startActivity(Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
            }

            val slop = ViewConfiguration.get(context).scaledTouchSlop
            var downX = 0f
            var downY = 0f
            var originX = 0
            var originY = 0
            var dragging = false
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        originX = params?.x ?: 0
                        originY = params?.y ?: 0
                        dragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        if (abs(dx) > slop || abs(dy) > slop) dragging = true
                        if (dragging) {
                            params?.let { layout ->
                                layout.x = originX + dx.roundToInt()
                                layout.y = originY + dy.roundToInt()
                                updateBounds(force = true)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (dragging) {
                            params?.let { layout ->
                                val x = layout.x
                                val y = layout.y
                                savedX = x
                                savedY = y
                                scope.launch {
                                    context.dataStore.edit {
                                        it[DesktopLyricsXKey] = x
                                        it[DesktopLyricsYKey] = y
                                    }
                                }
                            }
                        } else {
                            view.performClick()
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun lyricText(sizeSp: Float, colorValue: Int) = TextView(windowContext).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(colorValue)
        gravity = Gravity.CENTER
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), Color.BLACK)
    }

    private fun screenBounds(): Rect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            windowContext.resources.displayMetrics.let { Rect(0, 0, it.widthPixels, it.heightPixels) }
        }

    private fun dp(value: Int) = (value * windowContext.resources.displayMetrics.density).roundToInt()
}
