package com.ljyh.mei.ui.tablet.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.TabletAnimationStyle
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.component.player.overlay.PlayerPanel
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Exercise actual player presentation with engine-backed parts supplied as inert slots. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1400dp-h1000dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class TabletPlayerContentTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun layoutsShareTheSameActionsAndKeepPanelIntentWhileTheWindowChanges() {
        var width by mutableStateOf(1280.dp)
        var height by mutableStateOf(800.dp)
        var state by mutableStateOf(TabletPlayerUiState("Song", "Artist", false, PlayerPanel.Lyrics, false,
            animation = TabletAnimationStyle.CROSSFADE))
        var likes = 0
        var collapsed = 0
        val actions = TabletPlayerActions(
            { collapsed++ }, {}, { likes++ }, {},
            { state = state.copy(panel = PlayerPanel.Lyrics, detailsVisible = false) },
        )
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(LocalAppWindowLayout provides AppWindowLayout.Wide) {
                    BackHandler { collapsed++ }
                    Box(Modifier.width(width).height(height)) {
                        TabletPlayerContent(state, actions, TabletPlayerSlots(
                            artwork = { Box(it.testTag("cover")) },
                            controls = { modifier, _ -> Column(modifier.height(200.dp).testTag("controls")) {
                                Button({ state = state.copy(panel = PlayerPanel.Queue, detailsVisible = true) }, Modifier.testTag("open-queue")) { Text("队列入口") }
                            } },
                            panel = { visible, modifier -> Box(modifier.testTag("panel-${visible.name}")) },
                            actions = { modifier -> Row(modifier.testTag("detail-actions")) {
                                Button({ state = state.copy(panel = PlayerPanel.Comments, detailsVisible = true) }) { Text("评论入口") }
                            } },
                        ))
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.onNodeWithText("队列").assertDoesNotExist()
            compose.onNodeWithText("评论").assertDoesNotExist()
            val cover = compose.onNodeWithTag("cover").fetchSemanticsNode().boundsInRoot
            val lyrics = compose.onNodeWithTag("panel-Lyrics").fetchSemanticsNode().boundsInRoot
            assertTrue(cover.right < lyrics.left)
            val leftPane = compose.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot
            val controls = compose.onNodeWithTag("controls").fetchSemanticsNode().boundsInRoot
            val title = compose.onNodeWithText("Song", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(leftPane.width, lyrics.width, 1f)
            assertEquals(cover.left, controls.left, 1f)
            assertEquals(cover.right, controls.right, 1f)
            assertEquals(cover.left, title.left, 1f)
            compose.onNodeWithContentDescription("喜欢").performClick()
            compose.runOnIdle { assertEquals(1, likes) }
            compose.onNodeWithTag("open-queue").performScrollTo().performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("panel-Queue").assertExists()
            compose.runOnIdle { width = 700.dp }
            compose.waitForIdle()
            compose.onNodeWithTag("panel-Queue").assertExists()
            compose.runOnIdle { activity.get().onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
            compose.onNodeWithTag("panel-Queue").assertDoesNotExist()
            compose.onNodeWithTag("controls").assertExists()
            compose.runOnIdle { height = 360.dp }
            compose.waitForIdle()
            compose.onNodeWithTag("open-queue").performScrollTo().performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("panel-Queue").assertExists()
            compose.onNodeWithTag("detail-actions").assertExists()
            compose.onNodeWithText("评论入口").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("panel-Comments").assertExists()
            compose.onNodeWithTag("controls").assertDoesNotExist()
            compose.runOnIdle { activity.get().onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
            compose.runOnIdle { activity.get().onBackPressedDispatcher.onBackPressed() }
            compose.runOnIdle { assertEquals(1, collapsed) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
    @Test fun everyAnimationCanMoveFromQueueToCommentsAndBackToLyrics() {
        var state by mutableStateOf(TabletPlayerUiState("Song", "Artist", false, PlayerPanel.Lyrics, false))
        val actions = TabletPlayerActions({}, {}, {}, {}, {})
        TabletContentTestActivity.content = {
            MaterialTheme {
                Box(Modifier.width(1280.dp).height(800.dp)) {
                    TabletPlayerContent(state, actions, TabletPlayerSlots(
                        artwork = { Box(it) }, controls = { modifier, _ -> Box(modifier.height(180.dp)) },
                        panel = { panel, modifier -> Box(modifier.testTag("visible-${panel.name}")) },
                        actions = {},
                    ))
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            for (animation in TabletAnimationStyle.entries) {
                compose.runOnIdle { state = state.copy(animation = animation) }
                compose.waitForIdle()
                for ((label, panel) in listOf("队列" to PlayerPanel.Queue, "评论" to PlayerPanel.Comments, "歌词" to PlayerPanel.Lyrics)) {
                    compose.runOnIdle { state = state.copy(panel = panel) }
                    compose.waitForIdle()
                    compose.onNodeWithTag("visible-${panel.name}").assertExists()
                }
            }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

}
