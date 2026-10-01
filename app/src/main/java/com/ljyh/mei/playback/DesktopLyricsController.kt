package com.ljyh.mei.playback

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.hardware.display.DisplayManager
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.media3.common.Player
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ljyh.mei.MainActivity
import com.ljyh.mei.R
import com.ljyh.mei.constants.DesktopLyricsBackgroundKey
import com.ljyh.mei.constants.DesktopLyricsEnabledKey
import com.ljyh.mei.constants.DesktopLyricsXKey
import com.ljyh.mei.constants.DesktopLyricsYKey
import com.ljyh.mei.extensions.currentMetadata
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.lyric.LyricManager
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLineText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
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
    // A Service is not a visual Context. Resolve the display only when the overlay is needed.
    private val windowContext: Context by lazy {
        val display = context.getSystemService(DisplayManager::class.java)
            .getDisplay(Display.DEFAULT_DISPLAY)
            ?: error("Default display is unavailable")
        val displayContext = context.createDisplayContext(display)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            displayContext.createWindowContext(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                null,
            )
        } else {
            displayContext
        }
    }
    private val windowManager by lazy { windowContext.getSystemService(WindowManager::class.java) }
    private var window: LinearLayout? = null
    private var karaokeText: ComposeView? = null
    private var primaryText: TextView? = null
    private var translationText: TextView? = null
    private var playPauseButton: ImageButton? = null
    private var nextButton: ImageButton? = null
    private var params: WindowManager.LayoutParams? = null
    private var overlayLifecycle: OverlayLifecycleOwner? = null
    private var ticker: Job? = null
    private var currentSongId: String? = null
    private var lastLine: DesktopLyricLine? = null
    private val karaokeLineState = mutableStateOf<KaraokeLine?>(null)
    private var lastPlaying: Boolean? = null
    private var showBackground = true
    private var savedX: Int? = null
    private var savedY: Int? = null

    fun start() {
        scope.launch {
            val preferences = context.dataStore.data
            preferences.map {
                (it[DesktopLyricsEnabledKey] ?: false) to
                    (it[DesktopLyricsBackgroundKey] ?: true)
            }
                .distinctUntilChanged()
                .collect { (enabled, background) ->
                    showBackground = background
                    window?.background = windowBackground()
                    ticker?.cancel()
                    ticker = null
                    if (enabled) {
                        val snapshot = preferences.first()
                        savedX = snapshot[DesktopLyricsXKey]
                        savedY = snapshot[DesktopLyricsYKey]
                        ticker = launch {
                            while (true) {
                                try {
                                    refresh()
                                } catch (error: RuntimeException) {
                                    Timber.e(error, "Desktop lyrics overlay failed")
                                    hide()
                                    break
                                }
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
        if (!Settings.canDrawOverlays(context) || player.currentMediaItem == null) {
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

        val position = player.currentPosition
        val line = lyricManager.lyricData.value.desktopLineAt(position)
            ?: DesktopLyricLine(
                metadata.title,
                metadata.artists.joinToString("、") { it.name }.ifBlank { null },
            )
        if (window == null) show()
        val lineChanged = line != lastLine
        if (lineChanged) {
            karaokeLineState.value = line.karaokeLine
            karaokeText?.visibility = if (line.karaokeLine == null) View.GONE else View.VISIBLE
            primaryText?.apply {
                text = line.text
                visibility = if (line.karaokeLine == null) View.VISIBLE else View.GONE
            }
            translationText?.apply {
                text = line.translation.orEmpty()
                visibility = if (line.translation == null) View.GONE else View.VISIBLE
            }
            lastLine = line
        }
        val canPause = player.playWhenReady && player.playbackState != Player.STATE_ENDED
        if (lastPlaying != canPause) {
            playPauseButton?.apply {
                setImageResource(
                    if (canPause) R.drawable.desktop_lyric_pause
                    else R.drawable.desktop_lyric_play
                )
                contentDescription = if (canPause) "暂停" else "播放"
            }
            lastPlaying = canPause
        }
        nextButton?.apply {
            isEnabled = player.hasNextMediaItem()
            alpha = if (isEnabled) 1f else 0.4f
        }
        updateBounds()
    }

    private fun show() {
        val content = makeWindow()
        val lifecycle = OverlayLifecycleOwner()
        content.setViewTreeLifecycleOwner(lifecycle)
        content.setViewTreeSavedStateRegistryOwner(lifecycle)
        lifecycle.start()
        lastLine = null
        lastPlaying = null
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
            overlayLifecycle = lifecycle
        } catch (error: RuntimeException) {
            lifecycle.destroy()
            Timber.w(error, "Could not show desktop lyrics overlay")
            hide()
        }
    }

    private fun hide() {
        overlayLifecycle?.destroy()
        overlayLifecycle = null
        window?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (error: RuntimeException) {
                Timber.w(error, "Could not remove desktop lyrics overlay")
            }
        }
        window = null
        karaokeText = null
        karaokeLineState.value = null
        primaryText = null
        translationText = null
        playPauseButton = null
        nextButton = null
        params = null
        lastLine = null
        lastPlaying = null
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
        val main = lyricText(18f, Color.WHITE)
        val translation = lyricText(13f, Color.rgb(206, 206, 211))
        val karaoke = ComposeView(windowContext).apply {
            visibility = View.GONE
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val line = karaokeLineState.value
                if (line != null) {
                    var position by remember(line) {
                        mutableIntStateOf(player.currentPosition.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())
                    }
                    LaunchedEffect(line) {
                        while (isActive) {
                            position = player.currentPosition.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                            delay(50)
                        }
                    }
                    KaraokeLineText(
                        line = line,
                        currentTimeProvider = { position },
                        modifier = Modifier.fillMaxWidth(),
                        normalLineTextStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            shadow = Shadow(ComposeColor.Black, Offset(0f, 1f), 4f),
                        ),
                        accompanimentLineTextStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            shadow = Shadow(ComposeColor.Black, Offset(0f, 1f), 4f),
                        ),
                        activeColor = ComposeColor.White,
                        blendMode = BlendMode.SrcOver,
                        showTranslation = false,
                        showPhonetic = false,
                    )
                }
            }
        }
        karaokeText = karaoke
        primaryText = main
        translationText = translation

        val controls = LinearLayout(windowContext).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            addView(controlButton(R.drawable.desktop_lyric_previous, "上一首") {
                if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                else player.seekTo(0)
                if (!player.playWhenReady) player.play()
            })
            val playPause = controlButton(
                R.drawable.desktop_lyric_pause,
                "暂停",
            ) {
                if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) {
                    player.pause()
                } else {
                    if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                    if (player.playbackState == Player.STATE_IDLE) player.prepare()
                    player.play()
                }
            }
            playPauseButton = playPause
            addView(playPause)
            val next = controlButton(R.drawable.desktop_lyric_next, "下一首") {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    if (!player.playWhenReady) player.play()
                }
            }
            nextButton = next
            addView(next)
            addView(controlButton(R.drawable.desktop_lyric_close, "关闭桌面歌词") {
                dismissByUser()
            })
        }

        return LinearLayout(windowContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(12), dp(20), dp(12))
            minimumHeight = dp(56)
            this.background = windowBackground()
            contentDescription = "桌面歌词，拖动可移动，点击返回 Mei"
            addView(karaoke, LinearLayout.LayoutParams(-1, -2))
            addView(main, LinearLayout.LayoutParams(-1, -2))
            addView(translation, LinearLayout.LayoutParams(-1, -2))
            addView(controls, LinearLayout.LayoutParams(-1, dp(48)))
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

    private fun controlButton(icon: Int, label: String, action: () -> Unit) =
        ImageButton(windowContext).apply {
            setImageResource(icon)
            contentDescription = label
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = RippleDrawable(
                ColorStateList.valueOf(Color.argb(65, 255, 255, 255)),
                null,
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.WHITE)
                },
            )
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            setOnClickListener { action() }
        }

    private fun dismissByUser() {
        ticker?.cancel()
        ticker = null
        hide()
        scope.launch {
            context.dataStore.edit { it[DesktopLyricsEnabledKey] = false }
        }
    }

    private fun windowBackground() = if (showBackground) {
        GradientDrawable().apply {
            setColor(Color.argb(230, 27, 27, 31))
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), Color.argb(40, 255, 255, 255))
        }
    } else {
        null
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

private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateController.savedStateRegistry

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun start() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
