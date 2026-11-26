package com.ljyh.mei.ui.screen.artist

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.imageLoader
import coil3.intercept.Interceptor
import coil3.request.allowHardware
import com.ljyh.mei.ui.screen.artist.component.ArtistColorBackdrop
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import com.ljyh.mei.utils.image.ArtworkRequestInterceptor
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
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
class TabletNeteaseArtistBackdropTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun failedAvatarFallsBackToCoverSharedByBackgroundAndForeground() = verify(true, 2)
    @Test fun showingAndHidingTheBackdropDoesNotReloadThePortrait() = verify(false, 1)

    @Test fun failedLandscapeCoverFallsBackToAvatarWithOnePaletteExtraction() = verify(true, 2, preferCover = true)

    private fun verify(failedAvatar: Boolean, requests: Int, preferCover: Boolean = false) {
        val context = RuntimeEnvironment.getApplication()
        val previous = context.imageLoader
        val count = AtomicInteger()
        val portrait = mutableStateOf<Painter?>(null)
        val visible = mutableStateOf(true)
        val palette = AtomicReference<Color>()
        val colorUrl = AtomicReference<String>()
        val colorRequests = AtomicInteger()
        val file = File.createTempFile("netease-portrait-", ".png")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(android.graphics.Color.BLUE)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
        val url = file.toURI().toString()
        val state = ArtistDetailUiState("1", ArtistDetail.Data.Artist(name = "网易云歌手",
            avatar = if (failedAvatar && !preferCover) "$url.missing" else url,
            cover = if (failedAvatar && preferCover) "$url.missing" else url), emptyList(),
            false, null, emptyList(), false, null, emptyList(), false, null, null, true,
            appleArtworkEnabled = false)
        val loader = ImageLoader.Builder(context).allowHardware(false).diskCache(null).components {
            add(ArtworkRequestInterceptor())
            add(Interceptor { chain -> count.incrementAndGet(); chain.proceed() })
        }.build()
        SingletonImageLoader.setUnsafe(loader)
        try {
            TabletContentTestActivity.content = {
                MaterialTheme {
                    Box(Modifier.requiredSize(1000.dp, 720.dp)) {
                        NeteaseArtistArtwork(state, preferCover = preferCover, onPortraitChanged = { status, painter, color ->
                            if (status == ArtistArtworkLoadState.Success) portrait.value = painter
                            if (color != null) palette.set(color)
                        }, onColorRequested = { loadedUrl, _ ->
                            colorUrl.set(loadedUrl)
                            colorRequests.incrementAndGet()
                            Color.Blue
                        }) { painter, _ ->
                            if (visible.value && painter != null) ArtistColorBackdrop(painter)
                        }
                        portrait.value?.let { Image(it, "歌手照片", Modifier.requiredSize(192.dp)) }
                    }
                }
            }
            val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
            try {
                compose.waitUntil(10_000) { compose.waitForIdle(); portrait.value != null && palette.get() != null }
                assertEquals(requests, count.get())
                assertEquals(url, colorUrl.get())
                assertEquals(Color.Blue, palette.get())
                for (shown in listOf(false, true)) {
                    compose.runOnIdle { visible.value = shown }
                    compose.waitForIdle()
                }
                assertNotNull(portrait.value)
                assertEquals(1, colorRequests.get())
                assertEquals("Foreground and background share one decode", requests, count.get())
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
