package com.ljyh.mei.ui.screen.artist

import com.ljyh.mei.utils.color.immersiveArtworkBackground

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import com.ljyh.mei.data.repository.artwork.AppleArtistImage
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class ArtistArtworkThemeTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun landscapePhotoPaletteKeepsBothPanesReadableAndWaitsForThePhoto() {
        val ready = mutableStateOf(false)
        val palette = mutableStateOf(Color.White)
        val scheme = AtomicReference<ColorScheme>()
        val ink = AtomicReference<Color>()
        val original = lightColorScheme()
        TabletContentTestActivity.content = {
            MaterialTheme(colorScheme = original) {
                ArtistArtworkTheme(null, ready.value, sourceColor = palette.value) {
                    val current = MaterialTheme.colorScheme
                    val foreground = LocalContentColor.current
                    SideEffect { scheme.set(current); ink.set(foreground) }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            assertEquals(original.background, scheme.get().background)
            for ((background, foreground) in listOf(Color.White to Color.Black, Color(0xFF142840) to Color.White)) {
                compose.runOnIdle { ready.value = true; palette.value = background }
                compose.waitForIdle()
                assertEquals(background, scheme.get().background)
                assertEquals(background, scheme.get().surface)
                assertEquals(foreground, scheme.get().onBackground)
                assertEquals(foreground, scheme.get().onSurface)
                assertEquals(foreground, ink.get())
            }
            compose.runOnIdle { ready.value = false }
            compose.waitForIdle()
            assertEquals(original.background, scheme.get().background)
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun neteaseUsesTheSameImmersivePaletteAsAppleAndNeverFadesBackToTheLightTheme() {
        val apple = mutableStateOf(true)
        val dark = mutableStateOf(false)
        val scheme = AtomicReference<ColorScheme>()
        val ink = AtomicReference<Color>()
        val palette = Color(0xFF2871A5)
        val artwork = AppleArtistArtwork("1", "歌手", AppleArtistImage("", 1000, 1000, "2871a5"))
        TabletContentTestActivity.content = {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                ArtistArtworkTheme(if (apple.value) artwork else null, true, immersive = true,
                    sourceColor = if (apple.value) null else palette) {
                    val current = MaterialTheme.colorScheme
                    val color = LocalContentColor.current
                    SideEffect { scheme.set(current); ink.set(color) }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            val appleColors = scheme.get()
            for (isDark in listOf(false, true)) {
                compose.runOnIdle { apple.value = false; dark.value = isDark }
                compose.waitForIdle()
                assertEquals(appleColors.background, scheme.get().background)
                assertEquals(appleColors.onBackground, scheme.get().onBackground)
                assertEquals(appleColors.primary, scheme.get().primary)
                assertEquals(appleColors.onSurfaceVariant, scheme.get().onSurfaceVariant)
                assertEquals(immersiveArtworkBackground(palette), scheme.get().background)
                assertEquals(scheme.get().onBackground, ink.get())
                assertNotEquals(Color.White, scheme.get().background)
            }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
