package com.ljyh.mei.ui.component.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.painter.Painter
import com.ljyh.mei.ui.component.sheet.playerContainerFrame

internal data class PlayerExpansionOrigin(val surface: Rect, val artwork: Rect)

/** Geometry and a cached thumbnail only; playback and page state remain in their existing owners. */
@Stable
internal class PlayerExpansionState {
    private var measured by mutableStateOf<Pair<Size, PlayerExpansionOrigin>?>(null)
    private var thumbnail by mutableStateOf<Pair<String, Painter>?>(null)

    fun recordOrigin(surface: Rect, artwork: Rect, window: Size, canRecord: Boolean) {
        if (canRecord) measured = window to PlayerExpansionOrigin(surface, artwork)
    }

    fun recordThumbnail(url: String, painter: Painter) { thumbnail = url to painter }
    fun thumbnailFor(url: String?): Painter? = thumbnail?.takeIf { it.first == url }?.second

    fun originInHost(hostOrigin: Offset, window: Size): PlayerExpansionOrigin? {
        val measurement = measured?.takeIf { it.first == window }?.second ?: return null
        val origin = PlayerExpansionOrigin(measurement.surface.translate(-hostOrigin), measurement.artwork.translate(-hostOrigin))
        if (playerContainerFrame(origin.surface, window, 0f, 0f) == null) return null
        val artwork = origin.artwork
        if (listOf(artwork.left, artwork.top, artwork.right, artwork.bottom).any { !it.isFinite() } ||
            artwork.width <= 0f || artwork.height <= 0f ||
            artwork.left < origin.surface.left || artwork.top < origin.surface.top ||
            artwork.right > origin.surface.right || artwork.bottom > origin.surface.bottom) return null
        return origin
    }
}

/** Animate the visible cover bounds, so pause scaling is applied only once at the target. */
internal fun sharedPlayerArtworkBounds(source: Rect, target: Rect, scale: Float, progress: Float): Rect {
    if (target.width <= 0f || target.height <= 0f) return source
    val p = progress.coerceIn(0f, 1f)
    val insetX = target.width * (1f - scale) / 2f
    val insetY = target.height * (1f - scale) / 2f
    val visibleTarget = Rect(target.left + insetX, target.top + insetY, target.right - insetX, target.bottom - insetY)
    return lerp(source, visibleTarget, p)
}
