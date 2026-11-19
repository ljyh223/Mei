package com.ljyh.mei.ui.component.artwork.album

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Small, dependency-free perspective renderer. The lid and tray are separate solid meshes. */
internal class AlbumCdRenderer(private val label: AlbumPackageLabel) {
    private enum class Surface { Cover, Insert, Tray, Back, Spine, Edge }
    private data class Face(val vertices: List<AlbumPackageVertex>, val surface: Surface)
    private data class ProjectedFace(val points: List<Offset>, val depth: Float, val surface: Surface)

    private val mirror = AlbumMirror()
    private val ink = AlbumPackageInk()
    private val paint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()
    private val path = Path()
    private val source = floatArrayOf(0f, 0f, TextureSize, 0f, TextureSize, TextureSize, 0f, TextureSize)
    private val destination = FloatArray(8)

    fun draw(scope: DrawScope, cover: Painter, yaw: Float, pitch: Float, opening: Float) = with(scope) {
        val hinge = radians(opening)
        val progress = (opening / 135f).coerceIn(0f, 1f)
        var scale = min(size.width * (.66f - .12f * progress), size.height * .72f)
        val pivotX = min(0f, cos(hinge)) * .5f
        val center = Offset(size.width / 2f, size.height * AlbumModelCenterY)
        var projectedCenter = Offset.Zero
        fun articulated(point: AlbumPackageVertex, lid: Boolean): AlbumPackageVertex {
            if (!lid) return point
            val x = point.x + .5f
            val z = point.z - .04f
            return AlbumPackageVertex(-.5f + x * cos(hinge) - z * sin(hinge), point.y,
                .04f + x * sin(hinge) + z * cos(hinge))
        }
        fun project(point: AlbumPackageVertex): Offset = center + (albumPerspectivePoint(point) - projectedCenter) * scale
        val worldMeshes = box(-.065f, .012f, Surface.Tray, Surface.Back).map { face ->
            Face(face.vertices.map { articulated(it, lid = false) }, face.surface)
        } + box(.04f, .057f, Surface.Cover, Surface.Insert).map { face ->
            Face(face.vertices.map { articulated(it, lid = true) }, face.surface)
        }
        val groundY = worldMeshes.maxOf { face -> face.vertices.maxOf { it.y } }
        val camera = AlbumCamera(yaw, pitch, pivotX)
        fun cameraMeshes(reflected: Boolean) = worldMeshes.map { face ->
            Face(face.vertices.map { point ->
                camera.view(if (reflected) albumReflectedVertex(point, groundY) else point)
            }, face.surface)
        }
        val meshes = cameraMeshes(false)
        // Fit around the projected bounds after the reading camera turns. The hinge's
        // unrotated midpoint leaves the open booklet off-centre and unnecessarily small.
        val projectedPoints = meshes.flatMap { it.vertices }.map(::albumPerspectivePoint)
        projectedCenter = Offset(
            (projectedPoints.minOf { it.x } + projectedPoints.maxOf { it.x }) / 2f,
            (projectedPoints.minOf { it.y } + projectedPoints.maxOf { it.y }) / 2f,
        )
        projectedPoints.forEach { point ->
            scale = min(scale, size.width * .45f / abs(point.x - projectedCenter.x).coerceAtLeast(.001f))
            scale = min(scale, size.height * AlbumModelHalfHeight / abs(point.y - projectedCenter.y).coerceAtLeast(.001f))
        }
        fun prepare(meshes: List<Face>, reflected: Boolean) = meshes.mapNotNull { face ->
            val points = face.vertices.map(::project)
            if (albumFaceIsVisible(points, reflected)) {
                ProjectedFace(points, face.vertices.sumOf { it.z.toDouble() }.toFloat() / 4f, face.surface)
            } else null
        }.sortedBy { it.depth }
        val original = prepare(meshes, false)
        val reflected = prepare(cameraMeshes(true), true)
        val modelBottom = original.flatMap { it.points }.maxOfOrNull { it.y } ?: center.y
        val reflectionPoints = reflected.flatMap { it.points }
        val geometry = if (reflectionPoints.isEmpty()) null else albumReflectionGeometry(
            size.height, modelBottom, reflectionPoints.minOf { it.y }, reflectionPoints.maxOf { it.y },
        )
        fun drawFaces(projectedFaces: List<ProjectedFace>) {
            projectedFaces.forEach { face ->
                face.points.forEachIndexed { index, point ->
                    destination[index * 2] = point.x
                    destination[index * 2 + 1] = point.y
                }
                if (!matrix.setPolyToPoly(source, 0, destination, 0, 4)) return@forEach
                val canvas = drawContext.canvas.nativeCanvas
                canvas.save()
                try {
                    canvas.concat(matrix)
                    canvas.clipRect(0f, 0f, TextureSize, TextureSize)
                    drawSurface(scope, canvas, cover, face.surface, yaw, opening)
                    paint.shader = null
                    paint.color = 0x88A5A5A5.toInt()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1.8f
                    canvas.drawRect(1f, 1f, TextureSize - 1f, TextureSize - 1f, paint)
                    paint.style = Paint.Style.FILL
                } finally {
                    canvas.restore()
                }
            }
        }
        mirror.draw(scope, geometry, reflection = { drawFaces(reflected) }, content = { drawFaces(original) })
    }

