package com.ljyh.mei.ui.tablet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.setting.SplitSettingsFrame
import com.ljyh.mei.ui.screen.setting.SplitSettingsHost
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.ui.screen.setting.selectSettingsCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Checks composition state without installing an APK or performing visual verification. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class TabletSettingsFrameTest {
    @get:Rule val compose = createEmptyComposeRule()
    class HostActivity : ComponentActivity() {
        companion object { var content: @Composable () -> Unit = {} }
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent { content() }
        }
    }

    @Test
    fun categoryNavigationKeepsActualSidebarScrollPosition() {
        var nav: NavHostController? = null
        HostActivity.content = {
            val controller = rememberNavController()
            val graph = remember(controller) {
                controller.createGraph(startDestination = Screen.Setting.route) {
                    composable(Screen.Setting.route) { }
                    composable(Screen.CacheSettings.route) { }
                    composable(Screen.AppearanceSettings.route) { }
                }
            }
            LaunchedEffect(controller, graph) { controller.graph = graph }
            SideEffect { nav = controller }
            MaterialTheme {
                CompositionLocalProvider(LocalPlayerAwareWindowInsets provides WindowInsets(0)) {
                    SplitSettingsHost(controller, Modifier.fillMaxWidth().height(300.dp)) { }
                }
            }
        }
        val activity = Robolectric.buildActivity(HostActivity::class.java).setup()
        try {
            compose.waitForIdle()
            val sidebar = compose.onNode(hasScrollAction())
            sidebar.performScrollToNode(hasText("缓存"))
            val before = sidebar.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
            org.junit.Assert.assertTrue(before > 0f)
            compose.runOnIdle { nav!!.selectSettingsCategory(Screen.CacheSettings.route) }
            compose.waitForIdle()
            val after = sidebar.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
            assertEquals(before, after, 0f)
            assertEquals(Screen.CacheSettings.route, nav!!.currentDestination!!.route)
        } finally {
            activity.pause().stop().destroy()
            HostActivity.content = {}
        }
    }

    @Test
    @Config(qualifiers = "w780dp-h420dp")
    fun phoneLandscapeShowsCategoriesAndDetailSideBySide() {
        lateinit var nav: NavHostController
        HostActivity.content = {
            val controller = rememberNavController()
            val graph = remember(controller) {
                controller.createGraph(startDestination = Screen.Setting.route) {
                    composable(Screen.Setting.route) {}
                    composable(Screen.AppearanceSettings.route) {}
                    composable(Screen.GlassSettings.route) {}
                }
            }
            LaunchedEffect(controller, graph) { controller.graph = graph }
            SideEffect { nav = controller }
            MaterialTheme {
                CompositionLocalProvider(
                    LocalAppWindowLayout provides AppWindowLayout.Compact,
                    LocalPhoneLandscape provides true,
                    LocalPageStartInset provides 72.dp,
                    LocalPlayerAwareWindowInsets provides WindowInsets(0),
                ) {
                    SplitSettingsHost(controller, Modifier.width(740.dp).height(330.dp)) {
                        val entry by controller.currentBackStackEntryAsState()
                        val name = when (entry?.destination?.route) {
                            Screen.GlassSettings.route -> "玻璃详情"
                            Screen.AppearanceSettings.route -> "外观详情"
                            else -> "播放详情"
                        }
                        MeiPinnedPage(title = name, onNavigateBack = controller::navigateUp) { insets ->
                            Column(Modifier.verticalScroll(LocalMeiPageScroll.current!!).padding(insets)) {
                                MeiLargeTitle(name)
                                PreferenceSection("配置") { BasicText("设置项") }
                            }
                        }
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(HostActivity::class.java).setup()
        try {
            val playingCategory = compose.onNodeWithText("播放体验").assertIsDisplayed().assertIsSelected()
            val playingDetail = compose.onNodeWithText("播放详情").assertIsDisplayed()
            val detail = playingDetail.fetchSemanticsNode().boundsInRoot
            assertTrue(playingCategory.fetchSemanticsNode().boundsInRoot.right < detail.left)
            val categoryTitle = compose.onNodeWithText("设置").fetchSemanticsNode().boundsInRoot
            assertEquals(categoryTitle.top, detail.top, 1f)
            val categoriesHeading = compose.onNodeWithText("播放与界面").fetchSemanticsNode().boundsInRoot
            val detailHeading = compose.onNodeWithText("配置").fetchSemanticsNode().boundsInRoot
            assertEquals(categoriesHeading.top, detailHeading.top, 1f)
            val root = nav.currentBackStackEntry
            compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("播放体验"))).performScrollToNode(hasText("外观"))
            compose.onNodeWithText("外观").performClick().assertIsSelected()
            compose.onNodeWithText("外观详情").assertIsDisplayed()
            compose.onNodeWithContentDescription("返回").assertDoesNotExist()
            assertSame(root, nav.getBackStackEntry(Screen.Setting.route))
            compose.runOnIdle { nav.navigate(Screen.GlassSettings.route) }
            compose.onAllNodesWithText("玻璃详情").assertCountEquals(1)
            compose.onNodeWithContentDescription("返回").assertIsDisplayed().performClick()
            compose.onNodeWithText("外观详情").assertIsDisplayed()
        } finally {
            activity.pause().stop().destroy()
            HostActivity.content = {}
        }
    }

    @Test
    fun changingDetailKeepsSidebarCompositionAndState() {
        var detail by mutableStateOf("播放体验")
        var sidebarIdentity: Any? = null
        var sidebarState = ""
        var setSidebarState: (String) -> Unit = {}
        var displayedDetail = ""
        HostActivity.content = {
            SplitSettingsFrame(showSidebar = true, sidebar = {
                val identity = remember { Any() }
                var state by remember { mutableStateOf("初始") }
                BasicText(state)
                SideEffect {
                    sidebarIdentity = identity
                    sidebarState = state
                    setSidebarState = { state = it }
                }
            }) {
                BasicText(detail)
                SideEffect { displayedDetail = detail }
            }
        }
        val activity = Robolectric.buildActivity(HostActivity::class.java).setup()
        try {
            compose.waitForIdle()
            val initialIdentity = sidebarIdentity
            compose.runOnIdle { setSidebarState("已滚动"); detail = "外观" }
            compose.waitForIdle()
            assertSame(initialIdentity, sidebarIdentity)
            assertEquals("已滚动", sidebarState)
            assertEquals("外观", displayedDetail)
            compose.runOnIdle { detail = "缓存" }
            compose.waitForIdle()
            assertSame(initialIdentity, sidebarIdentity)
            assertEquals("已滚动", sidebarState)
            assertEquals("缓存", displayedDetail)
        } finally {
            activity.pause().stop().destroy()
            HostActivity.content = {}
        }
    }
}
