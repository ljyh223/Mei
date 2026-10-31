package com.ljyh.mei.ui.component.player.controls

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.ljyh.mei.constants.PlayerActionKey
import com.ljyh.mei.constants.TabletPlayerActionKey
import com.ljyh.mei.utils.preferences.dataStore
import com.ljyh.mei.utils.preferences.get
import com.ljyh.mei.ui.component.player.sheet.PlayerActionSettingsSheet
import kotlinx.coroutines.runBlocking
import com.ljyh.mei.playback.PlayMode
import com.ljyh.mei.ui.component.player.sheet.PlayerActionSettingsContent
import com.ljyh.mei.ui.model.PlayerAction
import com.ljyh.mei.ui.model.PlayerActionProfile
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
@Config(sdk = [36], qualifiers = "w600dp-h1000dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class PlayerActionCustomizationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun selectedPanelsAndDisabledSharingKeepStableToolbarSlots() {
        var selected by mutableStateOf<PlayerAction?>(null)
        var canShare by mutableStateOf(false)
        val actions = listOf(PlayerAction.QUEUE, PlayerAction.COMMENTS, PlayerAction.SHARE)
        TabletContentTestActivity.content = {
            MaterialTheme {
                Column(Modifier.width(320.dp)) {
                    PlayerActionRow(actions, PlayMode.REPEAT_MODE_ALL, null, selected, true, canShare,
                        onAction = { selected = if (selected == it) null else it }, foreground = Color.White)
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.onNodeWithContentDescription("分享").assertIsNotEnabled()
            val bounds = compose.onNodeWithContentDescription("分享").fetchSemanticsNode().boundsInRoot
            compose.onNodeWithContentDescription("播放队列").performClick().assertIsSelected()
            compose.onNodeWithContentDescription("查看评论").performClick().assertIsSelected()
            compose.onNodeWithContentDescription("播放队列").assertIsNotSelected()
            compose.onNodeWithContentDescription("查看评论").performClick().assertIsNotSelected()
            compose.runOnIdle { canShare = true }
            compose.onNodeWithContentDescription("分享").assertIsEnabled()
            assertEquals(bounds, compose.onNodeWithContentDescription("分享").fetchSemanticsNode().boundsInRoot)
            compose.onNodeWithContentDescription("歌词显示").assertDoesNotExist()
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun addingRemovingReorderingAndResettingAllUpdateTheLiveConfiguration() {
        val profile = PlayerActionProfile.Tablet
        var actions by mutableStateOf(profile.defaults)
        TabletContentTestActivity.content = {
            MaterialTheme {
                Box(Modifier.width(360.dp).height(900.dp)) {
                    PlayerActionSettingsContent(profile, actions) { actions = it }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.onNodeWithText("分享").performScrollTo().performClick()
            compose.runOnIdle { assertTrue(PlayerAction.SHARE in actions) }
            compose.onNodeWithContentDescription("移除分享").performScrollTo().performClick()
            compose.runOnIdle { assertFalse(PlayerAction.SHARE in actions) }
            compose.onNodeWithText("分享").performScrollTo().assertIsEnabled()
            compose.onNodeWithContentDescription("前移查看评论").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(listOf(PlayerAction.PLAY_MODE, PlayerAction.COMMENTS, PlayerAction.QUEUE), actions) }
            compose.onNodeWithContentDescription("前移查看评论").assertIsNotEnabled()
            compose.onNodeWithContentDescription("移除播放模式").assertDoesNotExist()
            compose.onNodeWithText("恢复默认").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(profile.defaults, actions) }
            compose.onNodeWithText("分享").performScrollTo().performClick()
            compose.onNodeWithText("下载").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(5, actions.size) }
            compose.onNodeWithText("歌曲信息").performScrollTo().assertIsNotEnabled()
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
    @Test fun tabletChangesPersistWithoutOverwritingThePhoneArrangement() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            context.dataStore.edit {
                it[PlayerActionKey] = "more,sleep"
                it[TabletPlayerActionKey] = ""
            }
        }
        var visible by mutableStateOf(true)
        TabletContentTestActivity.content = {
            MaterialTheme {
                if (visible) Box(Modifier.width(360.dp).height(900.dp)) {
                    PlayerActionSettingsSheet({}, embedded = true, profile = PlayerActionProfile.Tablet)
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.onNodeWithText("分享").performScrollTo().performClick()
            compose.waitUntil(5000) { context.dataStore[TabletPlayerActionKey] == "mode,queue,comment,share" }
            assertEquals("more,sleep", context.dataStore[PlayerActionKey])
            compose.runOnIdle { visible = false }
            compose.waitForIdle()
            compose.runOnIdle { visible = true }
            compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("移除分享").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithContentDescription("移除分享").performScrollTo().assertExists()
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

}
