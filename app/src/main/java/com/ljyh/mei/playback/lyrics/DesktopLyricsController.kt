package com.ljyh.mei.playback.lyrics

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.hardware.display.DisplayManager
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
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.TextUnit
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
import com.ljyh.mei.R
import com.ljyh.mei.constants.DefaultDesktopLyricsControlsHideDelay
import com.ljyh.mei.constants.DefaultDesktopLyricsFontSize
import com.ljyh.mei.constants.DefaultDesktopLyricsTextColor
import com.ljyh.mei.constants.DefaultDesktopLyricsTranslationColor
import com.ljyh.mei.constants.DefaultDesktopLyricsTranslationFontSize
import com.ljyh.mei.constants.DesktopLyricsControlsHideDelayKey
import com.ljyh.mei.constants.DesktopLyricsEnabledKey
import com.ljyh.mei.constants.DesktopLyricsFontSizeKey
import com.ljyh.mei.constants.DesktopLyricsLockedKey
import com.ljyh.mei.constants.DesktopLyricsTextColorKey
import com.ljyh.mei.constants.DesktopLyricsTranslationColorKey
import com.ljyh.mei.constants.DesktopLyricsTranslationFontSizeKey
import com.ljyh.mei.constants.DesktopLyricsXKey
import com.ljyh.mei.constants.DesktopLyricsYKey
import com.ljyh.mei.extensions.currentMetadata
import com.ljyh.mei.playback.SmoothPlaybackPosition
import com.ljyh.mei.utils.lyric.LyricManager
import com.ljyh.mei.utils.preferences.dataStore
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLineText
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

