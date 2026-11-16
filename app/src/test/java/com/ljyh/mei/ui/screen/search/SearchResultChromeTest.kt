package com.ljyh.mei.ui.screen.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.glass.GlassAppearance
import com.ljyh.mei.ui.component.glass.LocalMeiGlassAppearance
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.glass.LocalMeiHazeState
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import dev.chrisbanes.haze.HazeState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Geometry and source ownership only; optical shader appearance still needs a real device. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1280dp-h1000dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class SearchResultChromeTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun categoriesSpreadAcrossWideWindowsAndRemainReachableOnNarrowWindowsWithLargeText() {
        var width by mutableStateOf(800.dp)
        var fontScale by mutableStateOf(1f)
        var selected by mutableStateOf(SearchType.All)
        TabletContentTestActivity.content = {
            MaterialTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(Modifier.width(width).testTag("categories")) {
                        SearchTypeFilterRow(selected, onSelect = { selected = it })
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            // Wide tabs use the page's available width rather than the input's 640dp cap.
            compose.onNodeWithText("综合").assertWidthIsAtLeast(140.dp)
            compose.onNodeWithText("歌单").assertIsDisplayed().performClick().assertIsSelected()
            compose.runOnIdle { width = 360.dp; selected = SearchType.All }
            for (type in SearchType.entries.filter { it != SearchType.History }) {
                compose.onNodeWithText(type.displayName).assertIsDisplayed().assertWidthIsAtLeast(48.dp)
                    .performClick().assertIsSelected()
            }
            compose.runOnIdle { width = 240.dp; fontScale = 3f; selected = SearchType.All }
            compose.onNodeWithText("歌单").performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
            compose.onNodeWithText("综合").performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test
    fun phoneLandscapePlacesFiltersNextToTheFieldWhenTheyFit() {
        var width by mutableStateOf(720.dp)
        var landscape by mutableStateOf(true)
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalPhoneLandscape provides landscape,
                    LocalPageStartInset provides 72.dp,
                ) {
                    Box(Modifier.width(width)) {
                        SearchResultHeader(
                            back = { Box(Modifier.size(48.dp)) },
                            field = { modifier -> Box(modifier.fillMaxWidth().height(56.dp).testTag("field")) },
                            filters = { modifier -> Box(modifier.fillMaxWidth().height(48.dp).testTag("filters")) },
                        )
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            fun fieldBounds() = compose.onNodeWithTag("field").fetchSemanticsNode().boundsInRoot
            fun filterBounds() = compose.onNodeWithTag("filters").fetchSemanticsNode().boundsInRoot
            assertTrue(filterBounds().left >= fieldBounds().right)
            assertEquals(fieldBounds().center.y, filterBounds().center.y, 1f)

            compose.runOnIdle { width = 480.dp }
            assertTrue(filterBounds().top >= fieldBounds().bottom)
            compose.runOnIdle { width = 720.dp; landscape = false }
            assertTrue(filterBounds().top >= fieldBounds().bottom)
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test
    fun measuredHeaderKeepsFirstResultReadableWithoutClippingTheScrollableSource() {
        var width by mutableStateOf(360.dp)
        var toolbarHeight by mutableStateOf(72.dp)
        var tabsVisible by mutableStateOf(true)
        var landscape by mutableStateOf(false)
        var glassPreference by mutableStateOf(false)
        var reduceTransparency by mutableStateOf(false)
        var inset = 0f
        var density = 1f
        var toolbarSource: HazeState? = null
        var contentSource: HazeState? = null
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = LocalDensity.current.density
                CompositionLocalProvider(
                    LocalPhoneLandscape provides landscape,
                    LocalMeiGlassEnabled provides glassPreference,
                    LocalMeiGlassAppearance provides GlassAppearance(reduceTransparency = reduceTransparency),
                ) {
                    SearchResultChrome(
                        modifier = Modifier.width(width).height(640.dp),
                        header = {
                            val source = LocalMeiHazeState.current
                            SideEffect { toolbarSource = source }
                            Column {
                                Box(Modifier.fillMaxWidth().height(toolbarHeight).testTag("toolbar"))
                                if (tabsVisible) Box(Modifier.fillMaxWidth().height(48.dp))
                            }
                        },
                    ) { padding ->
                        val source = LocalMeiHazeState.current
                        SideEffect { inset = padding.value; contentSource = source }
                        LazyColumn(Modifier.fillMaxSize().testTag("results"), contentPadding = PaddingValues(top = padding)) {
                            items(40) { index ->
                                Text("Result $index", Modifier.fillMaxWidth().height(56.dp).testTag("row-$index"))
                            }
                        }
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            for (size in listOf(360.dp, 800.dp, 1200.dp)) {
                for (height in listOf(72.dp, 112.dp)) for (tabs in listOf(false, true)) {
                    val live = tabs
                    compose.runOnIdle {
                        width = size
                        toolbarHeight = height
                        tabsVisible = tabs
                        glassPreference = true
                        reduceTransparency = !live
                    }
                    compose.onNodeWithTag("results").performScrollToIndex(0)
                    compose.waitForIdle()
                    val expected = height.value + (if (tabs) 48f else 0f) + (if (live) 34f else 0f)
                    assertEquals(expected, inset, 0.01f)
                    val viewport = compose.onNodeWithTag("results").fetchSemanticsNode().boundsInRoot
                    val toolbar = compose.onNodeWithTag("toolbar").fetchSemanticsNode().boundsInRoot
                    val first = compose.onNodeWithTag("row-0").fetchSemanticsNode().boundsInRoot
                    assertEquals(toolbar.top, viewport.top, 1f)
                    assertEquals(expected * density, first.top - viewport.top, 1f)
                    assertNotNull(toolbarSource)
                    assertNotNull(contentSource)
                    assertNotSame(toolbarSource, contentSource)
                    compose.onNodeWithTag("results").performSemanticsAction(SemanticsActions.ScrollBy) { scroll ->
                        scroll(0f, expected * density)
                    }
                    compose.waitForIdle()
                    val scrolled = compose.onNodeWithTag("row-0").fetchSemanticsNode().boundsInRoot
                    assertEquals(viewport.top, scrolled.top, 1f)
                }
            }
            compose.runOnIdle {
                landscape = true
                glassPreference = true
                reduceTransparency = false
            }
            compose.waitForIdle()
            assertEquals(112f + 48f + 12f, inset, 0.01f)
            compose.runOnIdle {
                glassPreference = false
                reduceTransparency = false
            }
            compose.waitForIdle()
            assertEquals(112f + 48f, inset, 0.01f)
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
