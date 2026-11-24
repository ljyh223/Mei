package com.ljyh.mei.ui.screen.artist.component

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import com.ljyh.mei.ui.component.player.cover.ArtworkTextureView

/** Draw one decoded texture twice; reflection never needs another decoder or CPU frame copy. */
internal class ArtistVideoBackdropView(context: Context) : FrameLayout(context) {
    val texture = ArtworkTextureView(context)
    private val fadePaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
    private val effects = if (Build.VERSION.SDK_INT >= 31) ArtistVideoEffects() else null
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
        fadePaint.shader = LinearGradient(0f, 0f, 0f, w.toFloat(),
            intArrayOf(Color.WHITE, Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT),
            floatArrayOf(0f, 0.25f, 0.65f, 1f), Shader.TileMode.CLAMP)
        if (Build.VERSION.SDK_INT >= 31) setRenderEffect(effects?.effect(w, h, resources.displayMetrics.density, extraBlur))
    }

    fun updatePresentation(ready: Boolean, scrollPixels: Float) {
        val density = resources.displayMetrics.density
        val scroll = scrollPixels / density
        val zoom = ArtistBackdropGeometry.scale(scroll)
        scaleX = zoom
        scaleY = zoom
        translationY = -scrollPixels.coerceAtLeast(0f) * 0.4f
        alpha = if (ready) ArtistBackdropGeometry.opacity(scroll) else 0f
        val radius = (ArtistBackdropGeometry.extraBlur(scroll) * density).toInt()
        if (radius != extraBlur && width > 0 && height > 0) {
            extraBlur = radius
            if (Build.VERSION.SDK_INT >= 31) setRenderEffect(effects?.effect(width, height, density, radius))
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        val save = if (Build.VERSION.SDK_INT < 31) canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
            else canvas.save()
        drawChild(canvas, texture, drawingTime)
        val reflection = canvas.save()
        canvas.translate(0f, texture.height * 4f)
        canvas.scale(1f, -3f)
        drawChild(canvas, texture, drawingTime)
        canvas.restoreToCount(reflection)
        if (Build.VERSION.SDK_INT < 31) canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fadePaint)
        canvas.restoreToCount(save)
    }
}

@RequiresApi(31)
private class ArtistVideoEffects {
    private val variable = if (Build.VERSION.SDK_INT >= 33) ArtistVariableBlur() else null
    private var previousDensity = 0f
    private var bounds = Size.Zero
    private var base: RenderEffect? = null

    fun effect(width: Int, height: Int, density: Float, extraPixels: Int): RenderEffect {
        val size = Size(width.toFloat(), height.toFloat())
        if (size != bounds || density != previousDensity || base == null) {
            val image = if (Build.VERSION.SDK_INT >= 33) variable?.effect(size, density) else null
            val mask = RenderEffect.createShaderEffect(LinearGradient(0f, 0f, 0f, size.width,
                intArrayOf(Color.WHITE, Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT),
                floatArrayOf(0f, 0.25f, 0.65f, 1f), Shader.TileMode.CLAMP))
            base = RenderEffect.createBlendModeEffect(image ?: RenderEffect.createOffsetEffect(0f, 0f), mask, BlendMode.DST_IN)
            bounds = size
            previousDensity = density
        }
        return if (extraPixels > 0) RenderEffect.createBlurEffect(extraPixels.toFloat(), extraPixels.toFloat(),
            checkNotNull(base), Shader.TileMode.MIRROR) else checkNotNull(base)
    }
}
