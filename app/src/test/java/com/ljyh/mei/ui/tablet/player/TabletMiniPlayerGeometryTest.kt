package com.ljyh.mei.ui.tablet.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.sheet.*
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import com.ljyh.mei.ui.tablet.component.TabletMiniPlayer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1400dp-h1000dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class TabletMiniPlayerGeometryTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun theRealMiniReportsIts64DpSurfaceAnd48DpArtworkWithoutRecentering() {
        val state = TabletPlayerExpansionState()
        var density = 1f
        var paused = 0
        val metadata = MediaMetadata(1L, "Song", "", emptyList(), 1000L, MediaMetadata.Album(2L, "Album"))
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = LocalDensity.current.density
                Box(Modifier.size(1280.dp, 800.dp)) {
                    TabletMiniPlayer(metadata, true, true, true, {}, { paused++ }, {},
                        modifier = Modifier.offset(588.dp, 700.dp).size(340.dp, 64.dp),
                        onBounds = { surface, artwork ->
                            state.recordOrigin(surface, artwork, Size(1280 * density, 800 * density), true)
                        })
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.runOnIdle {
                val source = state.originInHost(Offset.Zero, Size(1280 * density, 800 * density))!!
                assertEquals(588f * density, source.surface.left, .1f)
                assertEquals(64f * density, source.surface.height, .1f)
                assertEquals(48f * density, source.artwork.width, .1f)
                assertEquals(596f * density, source.artwork.left, .1f)
                assertEquals(708f * density, source.artwork.top, .1f)
            }
            compose.onNodeWithContentDescription("暂停").performClick()
            compose.runOnIdle { assertEquals(1, paused) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
    @Test fun theCollapsedSharedContainerKeepsTheMiniControlsAndLeavesNavigationTouchable() {
        var density = 1f
        var paused = 0
        var navigationClicks = 0
        val metadata = MediaMetadata(1L, "Song", "", emptyList(), 1000L, MediaMetadata.Album(2L, "Album"))
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = LocalDensity.current.density
                val sheet = rememberBottomSheetState(0.dp, 800.dp, 72.dp, initialAnchor = collapsedAnchor, allowDismissGesture = false)
                Box(Modifier.size(1280.dp, 800.dp).testTag("host")) {
                    Box(Modifier.fillMaxSize().clickable { navigationClicks++ })
                    BottomSheet(sheet,
                        expansionBounds = Rect(588f * density, 700f * density, 928f * density, 764f * density),
                        morphSpec = BottomSheetMorphSpec(collapsedHeight = 64.dp, collapsedBottomMargin = 28.dp),
                        keepExpandedContentComposed = true,
                        transparentCollapsedContainer = true,
                        collapsedContent = {
                            TabletMiniPlayer(metadata, true, true, true, {}, { paused++ }, {},
                                modifier = Modifier.offset(x = 588.dp).size(340.dp, 64.dp),
                                drawCover = false)
                        },
                    ) { Box(Modifier.fillMaxSize()) }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            val pause = compose.onNodeWithContentDescription("暂停").fetchSemanticsNode().boundsInRoot
            assertEquals(854f * density, pause.center.x, .1f)
            assertEquals(732f * density, pause.center.y, .1f)
            compose.onNodeWithContentDescription("暂停").performTouchInput { click() }
            compose.runOnIdle { assertEquals(1, paused) }
            compose.onNodeWithTag("host").performTouchInput { click(Offset(400f * density, 732f * density)) }
            compose.runOnIdle { assertEquals(1, navigationClicks) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

}
