package com.ljyh.mei.ui.component.player.cover

import android.content.Context
import android.graphics.Matrix
import android.view.TextureView

/** Aspect-correct texture output shared by album covers and decorative artist video. */
internal class ArtworkTextureView(context: Context) : TextureView(context) {
    private var videoWidth = 0
    private var videoHeight = 0
    private var pixelRatio = 1f
    private var fitWidth = false

    fun setVideoDimensions(width: Int, height: Int, ratio: Float, fitWidth: Boolean) {
        videoWidth = width
        videoHeight = height
        pixelRatio = ratio
        this.fitWidth = fitWidth
        updateTransform()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateTransform()
    }

    private fun updateTransform() {
        if (width <= 0 || height <= 0 || videoWidth <= 0 || videoHeight <= 0 || pixelRatio <= 0f) {
            setTransform(Matrix())
            return
        }
        val videoRatio = videoWidth * pixelRatio / videoHeight
        val viewRatio = width.toFloat() / height
        if (fitWidth) {
            // TextureView initially stretches the decoded frame to its bounds. Correct only its
            // height, anchored at the top, so the video keeps its aspect ratio and both sides.
            setTransform(Matrix().apply {
                setScale(1f, viewRatio / videoRatio, width / 2f, 0f)
            })
            return
        }
        val scaleX = if (videoRatio > viewRatio) videoRatio / viewRatio else 1f
        val scaleY = if (videoRatio < viewRatio) viewRatio / videoRatio else 1f
        setTransform(Matrix().apply { setScale(scaleX, scaleY, width / 2f, height / 2f) })
    }
}
