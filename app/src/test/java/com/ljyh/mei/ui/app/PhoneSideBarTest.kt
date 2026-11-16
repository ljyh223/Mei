package com.ljyh.mei.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.screen.Index
import com.ljyh.mei.ui.screen.MeiDestinationSurface
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class PhoneSideBarTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun shortSideBarFitsEveryControlWithoutScrolling() {
        val height = mutableStateOf(260.dp)
        val route = mutableStateOf(Index.Home.route)
        var opened = 0
        var searched = 0
        TabletContentTestActivity.content = {
            MaterialTheme {
                PhoneSideBar(route.value, showPlayer = true, coverUrl = null,
                    onSelect = { route.value = it.route }, onSearch = { searched++ },
                    onOpenPlayer = { opened++ }, visibility = 1f,
                    modifier = Modifier.height(height.value).testTag("side-bar"))
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            assertTrue(usePhoneSideBar(740f, 380f))
            assertFalse(usePhoneSideBar(380f, 740f))
            val rail = compose.onNodeWithTag("side-bar")
            val player = compose.onNodeWithContentDescription("打开播放器")
            val search = compose.onNodeWithContentDescription("搜索")
            val tabLabels = Index.entries.map { it.label }
            fun buttonBounds() = (tabLabels + listOf("搜索", "打开播放器")).map { label ->
                compose.onNodeWithContentDescription(label).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            }
            for (availableHeight in listOf(260.dp, 294.dp, 380.dp)) {
                compose.runOnIdle { height.value = availableHeight }
                val railBounds = rail.fetchSemanticsNode().boundsInRoot
                val before = buttonBounds()
                assertTrue(before.first().top > railBounds.top)
                assertTrue(before.last().bottom < railBounds.bottom)
                assertTrue(before.zipWithNext().all { (a, b) -> a.bottom <= b.top })
                rail.performTouchInput { swipeUp(); swipeDown() }
                assertEquals(before, buttonBounds())
                compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
                player.performClick()
                search.performClick()
            }
            compose.runOnIdle { assertEquals(3, opened); assertEquals(3, searched) }
            compose.onNodeWithContentDescription("发现").performClick().assertIsSelected().assertHeightIsEqualTo(48.dp)
            player.assertHeightIsEqualTo(48.dp)
            compose.onNodeWithContentDescription("播放").assertDoesNotExist()
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun horizontalArtworkCanScrollUnderTheFloatingTab() {
        var density = 1f
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = LocalDensity.current.density
                Box(Modifier.size(width = 320.dp, height = 300.dp)) {
                    CompositionLocalProvider(
                        LocalAppWindowLayout provides AppWindowLayout.Compact,
                        LocalPageStartInset provides PhoneSideBarWidth,
                    ) {
                        MeiDestinationSurface(drawsBehindSideBar = true) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().testTag("shelf"),
                                contentPadding = meiPageHorizontalPadding(),
                            ) {
                                items(6) { index ->
                                    Box(Modifier.size(144.dp).testTag("artwork-$index"))
                                }
                            }
                        }
                    }
                    PhoneSideBar(
                        selectedRoute = Index.Home.route,
                        showPlayer = false,
                        coverUrl = null,
                        onSelect = {},
                        onSearch = {},
                        onOpenPlayer = {},
                        visibility = 1f,
                    )
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val shelf = compose.onNodeWithTag("shelf")
            val viewport = shelf.fetchSemanticsNode().boundsInRoot
            val tab = compose.onNodeWithContentDescription(Index.Home.label).fetchSemanticsNode().boundsInRoot
            val card = compose.onNodeWithTag("artwork-0")
            assertTrue(viewport.left < tab.left)
            assertTrue(card.fetchSemanticsNode().boundsInRoot.left > tab.right)

            shelf.performSemanticsAction(SemanticsActions.ScrollBy) { scroll -> scroll(72f * density, 0f) }
            compose.waitForIdle()
            val scrolled = card.fetchSemanticsNode().boundsInRoot
            assertTrue(scrolled.left < tab.right)
            assertTrue(scrolled.right > tab.left)
            assertTrue(scrolled.top < tab.bottom)
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun landscapeViewportReachesBothEdgesWhileControlsAvoidTheCutout() {
        val cutoutOnRight = mutableStateOf(true)
        val landscape = mutableStateOf(true)
        val cutoutWidth = 40.dp
        var density = 1f
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = LocalDensity.current.density
                Box(Modifier.size(width = 320.dp, height = 240.dp).testTag("window")) {
                    CompositionLocalProvider(
                        LocalAppWindowLayout provides AppWindowLayout.Compact,
                        LocalPhoneLandscape provides landscape.value,
                        LocalPageStartInset provides (if (landscape.value) PhoneSideBarWidth else 0.dp),
                    ) {
                        MeiDestinationSurface(
                            drawsBehindSideBar = true,
                            drawsToHorizontalEdgesInLandscape = true,
                            horizontalSafeInsets = if (cutoutOnRight.value) WindowInsets(right = cutoutWidth)
                                else WindowInsets(left = cutoutWidth),
                        ) {
                            Box(Modifier.fillMaxSize().testTag("viewport")) {
                                Box(Modifier.fillMaxWidth().padding(meiPageHorizontalPadding())) {
                                    Box(Modifier.fillMaxWidth().height(48.dp).testTag("controls"))
                                }
                            }
                        }
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val window = compose.onNodeWithTag("window").fetchSemanticsNode().boundsInRoot
            fun viewport() = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
            fun controls() = compose.onNodeWithTag("controls").fetchSemanticsNode().boundsInRoot
            val gutter = 20.dp
            for (right in listOf(true, false)) {
                compose.runOnIdle { cutoutOnRight.value = right }
                assertEquals(window, viewport())
                val start = PhoneSideBarWidth + gutter + if (right) 0.dp else cutoutWidth
                val end = gutter + if (right) cutoutWidth else 0.dp
                assertEquals(start.value * density, controls().left - window.left, 1f)
                assertEquals(end.value * density, window.right - controls().right, 1f)
            }
            compose.runOnIdle { landscape.value = false }
            assertEquals(cutoutWidth.value * density, viewport().left - window.left, 1f)
            assertEquals(gutter.value * density, controls().left - viewport().left, 1f)
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun olderPageConsumesSideSpaceOnlyOnce() {
        var density = 1f
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = LocalDensity.current.density
                Box(Modifier.size(width = 320.dp, height = 300.dp).testTag("window")) {
                    CompositionLocalProvider(
                        LocalAppWindowLayout provides AppWindowLayout.Compact,
                        LocalPageStartInset provides PhoneSideBarWidth,
                    ) {
                        MeiDestinationSurface(drawsBehindSideBar = false) {
                            Box(Modifier.padding(meiPageHorizontalPadding())) {
                                Box(Modifier.size(48.dp).testTag("legacy-control"))
                            }
                        }
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val viewport = compose.onNodeWithTag("window").fetchSemanticsNode().boundsInRoot
            val control = compose.onNodeWithTag("legacy-control").fetchSemanticsNode().boundsInRoot
            assertEquals((PhoneSideBarWidth.value + 20f) * density, control.left - viewport.left, 1f)
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
