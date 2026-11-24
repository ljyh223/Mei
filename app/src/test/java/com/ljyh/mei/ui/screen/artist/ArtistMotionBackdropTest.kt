package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.ljyh.mei.ui.screen.artist.component.ArtistMotionBackdrop
import com.ljyh.mei.ui.screen.artist.component.ArtistVideoBackdropView
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
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

/** State/lifecycle checks with a fake decoder, not screenshots or network playback. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ArtistMotionBackdropTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun waitsForFirstFramePausesOffscreenAndReleasesOnExit() {
        val decoder = Decoder()
        val ready = AtomicBoolean()
        val scroll = mutableFloatStateOf(0f)
        var created = 0
        TabletContentTestActivity.content = {
            ArtistMotionBackdrop("https://example.org/video.m3u8", "1", { scroll.floatValue }, { ready.set(it) },
                Modifier.requiredSize(1000.dp, 720.dp), playerFactory = { _, _, _ -> created++; decoder.player })
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            assertEquals(1, created)
            assertTrue(decoder.playing)
            assertFalse(ready.get())
            compose.runOnIdle {
                decoder.listener!!.onVideoSizeChanged(VideoSize(1920, 1080))
                decoder.listener!!.onRenderedFirstFrame()
            }
            compose.waitForIdle()
            assertTrue(ready.get())
            compose.runOnIdle { decoder.listener!!.onSurfaceSizeChanged(0, 0) }
            compose.waitForIdle()
            assertFalse(ready.get())
            compose.runOnIdle { decoder.listener!!.onRenderedFirstFrame() }
            compose.waitForIdle()
            assertTrue(ready.get())
            compose.runOnIdle { scroll.floatValue = 5000f }
            compose.waitForIdle()
            assertFalse(decoder.playing)
            compose.runOnIdle { scroll.floatValue = 0f }
            compose.waitForIdle()
            assertTrue(decoder.playing)
            activity.pause()
            compose.waitForIdle()
            assertFalse(decoder.playing)
            activity.resume()
            compose.waitForIdle()
            assertTrue(decoder.playing)
            assertEquals(1, created)
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
        assertTrue(decoder.released)
    }

    @Test fun streamErrorWithdrawsVideoAndDoesNotRetryOnEveryScroll() {
        val decoder = Decoder()
        val ready = AtomicBoolean()
        val scroll = mutableFloatStateOf(0f)
        var created = 0
        TabletContentTestActivity.content = {
            ArtistMotionBackdrop("https://example.org/video.m3u8", "1", { scroll.floatValue }, { ready.set(it) },
                Modifier.requiredSize(1000.dp, 720.dp), playerFactory = { _, _, _ -> created++; decoder.player })
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.runOnIdle {
                decoder.listener!!.onVideoSizeChanged(VideoSize(1920, 1080))
                decoder.listener!!.onRenderedFirstFrame()
            }
            compose.waitForIdle()
            assertTrue(ready.get())
            compose.runOnIdle { decoder.listener!!.onPlayerError(PlaybackException("fixture", null,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)) }
            compose.waitForIdle()
            assertFalse(ready.get())
            assertTrue(decoder.released)
            for (offset in listOf(5000f, 0f)) {
                compose.runOnIdle { scroll.floatValue = offset }
                compose.waitForIdle()
            }
            assertEquals(1, created)
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun nativeHostSizesOneTextureAndBuildsBackdropEffects() {
        val view = ArtistVideoBackdropView(RuntimeEnvironment.getApplication())
        val w = android.view.View.MeasureSpec.makeMeasureSpec(1000, android.view.View.MeasureSpec.EXACTLY)
        val h = android.view.View.MeasureSpec.makeMeasureSpec(720, android.view.View.MeasureSpec.EXACTLY)
        view.measure(w, h)
        view.layout(0, 0, 1000, 720)
        assertEquals(1, view.childCount)
        assertEquals(500, view.texture.height)
        view.updatePresentation(false, 0f)
        assertEquals(0f, view.alpha)
        view.updatePresentation(true, 0f)
        assertEquals(1f, view.alpha)
        view.updatePresentation(true, 400f * view.resources.displayMetrics.density)
        assertEquals(0f, view.alpha)
    }

    private class Decoder {
        var listener: Player.Listener? = null
        var playing = false
        var released = false
        val player = Proxy.newProxyInstance(ExoPlayer::class.java.classLoader, arrayOf(ExoPlayer::class.java)) { proxy, method, args ->
            when (method.name) {
                "addListener" -> { listener = args!![0] as Player.Listener; null }
                "removeListener" -> { listener = null; null }
                "setPlayWhenReady" -> { playing = args!![0] as Boolean; null }
                "getPlayWhenReady" -> playing
                "release" -> { released = true; null }
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args!![0]
                "toString" -> "ArtistDecoderFixture"
                else -> null
            }
        } as ExoPlayer
    }
}
