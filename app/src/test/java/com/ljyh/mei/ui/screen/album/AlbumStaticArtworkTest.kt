package com.ljyh.mei.ui.screen.album

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.WindowInsets
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.imageLoader
import coil3.intercept.Interceptor
import coil3.request.allowHardware
import com.ljyh.mei.constants.AlbumArtworkMode
import com.ljyh.mei.data.model.domain.AlbumOverview
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import com.ljyh.mei.utils.image.ArtworkRequestInterceptor
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@OptIn(DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class AlbumStaticArtworkTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun coverReflectionAndPaletteReuseOneDecodeAcrossUnrelatedPageChanges() {
        val context = RuntimeEnvironment.getApplication()
        val previous = context.imageLoader
        val requests = AtomicInteger()
        val palettes = AtomicInteger()
        val subscribed = mutableStateOf(false)
        val file = File.createTempFile("album-static-", ".png")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(android.graphics.Color.BLUE)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
        val url = file.toURI().toString()
        val loader = ImageLoader.Builder(context).allowHardware(false).diskCache(null).components {
            add(ArtworkRequestInterceptor())
            add(Interceptor { chain -> requests.incrementAndGet(); chain.proceed() })
        }.build()
        SingletonImageLoader.setUnsafe(loader)
        try {
            TabletContentTestActivity.content = {
                MaterialTheme {
                    CompositionLocalProvider(LocalAppWindowLayout provides AppWindowLayout.Compact,
                        LocalMeiGlassEnabled provides false, LocalPlayerAwareWindowInsets provides WindowInsets(0)) {
                        Box(Modifier.requiredSize(360.dp, 760.dp)) {
                            AdaptiveAlbumContent(AlbumDetailUiState(
                                AlbumOverview(1, "Static album", url, "", 0, emptyList()),
                                null, emptyList(), emptyList(), subscribed.value, false, true, true, "", false,
                                artworkMode = AlbumArtworkMode.Static,
                            ), AlbumDetailActions({}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> },
                                onStaticArtworkChanged = { decodedUrl, image ->
                                    assertEquals(url, decodedUrl)
                                    assertNotNull(image)
                                    palettes.incrementAndGet()
                                    Color.Blue
                                }))
                        }
                    }
                }
            }
            val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
            try {
                compose.waitUntil(10_000) { compose.waitForIdle(); palettes.get() > 0 }
                compose.runOnIdle { subscribed.value = true }
                compose.waitForIdle()
                assertEquals("Only the displayed image is requested", 1, requests.get())
                assertEquals("Palette work is not restarted by page colors or subscription state", 1, palettes.get())
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
