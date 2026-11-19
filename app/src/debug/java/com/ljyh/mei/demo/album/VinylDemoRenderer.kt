package com.ljyh.mei.demo.album

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.SweepGradient
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.cos
import kotlin.math.sin

/** A record slides through a paper sleeve; both label faces and the circular rim have depth. */
internal class VinylDemoRenderer {
    private val scene = MediaDemoScene()
    private val ink = MediaDemoInk()
    private val path = Path()
    private val sleeve = mediaDemoBox(.5f, .5f, .025f,
        MediaDemoMaterial.SleeveCover, MediaDemoMaterial.SleeveBack, MediaDemoMaterial.SleeveEdge)

    fun draw(scope: DrawScope, cover: Painter, yaw: Float, pitch: Float, extraction: Float, rotation: Float) {
        val progress = extraction.coerceIn(0f, 1f)
        val recordCenter = .22f + .84f * progress
        val meshes = sleeve + record(recordCenter)
        val hiddenWidth = ((.5f - (recordCenter - .49f)) / .98f * 600f).coerceIn(0f, 600f)
        scene.draw(scope, meshes, yaw, pitch,
            pivotX = (recordCenter - .01f) / 2f, preferredScale = .62f - .20f * progress,
        ) { canvas, material ->
            when (material) {
                MediaDemoMaterial.SleeveCover -> {
                    canvas.drawRect(0f, 0f, 600f, 600f, ink.fill(0xFF2C3825.toInt()))
                    ink.artwork(scope, canvas, cover, 3f, 3f, 594f)
                    canvas.drawRect(2f, 2f, 598f, 598f, ink.stroke(0xFF9DA68D.toInt(), 2f))
                    ink.sheen(canvas, 600f, 600f, yaw, .07f)
                }
                MediaDemoMaterial.SleeveBack -> {
                    canvas.drawRect(0f, 0f, 600f, 600f, ink.fill(0xFF283222.toInt()))
                    ink.text(canvas, "ARMAGEDDON", 40f, 75f, 34f, 0xFFD7DFBA.toInt(), bold = true)
                    ink.text(canvas, "aespa / THE 1ST ALBUM", 42f, 112f, 18f, 0xFFB2BE98.toInt())
                    canvas.drawRect(42f, 151f, 558f, 153f, ink.fill(0xFF73825B.toInt()))
                    ink.text(canvas, "SIDE A", 42f, 205f, 16f, 0xFFB2BE98.toInt())
                    ink.text(canvas, "SIDE B", 320f, 205f, 16f, 0xFFB2BE98.toInt())
                    repeat(5) { index ->
                        val y = 239f + index * 37f
                        canvas.drawRect(42f, y, 226f - index * 13f, y + 3f, ink.fill(0xFF667551.toInt()))
                        canvas.drawRect(320f, y, 517f - index * 17f, y + 3f, ink.fill(0xFF667551.toInt()))
                    }
                    ink.text(canvas, "MEI / RECORD COLLECTION", 42f, 548f, 16f, 0xFFB2BE98.toInt())
                }
                MediaDemoMaterial.SleeveEdge -> {
                    canvas.drawRect(0f, 0f, 600f, 600f, ink.fill(0xFF6F765C.toInt()))
                    canvas.drawRect(0f, 250f, 600f, 350f, ink.fill(0xFF3F4A33.toInt()))
                }
                MediaDemoMaterial.RecordA, MediaDemoMaterial.RecordB ->
                    recordFace(scope, canvas, cover, material == MediaDemoMaterial.RecordB, rotation, yaw, hiddenWidth)
                MediaDemoMaterial.RecordEdge ->
                    canvas.drawRect(0f, 0f, 600f, 600f, ink.fill(0xFF30342F.toInt()))
                else -> Unit
            }
        }
    }