    private fun box(back: Float, front: Float, face: Surface, reverse: Surface): List<Face> {
        fun p(x: Float, y: Float, z: Float) = AlbumPackageVertex(x, y, z)
        return listOf(
            Face(listOf(p(-.5f,-.5f,front),p(.5f,-.5f,front),p(.5f,.5f,front),p(-.5f,.5f,front)), face),
            Face(listOf(p(.5f,-.5f,back),p(-.5f,-.5f,back),p(-.5f,.5f,back),p(.5f,.5f,back)), reverse),
            Face(listOf(p(-.5f,-.5f,back),p(-.5f,-.5f,front),p(-.5f,.5f,front),p(-.5f,.5f,back)), Surface.Spine),
            Face(listOf(p(.5f,-.5f,front),p(.5f,-.5f,back),p(.5f,.5f,back),p(.5f,.5f,front)), Surface.Edge),
            Face(listOf(p(-.5f,-.5f,back),p(.5f,-.5f,back),p(.5f,-.5f,front),p(-.5f,-.5f,front)), Surface.Edge),
            Face(listOf(p(-.5f,.5f,front),p(.5f,.5f,front),p(.5f,.5f,back),p(-.5f,.5f,back)), Surface.Edge),
        )
    }

    private fun drawSurface(scope: DrawScope, canvas: Canvas, cover: Painter, surface: Surface, yaw: Float, opening: Float) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = when (surface) {
            Surface.Cover -> 0xFF303030.toInt()
            Surface.Insert, Surface.Back -> 0xFF313131.toInt()
            Surface.Tray -> 0xFF1A1A1A.toInt()
            Surface.Spine -> 0xFF535353.toInt()
            Surface.Edge -> 0xFF868686.toInt()
        }
        canvas.drawRect(0f, 0f, TextureSize, TextureSize, paint)
        when (surface) {
            Surface.Cover -> {
                // Reuse Coil's app-level loader and painter; no bitmap copies or extra image cache.
                canvas.save()
                canvas.translate(36f, 0f)
                canvas.scale(.94f, 1f)
                with(scope) { with(cover) { draw(Size(TextureSize, TextureSize)) } }
                canvas.restore()
                paint.color = 0xFF3A3A3A.toInt()
                canvas.drawRect(0f, 0f, 36f, TextureSize, paint)
                paint.color = 0x77A6A6A6
                canvas.drawRect(31f, 0f, 34f, TextureSize, paint)
                sheen(canvas, yaw + opening, .16f)
            }
            Surface.Tray -> disc(scope, canvas, cover, yaw)
            Surface.Insert, Surface.Back -> ink.liner(canvas, label, TextureSize)
            Surface.Spine -> {
                sheen(canvas, yaw, .30f)
                text(canvas, "${label.title} / ${label.artist}", 36f, 330f, 24f, 0xFFE2E2E2.toInt())
            }
            Surface.Edge -> sheen(canvas, yaw, .32f)
        }
    }

    private fun disc(scope: DrawScope, canvas: Canvas, cover: Painter, yaw: Float) {
        val bounds = RectF(25f, 25f, 575f, 575f)
        paint.color = 0xFF0A0A0A.toInt()
        canvas.drawRoundRect(13f, 13f, 587f, 587f, 12f, 12f, paint)
        paint.color = 0xFFBBBBBB.toInt()
        canvas.drawOval(bounds, paint)
        canvas.save()
        path.reset()
        path.addCircle(300f, 300f, 267f, Path.Direction.CW)
        canvas.clipPath(path)
        with(scope) { with(cover) { draw(Size(TextureSize, TextureSize)) } }
        with(scope) {
            drawCircle(
                Brush.sweepGradient(listOf(Color.White.copy(alpha = .02f), Color.White.copy(alpha = .35f),
                    Color(0xFFC7C7C7).copy(alpha = .10f), Color.Transparent, Color(0xFFDCDCDC).copy(alpha = .30f),
                    Color.White.copy(alpha = .02f)), center = Offset(300f, 300f)),
                radius = 267f, center = Offset(300f, 300f),
            )
        }
        sheen(canvas, yaw, .20f)
        canvas.restore()
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.3f
        repeat(6) { index ->
            paint.color = 0x449C9C9C
            canvas.drawCircle(300f, 300f, 240f + index * 4.5f, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = 0xFFD6D6D6.toInt()
        canvas.drawCircle(300f, 300f, 55f, paint)
        paint.color = 0xFF878787.toInt()
        canvas.drawCircle(300f, 300f, 42f, paint)
        paint.color = 0xFFCDCDCD.toInt()
        canvas.drawCircle(300f, 300f, 33f, paint)
        paint.color = 0xFF151515.toInt()
        canvas.drawCircle(300f, 300f, 12f, paint)
    }

    private fun sheen(canvas: Canvas, angle: Float, alpha: Float) {
        val shift = sin(radians(angle)) * 210f
        val highlight = ((alpha * 255).toInt() shl 24) or 0xFFFFFF
        val softHighlight = ((alpha * 127).toInt() shl 24) or 0xFFFFFF
        paint.shader = LinearGradient(-100f + shift, 0f, 650f + shift, 450f,
            intArrayOf(0x00FFFFFF, highlight, 0x00FFFFFF, softHighlight, 0x00FFFFFF),
            floatArrayOf(0f, .27f, .46f, .73f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, TextureSize, TextureSize, paint)
        paint.shader = null
    }

    private fun text(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
        canvas.drawText(android.text.TextUtils.ellipsize(value, paint, (TextureSize - x - 24f).coerceAtLeast(1f), android.text.TextUtils.TruncateAt.END).toString(), x, y, paint)
    }

    private fun radians(degrees: Float) = degrees * (PI.toFloat() / 180f)

    private companion object { const val TextureSize = 600f }
}
