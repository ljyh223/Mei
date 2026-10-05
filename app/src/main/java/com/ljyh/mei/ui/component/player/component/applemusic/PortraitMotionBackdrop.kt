package com.ljyh.mei.ui.component.player.component.applemusic

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.content.Context
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import kotlin.math.exp

/** A gray mask is needed only when most of the visible video frame is nearly white. */
internal fun shouldUseGrayMask(pixels: IntArray, wasEnabled: Boolean): Boolean {
    if (pixels.isEmpty()) return false
    val whiteCount = pixels.count { pixel ->
        ((pixel shr 16) and 0xff) >= 228 &&
            ((pixel shr 8) and 0xff) >= 228 &&
            (pixel and 0xff) >= 228
    }
    val whiteFraction = whiteCount.toFloat() / pixels.size
    return whiteFraction >= if (wasEnabled) 0.4f else 0.55f
}

internal fun motionControlBackground(edgeColor: Color, hasLargeWhiteArea: Boolean): Color {
    if (!hasLargeWhiteArea) return edgeColor
    val brightest = maxOf(edgeColor.red, edgeColor.green, edgeColor.blue)
    if (brightest <= 0.65f) return edgeColor
    val grayAlpha = ((brightest - 0.65f) / (brightest - 0.188f)).coerceIn(0f, 1f)
    return Color(0xFF303030).copy(alpha = grayAlpha).compositeOver(edgeColor)
}

internal fun averageMotionColor(pixels: IntArray): Color {
    if (pixels.isEmpty()) return Color.Transparent
    var red = 0L
    var green = 0L
    var blue = 0L
    for (pixel in pixels) {
        red += (pixel shr 16) and 0xff
        green += (pixel shr 8) and 0xff
        blue += pixel and 0xff
    }
    return Color(
        red / pixels.size / 255f,
        green / pixels.size / 255f,
        blue / pixels.size / 255f,
    )
}

internal class PortraitMotionBackdropState {
    var edgeColor: Color? = null
        private set
    var hasLargeWhiteArea by mutableStateOf(false)
        private set
    private var backdropView: PortraitMotionBackdropView? = null
    private var pendingStrip: Bitmap? = null
    private var hasVideoFrame = false

    fun bind(view: PortraitMotionBackdropView?) {
        backdropView = view
        edgeColor?.let { view?.controlColor = it.toArgb() }
        if (view != null) {
            pendingStrip?.let(view::setStrip)
            pendingStrip = null
        }
    }

    fun acceptPreview(bitmap: Bitmap) {
        if (hasVideoFrame) {
            bitmap.recycle()
            return
        }
        process(bitmap)
    }

    fun accept(bitmap: Bitmap) {
        hasVideoFrame = true
        process(bitmap)
    }

    private fun process(bitmap: Bitmap) {
        try {
            val framePixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(framePixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            hasLargeWhiteArea = shouldUseGrayMask(framePixels, hasLargeWhiteArea)
            // The Android player samples and blurs only the bottom eighth of a low-resolution
            // texture frame, then derives the controls color from that same processed strip.
            val strip = motionBottomStrip(bitmap)
            val reduced = Bitmap.createScaledBitmap(strip, 2, 2, true)
            val pixels = IntArray(4)
            reduced.getPixels(pixels, 0, 2, 0, 0, 2, 2)
            if (reduced !== strip) reduced.recycle()
            val sampledColor = averageMotionColor(pixels)
            edgeColor = sampledColor
            backdropView?.let { view ->
                view.controlColor = sampledColor.toArgb()
                view.setStrip(strip)
            } ?: run {
                pendingStrip?.recycle()
                pendingStrip = strip
            }
        } finally {
            bitmap.recycle()
        }
    }
}

/** Matches the Android player's overlapping blur view and its separate solid-color gradient. */
internal class PortraitMotionBackdropView(context: Context) : View(context) {
    private var strip: Bitmap? = null
    private val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var videoBottomPx: Float = 0f
        set(value) {
            if (field != value) { field = value; invalidate() }
        }
    var controlColor: Int = AndroidColor.BLACK
        set(value) {
            if (field != value) { field = value; invalidate() }
        }

    init {
        isClickable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setStrip(bitmap: Bitmap) {
        // HWUI may still be rendering the previous bitmap on another thread.
        strip = bitmap
        invalidate()
    }

    override fun onDetachedFromWindow() {
        strip = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || videoBottomPx <= 0f) return
        val frame = strip
        if (frame != null) {
            colorPaint.shader = null
            colorPaint.color = controlColor
            canvas.drawRect(0f, videoBottomPx, w, h, colorPaint)
        }
        // The APK's motion_legibility_gradient is a fixed #B3171717 tint masked by a
        // 20% -> 0% -> 100% alpha ramp, spanning the entire player behind the sampled blur.
        colorPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0x24171717, AndroidColor.TRANSPARENT, 0xB3171717.toInt()),
            floatArrayOf(0f, 0.2f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, colorPaint)
        if (frame == null) return
        val overlap = 150f * resources.displayMetrics.density
        val blurTop = (videoBottomPx - overlap).coerceAtLeast(0f)
        val blurHeight = h - blurTop
        val overlapHeight = videoBottomPx - blurTop
        if (blurHeight <= 0f || overlapHeight <= 0f) return