    private fun record(center: Float): List<MediaDemoFace> {
        val r = .49f
        val z = .007f
        fun p(x: Float, y: Float, z: Float) = MediaDemoVertex(center + x, y, z)
        val faces = mutableListOf(
            MediaDemoFace(listOf(p(-r,-r,z), p(r,-r,z), p(r,r,z), p(-r,r,z)), MediaDemoMaterial.RecordA),
            MediaDemoFace(listOf(p(r,-r,-z), p(-r,-r,-z), p(-r,r,-z), p(r,r,-z)), MediaDemoMaterial.RecordB),
        )
        repeat(64) { index ->
            val a = radians(index * 360f / 64)
            val b = radians((index + 1) * 360f / 64)
            var ax = cos(a) * r; var ay = sin(a) * r
            var bx = cos(b) * r; var by = sin(b) * r
            val opening = .5f - center
            // Hide the part inside the sleeve before sorting surfaces by depth. Otherwise a
            // rotated disc's average depth can incorrectly paint it over the sleeve artwork.
            if (ax <= opening && bx <= opening) return@repeat
            if (ax < opening) {
                val fraction = (opening - ax) / (bx - ax)
                ay += (by - ay) * fraction
                ax = opening
            } else if (bx < opening) {
                val fraction = (opening - ax) / (bx - ax)
                by = ay + (by - ay) * fraction
                bx = opening
            }
            faces += MediaDemoFace(listOf(
                p(ax, ay, z), p(ax, ay, -z), p(bx, by, -z), p(bx, by, z),
            ), MediaDemoMaterial.RecordEdge)
        }
        return faces
    }

    private fun recordFace(
        scope: DrawScope, canvas: Canvas, cover: Painter, sideB: Boolean,
        rotation: Float, yaw: Float, hiddenWidth: Float,
    ) {
        canvas.save()
        try {
            if (sideB) canvas.clipRect(0f, 0f, 600f - hiddenWidth, 600f)
            else canvas.clipRect(hiddenWidth, 0f, 600f, 600f)
            // The center hole remains transparent rather than becoming a black printed dot.
            path.reset()
            path.fillType = Path.FillType.EVEN_ODD
            path.addCircle(300f, 300f, 300f, Path.Direction.CW)
            path.addCircle(300f, 300f, 5f, Path.Direction.CW)
            canvas.clipPath(path)
            canvas.drawCircle(300f, 300f, 300f, ink.fill(0xFF101310.toInt()))
            repeat(62) { index ->
                canvas.drawCircle(300f, 300f, 116f + index * 2.8f,
                    ink.stroke(if (index % 7 == 0) 0xFF3C423A.toInt() else 0xFF252C24.toInt(), .8f))
            }
            ink.fill(0xFFFFFFFF.toInt())
            ink.paint.shader = SweepGradient(300f, 300f,
                intArrayOf(0x00111111, 0x486C7960, 0x05111111, 0x00444444, 0x3856634E, 0x00111111), null)
            canvas.save()
            canvas.rotate(yaw * .35f, 300f, 300f)
            canvas.drawCircle(300f, 300f, 299f, ink.paint)
            canvas.restore()
            ink.paint.shader = null
            canvas.save()
            canvas.rotate(if (sideB) -rotation else rotation, 300f, 300f)
            canvas.save()
            path.reset(); path.fillType = Path.FillType.WINDING
            path.addCircle(300f, 300f, 108f, Path.Direction.CW)
            canvas.clipPath(path)
            ink.artwork(scope, canvas, cover, 192f, 192f, 216f)
            canvas.restore()
            canvas.drawCircle(300f, 300f, 108f, ink.stroke(0xFFB5C28D.toInt(), 2f))
            canvas.drawRoundRect(221f, 258f, 379f, 286f, 4f, 4f, ink.fill(0xDB192113.toInt()))
            ink.text(canvas, "ARMAGEDDON", 228f, 278f, 18f, 0xFFE3EBCF.toInt(), bold = true)
            ink.text(canvas, if (sideB) "B" else "A", 290f, 357f, 24f, 0xFFE3EBCF.toInt(), bold = true)
            canvas.restore()
            canvas.drawCircle(300f, 300f, 15f, ink.stroke(0xFF86937A.toInt(), 1.3f))
            canvas.drawCircle(300f, 300f, 298.5f, ink.stroke(0xFF65705C.toInt(), 1.5f))
        } finally { canvas.restore() }
    }
}
