package com.ljyh.mei.ui.component.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.AccompanimentLyricTextSizeKey
import com.ljyh.mei.constants.NormalLyricTextBoldKey
import com.ljyh.mei.ui.screen.setting.component.LyricStylePreferences
import com.ljyh.mei.ui.component.player.sheet.LyricStyleSheet
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import com.ljyh.mei.utils.preferences.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w600dp-h900dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class PlayerLyricStyleTest {
    @Test fun everyLyricRoleFollowsPlayerInkInsteadOfAnOldWhiteTextStyle() {
        var foreground by mutableStateOf(Color.Black)
        var style: PlayerLyricStyle? = null
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(LocalPlayerForegroundColor provides foreground,
                    LocalTextStyle provides LocalTextStyle.current.copy(color = Color.White)) {
                    val current = rememberPlayerLyricStyle()
                    SideEffect { style = current }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.runOnIdle {
                val current = requireNotNull(style)
                assertEquals(Color.Black, current.main.color)
                assertEquals(Color.Black, current.translation.color)
                assertEquals(Color.Black, current.phonetic.color)
                foreground = Color.White
            }
            compose.runOnIdle {
                val current = requireNotNull(style)
                assertEquals(Color.White, current.main.color)
                assertEquals(Color.White, current.translation.color)
                assertEquals(Color.White, current.phonetic.color)
            }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun compactControlsLeaveRoomForLyricsAndUpdateEachSelectedPart() {
        val store = RuntimeEnvironment.getApplication().dataStore
        runBlocking { store.edit { it.clear() } }
        var style: PlayerLyricStyle? = null
        var density = 1f
        TabletContentTestActivity.content = {
            MaterialTheme {
                density = androidx.compose.ui.platform.LocalDensity.current.density
                val current = rememberPlayerLyricStyle()
                SideEffect { style = current }
                Box(Modifier.width(360.dp).testTag("editor")) { LyricStyleSheet() }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val bounds = compose.onNodeWithTag("editor").fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.height < 240f * density)
            compose.onNodeWithContentDescription("增大字号").performClick()
            compose.waitUntil { style?.main?.fontSize == 36.sp }
            compose.onNodeWithContentDescription("增大字号").assertIsNotEnabled()
            compose.onNodeWithText("翻译").performClick()
            compose.onNodeWithContentDescription("增大字号").performClick()
            compose.waitUntil { style?.translation?.fontSize == 22.sp }
            compose.onNodeWithText("显示翻译").performClick()
            compose.waitUntil { style?.showTranslation == false }
            compose.onNodeWithText("音译").performClick()
            compose.onNodeWithContentDescription("减小字号").performClick()
            compose.waitUntil { style?.phonetic?.fontSize == 18.sp }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
            runBlocking { store.edit { it.clear() } }
        }
    }

    @Test
    fun styleControlsUpdateTheRendererAndSurviveRecreatingTheContent() {
        val store = RuntimeEnvironment.getApplication().dataStore
        runBlocking { store.edit { it.clear() } }
        var style: PlayerLyricStyle? = null
        var generation by mutableIntStateOf(0)
        TabletContentTestActivity.content = {
            MaterialTheme {
                key(generation) {
                    val current = rememberPlayerLyricStyle()
                    SideEffect { style = current }
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        LyricStylePreferences()
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitUntil { style?.main?.fontSize == 34.sp }
            compose.onNodeWithText("主歌词字体加粗").performClick()
            compose.waitUntil { style?.main?.fontWeight == FontWeight.Normal }
            compose.onNodeWithText("翻译歌词字体大小").performScrollTo().performClick()
            compose.onNodeWithText("28").performClick()
            compose.waitUntil { style?.translation?.fontSize == 28.sp }
            compose.onNodeWithText("显示翻译").performScrollTo().performClick()
            compose.waitUntil { style?.showTranslation == false }
            compose.onNodeWithText("显示音译").performScrollTo().performClick()
            compose.waitUntil { style?.showPhonetic == false }
            compose.runOnIdle { generation++ }
            compose.waitUntil {
                style?.translation?.fontSize == 28.sp && style?.main?.fontWeight == FontWeight.Normal &&
                    style?.showTranslation == false && style?.showPhonetic == false
            }
            val saved = runBlocking { store.data.first() }
            assertEquals("Size28", saved[AccompanimentLyricTextSizeKey])
            assertEquals(false, saved[NormalLyricTextBoldKey])
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
            runBlocking { store.edit { it.clear() } }
        }
    }

    @Test
    fun previewCanPauseAndResumeWithoutAPlaybackConnection() {
        compose.mainClock.autoAdvance = false
        TabletContentTestActivity.content = { MaterialTheme { LyricStylePreview() } }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.onNodeWithContentDescription("暂停样式预览").performClick()
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            compose.onNodeWithContentDescription("播放样式预览").assertExists().performClick()
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            compose.onNodeWithContentDescription("暂停样式预览").assertExists()
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
