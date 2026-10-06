package com.ljyh.mei.ui.screen.setting

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class DeveloperPreviewVisibilityTest {
    @Test
    fun `visible fraction follows viewport clipping`() {
        val viewport = Rect(0f, 0f, 100f, 200f)
        assertEquals(0.5f, visibleHeightFraction(Rect(0f, 100f, 100f, 300f), viewport), 0.001f)
        assertEquals(0f, visibleHeightFraction(Rect(0f, 210f, 100f, 310f), viewport), 0.001f)
    }

    @Test
    fun `preview switches only when the next video is clearly visible`() {
        assertEquals(PreviewVariant.SQUARE, chooseVisiblePreview(0.8f, 0f, null))
        assertEquals(PreviewVariant.SQUARE, chooseVisiblePreview(0.42f, 0.58f, PreviewVariant.SQUARE))
        assertEquals(PreviewVariant.PORTRAIT, chooseVisiblePreview(0.2f, 0.8f, PreviewVariant.SQUARE))
        assertEquals(null, chooseVisiblePreview(0.2f, 0.2f, PreviewVariant.PORTRAIT))
    }
}
