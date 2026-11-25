package com.ljyh.mei.data.repository

import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import coil3.asImage
import coil3.Image
import com.ljyh.mei.data.model.room.CacheColor
import com.ljyh.mei.di.dao.ColorDao
import com.ljyh.mei.di.repository.ColorRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoadedArtworkColorTest {
    @Test fun extractsFromTheDisplayedBitmapWithoutOwningOrRecyclingItAndReusesThePalette() = runBlocking {
        val dao = Colors()
        val repository = ColorRepository(dao)
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)
        try {
            val first = repository.getColorFromLoadedArtwork("cover", bitmap.asImage())
            assertTrue(android.graphics.Color.red(first.toArgb()) >= 240)
            assertTrue(android.graphics.Color.blue(first.toArgb()) < 16)
            assertFalse(bitmap.isRecycled)
            assertEquals(android.graphics.Color.RED, bitmap.getPixel(0, 0))
            assertEquals(first, repository.getColorFromLoadedArtwork("cover", bitmap.asImage()))
            assertEquals(1, dao.reads)
            assertEquals(1, dao.writes)
            assertEquals(first.toArgb(), dao.rows.getValue("cover-v2:cover").color)
        } finally { bitmap.recycle() }
    }

    @Test fun nonBitmapArtworkUsesAFallbackWithoutDrawingTheDisplayedAnimation() = runBlocking {
        val image = object : Image {
            override val size = 0L
            override val width = 64
            override val height = 64
            override val shareable = false
            override fun draw(canvas: android.graphics.Canvas) { error("Do not draw shared animation state") }
        }
        assertEquals(0xFF777777.toInt(), ColorRepository(Colors()).getColorFromLoadedArtwork("animated", image).toArgb())
    }

    private class Colors : ColorDao {
        val rows = mutableMapOf<String, CacheColor>()
        var reads = 0
        var writes = 0
        override fun getColor(url: String): CacheColor? { reads++; return rows[url] }
        override suspend fun insertColor(color: CacheColor) { writes++; rows[color.url] = color }
    }
}
