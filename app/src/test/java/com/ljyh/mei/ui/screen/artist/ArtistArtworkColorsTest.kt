package com.ljyh.mei.ui.screen.artist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistArtworkColorsTest {
    @Test fun brightArtworkBackgroundsKeepWhiteSecondaryTextReadable() {
        // Neutral, cool portrait and olive landscape palettes from the supported artwork types.
        for (source in listOf(Color.White, Color(0xFF8A9BA0), Color(0xFF606C51))) {
            val background = artistImmersiveBackground(source)
            val secondary = Color.White.copy(alpha = ArtistImmersiveSecondaryOpacity).compositeOver(background)
            val contrast = (secondary.luminance() + 0.05f) / (background.luminance() + 0.05f)
            assertTrue("White secondary text needs 4.5:1 contrast", contrast >= 4.5f)
        }
    }

    @Test fun darkArtworkColorsArePreserved() {
        val source = Color(0xFF182B25)
        assertEquals(source, artistImmersiveBackground(source))
    }
}
