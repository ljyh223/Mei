package com.ljyh.mei.demo.album

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
internal class AlbumCaseRenderer {
    private data class Point(val x: Float, val y: Float, val z: Float)
    private enum class Surface { Cover, Insert, Tray, Back, Spine, Edge }
    private data class Face(val vertices: List<Point>, val surface: Surface)
    private data class ProjectedFace(val points: List<Offset>, val depth: Float, val surface: Surface)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()
    private val path = Path()
    private val source = floatArrayOf(0f, 0f, TextureSize, 0f, TextureSize, TextureSize, 0f, TextureSize)
    private val destination = FloatArray(8)

    fun draw(scope: DrawScope, cover: Painter, yaw: Float, pitch: Float, opening: Float) = with(scope) {
        val hinge = radians(opening)
        val turn = radians(yaw % 360f)
        val tilt = radians(pitch)
        val progress = (opening / 155f).coerceIn(0f, 1f)
        var scale = min(size.width * (.66f - .27f * progress), size.height * .72f)
        val pivotX = min(0f, cos(hinge)) * .5f
        val center = Offset(size.width / 2f, size.height * .46f)
        fun transform(point: Point, lid: Boolean): Point {
            val hinged = if (lid) {
                val x = point.x + .5f
                val z = point.z - .04f
                Point(-.5f + x * cos(hinge) - z * sin(hinge), point.y,
                    .04f + x * sin(hinge) + z * cos(hinge))
            } else point
            val x = hinged.x - pivotX
            val rotatedX = x * cos(turn) + hinged.z * sin(turn)
            val rotatedZ = -x * sin(turn) + hinged.z * cos(turn)
            return Point(rotatedX, hinged.y * cos(tilt) - rotatedZ * sin(tilt),
                hinged.y * sin(tilt) + rotatedZ * cos(tilt))
        }
        fun project(point: Point): Offset {
            val perspective = 4.8f / (4.8f - point.z)
            return Offset(center.x + point.x * scale * perspective, center.y + point.y * scale * perspective)
        }
        val meshes = box(-.065f, .012f, Surface.Tray, Surface.Back).map { face ->
            Face(face.vertices.map { transform(it, lid = false) }, face.surface)
        } + box(.04f, .057f, Surface.Cover, Surface.Insert).map { face ->
            Face(face.vertices.map { transform(it, lid = true) }, face.surface)
        }
        // Keep every corner in the stage even at steep pitch or an edge-on rotation.
        meshes.flatMap { it.vertices }.forEach { point ->
            val perspective = 4.8f / (4.8f - point.z)
            scale = min(scale, size.width * .45f / abs(point.x * perspective).coerceAtLeast(.001f))
            scale = min(scale, size.height * .38f / abs(point.y * perspective).coerceAtLeast(.001f))
        }
        val faces = meshes.mapNotNull { face ->
            val points = face.vertices.map(::project)
            val a = points[1] - points[0]
            val b = points[2] - points[0]
            // Cull back-facing surfaces so artwork is never mirrored through the box.
            if (a.x * b.y - a.y * b.x > .1f) {
                ProjectedFace(points, face.vertices.sumOf { it.z.toDouble() }.toFloat() / 4f, face.surface)
            } else null
        }

        // A fixed floor shadow grounds the object without moving any layout bounds.
        repeat(12) { layer ->
            val width = scale * (1.05f + layer * .055f)
            drawOval(Color.Black.copy(alpha = .012f),
                topLeft = Offset(center.x - width / 2f, center.y + scale * .64f - layer),
                size = Size(width, scale * .075f + layer * 2f))
        }
        faces.sortedBy { it.depth }.forEach { face ->
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
                paint.color = 0x889CA99A.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.8f
                canvas.drawRect(1f, 1f, TextureSize - 1f, TextureSize - 1f, paint)
                paint.style = Paint.Style.FILL
            } finally {
                canvas.restore()
            }
        }
    }

    private fun box(back: Float, front: Float, face: Surface, reverse: Surface): List<Face> {
        fun p(x: Float, y: Float, z: Float) = Point(x, y, z)
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
            Surface.Cover -> 0xFF263521.toInt()
            Surface.Insert, Surface.Back -> 0xFF29342A.toInt()
            Surface.Tray -> 0xFF171B18.toInt()
            Surface.Spine -> 0xFF4C554D.toInt()
            Surface.Edge -> 0xFF7C8981.toInt()
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
                paint.color = 0xFF333C35.toInt()
                canvas.drawRect(0f, 0f, 36f, TextureSize, paint)
                paint.color = 0x779DAA98
                canvas.drawRect(31f, 0f, 34f, TextureSize, paint)
                sheen(canvas, yaw + opening, .16f)
            }
            Surface.Tray -> disc(scope, canvas, cover, yaw)
            Surface.Insert, Surface.Back -> {
                text(canvas, "aespa", 42f, 64f, 20f, 0xFFBFCBB5.toInt())
                text(canvas, "ARMAGEDDON", 42f, 106f, 33f, 0xFFF0F2E9.toInt(), bold = true)
                text(canvas, "THE 1ST ALBUM", 42f, 145f, 18f, 0xFFBFCBB5.toInt())
                paint.color = 0x667D8B74
                canvas.drawRect(42f, 182f, 558f, 183f, paint)
                text(canvas, "CD / STEREO", 42f, 232f, 17f, 0xFFD0D9C6.toInt())
                text(canvas, "MEI COLLECTION", 42f, 528f, 16f, 0xFFBFCBB5.toInt())
                if (surface == Surface.Back) {
                    // Printed back insert, intentionally distinct from the front cover.
                    repeat(34) { index ->
                        val x = 365f + index * 5f
                        paint.color = 0xFFD1D8C7.toInt()
                        canvas.drawRect(x, 450f, x + if (index % 3 == 0) 3f else 1.4f, 520f, paint)
                    }
                }
            }
            Surface.Spine -> {
                sheen(canvas, yaw, .30f)
                text(canvas, "ARMAGEDDON  /  aespa", 36f, 330f, 24f, 0xFFDDE5D5.toInt())
            }
            Surface.Edge -> sheen(canvas, yaw, .32f)
        }
    }

    private fun disc(scope: DrawScope, canvas: Canvas, cover: Painter, yaw: Float) {
        val bounds = RectF(25f, 25f, 575f, 575f)
        paint.color = 0xFF080B09.toInt()
        canvas.drawRoundRect(13f, 13f, 587f, 587f, 12f, 12f, paint)
        paint.color = 0xFFB3BEB0.toInt()
        canvas.drawOval(bounds, paint)
        canvas.save()
        path.reset()
        path.addCircle(300f, 300f, 267f, Path.Direction.CW)
        canvas.clipPath(path)
        with(scope) { with(cover) { draw(Size(TextureSize, TextureSize)) } }
        with(scope) {
            drawCircle(
                Brush.sweepGradient(listOf(Color.White.copy(alpha = .02f), Color.White.copy(alpha = .35f),
                    Color(0xFFB6C9E5).copy(alpha = .10f), Color.Transparent, Color(0xFFD4E4A2).copy(alpha = .30f),
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
            paint.color = 0x448DA382
            canvas.drawCircle(300f, 300f, 240f + index * 4.5f, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = 0xFFD1D7D5.toInt()
        canvas.drawCircle(300f, 300f, 55f, paint)
        paint.color = 0xFF7D8A88.toInt()
        canvas.drawCircle(300f, 300f, 42f, paint)
        paint.color = 0xFFC7CFCC.toInt()
        canvas.drawCircle(300f, 300f, 33f, paint)
        paint.color = 0xFF111713.toInt()
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
        canvas.drawText(value, x, y, paint)
    }

    private fun radians(degrees: Float) = degrees * (PI.toFloat() / 180f)

    private companion object { const val TextureSize = 600f }
}
