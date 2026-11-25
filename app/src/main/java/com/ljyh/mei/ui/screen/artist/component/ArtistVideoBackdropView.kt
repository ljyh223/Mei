package com.ljyh.mei.ui.screen.artist.component

import com.ljyh.mei.ui.component.artwork.backdrop.ArtworkBackdropGeometry
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import com.ljyh.mei.ui.component.player.cover.ArtworkTextureView

/** One texture is composed before filtering; neither child may escape its destination region. */
internal class ArtistVideoBackdropView(context: Context) : FrameLayout(context) {
    val texture = ArtworkTextureView(context)
    private val fadePaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
    private val composition = if (Build.VERSION.SDK_INT >= 29) ArtistVideoComposition() else null
    private var extraBlur = 0

    init {
        clipChildren = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        addView(texture)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(w, h)
        texture.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(w / 2, MeasureSpec.EXACTLY))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        texture.layout(0, 0, width, width / 2)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        fadePaint.shader = artistVideoFade(w.toFloat())
        if (Build.VERSION.SDK_INT >= 29) composition?.resize(w, h, resources.displayMetrics.density, extraBlur)
    }

    fun updatePresentation(ready: Boolean, scrollPixels: Float) {
        val density = resources.displayMetrics.density
        val scroll = scrollPixels / density
        val zoom = ArtworkBackdropGeometry.scale(scroll)
        scaleX = zoom
        scaleY = zoom
        translationY = -scrollPixels.coerceAtLeast(0f) * 0.4f
        alpha = if (ready) ArtworkBackdropGeometry.opacity(scroll) else 0f
        val radius = (ArtworkBackdropGeometry.extraBlur(scroll) * density).toInt()
        if (radius != extraBlur) {
            extraBlur = radius
            if (Build.VERSION.SDK_INT >= 29) composition?.setExtraBlur(radius)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return
        if (Build.VERSION.SDK_INT >= 29 && canvas.isHardwareAccelerated) {
            // RenderEffect belongs to an explicitly drawn node, not an outer View whose
            // recording path is controlled by AndroidView interop. Mask after filtering.
            composition?.record { drawFrames(it) }?.let(canvas::drawRenderNode)
        } else {
            val save = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
            drawFrames(canvas)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fadePaint)
            canvas.restoreToCount(save)
        }
    }

    private fun drawFrames(canvas: Canvas) {
        drawArtistVideoFrames(canvas, width.toFloat(), texture.height.toFloat(), height.toFloat()) {
            drawChild(it, texture, drawingTime)
        }
    }

    override fun onDetachedFromWindow() {
        if (Build.VERSION.SDK_INT >= 29) composition?.discard()
        super.onDetachedFromWindow()
    }
}

internal fun artistVideoFade(width: Float): Shader = LinearGradient(0f, 0f, 0f, width,
    intArrayOf(Color.WHITE, Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT),
    floatArrayOf(0f, 0.25f, 0.65f, 1f), Shader.TileMode.CLAMP)

/** In both draws, the original lower edge maps to the exact same seam coordinate. */
internal inline fun drawArtistVideoFrames(canvas: Canvas, width: Float, frameHeight: Float,
    height: Float, drawFrame: (Canvas) -> Unit) {
    val original = canvas.save()
    canvas.clipRect(0f, 0f, width, minOf(frameHeight, height))
    drawFrame(canvas)
    canvas.restoreToCount(original)
    if (height > frameHeight) {
        val reflection = canvas.save()
        canvas.clipRect(0f, frameHeight, width, height)
        canvas.translate(0f, frameHeight * 4f)
        canvas.scale(1f, -3f)
        drawFrame(canvas)
        canvas.restoreToCount(reflection)
    }
}
