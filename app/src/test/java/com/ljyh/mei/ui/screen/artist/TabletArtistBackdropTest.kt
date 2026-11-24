package com.ljyh.mei.ui.screen.artist

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
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
import org.junit.Assert.assertNotNull
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

    @Test fun failedBannerFallsBackToPortraitAndScrollingDoesNotReloadTheImage() =
        verify(2, TabletArtistPhotoKind.Portrait) { portrait ->
            AppleArtistArtwork("1", "测试歌手", portrait,
                banner = portrait.copy(url = portrait.url + ".missing", width = 1920, height = 1080))
        }

    @Test fun fullscreenPreviewIsUsedBeforeAStorePromoWithoutRequiringAWordmark() =
        verify(1, TabletArtistPhotoKind.MotionPreview) { portrait ->
            AppleArtistArtwork("1", "测试歌手", portrait.copy(url = portrait.url + ".unrequested"),
                banner = portrait.copy(url = portrait.url + ".promo", width = 4320, height = 1080),
                motionPreview = portrait.copy(width = 3840, height = 2160))
        }

    @Test fun failedMotionAndHeroAdvanceToThePortrait() =
        verify(3, TabletArtistPhotoKind.Portrait) { portrait ->
            AppleArtistArtwork("1", "测试歌手", portrait,
                banner = portrait.copy(url = portrait.url + ".hero", width = 4320, height = 2160),
                motionPreview = portrait.copy(url = portrait.url + ".motion", width = 3840, height = 2160))
        }

    @Test fun ultraWideStorePromoDoesNotReplaceTheForegroundPortrait() =
        verify(1, TabletArtistPhotoKind.Portrait) { portrait ->
            AppleArtistArtwork("1", "测试歌手", portrait,
                banner = portrait.copy(url = portrait.url + ".promo", width = 4320, height = 1080))
        }

    private fun verify(expectedRequests: Int, expectedKind: TabletArtistPhotoKind,
        artworkFor: (AppleArtistImage) -> AppleArtistArtwork) {
        val context = RuntimeEnvironment.getApplication()
        val previous = context.imageLoader
        val starts = AtomicInteger()
        val loadedColor = AtomicReference<String>()
        val scroll = mutableFloatStateOf(0f)
        val foreground = mutableStateOf<androidx.compose.ui.graphics.painter.Painter?>(null)
        val loadedKind = AtomicReference<TabletArtistPhotoKind>()
        val file = File.createTempFile("artist-portrait-", ".png")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(android.graphics.Color.BLUE)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
        val artwork = artworkFor(AppleArtistImage(file.toURI().toString(), 64, 64, "182b25"))
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
                        TabletArtistBackdrop(artwork, { scroll.floatValue }) { state, photo, painter ->
                            if (state == ArtistArtworkLoadState.Success) {
                                loadedColor.set(photo.image.background)
                                loadedKind.set(photo.kind)
                                if (!photo.hasHero) foreground.value = painter
                            }
                        }
                        foreground.value?.let { Image(it, "歌手照片", Modifier.requiredSize(192.dp)) }
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
                assertEquals(expectedKind, loadedKind.get())
                if (expectedKind == TabletArtistPhotoKind.Portrait) assertNotNull(foreground.value)
                assertEquals("Only sequential candidates are requested", expectedRequests, starts.get())
                for (offset in listOf(100f, 220f, 400f, 0f)) {
                    compose.runOnIdle { scroll.floatValue = offset }
                    compose.waitForIdle()
                }
                assertEquals("Backdrop, foreground and scrolling reuse the decoded painter", expectedRequests, starts.get())
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
