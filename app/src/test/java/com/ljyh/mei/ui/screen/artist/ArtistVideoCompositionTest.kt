package com.ljyh.mei.ui.screen.artist

import android.graphics.Matrix
import android.graphics.Rect
import com.ljyh.mei.ui.screen.artist.component.ArtistVideoComposition
import com.ljyh.mei.ui.screen.artist.component.drawArtistVideoFrames
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** GPU display-list ownership and drawing geometry, without decoded video or pixel assertions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtistVideoCompositionTest {
    @Test fun originalAndReflectionShareOneFilteredInputAndMeetAtTheSameEdge() {
        val composition = ArtistVideoComposition()
        composition.resize(1000, 720, 1f, 0)
        val seams = mutableListOf<FloatArray>()
        val clips = mutableListOf<Rect>()
        var recordings = 0
        val output = composition.record { canvas ->
            recordings++
            drawArtistVideoFrames(canvas, 1000f, 500f, 720f) { frame ->
                clips += frame.clipBounds
                val matrix = Matrix()
                @Suppress("DEPRECATION")
                frame.getMatrix(matrix)
                seams += floatArrayOf(0f, 500f, 1000f, 500f).also { matrix.mapPoints(it) }
            }
        }
        assertEquals(1, recordings)
        assertEquals(2, seams.size)
        assertArrayEquals(seams[0], seams[1], 0.001f)
        assertArrayEquals(floatArrayOf(0f, 500f, 1000f, 500f), seams[0], 0.001f)
        assertEquals(Rect(0, 0, 1000, 500), clips[0])
        assertEquals(500, clips[1].bottom)
        assertTrue("Only the visible lower source edge is reflected", clips[1].top > 0)
        assertTrue(composition.imageNode.hasDisplayList())
        assertTrue(output.hasDisplayList())
        assertSame(composition.outputNode, output)
        assertTrue(output.clipToBounds)
        composition.discard()
        assertFalse(composition.imageNode.hasDisplayList())
        assertFalse(output.hasDisplayList())
    }

    @Test fun resizeAndRerecordReplaceTheCompositionWithoutHoldingOldDisplayLists() {
        val composition = ArtistVideoComposition()
        composition.resize(1000, 720, 1f, 0)
        composition.record { }
        composition.discard()
        composition.resize(720, 720, 2f, 20)
        val output = composition.record { }
        assertEquals(720, composition.imageNode.width)
        assertEquals(720, output.width)
        assertEquals(720, output.height)
        assertTrue(output.hasDisplayList())
        composition.discard()
    }
}