private data class OverlaySettings(
    val enabled: Boolean,
    val locked: Boolean,
    val textColor: String,
    val translationColor: String,
    val fontSize: Int,
    val translationFontSize: Int,
    val controlsHideDelay: Int,
)

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
    private var controls: LinearLayout? = null
    private var previousButton: ImageButton? = null
    private var playPauseButton: ImageButton? = null
    private var nextButton: ImageButton? = null
    private var lockButton: ImageButton? = null
    private var controlsHideJob: Job? = null
    private var params: WindowManager.LayoutParams? = null
    private var overlayLifecycle: OverlayLifecycleOwner? = null
    private var ticker: Job? = null
    private var currentSongId: String? = null
    private var lastLine: DesktopLyricLine? = null
    private val karaokeLineState = mutableStateOf<KaraokeLine?>(null)
    private val karaokeFontSizeState = mutableIntStateOf(DefaultDesktopLyricsFontSize)
    private val karaokeActiveColorState = mutableStateOf(ComposeColor.White)
    private var lastPlaying: Boolean? = null
    private var locked = false
    private var controlsVisible = true
    private var controlsHideDelaySeconds = DefaultDesktopLyricsControlsHideDelay
    private var lyricsFontSize = DefaultDesktopLyricsFontSize
    private var translationFontSize = DefaultDesktopLyricsTranslationFontSize
    private var lyricsColor = Color.WHITE
    private var translationColor = Color.rgb(206, 206, 211)
    private var overlayEnabled = false
    private var savedX: Int? = null
    private var savedY: Int? = null

    fun start() {
        scope.launch {
            val preferences = context.dataStore.data
            preferences.map { values ->
                OverlaySettings(
                    enabled = values[DesktopLyricsEnabledKey] ?: false,
                    locked = values[DesktopLyricsLockedKey] ?: false,
                    textColor = values[DesktopLyricsTextColorKey] ?: DefaultDesktopLyricsTextColor,
                    translationColor = values[DesktopLyricsTranslationColorKey]
                        ?: DefaultDesktopLyricsTranslationColor,
                    fontSize = values[DesktopLyricsFontSizeKey] ?: DefaultDesktopLyricsFontSize,
                    translationFontSize = values[DesktopLyricsTranslationFontSizeKey]
                        ?: DefaultDesktopLyricsTranslationFontSize,
                    controlsHideDelay = values[DesktopLyricsControlsHideDelayKey]
                        ?: DefaultDesktopLyricsControlsHideDelay,
                )
            }
                .distinctUntilChanged()
                .collect { settings ->
                    val wasEnabled = overlayEnabled
                    val wasLocked = locked
                    locked = settings.locked
                    if (wasLocked && !locked) controlsVisible = true
                    lyricsFontSize = settings.fontSize
                    translationFontSize = settings.translationFontSize
                    lyricsColor = parseColor(settings.textColor, Color.WHITE)
                    translationColor = parseColor(
                        settings.translationColor,
                        Color.rgb(206, 206, 211),
                    )
                    controlsHideDelaySeconds = settings.controlsHideDelay.coerceIn(0, 30)
                    karaokeFontSizeState.intValue = lyricsFontSize
                    karaokeActiveColorState.value = ComposeColor(lyricsColor)

                    if (settings.enabled && !wasEnabled) {
                        overlayEnabled = true
                        controlsVisible = true
                        val snapshot = preferences.first()
                        savedX = snapshot[DesktopLyricsXKey]
                        savedY = snapshot[DesktopLyricsYKey]
                        ticker = launch {
                            while (isActive) {
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
                    } else if (!settings.enabled) {
                        overlayEnabled = false
                        ticker?.cancel()
                        ticker = null
                        hide()
                    } else {
                        applyOverlaySettings(lockChanged = wasLocked != locked)
                    }
                }
        }
    }

    fun close() {
        ticker?.cancel()
        ticker = null
        controlsHideJob?.cancel()
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
                visibility = if (line.translation == null) View.INVISIBLE else View.VISIBLE
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
        controlsVisible = true
        val content = makeWindow()
        val lifecycle = OverlayLifecycleOwner()
        content.setViewTreeLifecycleOwner(lifecycle)
        content.setViewTreeSavedStateRegistryOwner(lifecycle)
        lifecycle.start()
        lastLine = null
        lastPlaying = null
        val bounds = screenBounds()
        val layout = WindowManager.LayoutParams(
            overlayWidth(),
            overlayHeight(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX ?: (bounds.width() - dp(300)) / 2
            y = savedY ?: (bounds.height() * 0.18f).roundToInt()
            if (locked) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        try {
            windowManager.addView(content, layout)
            window = content
            params = layout
            overlayLifecycle = lifecycle
            applyOverlaySettings(lockChanged = true)
        } catch (error: RuntimeException) {
            lifecycle.destroy()
            Timber.w(error, "Could not show desktop lyrics overlay")
            hide()
        }
    }

    private fun hide() {
        controlsHideJob?.cancel()
        controlsHideJob = null
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
        controls = null
        previousButton = null
        playPauseButton = null
        nextButton = null
        lockButton = null
        params = null
        lastLine = null
        lastPlaying = null
    }

    private fun updateBounds(force: Boolean = false) {
        val view = window ?: return
        val layout = params ?: return
        val bounds = screenBounds()
        val width = view.width.takeIf { it > 0 } ?: dp(300)
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
        if (!force && layout.x == x && layout.y == y) return
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
        val main = lyricText(lyricsFontSize.toFloat(), lyricsColor, Gravity.BOTTOM)
        val translation = lyricText(translationFontSize.toFloat(), translationColor, Gravity.TOP)
        val karaoke = ComposeView(windowContext).apply {
            visibility = View.GONE
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val playbackClock = remember(player) { SmoothPlaybackPosition() }
                DisposableEffect(player, playbackClock) {
                    val listener = object : Player.Listener {
                        override fun onPositionDiscontinuity(
                            oldPosition: Player.PositionInfo,
                            newPosition: Player.PositionInfo,
                            reason: Int,
                        ) {
                            playbackClock.reset()
                        }
                    }
                    player.addListener(listener)
                    onDispose { player.removeListener(listener) }
                }
                val line = karaokeLineState.value
                val fontSize by karaokeFontSizeState
                val activeColor by karaokeActiveColorState
                if (line != null) {
                    var position by remember(line) {
                        mutableIntStateOf(player.currentPosition.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())
                    }
                    LaunchedEffect(line) {
                        while (isActive) {
                            withFrameNanos { frameTimeNanos ->
                                val duration = player.duration
                                    .takeIf { it > 0L }
                                    ?.coerceAtMost(Int.MAX_VALUE.toLong())
                                    ?: Int.MAX_VALUE.toLong()
                                position = playbackClock.sample(
                                    rawPositionMs = player.currentPosition,
                                    frameTimeNanos = frameTimeNanos,
                                    isPlaying = player.isPlaying,
                                    speed = player.playbackParameters.speed,
                                    durationMs = duration,
                                )
                            }
                        }
                    }
                    val lyricStyle = remember(fontSize, activeColor) {
                        TextStyle(
                            fontSize = fontSize.sp,
                            lineHeight = TextUnit.Unspecified,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = activeColor,
                            textMotion = TextMotion.Animated,
                            shadow = Shadow(ComposeColor.Black, Offset(0f, 1f), 4f),
                        )
                    }
                    KaraokeLineText(
                        // This API has start/end alignment only. Normalize TTML alignment
                        // metadata so consecutive lines don't jump between sides.
                        line = when (line) {
                            is KaraokeLine.MainKaraokeLine -> line.copy(alignment = KaraokeAlignment.Unspecified)
                            is KaraokeLine.AccompanimentKaraokeLine -> line.copy(alignment = KaraokeAlignment.Unspecified)
                        },
                        currentTimeProvider = { position },
                        modifier = Modifier.fillMaxWidth(),
                        normalLineTextStyle = lyricStyle,
                        accompanimentLineTextStyle = lyricStyle,
                        activeColor = activeColor,
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
        val lyricAreaView = FrameLayout(windowContext).apply {
            addView(karaoke, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
            addView(main, FrameLayout.LayoutParams(-1, -1))
        }

        val controlsRow = LinearLayout(windowContext).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            val previous = controlButton(R.drawable.desktop_lyric_previous, "上一首") {
                if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                else player.seekTo(0)
                if (!player.playWhenReady) player.play()
            }
            previousButton = previous
            addView(previous)
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
            val lock = controlButton(
                if (locked) R.drawable.desktop_lyric_unlock else R.drawable.desktop_lyric_lock,
                if (locked) "解锁桌面歌词" else "锁定桌面歌词",
            ) {
                setLocked(!locked)
            }
            lockButton = lock
            addView(lock)
            addView(controlButton(R.drawable.desktop_lyric_close, "关闭桌面歌词") {
                dismissByUser()
            })
        }
        controls = controlsRow

        return LinearLayout(windowContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
            this.background = windowBackground()
            contentDescription = "桌面歌词"
            addView(controlsRow, LinearLayout.LayoutParams(-2, dp(48)))
            addView(lyricAreaView, LinearLayout.LayoutParams(-1, lyricAreaHeightPx()))
            addView(translation, LinearLayout.LayoutParams(-1, translationAreaHeightPx()))
            setOnClickListener { revealControls() }
            main.setOnClickListener { revealControls() }
            translation.setOnClickListener { revealControls() }
            karaoke.setOnClickListener { revealControls() }

            val slop = ViewConfiguration.get(context).scaledTouchSlop
            var downX = 0f
            var downY = 0f
            var originX = 0
            var originY = 0
            var dragging = false
            val overlayTouchListener = View.OnTouchListener { view, event ->
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
                        if (!locked && (abs(dx) > slop || abs(dy) > slop)) dragging = true
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
                            scheduleControlsHide()
                        } else {
                            view.performClick()
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        dragging = false
                        true
                    }
                    else -> false
                }
            }
            setOnTouchListener(overlayTouchListener)
            karaoke.setOnTouchListener(overlayTouchListener)
            main.setOnTouchListener(overlayTouchListener)
            translation.setOnTouchListener(overlayTouchListener)
        }
    }

    private fun controlButton(icon: Int, label: String, action: () -> Unit) =
        ImageButton(windowContext).apply {
            setImageResource(icon)
            contentDescription = label
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            background = RippleDrawable(
                ColorStateList.valueOf(Color.argb(65, 255, 255, 255)),
                null,
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.WHITE)
                },
            )
            setPadding(dp(8), dp(8), dp(8), dp(8))
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            setOnClickListener {
                action()
                scheduleControlsHide()
            }
        }

    private fun applyOverlaySettings(lockChanged: Boolean) {
        if (lockChanged) applyLockState()
        primaryText?.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, lyricsFontSize.toFloat())
            setTextColor(lyricsColor)
        }
        translationText?.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, translationFontSize.toFloat())
            setTextColor(translationColor)
        }
        karaokeFontSizeState.intValue = lyricsFontSize
        karaokeActiveColorState.value = ComposeColor(lyricsColor)
        val view = window ?: return
        view.background = windowBackground()
        view.setPadding(dp(16), dp(8), dp(16), dp(8))
        controls?.visibility = when {
            locked -> View.GONE
            controlsVisible -> View.VISIBLE
            else -> View.INVISIBLE
        }
        (primaryText?.parent as? FrameLayout)?.layoutParams =
            LinearLayout.LayoutParams(-1, lyricAreaHeightPx())
        translationText?.layoutParams = LinearLayout.LayoutParams(-1, translationAreaHeightPx())
        view.requestLayout()
        applyFixedGeometry()
        updateBounds(force = true)
        scheduleControlsHide()
    }

    private fun applyLockState() {
        if (locked) {
            controlsVisible = false
            controlsHideJob?.cancel()
            controlsHideJob = null
        }
        controls?.visibility = when {
            locked -> View.GONE
            controlsVisible -> View.VISIBLE
            else -> View.INVISIBLE
        }
        window?.let { view ->
            val layout = params ?: return@let
            layout.flags = if (locked) {
                layout.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            } else {
                layout.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            }
            runCatching { windowManager.updateViewLayout(view, layout) }
                .onFailure { Timber.w(it, "Could not update desktop lyrics touch behavior") }
        }
    }

    private fun setLocked(value: Boolean) {
        locked = value
        applyLockState()
        window?.requestLayout()
        updateBounds(force = true)
        scope.launch {
            context.dataStore.edit { it[DesktopLyricsLockedKey] = value }
        }
        if (!locked) revealControls()
        else {
            controlsVisible = false
            controlsHideJob?.cancel()
            window?.background = null
        }
        applyFixedGeometry()
    }

    private fun revealControls() {
        if (locked) return
        controlsVisible = true
        controls?.visibility = View.VISIBLE
        window?.apply {
            background = windowBackground()
            requestLayout()
        }
        applyLockState()
        updateBounds(force = true)
        scheduleControlsHide()
    }

    private fun scheduleControlsHide() {
        controlsHideJob?.cancel()
        controlsHideJob = null
        if (!controlsVisible || controlsHideDelaySeconds == 0 || window == null) return
        controlsHideJob = scope.launch {
            delay(controlsHideDelaySeconds * 1_000L)
            controlsVisible = false
            controls?.visibility = View.INVISIBLE
            window?.apply {
                background = null
                requestLayout()
            }
            updateBounds(force = true)
        }
    }

    private fun parseColor(value: String, fallback: Int): Int =
        runCatching { Color.parseColor(value) }.getOrDefault(fallback)

    private fun dismissByUser() {
        ticker?.cancel()
        ticker = null
        hide()
        scope.launch {
            context.dataStore.edit { it[DesktopLyricsEnabledKey] = false }
        }
    }

    private fun windowBackground() = if (controlsVisible && !locked) {
        GradientDrawable().apply {
            setColor(Color.argb(230, 27, 27, 31))
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), Color.argb(40, 255, 255, 255))
        }
    } else {
        null
    }

    private fun lyricText(sizeSp: Float, colorValue: Int, verticalGravity: Int) = TextView(windowContext).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(colorValue)
        gravity = Gravity.CENTER_HORIZONTAL or verticalGravity
        includeFontPadding = false
        maxWidth = overlayWidth() - dp(32)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), Color.BLACK)
    }

    private fun overlayWidth(): Int = minOf(dp(420), (screenBounds().width() - dp(32)).coerceAtLeast(dp(1)))

    private fun lyricAreaHeightPx(): Int = dp(maxOf(64, (lyricsFontSize * fontScale() * 2.8f).roundToInt()))

    private fun translationAreaHeightPx(): Int =
        dp(maxOf(36, (translationFontSize * fontScale() * 2.4f).roundToInt()))

    private fun overlayHeight(): Int =
        dp(16 + (if (locked) 0 else 48)) + lyricAreaHeightPx() + translationAreaHeightPx()

    private fun fontScale(): Float = windowContext.resources.configuration.fontScale

    private fun applyFixedGeometry() {
        val view = window ?: return
        val layout = params ?: return
        val width = overlayWidth()
        val height = overlayHeight()
        if (layout.width != width || layout.height != height) {
            layout.width = width
            layout.height = height
            runCatching { windowManager.updateViewLayout(view, layout) }
                .onFailure { Timber.w(it, "Could not resize desktop lyrics overlay") }
        }
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
