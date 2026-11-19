package com.ljyh.mei.demo.album

import android.graphics.Canvas
import android.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.cos
import kotlin.math.sin

/** A portrait hinged case presents its J-card before revealing the removable cassette. */
internal class CassetteDemoRenderer {
    private val scene = MediaDemoScene()
    private val ink = MediaDemoInk()
    private val path = Path()
    private val body = mediaDemoBox(.79f, .5f, .055f,
        MediaDemoMaterial.CassetteA, MediaDemoMaterial.CassetteB, MediaDemoMaterial.CassetteEdge, 960f)
    private val lid = mediaDemoBox(.55f, .86f, .008f,
        MediaDemoMaterial.CassetteCover, MediaDemoMaterial.CassetteInsert, MediaDemoMaterial.CassetteCaseEdge, 384f)
    private val tray = buildList {
        // The tray is open at the front: a backing plate plus four retaining walls.
        addAll(translated(mediaDemoBox(.55f, .86f, .015f,
            MediaDemoMaterial.CassetteTray, MediaDemoMaterial.CassetteBack, MediaDemoMaterial.CassetteCaseEdge, 384f), z = -.105f))
        fun wall(x: Float, y: Float, halfWidth: Float, halfHeight: Float) {
            addAll(translated(mediaDemoBox(halfWidth, halfHeight, .105f,
                MediaDemoMaterial.CassetteCaseEdge, MediaDemoMaterial.CassetteCaseEdge,
                MediaDemoMaterial.CassetteCaseEdge, 384f), x = x, y = y, z = CassetteCaseRimZ - .105f))
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
                MediaDemoVertex(-.55f + x * cos(hinge) - point.z * sin(hinge), point.y,
                    .115f + x * sin(hinge) + point.z * cos(hinge))
            })
        }
        val tapeFaces = body.map { face ->
            face.copy(vertices = face.vertices.map { point ->
                MediaDemoVertex(
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
            when (material) {
                MediaDemoMaterial.CassetteA, MediaDemoMaterial.CassetteB ->
                    cassetteFace(scope, canvas, cover, material == MediaDemoMaterial.CassetteB, reelRotation, yaw)
                MediaDemoMaterial.CassetteEdge -> {
                    canvas.drawRect(0f, 0f, 960f, 600f, ink.fill(0xFF625240.toInt()))
                    canvas.drawRect(0f, 255f, 960f, 345f, ink.fill(0xFF27251F.toInt()))
                    ink.sheen(canvas, 960f, 600f, yaw, .22f)
                }
                MediaDemoMaterial.CassetteCover -> packageCover(scope, canvas, cover, yaw)
                MediaDemoMaterial.CassetteInsert -> packageInsert(canvas)
                MediaDemoMaterial.CassetteTray -> packageTray(canvas)
                MediaDemoMaterial.CassetteBack -> packageBack(canvas)
                MediaDemoMaterial.CassetteCaseEdge -> {
                    canvas.drawRect(0f, 0f, 384f, 600f, ink.fill(0xFF444D41.toInt()))
                    canvas.drawRect(4f, 4f, 380f, 596f, ink.stroke(0xFFA2AB99.toInt(), 4f))
                    canvas.drawRect(12f, 12f, 372f, 588f, ink.stroke(0xFF697660.toInt(), 2f))
                    ink.sheen(canvas, 384f, 600f, yaw, .30f)
                }
                else -> Unit
            }
        }
    }

    private fun translated(mesh: List<MediaDemoFace>, x: Float = 0f, y: Float = 0f, z: Float = 0f) =
        mesh.map { face -> face.copy(vertices = face.vertices.map { point ->
            MediaDemoVertex(point.x + x, point.y + y, point.z + z)
        }) }

    private fun packageCover(scope: DrawScope, canvas: Canvas, cover: Painter, yaw: Float) {
        canvas.drawRect(0f, 0f, 384f, 600f, ink.fill(0xFF414A38.toInt()))
        // Texture proportions match the portrait panel, keeping the square artwork square.
        canvas.drawRect(13f, 13f, 371f, 587f, ink.fill(0xFFD5C9AA.toInt()))
        ink.artwork(scope, canvas, cover, 22f, 22f, 340f)
        ink.text(canvas, "ARMAGEDDON", 30f, 409f, 26f, 0xFF293722.toInt(), bold = true)
        ink.text(canvas, "aespa", 30f, 443f, 24f, 0xFF4B583E.toInt())
        canvas.drawRect(30f, 469f, 354f, 471f, ink.fill(0xFF9EA780.toInt()))
        ink.text(canvas, "THE 1ST ALBUM", 30f, 506f, 16f, 0xFF4B583E.toInt())
        ink.text(canvas, "CASSETTE   /   A + B", 30f, 551f, 14f, 0xFF4B583E.toInt())
        // Plastic lip, molded spine and catch surround the printed card.
        canvas.drawRect(0f, 0f, 384f, 600f, ink.stroke(0xFFC4CFBC.toInt(), 4f))
        canvas.drawLine(10f, 12f, 10f, 588f, ink.stroke(0xFF9EAE91.toInt(), 3f))
        canvas.drawRoundRect(370f, 270f, 383f, 330f, 4f, 4f, ink.fill(0xFF9CA98D.toInt()))
        ink.sheen(canvas, 384f, 600f, yaw, .15f)
    }

    private fun packageInsert(canvas: Canvas) {
        canvas.drawRect(0f, 0f, 384f, 600f, ink.fill(0xFFCFC4A8.toInt()))
        canvas.drawRect(17f, 17f, 367f, 583f, ink.stroke(0xFF78856A.toInt(), 1.5f))
        ink.text(canvas, "aespa", 36f, 73f, 28f, 0xFF34412B.toInt(), bold = true)
        ink.text(canvas, "ARMAGEDDON", 36f, 112f, 23f, 0xFF34412B.toInt(), bold = true)
        ink.text(canvas, "THE 1ST ALBUM", 36f, 142f, 13f, 0xFF617051.toInt())
        canvas.drawRect(36f, 175f, 348f, 177f, ink.fill(0xFF879472.toInt()))
        ink.text(canvas, "SIDE A", 36f, 224f, 16f, 0xFF34412B.toInt(), bold = true)
        ink.text(canvas, "SIDE B", 36f, 375f, 16f, 0xFF34412B.toInt(), bold = true)
        repeat(3) { index ->
            val y = 250f + index * 24f
            canvas.drawRect(36f, y, 318f - index * 35f, y + 2f, ink.fill(0xFF97A180.toInt()))
            canvas.drawRect(36f, y + 151f, 304f - index * 29f, y + 153f, ink.fill(0xFF97A180.toInt()))
        }
        ink.text(canvas, "MEI / TAPE COLLECTION", 36f, 550f, 13f, 0xFF617051.toInt())
    }

    private fun packageTray(canvas: Canvas) {
        canvas.drawRect(0f, 0f, 384f, 600f, ink.fill(0xFF252F23.toInt()))
        canvas.drawRoundRect(23f, 28f, 361f, 572f, 18f, 18f, ink.stroke(0xFF69795B.toInt(), 3f))
        canvas.drawRoundRect(40f, 44f, 344f, 556f, 13f, 13f, ink.fill(0xFF1B251A.toInt()))
        listOf(199f, 401f).forEach { y ->
            canvas.drawCircle(200f, y, 28f, ink.fill(0xFF101A11.toInt()))
            canvas.drawCircle(200f, y, 22f, ink.fill(0xFF89947B.toInt()))
            canvas.drawCircle(200f, y, 12f, ink.fill(0xFF404E37.toInt()))
            canvas.drawCircle(200f, y, 7f, ink.fill(0xFFAEB99E.toInt()))
        }
        listOf(59f, 325f).forEach { x ->
            canvas.drawRoundRect(x, 80f, x + 9f, 520f, 4f, 4f, ink.fill(0xFF45553A.toInt()))
        }
        ink.text(canvas, "CASSETTE", 130f, 544f, 17f, 0xFF8A9979.toInt())
    }

    private fun packageBack(canvas: Canvas) {
        canvas.drawRect(0f, 0f, 384f, 600f, ink.fill(0xFF35402F.toInt()))
        ink.text(canvas, "ARMAGEDDON", 31f, 70f, 24f, 0xFFD5DFBF.toInt(), bold = true)
        ink.text(canvas, "aespa / CASSETTE", 31f, 105f, 15f, 0xFFB2C09B.toInt())
        canvas.drawRect(31f, 135f, 353f, 137f, ink.fill(0xFF899976.toInt()))
        ink.text(canvas, "SIDE A + SIDE B", 31f, 190f, 16f, 0xFFB2C09B.toInt())
        repeat(5) { index ->
            val y = 227f + index * 34f
            canvas.drawRect(31f, y, 321f - index * 27f, y + 3f, ink.fill(0xFF768761.toInt()))
        }
        ink.text(canvas, "MEI / TAPE COLLECTION", 31f, 549f, 13f, 0xFFB2C09B.toInt())
        canvas.drawRect(4f, 4f, 380f, 596f, ink.stroke(0xFFB8C3A7.toInt(), 3f))
    }

    private fun cassetteFace(scope: DrawScope, canvas: Canvas, cover: Painter, sideB: Boolean, rotation: Float, yaw: Float) {
        canvas.drawRoundRect(0f, 0f, 960f, 600f, 30f, 30f, ink.fill(0xFF2A2821.toInt()))
        canvas.drawRoundRect(9f, 9f, 951f, 591f, 24f, 24f, ink.stroke(0xFF95806A.toInt(), 3f))
        canvas.drawRoundRect(29f, 27f, 931f, 573f, 18f, 18f, ink.stroke(0xFF504A3C.toInt(), 2f))
        // A warm paper label wraps the smoky plastic shell, leaving its transport exposed.
        canvas.drawRoundRect(74f, 48f, 886f, 401f, 17f, 17f, ink.fill(0xFFD5C7A4.toInt()))
        ink.artwork(scope, canvas, cover, 98f, 68f, 102f)
        ink.text(canvas, "ARMAGEDDON", 224f, 112f, 35f, 0xFF26321F.toInt(), bold = true)
        ink.text(canvas, "aespa   /   THE 1ST ALBUM", 225f, 150f, 18f, 0xFF49513A.toInt())
        ink.text(canvas, if (sideB) "B" else "A", 807f, 112f, 44f, 0xFF26321F.toInt(), bold = true)
        canvas.drawRect(98f, 182f, 862f, 185f, ink.fill(0xFF8F9871.toInt()))
        canvas.drawRoundRect(145f, 207f, 815f, 365f, 66f, 66f, ink.fill(0xFF151A13.toInt()))
        canvas.drawRoundRect(152f, 214f, 808f, 358f, 61f, 61f, ink.stroke(0xFF7B7E65.toInt(), 2f))
        // Tape packs differ in size; the hubs turn at different rates as tape winds across.
        reel(canvas, 291f, 286f, if (sideB) 70f else 58f, rotation, sideB)
        reel(canvas, 669f, 286f, if (sideB) 58f else 70f, rotation * 1.24f, sideB)
        canvas.drawLine(291f, 215f, 669f, 215f, ink.stroke(0xFF7A5835.toInt(), 3f))
        canvas.drawRoundRect(402f, 249f, 558f, 323f, 8f, 8f, ink.fill(0xFF333A25.toInt()))
        repeat(7) { index ->
            val x = 420f + index * 20f
            canvas.drawLine(x, 274f, x, if (index % 3 == 0) 300f else 290f, ink.stroke(0xFFA4AF8A.toInt(), 2f))
        }
        ink.text(canvas, "STEREO", 102f, 389f, 15f, 0xFF49513A.toInt())
        ink.text(canvas, "MEI / CASSETTE", 678f, 389f, 15f, 0xFF49513A.toInt())
        // Recessed head opening, capstan holes and guide rollers make the silhouette recognizable.
        path.reset()
        path.moveTo(227f, 433f); path.lineTo(733f, 433f)
        path.lineTo(790f, 575f); path.lineTo(170f, 575f); path.close()
        canvas.drawPath(path, ink.fill(0xFF39372C.toInt()))
        canvas.drawPath(path, ink.stroke(0xFF766C57.toInt(), 2f))
        canvas.drawRoundRect(386f, 508f, 574f, 561f, 7f, 7f, ink.fill(0xFF10150F.toInt()))
        canvas.drawRect(421f, 519f, 539f, 544f, ink.fill(0xFF675F4D.toInt()))
        listOf(272f, 688f).forEach { x ->
            canvas.drawCircle(x, 519f, 20f, ink.fill(0xFF10150F.toInt()))
            canvas.drawCircle(x, 519f, 9f, ink.fill(0xFF8F9687.toInt()))
        }
        listOf(341f, 619f).forEach { x ->
            canvas.drawCircle(x, 536f, 9f, ink.fill(0xFF11140E.toInt()))
        }
        listOf(50f to 48f, 910f to 48f, 52f to 551f, 908f to 551f).forEach { (x, y) ->
            canvas.drawCircle(x, y, 10f, ink.fill(0xFFAEAB98.toInt()))
            canvas.drawLine(x - 5f, y - 3f, x + 5f, y + 3f, ink.stroke(0xFF4F5146.toInt(), 2f))
        }
        ink.sheen(canvas, 960f, 600f, yaw, .12f)
    }

    private fun reel(canvas: Canvas, x: Float, y: Float, radius: Float, rotation: Float, sideB: Boolean) {
        canvas.drawCircle(x, y, radius, ink.fill(0xFF644832.toInt()))
        repeat(5) { index ->
            canvas.drawCircle(x, y, radius - index * 5f, ink.stroke(0xFF302A1C.toInt(), 1.2f))
        }
        canvas.drawCircle(x, y, 37f, ink.fill(0xFFCECCB6.toInt()))
        canvas.drawCircle(x, y, 28f, ink.fill(0xFF272D21.toInt()))
        repeat(6) { index ->
            val angle = radians(index * 60f + if (sideB) -rotation else rotation)
            val cx = x + cos(angle) * 28f
            val cy = y + sin(angle) * 28f
            canvas.drawCircle(cx, cy, 6.5f, ink.fill(0xFFCECCB6.toInt()))
        }
        canvas.drawCircle(x, y, 15f, ink.fill(0xFF10170F.toInt()))
    }
}