        val bitmapShader = BitmapShader(frame, Shader.TileMode.CLAMP, Shader.TileMode.MIRROR)
        val matrix = Matrix().apply {
            setScale(1.25f * w / frame.width, blurHeight / frame.height)
            postTranslate(-0.125f * w, -overlapHeight)
        }
        bitmapShader.setLocalMatrix(matrix)
        blurPaint.shader = bitmapShader

        // The original blur view has scaleY=-1. Its radial alpha creates the curved seam.
        val halfOverlap = overlapHeight * 0.5f
        val radiusOffset = (w * w / 8f) / halfOverlap + halfOverlap * 0.5f
        val radius = overlapHeight - halfOverlap + radiusOffset
        maskPaint.shader = RadialGradient(
            w * 0.5f,
            blurHeight - halfOverlap + radiusOffset,
            radius,
            intArrayOf(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT, AndroidColor.WHITE),
            floatArrayOf(0f, (radiusOffset / radius).coerceIn(0f, 1f), 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.save()
        canvas.translate(0f, blurTop + blurHeight)
        canvas.scale(1f, -1f)
        val layer = canvas.saveLayer(0f, 0f, w, blurHeight, null)
        canvas.drawRect(0f, 0f, w, blurHeight, blurPaint)
        canvas.drawRect(0f, 0f, w, blurHeight, maskPaint)
        canvas.restoreToCount(layer)
        canvas.restore()

        val gradientEnd = videoBottomPx + h * 0.4f
        colorPaint.shader = LinearGradient(
            0f, videoBottomPx, 0f, gradientEnd,
            intArrayOf(
                controlColor and 0x00ffffff,
                (controlColor and 0x00ffffff) or (204 shl 24),
                (controlColor and 0x00ffffff) or (242 shl 24),
                (controlColor and 0x00ffffff) or (255 shl 24),
            ),
            floatArrayOf(0f, 0.7f, 0.9f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, videoBottomPx, w, h, colorPaint)
    }
}

private fun motionBottomStrip(frame: Bitmap): Bitmap {
    val w = frame.width
    val stripHeight = (frame.height / 8).coerceAtLeast(2)
    val source = IntArray(w * stripHeight)
    frame.getPixels(source, 0, w, 0, frame.height - stripHeight, w, stripHeight)
    val saturated = IntArray(source.size)
    for (i in source.indices) {
        val pixel = source[i]
        val red = (pixel ushr 16) and 255
        val green = (pixel ushr 8) and 255
        val blue = pixel and 255
        val gray = red * 0.213f + green * 0.715f + blue * 0.072f
        fun channel(value: Int) = (gray + (value - gray) * 1.4f).toInt().coerceIn(0, 255)
        saturated[i] = AndroidColor.rgb(channel(red), channel(green), channel(blue))
    }
    val horizontal = IntArray(source.size)
    val blurred = IntArray(source.size)
    val radius = 25
    val sigma = radius / 3f
    val weights = FloatArray(radius * 2 + 1) { index ->
        val distance = (index - radius).toFloat() / sigma
        exp(-0.5f * distance * distance)
    }
    val weightTotal = weights.sum()
    for (y in 0 until stripHeight) for (x in 0 until w) {
        var red = 0f; var green = 0f; var blue = 0f
        for (offset in -radius..radius) {
            val pixel = saturated[y * w + (x + offset).coerceIn(0, w - 1)]
            val weight = weights[offset + radius]
            red += ((pixel ushr 16) and 255) * weight
            green += ((pixel ushr 8) and 255) * weight
            blue += (pixel and 255) * weight
        }
        horizontal[y * w + x] = AndroidColor.rgb(
            (red / weightTotal).toInt(),
            (green / weightTotal).toInt(),
            (blue / weightTotal).toInt(),
        )
    }
    for (y in 0 until stripHeight) for (x in 0 until w) {
        var red = 0f; var green = 0f; var blue = 0f
        for (offset in -radius..radius) {
            val pixel = horizontal[(y + offset).coerceIn(0, stripHeight - 1) * w + x]
            val weight = weights[offset + radius]
            red += ((pixel ushr 16) and 255) * weight
            green += ((pixel ushr 8) and 255) * weight
            blue += (pixel and 255) * weight
        }
        // The player applies a 35% black scrim, then a 4% white scrim to the blurred strip.
        fun scrim(value: Float) = ((value / weightTotal * 0.65f * 0.96f) + 255f * 0.04f)
            .toInt().coerceIn(0, 255)
        blurred[y * w + x] = AndroidColor.rgb(scrim(red), scrim(green), scrim(blue))
    }
    return Bitmap.createBitmap(blurred, w, stripHeight, Bitmap.Config.ARGB_8888)
}
