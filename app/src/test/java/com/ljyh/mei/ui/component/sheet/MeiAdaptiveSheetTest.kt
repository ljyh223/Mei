package com.ljyh.mei.ui.component.sheet

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.domain.DiscoverCategory
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.screen.main.findmusic.DiscoverCategorySheet
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1200dp-h900dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class MeiAdaptiveSheetTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    @Config(qualifiers = "w800dp-h360dp")
    fun cachedMembershipIsAnInformationalHintAndDoesNotDisableSelectingThePlaylist() {
        val playlist = Playlist("123", "已缓存的歌单", "", "9", "", "", 2)
        var selected = false
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalAppWindowLayout provides AppWindowLayout.Compact,
                    LocalPhoneLandscape provides true,
                ) {
                    AddToPlaylistSheet(
                        playlists = listOf(playlist),
                        containingPlaylistIds = setOf(playlist.id),
                        onDismiss = {},
                        onSelectPlaylist = { selected = true },
                        onCreateNewPlaylist = {},
                    )
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.onNodeWithText("2首歌曲 · 已在歌单中").assertIsDisplayed()
            compose.onNode(hasText(playlist.title) and hasClickAction()).assertIsEnabled().performClick()
            compose.runOnIdle { assertTrue(selected) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun compactSideContentWrapsItsControlsAtTheBottom() {
        var compact by mutableStateOf(true)
        var density = 1f
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = LocalDensity.current.density
                CompositionLocalProvider(LocalAppWindowLayout provides AppWindowLayout.Wide) {
                    MeiAdaptiveSheet(
                        onDismissRequest = {},
                        modifier = Modifier.testTag("compact-panel"),
                        compactContent = compact,
                        compactTitle = "歌词样式",
                    ) {
                        Box(Modifier.fillMaxWidth().height(160.dp))
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val panel = compose.onNodeWithTag("compact-panel").fetchSemanticsNode().boundsInRoot
            assertTrue(panel.height < 240f * density)
            assertTrue(panel.top > 600f * density)
            compose.onNodeWithText("歌词样式").assertExists()
            compose.runOnIdle { compact = false }
            val regular = compose.onNodeWithTag("compact-panel").fetchSemanticsNode().boundsInRoot
            assertTrue(regular.height > 500f * density)
            assertTrue(regular.top < panel.top)
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun contentMovesBetweenContainersWithoutLosingDraftsOrRestartingWork() {
        var layout by mutableStateOf(AppWindowLayout.Wide)
        var starts = 0
        var density = 1f
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = LocalDensity.current.density
                CompositionLocalProvider(LocalAppWindowLayout provides layout) {
                    MeiAdaptiveSheet(onDismissRequest = {}) { close ->
                        var draft by rememberSaveable { mutableStateOf("") }
                        var clicks by remember { mutableIntStateOf(0) }
                        LaunchedEffect(Unit) { starts++ }
                        Column(Modifier.fillMaxWidth().height(meiSheetContentHeight(.4f, 320.dp)).testTag("content")) {
                            TextField(draft, { draft = it }, Modifier.testTag("draft"))
                            Button({ clicks++ }) { Text("计数 $clicks") }
                            Button(close) { Text("完成") }
                        }
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            val side = compose.onNodeWithTag("content").fetchSemanticsNode().boundsInRoot
            assertEquals(420f * density, side.width, density)
            assertTrue(side.left > 600f * density)
            assertTrue(side.height > 500f * density)
            compose.onNodeWithTag("draft").performTextInput("我的歌单")
            compose.onNodeWithText("计数 0").performClick()
            compose.runOnIdle { layout = AppWindowLayout.Compact }
            compose.waitForIdle()
            val bottom = compose.onNodeWithTag("content").fetchSemanticsNode().boundsInRoot
            assertTrue(bottom.height <= 320f * density + 1f)
            compose.onNodeWithTag("draft").assertTextContains("我的歌单")
            compose.onNodeWithText("计数 1").assertExists()
            compose.runOnIdle { layout = AppWindowLayout.Wide }
            compose.waitForIdle()
            compose.onNodeWithTag("draft").assertTextContains("我的歌单")
            compose.onNodeWithText("计数 1").assertExists()
            compose.runOnIdle { assertEquals(1, starts) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun editingAndNestedNavigationHandleBackBeforeClosingTheModal() {
        var editing by mutableStateOf(true)
        var depth by mutableIntStateOf(1)
        var closed = 0
        var open by mutableStateOf(true)
        var dispatcher: OnBackPressedDispatcher? = null
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(LocalAppWindowLayout provides AppWindowLayout.Wide) {
                    if (open) MeiAdaptiveSheet(
                        onDismissRequest = { closed++; open = false },
                        dismissEnabled = !editing,
                        onDismissBlocked = { editing = false },
                        onNavigateBack = if (depth > 0) ({ depth-- }) else null,
                    ) {
                        dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
                        Text("当前层 $depth")
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.runOnIdle { dispatcher!!.onBackPressed() }
            compose.waitForIdle()
            compose.runOnIdle { assertFalse(editing); assertEquals(1, depth); assertEquals(0, closed) }
            compose.runOnIdle { dispatcher!!.onBackPressed() }
            compose.waitForIdle()
            compose.onNodeWithText("当前层 0").assertExists()
            compose.runOnIdle { assertEquals(0, closed) }
            compose.onNodeWithContentDescription("关闭弹层").performClick()
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(1, closed) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun scrimAndOutwardSwipeCloseTheSideModalButInnerTapsDoNot() {
        var open by mutableStateOf(true)
        var closed = 0
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(LocalAppWindowLayout provides AppWindowLayout.Wide) {
                    if (open) MeiAdaptiveSheet(onDismissRequest = { closed++; open = false }) {
                        Column(Modifier.fillMaxWidth().height(meiSheetContentHeight()).testTag("body")) {
                            Text("面板内容")
                        }
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.onNodeWithTag("body").performTouchInput { click(center) }
            compose.runOnIdle { assertEquals(0, closed) }
            compose.onNodeWithTag("body").performTouchInput { swipeRight() }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(1, closed); open = true }
            compose.waitForIdle()
            compose.onNode(isDialog()).performTouchInput { click(Offset(12f, 250f)) }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(2, closed) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun bottomContainerSupportsBothVerticalDismissAndBack() {
        var open by mutableStateOf(true)
        var closed = 0
        var dispatcher: OnBackPressedDispatcher? = null
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(LocalAppWindowLayout provides AppWindowLayout.Compact) {
                    if (open) MeiAdaptiveSheet(onDismissRequest = { closed++; open = false }) {
                        dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
                        Box(Modifier.fillMaxWidth().height(300.dp).testTag("bottom-body")) { Text("底部内容") }
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.onNodeWithTag("bottom-body").performTouchInput { swipeDown() }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(1, closed); open = true }
            compose.waitForIdle()
            compose.runOnIdle { dispatcher!!.onBackPressed() }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(2, closed) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test
    @Config(qualifiers = "w1200dp-h400dp")
    fun createPlaylistUsesThePanelViewportAndKeepsTheSubmitActionReachable() {
        var confirmed = ""
        var cancelled = 0
        var open by mutableStateOf(true)
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(LocalAppWindowLayout provides AppWindowLayout.Wide) {
                    if (open) CreatePlaylistSheet(
                        onDismiss = { cancelled++; open = false },
                        onConfirm = { text, _ -> confirmed = text },
                    )
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.onNode(hasSetTextAction()).performTextInput("新的歌单")
            compose.onNodeWithText("完成").performScrollTo().assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals("新的歌单", confirmed) }
            compose.onNodeWithText("取消").performScrollTo()
            compose.mainClock.autoAdvance = false
            compose.onNodeWithText("取消").performClick()
            compose.runOnIdle { assertEquals(0, cancelled) }
            compose.mainClock.advanceTimeBy(1000)
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(1, cancelled) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test
    @Config(qualifiers = "w560dp-h300dp")
    fun ordinaryPhoneLandscapeSheetMovesToTheSideOnAShortWindow() {
        var density = 1f
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = LocalDensity.current.density
                CompositionLocalProvider(
                    LocalAppWindowLayout provides AppWindowLayout.Compact,
                    LocalPhoneLandscape provides true,
                ) {
                    MeiAdaptiveSheet(onDismissRequest = {}) {
                        Box(Modifier.fillMaxSize().testTag("page-sheet"))
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val window = compose.onNode(isDialog()).fetchSemanticsNode().boundsInRoot
            val panel = compose.onNodeWithTag("page-sheet").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertEquals(352f * density, panel.width, density)
            assertTrue(panel.left > window.left + 48f * density)
            assertTrue(panel.right < window.right)
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp")
    fun addToPlaylistScrollsTheSearchAwayAndShowsFourFullRowsOnPhoneLandscape() {
        val playlists = (1..12).map { index ->
            val id = index.toString().padStart(2, '0')
            Playlist(id, "测试歌单 $id", "", "", "", "", 12)
        }
        var density = 1f
        var selected: Playlist? = null
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = LocalDensity.current.density
                CompositionLocalProvider(
                    LocalAppWindowLayout provides AppWindowLayout.Compact,
                    LocalPhoneLandscape provides true,
                ) {
                    MeiAdaptiveSheet(
                        onDismissRequest = {},
                        onNavigateBack = {},
                        compactTitle = AddToPlaylistTitle,
                    ) { close ->
                        AddToPlaylistSheet(
                            playlists = playlists,
                            onDismiss = close,
                            onSelectPlaylist = { selected = it },
                            onCreateNewPlaylist = {},
                            embedded = true,
                        )
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val title = compose.onNodeWithText(AddToPlaylistTitle).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val close = compose.onNodeWithContentDescription("关闭弹层").fetchSemanticsNode().boundsInRoot
            assertEquals(close.center.y, title.center.y, 1f)
            val list = compose.onNode(hasScrollToIndexAction())
            list.performScrollToIndex(2)
            compose.onNode(hasSetTextAction()).assertIsNotDisplayed()
            val viewport = list.fetchSemanticsNode().boundsInRoot
            val visibleRows = playlists.take(4).map { playlist ->
                compose.onNode(hasClickAction() and hasText(playlist.title))
                    .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            }
            assertTrue("rows=$visibleRows, viewport=$viewport, density=$density", visibleRows.all { row ->
                row.height >= 48f * density - 1f &&
                    row.top >= viewport.top - 1f && row.bottom <= viewport.bottom + 1f
            })
            compose.onNodeWithText(playlists[3].title).performClick()
            compose.runOnIdle { assertEquals(playlists[3], selected) }

            list.performScrollToIndex(0)
            compose.onNode(hasSetTextAction()).assertIsDisplayed().performTextInput("测试歌单 07")
            compose.onNode(hasText("测试歌单 07") and hasClickAction() and !hasSetTextAction()).assertIsDisplayed()
            compose.onNodeWithText("测试歌单 01").assertDoesNotExist()
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test
    @Config(qualifiers = "w560dp-h300dp")
    fun selectingDiscoveryCategoryClosesTheSidePanelBeforeApplyingTheSelection() {
        var open by mutableStateOf(true)
        val events = mutableListOf<String>()
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalAppWindowLayout provides AppWindowLayout.Compact,
                    LocalPhoneLandscape provides true,
                ) {
                    if (open) DiscoverCategorySheet(
                        categories = Resource.Success(listOf(DiscoverCategory("摇滚", "风格"))),
                        selectedCategory = "全部",
                        onDismiss = {
                            events += "dismissed"
                            open = false
                        },
                        onCategorySelected = { events += it },
                        onRetry = {},
                    )
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val window = compose.onNode(isDialog()).fetchSemanticsNode().boundsInRoot
            val title = compose.onNodeWithText("歌单分类").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(title.left > window.left + 80f)
            compose.mainClock.autoAdvance = false
            compose.onNodeWithText("摇滚").assertIsDisplayed().performClick()
            compose.runOnIdle { assertTrue(events.isEmpty()) }
            compose.mainClock.advanceTimeBy(1000)
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(listOf("dismissed", "摇滚"), events) }
            compose.onNodeWithText("歌单分类").assertDoesNotExist()
        } finally {
            compose.mainClock.autoAdvance = true
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun phonesAndNarrowOrLargeTextWindowsRetainTheBottomPresentation() {
        assertEquals(MeiSheetPresentation.Bottom, meiSheetPresentation(1200f, 1f, false))
        assertEquals(MeiSheetPresentation.Bottom, meiSheetPresentation(599f, 1f, true))
        assertEquals(MeiSheetPresentation.Side, meiSheetPresentation(600f, 1f, true))
        assertEquals(MeiSheetPresentation.Bottom, meiSheetPresentation(650f, 1.5f, true))
        assertEquals(MeiSheetPresentation.Bottom, meiSheetPresentation(Float.NaN, 1f, true))
        assertEquals(MeiSheetPresentation.Side, meiSheetPresentation(480f, 1f, true, minSideWidthDp = 480f))
        assertEquals(MeiSheetPresentation.Bottom, meiSheetPresentation(479f, 1f, true, minSideWidthDp = 480f))
        assertEquals(MeiSheetPresentation.Bottom, meiSheetPresentation(560f, 1.5f, true, minSideWidthDp = 480f))
    }
}
