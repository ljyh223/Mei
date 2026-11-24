package com.ljyh.mei.ui.screen.artist

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.imageLoader
import coil3.intercept.Interceptor
import coil3.request.allowHardware
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import com.ljyh.mei.data.repository.artwork.AppleArtistImage
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import com.ljyh.mei.utils.image.ArtworkRequestInterceptor
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/** Verify source fallback and request ownership; no screenshot or pixel assertions. */
@OptIn(DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class TabletArtistBackdropTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun failedBannerFallsBackToPortraitAndScrollingDoesNotReloadTheImage() {
        val context = RuntimeEnvironment.getApplication()
        val previous = context.imageLoader
        val starts = AtomicInteger()
        val loadedColor = AtomicReference<String>()
        val scroll = mutableFloatStateOf(0f)
        val file = File.createTempFile("artist-portrait-", ".png")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(android.graphics.Color.BLUE)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
        val artwork = AppleArtistArtwork("1", "测试歌手",
            portrait = AppleArtistImage(file.toURI().toString(), 64, 64, "182b25"),
            banner = AppleArtistImage(file.toURI().toString() + ".missing", 1920, 1080, "aaaaaa"))
        val loader = ImageLoader.Builder(context).allowHardware(false).diskCache(null)
            .components {
                add(ArtworkRequestInterceptor())
                add(Interceptor { chain -> starts.incrementAndGet(); chain.proceed() })
            }.build()
        SingletonImageLoader.setUnsafe(loader)
        try {
            TabletContentTestActivity.content = {
                MaterialTheme {
                    Box(Modifier.requiredSize(1000.dp, 720.dp)) {
                        TabletArtistBackdrop(artwork, { scroll.floatValue }) { state, color ->
                            if (state == ArtistArtworkLoadState.Success) loadedColor.set(color)
                        }
                    }
                }
            }
            val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
            try {
                compose.waitUntil(10_000) {
                    compose.waitForIdle()
                    loadedColor.get() != null
                }
                assertEquals("182b25", loadedColor.get())
                assertEquals("Only the failed banner and its portrait fallback are requested", 2, starts.get())
                for (offset in listOf(100f, 220f, 400f, 0f)) {
                    compose.runOnIdle { scroll.floatValue = offset }
                    compose.waitForIdle()
                }
                assertEquals("Reflection and scrolling reuse the same decoded painter", 2, starts.get())
            } finally {
                activity.pause().stop().destroy()
                TabletContentTestActivity.content = {}
            }
        } finally {
            SingletonImageLoader.setUnsafe(previous)
            loader.shutdown()
            file.delete()
        }
    }
}
