package com.ljyh.mei.ui.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ljyh.mei.constants.PageTransitionSpeed
import com.ljyh.mei.constants.PageTransitionStyle
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.sheet.collapsedAnchor
import com.ljyh.mei.ui.component.sheet.rememberBottomSheetState
import com.ljyh.mei.ui.motion.PageNavigationMotion
import com.ljyh.mei.ui.screen.Screen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Lifecycle/state assertions only: no screenshots, APK installation or device rendering review. */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class MeiShellStateRestorationTest {
    @get:Rule val compose = createEmptyComposeRule()
    class HostActivity : ComponentActivity() {
        companion object { var content: @Composable () -> Unit = {} }
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent { content() }
        }
    }
    private fun idle() = compose.waitForIdle()

    @Test
    fun movablePageKeepsCompositionAcrossShellMovesAndSavedValuesAcrossActivityRecreation() {
        var wide by mutableStateOf(false)
        var value = ""
        var transient = -1
        var identity: Any? = null
        var shownLayout = ""
        var edit: (String) -> Unit = {}
        HostActivity.content = {
            val host = rememberMeiPageHost {
                var text by rememberSaveable { mutableStateOf("initial") }
                var counter by remember { mutableIntStateOf(0) }
                val token = remember { Any() }
                val observedText = text
                val observedCounter = counter
                BasicText("$observedText / $observedCounter")
                SideEffect {
                    value = observedText; transient = observedCounter; identity = token
                    edit = { text = it; counter++ }
                }
            }
            val spec = MeiAppPagesSpec(rememberNavController(), "home",
                PageNavigationMotion(PageTransitionStyle.Slide, PageTransitionSpeed.Standard),
                TopAppBarDefaults.pinnedScrollBehavior(), SharedMeiPageContent, Modifier)
            if (wide) key("wide") {
                SideEffect { shownLayout = "wide" }
                host(spec)
            } else key("compact") {
                SideEffect { shownLayout = "compact" }
                host(spec)
            }
        }
        val controller = Robolectric.buildActivity(HostActivity::class.java).setup()
        try {
            idle()
            assertEquals("initial", value)
            val original = identity
            compose.runOnIdle { edit("draft") }; idle()
            assertEquals("draft", value)
            assertEquals(1, transient)
            compose.runOnIdle { wide = true }; idle()
            assertEquals("wide", shownLayout)
            assertSame(original, identity)
            assertEquals("draft", value)
            assertEquals(1, transient)
            controller.recreate(); idle()
            assertNotSame(original, identity)
            assertEquals("draft", value)
            assertEquals(0, transient)
        } finally {
            controller.pause().stop().destroy()
            HostActivity.content = {}
        }
    }

    @Test
    fun savedPageValuesRestoreWhenRecreatedDirectlyIntoTheOtherShell() {
        // Deliberately not snapshot state: the old composition must never enter the new branch.
        var wide = false
        var shownLayout = ""
        var value = ""
        var identity: Any? = null
        var edit: (String) -> Unit = {}
        HostActivity.content = {
            val host = rememberMeiPageHost {
                var text by rememberSaveable { mutableStateOf("initial") }
                val token = remember { Any() }
                val observed = text
                BasicText(observed)
                SideEffect {
                    value = observed
                    identity = token
                    edit = { text = it }
                }
            }
            val spec = MeiAppPagesSpec(rememberNavController(), "home",
                PageNavigationMotion(PageTransitionStyle.Slide, PageTransitionSpeed.Standard),
                TopAppBarDefaults.pinnedScrollBehavior(), SharedMeiPageContent, Modifier)
            if (wide) key("wide") {
                SideEffect { shownLayout = "wide" }
                host(spec)
            } else key("compact") {
                SideEffect { shownLayout = "compact" }
                host(spec)
            }
        }
        val controller = Robolectric.buildActivity(HostActivity::class.java).setup()
        try {
            idle()
            val original = identity
            compose.runOnIdle { edit("restored draft") }; idle()
            assertEquals("restored draft", value)
            wide = true
            assertEquals("compact", shownLayout)
            controller.recreate(); idle()
            assertEquals("wide", shownLayout)
            assertNotSame(original, identity)
            assertEquals("restored draft", value)
        } finally {
            controller.pause().stop().destroy()
            HostActivity.content = {}
        }
    }

    @Test
    fun productionPageHostKeepsDetailStateAndBackStackAcrossShellMovesAndRecreation() {
        var wide by mutableStateOf(false)
        var nav: NavHostController? = null
        var value = ""
        var playlistId = -1L
        var edit: (String) -> Unit = {}
        fun pages() = object : MeiPageContent by SharedMeiPageContent {
            @Composable override fun Home(scrollBehavior: TopAppBarScrollBehavior) {
                BasicText("home")
            }
            @Composable override fun Playlist(id: Long) {
                var text by rememberSaveable { mutableStateOf("initial") }
                val observed = text
                BasicText(observed)
                SideEffect { playlistId = id; value = observed; edit = { text = it } }
            }
        }
        val compactPages = pages()
        val widePages = pages()
        HostActivity.content = {
            val controller = rememberNavController()
            SideEffect { nav = controller }
            val host = rememberMeiPageHost()
            val spec = MeiAppPagesSpec(controller, Screen.Home.route,
                PageNavigationMotion(PageTransitionStyle.Slide, PageTransitionSpeed.Standard),
                TopAppBarDefaults.pinnedScrollBehavior(), if (wide) widePages else compactPages, Modifier)
            if (wide) key("wide") { host(spec) }
            else key("compact") { host(spec) }
        }
        val activity = Robolectric.buildActivity(HostActivity::class.java).setup()
        try {
            idle()
            compose.runOnIdle { nav!!.navigate("${Screen.PlayList.route}/42") }; idle()
            assertEquals(42L, playlistId)
            compose.runOnIdle { edit("detail draft") }; idle()
            assertEquals("detail draft", value)
            val entry = nav!!.currentBackStackEntry!!.id
            compose.runOnIdle { wide = true }; idle()
            assertEquals(entry, nav!!.currentBackStackEntry!!.id)
            assertEquals("detail draft", value)
            activity.recreate(); idle()
            assertEquals(entry, nav!!.currentBackStackEntry!!.id)
            assertEquals(42L, playlistId)
            assertEquals("detail draft", value)
            compose.runOnIdle { assertTrue(nav!!.popBackStack()) }; idle()
            assertEquals(Screen.Home.route, nav!!.currentDestination!!.route)
        } finally {
            activity.pause().stop().destroy()
            HostActivity.content = {}
        }
    }

    @Test
    fun sharedSheetAnchorKeepsExpandedStateAcrossShellMovesAndActivityRecreation() {
        var wide by mutableStateOf(false)
        var sheet: BottomSheetState? = null
        HostActivity.content = {
            val anchor = rememberSaveable { mutableIntStateOf(collapsedAnchor) }
            if (wide) key("wide") {
                val current = rememberBottomSheetState(0.dp, 800.dp, 64.dp, savedAnchor = anchor)
                SideEffect { sheet = current }
            } else key("compact") {
                val current = rememberBottomSheetState(0.dp, 800.dp, 64.dp, savedAnchor = anchor)
                SideEffect { sheet = current }
            }
        }
        val controller = Robolectric.buildActivity(HostActivity::class.java).setup()
        try {
            idle()
            assertTrue(sheet!!.isCollapsed)
            compose.runOnIdle { sheet!!.expandSoft() }; idle()
            assertTrue(sheet!!.isExpanded)
            val compactSheet = sheet
            compose.runOnIdle { wide = true }; idle()
            assertNotSame(compactSheet, sheet)
            assertTrue(sheet!!.isExpanded)
            controller.recreate(); idle()
            assertTrue(sheet!!.isExpanded)
            compose.runOnIdle { sheet!!.collapseSoft() }; idle()
            compose.runOnIdle { wide = false }; idle()
            assertTrue(sheet!!.isCollapsed)
        } finally {
            controller.pause().stop().destroy()
            HostActivity.content = {}
        }
    }
}
