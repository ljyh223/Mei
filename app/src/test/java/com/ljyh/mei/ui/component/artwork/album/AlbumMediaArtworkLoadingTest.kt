package com.ljyh.mei.ui.component.artwork.album

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.imageLoader
import coil3.intercept.Interceptor
import coil3.request.allowHardware
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import java.io.IOException
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
class AlbumMediaArtworkLoadingTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun modelMechanismsWorkBeforeTheCoverArrivesAndKeepTheirPoseAfterSuccess() = verify(false)
    @Test fun failedCoverKeepsTheModelAndItsOpenStateAvailable() = verify(true)

    private fun verify(fail: Boolean) {
        val context = RuntimeEnvironment.getApplication()
        val previous = context.imageLoader
        val gate = CompletableDeferred<Unit>()
        val requests = AtomicInteger()
        val completed = AtomicInteger()
        val format = mutableStateOf(AlbumMediaStyle.Vinyl)
        val file = File.createTempFile("album-model-texture-", ".png")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(android.graphics.Color.BLUE)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
        val loader = ImageLoader.Builder(context).allowHardware(false).diskCache(null).components {
            add(Interceptor { chain ->
                requests.incrementAndGet()
                gate.await()
                try {
                    if (fail) throw IOException("fixture texture failure")
                    chain.proceed()
                } finally { completed.incrementAndGet() }
            })
        }.build()
        SingletonImageLoader.setUnsafe(loader)
        try {
            TabletContentTestActivity.content = {
                MaterialTheme {
                    Box(Modifier.requiredSize(380.dp, 380.dp)) {
                        AlbumMediaArtwork(file.toURI().toString(), null, "异步封面", interactive = true,
                            style = format.value)
                    }
                }
            }
            val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
            try {
                compose.waitUntil(10_000) { requests.get() > 0 }
                fun model() = compose.onNodeWithContentDescription("专辑《异步封面》封面")
                for ((style, initial, opened) in listOf(
                    Triple(AlbumMediaStyle.Vinyl, "抽出唱片", "收回唱片"),
                    Triple(AlbumMediaStyle.CD, "打开盒盖", "合上盒盖"),
                    Triple(AlbumMediaStyle.Cassette, "打开包装", "取出磁带"),
                )) {
                    compose.runOnIdle { format.value = style }
                    model().assertIsDisplayed().assertHasClickAction()
                    assertEquals(initial, model().fetchSemanticsNode().config[SemanticsActions.OnClick].label)
                    model().performClick()
                    assertEquals(opened, model().fetchSemanticsNode().config[SemanticsActions.OnClick].label)
                    assertFalse("Texture request is deliberately still pending", gate.isCompleted)
                }
                compose.runOnIdle { gate.complete(Unit) }
                compose.waitUntil(10_000) { compose.waitForIdle(); completed.get() > 0 }
                model().assertIsDisplayed().assertHasClickAction()
                assertEquals("Texture completion must not reset the opened cassette", "取出磁带",
                    model().fetchSemanticsNode().config[SemanticsActions.OnClick].label)
                assertEquals("Style and pose changes reuse the same cover request", 1, requests.get())
            } finally {
                activity.pause().stop().destroy()
                TabletContentTestActivity.content = {}
            }
        } finally {
            gate.cancel()
            SingletonImageLoader.setUnsafe(previous)
            loader.shutdown()
            file.delete()
        }
    }
}
