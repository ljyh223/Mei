package com.ljyh.mei.ui.component.artwork.album

import android.graphics.Canvas
import android.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.cos
import kotlin.math.sin

/** A portrait hinged case presents its J-card before revealing the removable cassette. */
internal class AlbumCassetteRenderer(private val label: AlbumPackageLabel) {
    private val scene = AlbumPackageScene()
    private val ink = AlbumPackageInk()
    private val path = Path()
    private val body = albumPackageBox(.79f, .5f, .055f,
        AlbumPackageMaterial.CassetteA, AlbumPackageMaterial.CassetteB, AlbumPackageMaterial.CassetteEdge, 960f)
    private val lid = albumPackageBox(.55f, .86f, .008f,
        AlbumPackageMaterial.CassetteCover, AlbumPackageMaterial.CassetteInsert, AlbumPackageMaterial.CassetteCaseEdge, 384f)
    private val tray = buildList {
        // The tray is open at the front: a backing plate plus four retaining walls.
        addAll(translated(albumPackageBox(.55f, .86f, .015f,
            AlbumPackageMaterial.CassetteTray, AlbumPackageMaterial.CassetteBack, AlbumPackageMaterial.CassetteCaseEdge, 384f), z = -.105f))
        fun wall(x: Float, y: Float, halfWidth: Float, halfHeight: Float) {
            addAll(translated(albumPackageBox(halfWidth, halfHeight, .105f,
                AlbumPackageMaterial.CassetteCaseEdge, AlbumPackageMaterial.CassetteCaseEdge,
                AlbumPackageMaterial.CassetteCaseEdge, 384f), x = x, y = y, z = CassetteCaseRimZ - .105f))
        }
        wall(-.53f, 0f, .02f, .86f)
        wall(.53f, 0f, .02f, .86f)
        wall(0f, -.84f, .51f, .02f)
        wall(0f, .84f, .51f, .02f)
        // Two visible knuckles mark the hinge rather than making the cover float.
        wall(-.565f, -.64f, .025f, .09f)
        wall(-.565f, .64f, .025f, .09f)
    }

    fun draw(scope: DrawScope, cover: Painter, yaw: Float, pitch: Float, progress: Float, reelRotation: Float) {
        val pose = cassettePackagePose(progress)
        val hinge = radians(pose.lidAngle)
        val tapeAngle = radians(pose.tapeAngle)
        val coverFaces = lid.map { face ->
            face.copy(vertices = face.vertices.map { point ->
                val x = point.x + .55f
                AlbumPackageVertex(-.55f + x * cos(hinge) - point.z * sin(hinge), point.y,
                    .115f + x * sin(hinge) + point.z * cos(hinge))
            })
        }
        val tapeFaces = body.map { face ->
            face.copy(vertices = face.vertices.map { point ->
                AlbumPackageVertex(
                    pose.tapeX + CassetteBodyScale * (point.x * cos(tapeAngle) - point.y * sin(tapeAngle)),
                    CassetteBodyScale * (point.x * sin(tapeAngle) + point.y * cos(tapeAngle)),
                    pose.tapeZ + point.z * CassetteBodyScale,
                )
            })
        }
        val faces = tray + coverFaces + tapeFaces
        val vertices = faces.flatMap { it.vertices }
        val center = (vertices.minOf { it.x } + vertices.maxOf { it.x }) / 2f
        scene.draw(scope, faces, yaw, pitch, pivotX = center, preferredScale = .62f) { canvas, material ->
            ink.textureWidth = when (material) {
                AlbumPackageMaterial.CassetteA, AlbumPackageMaterial.CassetteB, AlbumPackageMaterial.CassetteEdge -> 960f
                else -> 384f
            }
            when (material) {
                AlbumPackageMaterial.CassetteA, AlbumPackageMaterial.CassetteB ->
                    cassetteFace(scope, canvas, cover, material == AlbumPackageMaterial.CassetteB, reelRotation, yaw)
                AlbumPackageMaterial.CassetteEdge -> {
                    canvas.drawRect(0f, 0f, 960f, 600f, ink.fill(0xFF545454.toInt()))
                    canvas.drawRect(0f, 255f, 960f, 345f, ink.fill(0xFF252525.toInt()))
                    ink.sheen(canvas, 960f, 600f, yaw, .22f)
                }
                AlbumPackageMaterial.CassetteCover -> packageCover(scope, canvas, cover, yaw)
                AlbumPackageMaterial.CassetteInsert -> packageInsert(canvas)
                AlbumPackageMaterial.CassetteTray -> packageTray(canvas)
                AlbumPackageMaterial.CassetteBack -> packageBack(canvas)
                AlbumPackageMaterial.CassetteCaseEdge -> {
                    canvas.drawRect(0f, 0f, 384f, 600f, ink.fill(0xFF4A4A4A.toInt()))
                    canvas.drawRect(4f, 4f, 380f, 596f, ink.stroke(0xFFA8A8A8.toInt(), 4f))
                    canvas.drawRect(12f, 12f, 372f, 588f, ink.stroke(0xFF727272.toInt(), 2f))
                    ink.sheen(canvas, 384f, 600f, yaw, .30f)
                }
                else -> Unit
            }
        }
    }

