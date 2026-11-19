package com.ljyh.mei.ui.component.artwork.album

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import kotlin.math.ceil
import kotlin.math.floor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas

/** Composites independently projected mirror geometry; only alpha is changed here. */
internal class AlbumMirror {
    // The mask must cover every edge pixel. Antialiasing its rectangle leaves a partially
    // unmasked seam in a fractionally positioned saveLayer during rotation.
    private val mask = Paint().apply {
        isAntiAlias = false
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    fun draw(
        scope: DrawScope,
        geometry: AlbumReflectionGeometry?,
        reflection: () -> Unit,
        content: () -> Unit,
    ) {
        val canvas = scope.drawContext.canvas.nativeCanvas
        if (geometry != null) {
            val bottom = geometry.top + geometry.height
            val layerTop = floor(geometry.top)
            val layerBottom = ceil(bottom)
            val layerWidth = ceil(scope.size.width)
            val layer = canvas.saveLayer(0f, layerTop, layerWidth, layerBottom, null)
            try {
                canvas.clipRect(0f, layerTop, layerWidth, layerBottom)
                reflection()
                mask.shader = LinearGradient(0f, geometry.top, 0f, bottom,
                    intArrayOf(0x00FFFFFF, 0x46FFFFFF, 0x12FFFFFF, 0x00FFFFFF),
                    floatArrayOf(0f, .08f, .45f, .96f), Shader.TileMode.CLAMP)
                // Overscan the alpha mask; fade to transparent before reaching the clip edge.
                canvas.drawRect(-2f, layerTop - 2f, layerWidth + 2f, layerBottom + 2f, mask)
            } finally { canvas.restoreToCount(layer) }
        }
        content()
    }
}
