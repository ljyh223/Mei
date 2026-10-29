package com.ljyh.mei.ui.component.glass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Layout/state checks with empty boxes, without screenshots, image requests or optical rendering. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1000dp-h700dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class MeiArtworkTopBarStateTest {
    @get:Rule val compose = createEmptyComposeRule()

    class HostActivity : ComponentActivity() {
        companion object { var content: @Composable () -> Unit = {} }
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent { content() }
        }
    }

    @Test
    fun imageBoundaryControlsBackgroundAcrossReverseScrollResizeAndActivityRecreation() {
        var heroHeight by mutableStateOf(360.dp)
        var glass by mutableStateOf(false)
        var heroHeightPx = 0
        var toolbarHeightPx = 0
        var passed = false
        var mode = MeiTopBarBackground.Transparent
        var list: LazyListState? = null
        var scope: CoroutineScope? = null
        HostActivity.content = {
            val currentList = rememberLazyListState()
            val chrome = rememberMeiArtworkTopBarState(currentList, "hero")
            val currentScope = rememberCoroutineScope()
            val density = LocalDensity.current
            val measuredHeroHeight = heroHeight
            CompositionLocalProvider(LocalMeiGlassEnabled provides glass) {
                val background = meiArtworkTopBarBackground(chrome.artworkPassed)
                SideEffect {
                    heroHeightPx = with(density) { measuredHeroHeight.roundToPx() }
                    toolbarHeightPx = with(density) { 80.dp.roundToPx() }
                    passed = chrome.artworkPassed
                    mode = background
                    list = currentList
                    scope = currentScope
                }
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(state = currentList, modifier = Modifier.fillMaxSize()) {
                        item(key = "hero") {
                            Column {
                                Box(Modifier.fillMaxWidth().height(measuredHeroHeight).then(chrome.artworkModifier))
                                // Metadata belongs to the same list item but is outside the image.
                                Box(Modifier.fillMaxWidth().height(160.dp))
                            }
                        }
                        items(50) { Box(Modifier.fillMaxWidth().height(40.dp)) }
                    }
                    Box(Modifier.fillMaxWidth().height(80.dp).then(chrome.toolbarModifier))
                }
            }
        }
        fun scroll(index: Int, offset: Int = 0) {
            compose.runOnIdle { scope!!.launch { list!!.scrollToItem(index, offset) } }
            compose.waitForIdle()
        }
        val activity = Robolectric.buildActivity(HostActivity::class.java).setup()
        try {
            compose.waitForIdle()
            assertFalse(passed)
            assertEquals(MeiTopBarBackground.Transparent, mode)
            val oldBoundary = heroHeightPx - toolbarHeightPx
            scroll(0, oldBoundary - 1)
            assertFalse(passed)
            assertEquals(MeiTopBarBackground.Transparent, mode)
            scroll(0, oldBoundary)
            assertTrue(passed)
            assertEquals("Switch at the image edge, while metadata is still visible", 0, list!!.firstVisibleItemIndex)
            assertEquals(MeiTopBarBackground.Solid, mode)
            scroll(0)
            assertFalse(passed)
            assertEquals(MeiTopBarBackground.Transparent, mode)
            compose.runOnIdle { heroHeight = 520.dp }
            compose.waitForIdle()
            scroll(0, oldBoundary)
            assertFalse("A resized image must not reuse the old threshold", passed)
            scroll(0, heroHeightPx - toolbarHeightPx)
            assertTrue("Image=$heroHeightPx toolbar=$toolbarHeightPx offset=${list!!.firstVisibleItemScrollOffset}", passed)
            assertEquals(MeiTopBarBackground.Solid, mode)
            compose.runOnIdle { glass = true }
            compose.waitForIdle()
            assertEquals(MeiTopBarBackground.Solid, mode)
            scroll(0)
            assertFalse(passed)
            assertEquals(MeiTopBarBackground.Auto, mode)
            scroll(2)
            assertTrue(passed)
            activity.recreate()
            compose.waitForIdle()
            assertTrue("Restoring below an uncomposed hero still needs a solid toolbar", passed)
            assertEquals(MeiTopBarBackground.Solid, mode)
            scroll(0)
            assertFalse(passed)
            assertEquals(MeiTopBarBackground.Auto, mode)
        } finally {
            activity.pause().stop().destroy()
            HostActivity.content = {}
        }
    }
}