    private fun translated(mesh: List<AlbumPackageFace>, x: Float = 0f, y: Float = 0f, z: Float = 0f) =
        mesh.map { face -> face.copy(vertices = face.vertices.map { point ->
            AlbumPackageVertex(point.x + x, point.y + y, point.z + z)
        }) }

    private fun packageCover(scope: DrawScope, canvas: Canvas, cover: Painter, yaw: Float) {
        canvas.drawRect(0f, 0f, 384f, 600f, ink.fill(AlbumPackageColors.shell))
        // Texture proportions match the portrait panel, keeping the square artwork square.
        canvas.drawRect(13f, 13f, 371f, 587f, ink.fill(AlbumPackageColors.paper))
        ink.artwork(scope, canvas, cover, 22f, 22f, 340f)
        ink.text(canvas, label.title, 30f, 409f, 26f, AlbumPackageColors.text, bold = true)
        ink.text(canvas, label.artist, 30f, 443f, 24f, AlbumPackageColors.secondary)
        canvas.drawRect(30f, 469f, 354f, 471f, ink.fill(0xFFA2A2A2.toInt()))
        ink.text(canvas, label.year?.toString().orEmpty(), 30f, 506f, 16f, AlbumPackageColors.secondary)
        ink.text(canvas, "CASSETTE   /   A + B", 30f, 551f, 14f, AlbumPackageColors.secondary)
        // Plastic lip, molded spine and catch surround the printed card.
        canvas.drawRect(0f, 0f, 384f, 600f, ink.stroke(0xFFCBCBCB.toInt(), 4f))
        canvas.drawLine(10f, 12f, 10f, 588f, ink.stroke(0xFFA9A9A9.toInt(), 3f))
        canvas.drawRoundRect(370f, 270f, 383f, 330f, 4f, 4f, ink.fill(0xFFA4A4A4.toInt()))
        ink.sheen(canvas, 384f, 600f, yaw, .15f)
    }

    private fun packageInsert(canvas: Canvas) = ink.liner(canvas, label, 384f)

    private fun packageTray(canvas: Canvas) {
        canvas.drawRect(0f, 0f, 384f, 600f, ink.fill(0xFF2C2C2C.toInt()))
        canvas.drawRoundRect(23f, 28f, 361f, 572f, 18f, 18f, ink.stroke(0xFF737373.toInt(), 3f))
        canvas.drawRoundRect(40f, 44f, 344f, 556f, 13f, 13f, ink.fill(0xFF222222.toInt()))
        listOf(199f, 401f).forEach { y ->
            canvas.drawCircle(200f, y, 28f, ink.fill(0xFF171717.toInt()))
            canvas.drawCircle(200f, y, 22f, ink.fill(0xFF909090.toInt()))
            canvas.drawCircle(200f, y, 12f, ink.fill(0xFF494949.toInt()))
            canvas.drawCircle(200f, y, 7f, ink.fill(0xFFB5B5B5.toInt()))
        }
        listOf(59f, 325f).forEach { x ->
            canvas.drawRoundRect(x, 80f, x + 9f, 520f, 4f, 4f, ink.fill(0xFF505050.toInt()))
        }
        ink.text(canvas, "CASSETTE", 130f, 544f, 17f, 0xFF949494.toInt())
    }

    private fun packageBack(canvas: Canvas) = ink.liner(canvas, label, 384f)

