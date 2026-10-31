package com.ljyh.mei.ui.component.artwork

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.card.VinylAlbumCard
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Layout and interaction only; no images, screenshot rendering, or network requests. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w600dp-h1000dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class VinylAlbumLayoutTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun compactAndFullArtworkReserveTheSameSpaceBeforeAndAfterLoading() {
        var loading by mutableStateOf(true)
        var cover by mutableStateOf(56.dp)
        var measured = IntSize.Zero
        var expected = IntSize.Zero
        TabletContentTestActivity.content = {
            MaterialTheme {
                val density = LocalDensity.current
                SideEffect { expected = with(density) { IntSize((cover * VinylAlbumArtworkRatio).roundToPx(), cover.roundToPx()) } }
                Box(Modifier.width(500.dp).height(800.dp)) {
                    VinylAlbumArtwork(null, Modifier.width(cover * VinylAlbumArtworkRatio)
                        .onGloballyPositioned { measured = it.size }, isLoading = loading)
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            for (size in listOf(56.dp, 64.dp, 116.dp, 136.dp, 168.dp)) {
                compose.runOnIdle { cover = size; loading = true }
                compose.waitForIdle()
                val placeholderSize = measured
                assertEquals(expected.width, measured.width)
                assertEquals(expected.height.toDouble(), measured.height.toDouble(), 1.0)
                compose.runOnIdle { loading = false }
                compose.waitForIdle()
                assertEquals(placeholderSize, measured)
            }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun cardKeepsItsTextSpaceAndDisablesOnlyTheLoadingInteraction() {
        var loading by mutableStateOf(true)
        var measured = IntSize.Zero
        var clicks = 0
        TabletContentTestActivity.content = {
            MaterialTheme {
                Box(Modifier.width(500.dp).height(800.dp)) {
                    VinylAlbumCard("Album", "Artist", null,
                        Modifier.width(168.dp).testTag("album-card").onGloballyPositioned { measured = it.size },
                        isLoading = loading, onClick = { clicks++ })
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            val placeholderSize = measured
            compose.onNodeWithTag("album-card").performClick()
            compose.runOnIdle { assertEquals(0, clicks); loading = false }
            compose.waitForIdle()
            assertEquals(placeholderSize, measured)
            compose.onNodeWithText("Album", useUnmergedTree = true).assertExists()
            compose.onNodeWithText("Artist", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("album-card").performClick()
            compose.runOnIdle { assertEquals(1, clicks) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
