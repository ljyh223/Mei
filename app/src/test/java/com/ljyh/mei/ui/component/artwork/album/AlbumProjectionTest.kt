package com.ljyh.mei.ui.component.artwork.album

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.*
import org.junit.Test

class AlbumProjectionTest {
    private fun screen(camera: AlbumCamera, point: AlbumPackageVertex) = albumPerspectivePoint(camera.view(point)) * 200f
    private fun assertPoint(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, .0001f)
        assertEquals(expected.y, actual.y, .0001f)
    }

    @Test fun reflectionPreservesDistancesAndReturnsToTheSamePointWhenAppliedTwice() {
        val ground = .5f
        for (point in listOf(AlbumPackageVertex(-.5f, -.5f, .04f), AlbumPackageVertex(1.2f, .2f, -.12f))) {
            val reflected = albumReflectedVertex(point, ground)
            assertEquals(point.x, reflected.x, 0f)
            assertEquals(point.z, reflected.z, 0f)
            assertEquals(ground - point.y, reflected.y - ground, .000001f)
            val restored = albumReflectedVertex(reflected, ground)
            assertEquals(point.y, restored.y, .000001f)
        }
    }

    @Test fun everyGroundContactPointStaysJoinedAcrossDifferentDepthsAndCameraAngles() {
        val contacts = listOf(AlbumPackageVertex(-1.2f, .5f, .7f),
            AlbumPackageVertex(-.5f, .5f, .04f), AlbumPackageVertex(.5f, .5f, -.065f))
        for (yaw in listOf(-75f, -36f, 0f, 42f, 170f)) {
            for (pitch in listOf(-30f, -8f, 0f, 20f)) {
                val camera = AlbumCamera(yaw, pitch, -.3f)
                contacts.forEach { assertPoint(screen(camera, it), screen(camera, albumReflectedVertex(it, .5f))) }
            }
        }
        val camera = AlbumCamera(42f, -8f)
        assertTrue("The contact edge is not generally a horizontal screen line",
            abs(screen(camera, contacts.first()).y - screen(camera, contacts.last()).y) > 1f)
    }

    @Test fun aFrontFacingCircularDiscKeepsItsRadiusInTheMirrorWithoutVerticalCompression() {
        val camera = AlbumCamera(0f, 0f)
        val centre = AlbumPackageVertex(0f, 0f, .007f)
        val originalCentre = screen(camera, centre)
        val mirrorCentre = screen(camera, albumReflectedVertex(centre, .5f))
        for (index in 0 until 64) {
            val angle = index * 2.0 * PI / 64
            val point = AlbumPackageVertex(cos(angle).toFloat() * .5f, sin(angle).toFloat() * .5f, .007f)
            val originalRadius = (screen(camera, point) - originalCentre).getDistance()
            val reflectedRadius = (screen(camera, albumReflectedVertex(point, .5f)) - mirrorCentre).getDistance()
            assertEquals(originalRadius, reflectedRadius, .0001f)
        }
    }

    @Test fun reflectedWindingKeepsTheMatchingFrontTextureAndRejectsTheReverseFace() {
        val camera = AlbumCamera(0f, -8f)
        val front = listOf(AlbumPackageVertex(-.5f, -.5f, .05f), AlbumPackageVertex(.5f, -.5f, .05f),
            AlbumPackageVertex(.5f, .5f, .05f), AlbumPackageVertex(-.5f, .5f, .05f))
        val original = front.map { screen(camera, it) }
        val reflected = front.map { screen(camera, albumReflectedVertex(it, .5f)) }
        assertTrue(albumFaceIsVisible(original, reflected = false))
        assertTrue(albumFaceIsVisible(reflected, reflected = true))
        assertFalse(albumFaceIsVisible(reflected, reflected = false))
        assertFalse(albumFaceIsVisible(reflected.reversed(), reflected = true))
    }

    @Test fun mirrorCanSeeTheUndersideThatIsHiddenInTheNormalPass() {
        val camera = AlbumCamera(0f, -12f)
        val faces = albumPackageBox(.5f, .5f, .05f,
            AlbumPackageMaterial.SleeveCover, AlbumPackageMaterial.SleeveBack, AlbumPackageMaterial.SleeveEdge)
        fun visible(index: Int, reflected: Boolean) = albumFaceIsVisible(faces[index].vertices.map {
            screen(camera, if (reflected) albumReflectedVertex(it, .5f) else it)
        }, reflected)
        assertTrue(visible(4, false)) // top
        assertFalse(visible(5, false)) // underside
        assertFalse(visible(4, true))
        assertTrue(visible(5, true))
    }

    @Test fun reflectedFacesNeedTheirOwnDepthOrdering() {
        val camera = AlbumCamera(0f, -12f)
        val high = AlbumPackageVertex(0f, -.5f, 0f)
        val low = AlbumPackageVertex(0f, .5f, 0f)
        assertTrue(camera.view(high).z > camera.view(low).z)
        assertTrue(camera.view(albumReflectedVertex(high, .5f)).z < camera.view(albumReflectedVertex(low, .5f)).z)
    }
}
