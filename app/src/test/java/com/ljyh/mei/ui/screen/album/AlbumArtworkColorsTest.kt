package com.ljyh.mei.ui.screen.album

import androidx.compose.ui.graphics.Color
import com.ljyh.mei.data.repository.CoverPalette
import org.junit.Assert.*
import org.junit.Test

class AlbumArtworkColorsTest {
    @Test fun darkApplePaletteSuppliesThePageAndAllFourTextRoles() {
        val palette = CoverPalette("050a0c", "ccf5f0", "a1d4ce", "84bdb5", "b7dfd2")
        val colors = albumArtworkColors(palette, null, Color.White)
        assertEquals(Color(0xff050a0c), colors.background)
        assertEquals(Color(0xffccf5f0), colors.title)
        assertEquals(Color(0xffa1d4ce), colors.secondary)
        assertEquals(Color(0xff84bdb5), colors.tertiary)
        assertEquals(Color(0xffb7dfd2), colors.accent)
        listOf(colors.title, colors.secondary, colors.tertiary, colors.accent).forEach {
            assertTrue(albumColorContrast(it, colors.background) >= 4.5f)
        }
    }
    @Test fun lightApplePaletteKeepsItsWarmBackgroundAndDarkType() {
        val colors = albumArtworkColors(CoverPalette("#fff8ef", "201b16", "5f4937", "695348", "46351a"), null, Color.Black)
        assertEquals(Color(0xfffff8ef), colors.background)
        assertEquals(Color(0xff201b16), colors.title)
        assertEquals(Color(0xff46351a), colors.accent)
        assertTrue(albumColorContrast(colors.secondary, colors.background) >= 4.5f)
    }
    @Test fun portraitPaletteWinsAndMissingFieldsUseTheSquarePalette() {
        val square = CoverPalette("ffffff", "cceedd", "aabbcc", null, "ffffff")
        val portrait = CoverPalette("071011", "bbddcc", null, "b1d4c8", null)
        val colors = albumArtworkColors(portrait, square, Color.White)
        assertEquals(Color(0xff071011), colors.background)
        assertEquals(Color(0xffbbddcc), colors.title)
        assertEquals(Color(0xffaabbcc), colors.secondary)
        assertEquals(Color(0xffb1d4c8), colors.tertiary)
        assertEquals(Color.White, colors.accent)
    }
    @Test fun badOrUnreadableFieldsCannotCreateBlackTypeOnABlackPage() {
        val colors = albumArtworkColors(CoverPalette("#000000", "000000", "010101", "bad value", null), null, Color.White)
        assertEquals(Color.Black, colors.background)
        assertEquals(Color.White, colors.title)
        listOf(colors.title, colors.secondary, colors.tertiary, colors.accent).forEach {
            assertTrue(albumColorContrast(it, colors.background) >= 4.5f)
        }
        assertEquals(Color.White, albumArtworkColors(CoverPalette("invalid", null, null, null, null), null, Color.White).background)
        assertEquals(Color(0xffabcdef), albumPaletteColor("80abcdef"))
        assertNull(albumPaletteColor("12345"))
    }
}
