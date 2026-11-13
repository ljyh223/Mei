package com.ljyh.mei.ui.component.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import com.ljyh.mei.ui.component.player.MiniPlayerBarContent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w600dp-h1000dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class PlayerContainerLayoutTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun contentKeepsItsFullSizeAndOnlyTheVisibleEndpointIsInteractive() {
        lateinit var sheet: BottomSheetState
        var density = 1f
        var fullClicks = 0
        var behindClicks = 0
        val sizes = mutableListOf<IntSize>()
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = LocalDensity.current.density
                sheet = rememberBottomSheetState(0.dp, 800.dp, 64.dp, initialAnchor = collapsedAnchor, allowDismissGesture = false)
                Box(Modifier.width(400.dp).height(800.dp).testTag("host")) {
                    Box(Modifier.fillMaxSize().clickable { behindClicks++ })
                    BottomSheet(
                        state = sheet,
                        morphSpec = BottomSheetMorphSpec(collapsedHeight = 48.dp, collapsedBottomMargin = 36.dp),
                        containerMotion = BottomSheetContainerMotion.Sheet,
                        expansionBounds = Rect(20f * density, 700f * density, 380f * density, 748f * density),
                        keepExpandedContentComposed = true,
                        collapsedContent = { Text("Mini") },
                    ) {
                        Box(Modifier.fillMaxSize().onSizeChanged { sizes += it }) {
                            Button({ fullClicks++ }, Modifier.offset(150.dp, 375.dp)) { Text("Full action") }
                        }
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.onNodeWithText("Mini").assertExists()
            compose.onNodeWithText("Full action").assertDoesNotExist()
            // The full-size composable behind the clip must not intercept the browsing page.
            compose.onNodeWithTag("host").performTouchInput { click(Offset(10f * density, 10f * density)) }
            compose.runOnIdle { assertEquals(1, behindClicks) }
            compose.runOnIdle { sheet.anchoredDraggableState.dispatchRawDelta(368f * density) }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(.5f, sheet.progress, .001f) }
            compose.onNodeWithText("Mini").assertDoesNotExist()
            compose.onNodeWithText("Full action").assertDoesNotExist()
            compose.onNodeWithTag("host").performTouchInput { click(Offset(200f * density, 400f * density)) }
            compose.runOnIdle { assertEquals(0, fullClicks) }
            // A partial reversal updates the same host instead of starting another transition.
            compose.runOnIdle { sheet.anchoredDraggableState.dispatchRawDelta(-184f * density) }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(.25f, sheet.progress, .001f) }
            compose.runOnIdle { sheet.expandSoft() }
            compose.waitForIdle()
            compose.onNodeWithText("Full action").performClick()
            compose.runOnIdle { assertEquals(1, fullClicks) }
            compose.runOnIdle { sheet.collapseSoft() }
            compose.waitForIdle()
            compose.onNodeWithText("Mini").assertExists()
            compose.onNodeWithText("Full action").assertDoesNotExist()
            compose.onNodeWithTag("host").performTouchInput {
                swipe(Offset(200f * density, 724f * density), Offset(200f * density, 150f * density), durationMillis = 600)
            }
            compose.waitForIdle()
            compose.runOnIdle { assertTrue(sheet.isExpanded) }
            compose.runOnIdle {
                assertTrue(sizes.isNotEmpty())
                assertTrue(sizes.all { it == IntSize((400 * density).toInt(), (800 * density).toInt()) })
            }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
    @Test fun theRealMiniRecoversItsLayoutHeightInsteadOfFeedingBackAClippedHeight() {
        var density = 1f
        var source by mutableStateOf<Rect?>(null)
        var actualHeight = 0f
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = LocalDensity.current.density
                val sheet = rememberBottomSheetState(0.dp, 800.dp, 64.dp, initialAnchor = collapsedAnchor, allowDismissGesture = false)
                Box(Modifier.width(400.dp).height(800.dp)) {
                    BottomSheet(
                        state = sheet,
                        morphSpec = BottomSheetMorphSpec(collapsedHeight = 48.dp, collapsedBottomMargin = 36.dp),
                        expansionBounds = source ?: Rect(20f * density, 700f * density, 380f * density, 724f * density),
                        keepExpandedContentComposed = true,
                        transparentCollapsedContainer = true,
                        collapsedContent = {
                            MiniPlayerBarContent("Song", "Artist", null, false, false, {}, {}, {},
                                onSurfaceBounds = {
                                    actualHeight = it.height
                                    source = it
                                })
                        },
                    ) {}
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(48f * density, actualHeight, .1f) }
            compose.runOnIdle {
                source = Rect(20f * density, 700f * density, 380f * density, 772f * density)
            }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(48f * density, actualHeight, .1f) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

}
