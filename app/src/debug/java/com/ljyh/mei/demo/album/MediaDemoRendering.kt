package com.ljyh.mei.demo.album

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal data class MediaDemoVertex(val x: Float, val y: Float, val z: Float)
internal enum class MediaDemoMaterial {
    CassetteA, CassetteB, CassetteEdge, CassetteCover, CassetteInsert,
    CassetteTray, CassetteBack, CassetteCaseEdge, SleeveCover, SleeveBack, SleeveEdge,
    RecordA, RecordB, RecordEdge,
}
internal data class MediaDemoFace(
    val vertices: List<MediaDemoVertex>,
    val material: MediaDemoMaterial,
    val textureWidth: Float = 600f,
)

/** Perspective and depth ordering shared only by the two extra Debug demos. */
internal class MediaDemoScene {
    private val matrix = Matrix()
    private val source = FloatArray(8)
    private val destination = FloatArray(8)

    fun draw(
        scope: DrawScope,
        faces: List<MediaDemoFace>,
        yaw: Float,
        pitch: Float,
        pivotX: Float = 0f,
        preferredScale: Float = .54f,
        surface: (Canvas, MediaDemoMaterial) -> Unit,
    ) = with(scope) {
        val turn = radians(yaw % 360f)
        val tilt = radians(pitch)
        val meshes = faces.map { face ->
            face.copy(vertices = face.vertices.map { point ->
                val x = point.x - pivotX
                val rx = x * cos(turn) + point.z * sin(turn)
                val rz = -x * sin(turn) + point.z * cos(turn)
                MediaDemoVertex(rx, point.y * cos(tilt) - rz * sin(tilt), point.y * sin(tilt) + rz * cos(tilt))
            })
        }
        var scale = min(size.width * preferredScale, size.height * .72f)
        // Fit the whole physical object, including the fully extracted record, at every angle.
        meshes.forEach { face -> face.vertices.forEach { point ->
            val perspective = 4.8f / (4.8f - point.z)
            scale = min(scale, size.width * .45f / abs(point.x * perspective).coerceAtLeast(.001f))
            scale = min(scale, size.height * .38f / abs(point.y * perspective).coerceAtLeast(.001f))
        } }
        val center = Offset(size.width / 2f, size.height * .46f)
        fun project(p: MediaDemoVertex): Offset {
            val perspective = 4.8f / (4.8f - p.z)
            return Offset(center.x + p.x * scale * perspective, center.y + p.y * scale * perspective)
        }
        repeat(12) { layer ->
            val width = scale * (1.45f + layer * .055f)
            drawOval(Color.Black.copy(alpha = .012f),
                topLeft = Offset(center.x - width / 2f, center.y + scale * .62f - layer),
                size = Size(width, scale * .075f + layer * 2f))
        }
        meshes.sortedBy { face -> face.vertices.sumOf { it.z.toDouble() } / 4 }.forEach { face ->
            val points = face.vertices.map(::project)
            val a = points[1] - points[0]
            val b = points[2] - points[0]
            if (a.x * b.y - a.y * b.x <= .1f) return@forEach
            source[0] = 0f; source[1] = 0f
            source[2] = face.textureWidth; source[3] = 0f
            source[4] = face.textureWidth; source[5] = 600f
            source[6] = 0f; source[7] = 600f
            points.forEachIndexed { index, point ->
                destination[index * 2] = point.x
                destination[index * 2 + 1] = point.y
            }
            if (!matrix.setPolyToPoly(source, 0, destination, 0, 4)) return@forEach
            val canvas = drawContext.canvas.nativeCanvas
            canvas.save()
            try {
                canvas.concat(matrix)
                canvas.clipRect(0f, 0f, face.textureWidth, 600f)
                surface(canvas, face.material)
            } finally { canvas.restore() }
        }
    }
}

internal fun mediaDemoBox(
    halfWidth: Float,
    halfHeight: Float,
    halfDepth: Float,
    front: MediaDemoMaterial,
    back: MediaDemoMaterial,
    edge: MediaDemoMaterial,
    textureWidth: Float = 600f,
): List<MediaDemoFace> {
    val l = -halfWidth; val r = halfWidth
    val t = -halfHeight; val b = halfHeight
    val f = halfDepth; val z = -halfDepth
    fun face(material: MediaDemoMaterial, vararg p: MediaDemoVertex) = MediaDemoFace(p.toList(), material, textureWidth)
    fun p(x: Float, y: Float, z: Float) = MediaDemoVertex(x, y, z)
    return listOf(
        face(front, p(l,t,f), p(r,t,f), p(r,b,f), p(l,b,f)),
        face(back, p(r,t,z), p(l,t,z), p(l,b,z), p(r,b,z)),
        face(edge, p(l,t,z), p(l,t,f), p(l,b,f), p(l,b,z)),
        face(edge, p(r,t,f), p(r,t,z), p(r,b,z), p(r,b,f)),
        face(edge, p(l,t,z), p(r,t,z), p(r,t,f), p(l,t,f)),
        face(edge, p(l,b,f), p(r,b,f), p(r,b,z), p(l,b,z)),
    )
}

/** Drawing primitives use the existing Coil painter without rasterizing a second texture cache. */
internal class MediaDemoInk {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun fill(color: Int): Paint = paint.apply { shader = null; style = Paint.Style.FILL; this.color = color }
    fun stroke(color: Int, width: Float): Paint = fill(color).apply { style = Paint.Style.STROKE; strokeWidth = width }
    fun text(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        fill(color)
        paint.textSize = size
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        canvas.drawText(value, x, y, paint)
    }
    fun artwork(scope: DrawScope, canvas: Canvas, cover: Painter, x: Float, y: Float, width: Float, height: Float = width) {
        canvas.save()
        try {
            canvas.translate(x, y)
            canvas.scale(width / 600f, height / 600f)
            with(scope) { with(cover) { draw(Size(600f, 600f)) } }
        } finally { canvas.restore() }
    }
    fun sheen(canvas: Canvas, width: Float, height: Float, angle: Float, strength: Float = .18f) {
        val shift = sin(radians(angle)) * width * .3f
        val highlight = ((255 * strength).toInt() shl 24) or 0xFFFFFF
        fill(0xFFFFFFFF.toInt())
        paint.shader = LinearGradient(shift - width * .3f, 0f, width + shift, height,
            intArrayOf(0x00FFFFFF, highlight, 0x00FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, .42f, .63f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
    }
}

internal fun radians(degrees: Float) = degrees * (PI.toFloat() / 180f)
