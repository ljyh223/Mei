package com.ljyh.mei.ui.screen.artist.component

import com.ljyh.mei.ui.component.artwork.backdrop.ArtworkVariableBlur
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size

/** Explicit GPU passes: combined frame -> variable blur -> isolated fade -> scroll blur. */
@RequiresApi(29)
internal class ArtistVideoComposition {
    val imageNode = RenderNode("artist-video-image").apply { setClipToBounds(true) }
    val outputNode = RenderNode("artist-video-output").apply { setClipToBounds(true) }
    private val variable = if (Build.VERSION.SDK_INT >= 33) ArtworkVariableBlur() else null
    private val fadePaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
    private var width = 0
    private var height = 0

    fun resize(width: Int, height: Int, density: Float, extraBlur: Int) {
        this.width = width
        this.height = height
        imageNode.setPosition(0, 0, width, height)
        outputNode.setPosition(0, 0, width, height)
        fadePaint.shader = artistVideoFade(width.toFloat())
        if (Build.VERSION.SDK_INT >= 33) {
            imageNode.setRenderEffect(variable?.effect(Size(width.toFloat(), height.toFloat()), density))
        }
        setExtraBlur(extraBlur)
    }

    fun setExtraBlur(pixels: Int) {
        if (Build.VERSION.SDK_INT >= 31) outputNode.setRenderEffect(
            if (pixels > 0) RenderEffect.createBlurEffect(pixels.toFloat(), pixels.toFloat(), Shader.TileMode.MIRROR)
            else null)
    }

    fun record(drawContent: (Canvas) -> Unit): RenderNode {
        check(width > 0 && height > 0)
        val image = imageNode.beginRecording(width, height)
        try { drawContent(image) } finally { imageNode.endRecording() }
        val output = outputNode.beginRecording(width, height)
        try {
            // A real isolated layer guarantees DST_IN affects only the composited video,
            // even when the TextureView is hosted through Compose's AndroidView bridge.
            val layer = output.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
            output.drawRenderNode(imageNode)
            output.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fadePaint)
            output.restoreToCount(layer)
        } finally { outputNode.endRecording() }
        return outputNode
    }

    fun discard() {
        outputNode.discardDisplayList()
        imageNode.discardDisplayList()
    }
}
