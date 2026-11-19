package com.ljyh.mei.ui.component.artwork.album

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class AlbumPackageVertex(val x: Float, val y: Float, val z: Float)

/** Horizontal world-space mirror. Points touching the floor are invariant. */
internal fun albumReflectedVertex(point: AlbumPackageVertex, groundY: Float) =
    point.copy(y = 2f * groundY - point.y)

/** Model yaw followed by the shared camera pitch; reflection is applied before this view. */
internal class AlbumCamera(yaw: Float, pitch: Float, private val pivotX: Float = 0f) {
    private val turn = yaw % 360f * PI.toFloat() / 180f
    private val tilt = pitch * PI.toFloat() / 180f
    private val cosTurn = cos(turn)
    private val sinTurn = sin(turn)
    private val cosTilt = cos(tilt)
    private val sinTilt = sin(tilt)

    fun view(point: AlbumPackageVertex): AlbumPackageVertex {
        val x = point.x - pivotX
        val rx = x * cosTurn + point.z * sinTurn
        val rz = -x * sinTurn + point.z * cosTurn
        return AlbumPackageVertex(rx, point.y * cosTilt - rz * sinTilt,
            point.y * sinTilt + rz * cosTilt)
    }
}

/** Both passes use the same camera distance, viewport fit, centre and pixel scale. */
internal fun albumPerspectivePoint(point: AlbumPackageVertex): Offset {
    val perspective = 4.8f / (4.8f - point.z)
    return Offset(point.x * perspective, point.y * perspective)
}

internal fun albumFaceIsVisible(points: List<Offset>, reflected: Boolean): Boolean {
    if (points.size < 3) return false
    val a = points[1] - points[0]
    val b = points[2] - points[0]
    val winding = a.x * b.y - a.y * b.x
    // A reflection changes handedness; keep UV-to-vertex correspondence and reverse culling.
    return if (reflected) winding < -.1f else winding > .1f
}
