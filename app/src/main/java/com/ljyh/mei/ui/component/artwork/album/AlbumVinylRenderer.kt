package com.ljyh.mei.ui.component.artwork.album

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.SweepGradient
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.cos
import kotlin.math.sin

/** A record slides through a paper sleeve; both label faces and the circular rim have depth. */
internal class AlbumVinylRenderer(private val label: AlbumPackageLabel) {
    private val scene = AlbumPackageScene()
    private val ink = AlbumPackageInk()
    private val path = Path()
    private val recordSheen = SweepGradient(300f, 300f,
        intArrayOf(0x00FFFFFF, 0x02FFFFFF, 0x32ECE9E2, 0x06FFFFFF, 0x00FFFFFF,
            0x00FFFFFF, 0x24C8CBC9, 0x04FFFFFF, 0x00FFFFFF, 0x00FFFFFF),
        floatArrayOf(0f, .12f, .22f, .30f, .40f, .54f, .65f, .73f, .85f, 1f))
    private val sleeve = albumPackageBox(.5f, .5f, .025f,
        AlbumPackageMaterial.SleeveCover, AlbumPackageMaterial.SleeveBack, AlbumPackageMaterial.SleeveEdge)

    fun draw(scope: DrawScope, cover: Painter, yaw: Float, pitch: Float, extraction: Float, rotation: Float) {
        val progress = extraction.coerceIn(0f, 1f)
        val recordCenter = .22f + .84f * progress
        val meshes = sleeve + record(recordCenter)
        val hiddenWidth = ((.5f - (recordCenter - .49f)) / .98f * 600f).coerceIn(0f, 600f)
        scene.draw(scope, meshes, yaw, pitch,
            pivotX = (recordCenter - .01f) / 2f, preferredScale = .62f - .20f * progress,
        ) { canvas, material ->
            when (material) {
                AlbumPackageMaterial.SleeveCover -> {
                    canvas.drawRect(0f, 0f, 600f, 600f, ink.fill(0xFF343434.toInt()))
                    ink.artwork(scope, canvas, cover, 3f, 3f, 594f)
                    ink.paperGrain(canvas, 600f)
                    canvas.drawLine(6f, 5f, 594f, 5f, ink.stroke(0x44F2EDDF, 1.2f))
                    canvas.drawLine(5f, 6f, 5f, 594f, ink.stroke(0x2DF2EDDF, 1f))
                    canvas.drawRect(2f, 2f, 598f, 598f, ink.stroke(0xFFA2A2A2.toInt(), 2f))
                    ink.sheen(canvas, 600f, 600f, yaw, .07f)
                }
                AlbumPackageMaterial.SleeveBack -> ink.liner(canvas, label, 600f)
                AlbumPackageMaterial.SleeveEdge -> {
                    canvas.drawRect(0f, 0f, 600f, 600f, ink.fill(0xFFC9C6BF.toInt()))
                    canvas.drawRect(0f, 250f, 600f, 350f, ink.fill(0xFF9C9992.toInt()))
                }
                AlbumPackageMaterial.RecordA, AlbumPackageMaterial.RecordB ->
                    recordFace(scope, canvas, cover, material == AlbumPackageMaterial.RecordB, rotation, yaw, hiddenWidth)
                AlbumPackageMaterial.RecordEdge ->
                    canvas.drawRect(0f, 0f, 600f, 600f, ink.fill(0xFF333333.toInt()))
                else -> Unit
            }
        }
    }

    private fun record(center: Float): List<AlbumPackageFace> {
        val r = .49f
        val z = .007f
        fun p(x: Float, y: Float, z: Float) = AlbumPackageVertex(center + x, y, z)
        val faces = mutableListOf(
            AlbumPackageFace(listOf(p(-r,-r,z), p(r,-r,z), p(r,r,z), p(-r,r,z)), AlbumPackageMaterial.RecordA),
            AlbumPackageFace(listOf(p(r,-r,-z), p(-r,-r,-z), p(-r,r,-z), p(r,r,-z)), AlbumPackageMaterial.RecordB),
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
            faces += AlbumPackageFace(listOf(
                p(ax, ay, z), p(ax, ay, -z), p(bx, by, -z), p(bx, by, z),
            ), AlbumPackageMaterial.RecordEdge)
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
            canvas.drawCircle(300f, 300f, 300f, ink.fill(0xFF090A0B.toInt()))
            repeat(120) { index ->
                val radius = 116f + index * 1.50f + sin(index * .81f) * .28f
                canvas.drawCircle(300f, 300f, radius,
                    ink.stroke(if (index % 19 == 0) 0x24C4C0B8 else if (index % 5 == 0) 0x16C4C0B8 else 0x0BC4C0B8, .65f))
            }
            canvas.drawCircle(300f, 300f, 112f, ink.stroke(0x20C4C0B8, 1.8f))
            ink.fill(0xFFFFFFFF.toInt())
            ink.paint.shader = recordSheen
            canvas.save()
            canvas.rotate(yaw * .35f, 300f, 300f)
            canvas.drawCircle(300f, 300f, 299f, ink.paint)
            canvas.restore()
            ink.paint.shader = null
            canvas.save()
            canvas.rotate(if (sideB) -rotation else rotation, 300f, 300f)
            canvas.save()
            canvas.drawCircle(300f, 300f, 108f, ink.fill(AlbumPackageColors.paper))
            canvas.save()
            path.reset()
            path.addCircle(300f, 300f, 108f, Path.Direction.CW)
            canvas.clipPath(path)
            ink.paperGrain(canvas, 600f)
            canvas.restore()
            path.reset(); path.fillType = Path.FillType.WINDING
            path.addCircle(300f, 264f, 56f, Path.Direction.CW)
            canvas.clipPath(path)
            ink.artwork(scope, canvas, cover, 244f, 208f, 112f)
            canvas.restore()
            canvas.drawCircle(300f, 300f, 108f, ink.stroke(AlbumPackageColors.edge, 2f))
            ink.text(canvas, label.title, 210f, 352f, 16f, AlbumPackageColors.text, bold = true, maxWidth = 180f)
            ink.text(canvas, if (sideB) "B / STEREO" else "A / STEREO", 255f, 382f, 12f,
                AlbumPackageColors.secondary, maxWidth = 100f)
            canvas.restore()
            canvas.drawCircle(300f, 300f, 15f, ink.stroke(0xFF8E8E8E.toInt(), 1.3f))
            canvas.drawCircle(300f, 300f, 298.5f, ink.stroke(0x44C0BEB8, 1.2f))
        } finally { canvas.restore() }
    }
}
