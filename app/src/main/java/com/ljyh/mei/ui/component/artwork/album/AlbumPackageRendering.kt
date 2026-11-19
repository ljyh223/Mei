package com.ljyh.mei.ui.component.artwork.album

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal enum class AlbumPackageMaterial {
    CassetteA, CassetteB, CassetteEdge, CassetteCover, CassetteInsert,
    CassetteTray, CassetteBack, CassetteCaseEdge, SleeveCover, SleeveBack, SleeveEdge,
    RecordA, RecordB, RecordEdge,
}
internal data class AlbumPackageFace(
    val vertices: List<AlbumPackageVertex>,
    val material: AlbumPackageMaterial,
    val textureWidth: Float = 600f,
)

/** Perspective and depth ordering shared by cassette and vinyl artwork. */
internal class AlbumPackageScene {
    private data class ProjectedFace(val face: AlbumPackageFace, val points: List<Offset>, val depth: Float)
    private val mirror = AlbumMirror()
    private val matrix = Matrix()
    private val source = FloatArray(8)
    private val destination = FloatArray(8)

    fun draw(
        scope: DrawScope,
        faces: List<AlbumPackageFace>,
        yaw: Float,
        pitch: Float,
        pivotX: Float = 0f,
        preferredScale: Float = .54f,
        surface: (Canvas, AlbumPackageMaterial) -> Unit,
    ) = with(scope) {
        if (faces.isEmpty()) return@with
        val camera = AlbumCamera(yaw, pitch, pivotX)
        val groundY = faces.maxOf { face -> face.vertices.maxOf { it.y } }
        fun cameraMeshes(reflected: Boolean) = faces.map { face ->
            face.copy(vertices = face.vertices.map { point ->
                camera.view(if (reflected) albumReflectedVertex(point, groundY) else point)
            })
        }
        val meshes = cameraMeshes(false)
        var scale = min(size.width * preferredScale, size.height * .72f)
        // Fit only the actual object. Its reflection must share this exact fit, not a second
        // independently resized viewport that would break contact-point correspondence.
        meshes.forEach { face -> face.vertices.forEach { point ->
            val projected = albumPerspectivePoint(point)
            scale = min(scale, size.width * .45f / abs(projected.x).coerceAtLeast(.001f))
            scale = min(scale, size.height * AlbumModelHalfHeight / abs(projected.y).coerceAtLeast(.001f))
        } }
        val center = Offset(size.width / 2f, size.height * AlbumModelCenterY)
        fun project(point: AlbumPackageVertex) = center + albumPerspectivePoint(point) * scale
        fun prepare(meshes: List<AlbumPackageFace>, reflected: Boolean) = meshes.mapNotNull { face ->
            val points = face.vertices.map(::project)
            if (albumFaceIsVisible(points, reflected)) ProjectedFace(face, points,
                face.vertices.sumOf { it.z.toDouble() }.toFloat() / face.vertices.size) else null
        }.sortedBy { it.depth }
        val original = prepare(meshes, false)
        val reflected = prepare(cameraMeshes(true), true)
        val modelBottom = original.flatMap { it.points }.maxOfOrNull { it.y } ?: center.y
        val reflectedPoints = reflected.flatMap { it.points }
        val geometry = if (reflectedPoints.isEmpty()) null else albumReflectionGeometry(
            stageHeight = size.height, modelBottom = modelBottom,
            reflectionTop = reflectedPoints.minOf { it.y }, reflectionBottom = reflectedPoints.maxOf { it.y },
        )
        fun drawFaces(projectedFaces: List<ProjectedFace>) {
            projectedFaces.forEach { projected ->
                val face = projected.face
                source[0] = 0f; source[1] = 0f
                source[2] = face.textureWidth; source[3] = 0f
                source[4] = face.textureWidth; source[5] = 600f
                source[6] = 0f; source[7] = 600f
                projected.points.forEachIndexed { index, point ->
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
        mirror.draw(scope, geometry, reflection = { drawFaces(reflected) }, content = { drawFaces(original) })
    }
}

internal fun albumPackageBox(
    halfWidth: Float,
    halfHeight: Float,
    halfDepth: Float,
    front: AlbumPackageMaterial,
    back: AlbumPackageMaterial,
    edge: AlbumPackageMaterial,
    textureWidth: Float = 600f,
): List<AlbumPackageFace> {
    val l = -halfWidth; val r = halfWidth
    val t = -halfHeight; val b = halfHeight
    val f = halfDepth; val z = -halfDepth
    fun face(material: AlbumPackageMaterial, vararg p: AlbumPackageVertex) = AlbumPackageFace(p.toList(), material, textureWidth)
    fun p(x: Float, y: Float, z: Float) = AlbumPackageVertex(x, y, z)
    return listOf(
        face(front, p(l,t,f), p(r,t,f), p(r,b,f), p(l,b,f)),
        face(back, p(r,t,z), p(l,t,z), p(l,b,z), p(r,b,z)),
        face(edge, p(l,t,z), p(l,t,f), p(l,b,f), p(l,b,z)),
        face(edge, p(r,t,f), p(r,t,z), p(r,b,z), p(r,b,f)),
        face(edge, p(l,t,z), p(r,t,z), p(r,t,f), p(l,t,f)),
        face(edge, p(l,b,f), p(r,b,f), p(r,b,z), p(l,b,z)),
    )
}

/** Neutral physical material tokens; the cover artwork retains its original colours. */
internal object AlbumPackageColors {
    val paper = 0xFFF3F2EF.toInt()
    val text = 0xFF202124.toInt()
    val secondary = 0xFF606268.toInt()
    val rule = 0xFFD4D4D2.toInt()
    val shell = 0xFF43464B.toInt()
    val edge = 0xFFBFC2C6.toInt()
}

/** Drawing primitives reuse the Coil painter and a TextPaint; no extra texture cache. */
internal class AlbumPackageInk {
    val paint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    var textureWidth: Float = 600f
    private val paperFibres = android.graphics.Path().apply {
        val random = java.util.Random(1983L)
        repeat(150) {
            val x = random.nextFloat() * 600f
            val y = random.nextFloat() * 600f
            moveTo(x, y)
            rLineTo(.7f + random.nextFloat() * 2f, random.nextFloat() * .45f)
        }
    }

    fun paperGrain(canvas: Canvas, width: Float, height: Float = 600f) {
        canvas.save()
        try {
            canvas.scale(width / 600f, height / 600f)
            canvas.drawPath(paperFibres, stroke(0x16000000, .7f))
        } finally { canvas.restore() }
    }

    fun fill(color: Int): Paint = paint.apply { shader = null; style = Paint.Style.FILL; this.color = color }
    fun stroke(color: Int, width: Float): Paint = fill(color).apply { style = Paint.Style.STROKE; strokeWidth = width }
    fun text(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false, maxWidth: Float = (textureWidth - x - 16f).coerceAtLeast(1f)) {
        fill(color)
        paint.textSize = size
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        canvas.drawText(android.text.TextUtils.ellipsize(value, paint, maxWidth.coerceAtLeast(1f), android.text.TextUtils.TruncateAt.END).toString(), x, y, paint)
    }
    fun liner(canvas: Canvas, label: AlbumPackageLabel, width: Float) {
        val margin = width * .075f
        val available = width - margin * 2f
        canvas.drawRect(0f, 0f, width, 600f, fill(AlbumPackageColors.paper))
        paperGrain(canvas, width)
        text(canvas, label.title, margin, 65f, if (width < 500f) 24f else 32f,
            AlbumPackageColors.text, true, available)
        text(canvas, listOf(label.artist, label.year?.toString().orEmpty()).filter(String::isNotBlank).joinToString(" · "),
            margin, 102f, 17f, AlbumPackageColors.secondary, maxWidth = available)
        canvas.drawLine(margin, 132f, width - margin, 132f, stroke(AlbumPackageColors.rule, 1.5f))
        text(canvas, "TRACKLIST", margin, 169f, 13f, AlbumPackageColors.secondary, maxWidth = available)
        val largePrint = label.rowsPerPage <= 6
        label.tracks.forEachIndexed { row, track ->
            val y = 210f + row * if (largePrint) 50f else 38f
            val numberWidth = if (largePrint) 36f else 28f
            text(canvas, track.number.toString().padStart(2, '0'), margin, y, if (largePrint) 19f else 15f,
                AlbumPackageColors.secondary, maxWidth = numberWidth)
            val durationWidth = if (track.durationLabel.isEmpty()) 0f else if (largePrint) 62f else 48f
            text(canvas, track.title, margin + numberWidth + 4f, y, if (largePrint) 25f else 17f,
                AlbumPackageColors.text, maxWidth = available - numberWidth - 10f - durationWidth)
            if (durationWidth > 0f) text(canvas, track.durationLabel, width - margin - durationWidth, y,
                if (largePrint) 17f else 13f, AlbumPackageColors.secondary, maxWidth = durationWidth)
        }
        if (label.trackTotal == 0) text(canvas, "暂无曲目", margin, 215f, 18f, AlbumPackageColors.secondary, maxWidth = available)
        canvas.drawLine(margin, 536f, width - margin, 536f, stroke(AlbumPackageColors.rule, 1.5f))
        val range = label.tracks.let { tracks ->
            if (tracks.isEmpty()) "" else "${tracks.first().number.toString().padStart(2, '0')}—${tracks.last().number.toString().padStart(2, '0')} / ${label.trackTotal}"
        }
        text(canvas, range, margin, 566f, 13f, AlbumPackageColors.secondary, maxWidth = available)
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