    private fun cassetteFace(scope: DrawScope, canvas: Canvas, cover: Painter, sideB: Boolean, rotation: Float, yaw: Float) {
        canvas.drawRoundRect(0f, 0f, 960f, 600f, 30f, 30f, ink.fill(0xFF282828.toInt()))
        canvas.drawRoundRect(9f, 9f, 951f, 591f, 24f, 24f, ink.stroke(0xFF838383.toInt(), 3f))
        canvas.drawRoundRect(29f, 27f, 931f, 573f, 18f, 18f, ink.stroke(0xFF4A4A4A.toInt(), 2f))
        // A warm paper label wraps the smoky plastic shell, leaving its transport exposed.
        canvas.drawRoundRect(74f, 48f, 886f, 401f, 17f, 17f, ink.fill(AlbumPackageColors.paper))
        ink.artwork(scope, canvas, cover, 98f, 68f, 102f)
        ink.text(canvas, label.title, 224f, 112f, 35f, AlbumPackageColors.text, bold = true, maxWidth = 550f)
        ink.text(canvas, label.artist, 225f, 150f, 18f, AlbumPackageColors.secondary, maxWidth = 550f)
        ink.text(canvas, if (sideB) "B" else "A", 807f, 112f, 44f, 0xFF2E2E2E.toInt(), bold = true)
        canvas.drawRect(98f, 182f, 862f, 185f, ink.fill(0xFF939393.toInt()))
        canvas.drawRoundRect(145f, 207f, 815f, 365f, 66f, 66f, ink.fill(0xFF181818.toInt()))
        canvas.drawRoundRect(152f, 214f, 808f, 358f, 61f, 61f, ink.stroke(0xFF7C7C7C.toInt(), 2f))
        // Tape packs differ in size; the hubs turn at different rates as tape winds across.
        reel(canvas, 291f, 286f, if (sideB) 70f else 58f, rotation, sideB)
        reel(canvas, 669f, 286f, if (sideB) 58f else 70f, rotation * 1.24f, sideB)
        canvas.drawLine(291f, 215f, 669f, 215f, ink.stroke(0xFF5D5D5D.toInt(), 3f))
        canvas.drawRoundRect(402f, 249f, 558f, 323f, 8f, 8f, ink.fill(0xFF373737.toInt()))
        repeat(7) { index ->
            val x = 420f + index * 20f
            canvas.drawLine(x, 274f, x, if (index % 3 == 0) 300f else 290f, ink.stroke(0xFFAAAAAA.toInt(), 2f))
        }
        ink.text(canvas, "STEREO", 102f, 389f, 15f, 0xFF4E4E4E.toInt())
        ink.text(canvas, "MEI / CASSETTE", 678f, 389f, 15f, 0xFF4E4E4E.toInt())
        // Recessed head opening, capstan holes and guide rollers make the silhouette recognizable.
        path.reset()
        path.moveTo(227f, 433f); path.lineTo(733f, 433f)
        path.lineTo(790f, 575f); path.lineTo(170f, 575f); path.close()
        canvas.drawPath(path, ink.fill(0xFF373737.toInt()))
        canvas.drawPath(path, ink.stroke(0xFF6D6D6D.toInt(), 2f))
        canvas.drawRoundRect(386f, 508f, 574f, 561f, 7f, 7f, ink.fill(0xFF141414.toInt()))
        canvas.drawRect(421f, 519f, 539f, 544f, ink.fill(0xFF5F5F5F.toInt()))
        listOf(272f, 688f).forEach { x ->
            canvas.drawCircle(x, 519f, 20f, ink.fill(0xFF141414.toInt()))
            canvas.drawCircle(x, 519f, 9f, ink.fill(0xFF939393.toInt()))
        }
        listOf(341f, 619f).forEach { x ->
            canvas.drawCircle(x, 536f, 9f, ink.fill(0xFF131313.toInt()))
        }
        listOf(50f to 48f, 910f to 48f, 52f to 551f, 908f to 551f).forEach { (x, y) ->
            canvas.drawCircle(x, y, 10f, ink.fill(0xFFAAAAAA.toInt()))
            canvas.drawLine(x - 5f, y - 3f, x + 5f, y + 3f, ink.stroke(0xFF505050.toInt(), 2f))
        }
        ink.sheen(canvas, 960f, 600f, yaw, .12f)
    }

    private fun reel(canvas: Canvas, x: Float, y: Float, radius: Float, rotation: Float, sideB: Boolean) {
        canvas.drawCircle(x, y, radius, ink.fill(0xFF4C4C4C.toInt()))
        repeat(5) { index ->
            canvas.drawCircle(x, y, radius - index * 5f, ink.stroke(0xFF2A2A2A.toInt(), 1.2f))
        }
        canvas.drawCircle(x, y, 37f, ink.fill(0xFFCBCBCB.toInt()))
        canvas.drawCircle(x, y, 28f, ink.fill(0xFF2B2B2B.toInt()))
        repeat(6) { index ->
            val angle = radians(index * 60f + if (sideB) -rotation else rotation)
            val cx = x + cos(angle) * 28f
            val cy = y + sin(angle) * 28f
            canvas.drawCircle(cx, cy, 6.5f, ink.fill(0xFFCBCBCB.toInt()))
        }
        canvas.drawCircle(x, y, 15f, ink.fill(0xFF151515.toInt()))
    }
}
