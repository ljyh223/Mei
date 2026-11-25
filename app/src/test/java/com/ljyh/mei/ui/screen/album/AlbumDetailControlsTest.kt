package com.ljyh.mei.ui.screen.album

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.AlbumArtworkMode
import com.ljyh.mei.constants.AlbumModelMode
import com.ljyh.mei.data.model.domain.AlbumOverview
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w400dp-h900dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class AlbumDetailControlsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var controller: org.robolectric.android.controller.ActivityController<TabletContentTestActivity>? = null
    private fun show(content: @Composable () -> Unit) {
        TabletContentTestActivity.content = {
            MaterialTheme { CompositionLocalProvider(LocalMeiGlassEnabled provides false) {
                Box(Modifier.width(360.dp), content = { content() })
            } }
        }
        controller = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
    }
    @After fun finish() {
        controller?.pause()?.stop()?.destroy()
        TabletContentTestActivity.content = {}
    }
    private fun state() = AlbumDetailUiState(
        AlbumOverview(1, "测试专辑", "", "", 0, emptyList()), null, emptyList(), emptyList(),
        false, false, true, true, "", false,
    )
    private fun actions() = AlbumDetailActions({}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> })

    @Test fun heroFadeAndToolbarBoundaryFollowLongTitlesWithoutCoveringTheImage() {
        var expanded by mutableStateOf(false)
        var identityStart = 0f
        show {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, if (expanded) 1.8f else 1f)) {
                AlbumCoverHero(
                    minimumHeight = 400.dp,
                    artworkModifier = Modifier.testTag("imageBoundary"),
                    modifier = Modifier.testTag("hero"),
                    artwork = { start ->
                        SideEffect { identityStart = start }
                        Box(Modifier.fillMaxSize().testTag("image"))
                    },
                    identity = {
                        Column(Modifier.fillMaxWidth().testTag("identity")) {
                            androidx.compose.material3.Text(
                                if (expanded) "you seem pretty sad for a girl so in love" else "Lover",
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 3,
                            )
                            Spacer(Modifier.height(80.dp))
                        }
                    },
                )
            }
        }
        fun assertContinuousBoundary(): Float {
            val image = compose.onNodeWithTag("image").fetchSemanticsNode().boundsInRoot
            val text = compose.onNodeWithTag("identity").fetchSemanticsNode().boundsInRoot
            val boundary = compose.onNodeWithTag("imageBoundary").fetchSemanticsNode().boundsInRoot
            assertEquals("Fade anchor follows the measured identity", text.top, image.top + image.height * identityStart, 1f)
            assertEquals("Toolbar tracks image edge rather than metadata bottom", text.top, boundary.bottom, 1f)
            assertEquals(image.bottom, text.bottom, 1f)
            assertTrue("Large text must leave a useful image area", text.top - image.top >= image.width * .75f - 1f)
            return text.height
        }
        val shortHeight = assertContinuousBoundary()
        compose.runOnIdle { expanded = true }
        compose.waitForIdle()
        val longHeight = assertContinuousBoundary()
        assertTrue("Regression must exercise taller, wrapped metadata", longHeight > shortHeight)
    }

    @Test fun motionPaletteOverridesTheLightAppThemeAndToolbarForeground() {
        val cover = com.ljyh.mei.data.repository.DynamicCover(
            1, "", com.ljyh.mei.data.repository.DynamicCover.Source.APPLE_MUSIC, "album-test",
            com.ljyh.mei.data.repository.CoverPalette("080e10", "b8e5d4", "a0ccbd", "89b9ac", "b0d7c9"),
        )
        var background = androidx.compose.ui.graphics.Color.Unspecified
        var title = androidx.compose.ui.graphics.Color.Unspecified
        var chrome = androidx.compose.ui.graphics.Color.Unspecified
        show {
            AlbumArtworkTheme(state().copy(motionArtwork = cover)) {
                val colors = MaterialTheme.colorScheme
                val foreground = com.ljyh.mei.ui.component.glass.meiGlassContentColor()
                SideEffect { background = colors.surface; title = colors.onSurface; chrome = foreground }
            }
        }
        compose.runOnIdle {
            assertEquals(androidx.compose.ui.graphics.Color(0xff080e10), background)
            assertEquals(androidx.compose.ui.graphics.Color(0xffb8e5d4), title)
            assertEquals(title, chrome)
        }
    }

    @Test fun phoneLandscapeCannotRetainThePortraitMotionPalette() {
        val cover = com.ljyh.mei.data.repository.DynamicCover(
            1, "", com.ljyh.mei.data.repository.DynamicCover.Source.APPLE_MUSIC, "album-test",
            com.ljyh.mei.data.repository.CoverPalette("080e10", "ffffff", "ffffff", "ffffff", "ffffff"),
        )
        var background = androidx.compose.ui.graphics.Color.Unspecified
        show {
            CompositionLocalProvider(com.ljyh.mei.ui.component.layout.LocalPhoneLandscape provides true) {
                AlbumArtworkTheme(state().copy(motionArtwork = cover)) {
                    val surface = MaterialTheme.colorScheme.surface
                    SideEffect { background = surface }
                }
            }
        }
        compose.runOnIdle { assertEquals(androidx.compose.ui.graphics.Color.White, background) }
    }

    @Test fun shufflePlayAndDownloadAreOrderedAndDispatchDistinctActions() {
        var operation = ""
        show { AlbumActions(true, true, { operation = "shuffle" }, { operation = "play" }, { operation = "download" }) }
        val shuffle = compose.onNodeWithContentDescription("随机播放专辑").assertIsDisplayed()
        val play = compose.onNodeWithText("播放").assertIsDisplayed()
        val download = compose.onNodeWithContentDescription("下载专辑").assertIsDisplayed()
        assertTrue(shuffle.fetchSemanticsNode().boundsInRoot.right < play.fetchSemanticsNode().boundsInRoot.left)
        assertTrue(play.fetchSemanticsNode().boundsInRoot.right < download.fetchSemanticsNode().boundsInRoot.left)
        assertTrue(play.fetchSemanticsNode().boundsInRoot.width < shuffle.fetchSemanticsNode().boundsInRoot.width * 3)
        shuffle.performClick(); compose.runOnIdle { assertEquals("shuffle", operation) }
        play.performClick(); compose.runOnIdle { assertEquals("play", operation) }
        download.performClick(); compose.runOnIdle { assertEquals("download", operation) }
        compose.onNodeWithContentDescription("收藏专辑").assertDoesNotExist()
    }
    @Test fun unavailablePlayerDisablesPlaybackWhileEmptyAlbumsAlsoDisableDownload() {
        val hasSongs = mutableStateOf(true)
        show { AlbumActions(hasSongs.value, false, {}, {}, {}) }
        compose.onNodeWithContentDescription("随机播放专辑").assertIsNotEnabled()
        compose.onNodeWithText("播放").assertIsNotEnabled()
        compose.onNodeWithContentDescription("下载专辑").assertIsEnabled()
        compose.runOnIdle { hasSongs.value = false }
        compose.onNodeWithContentDescription("下载专辑").assertIsNotEnabled()
    }
    @Test fun subscriptionLoadingRetryAndSubscribedStatesRemainAccessibleInMoreMenu() {
        val state = mutableStateOf(state().copy(isSubscribed = null, isSubscriptionLoading = true, canSubscribe = false))
        var clicked = false
        show { AlbumDetailMoreButton(state.value, actions().copy(onSubscribe = { clicked = true })) }
        compose.onNodeWithText("收藏专辑").assertDoesNotExist()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("正在获取收藏状态").assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(isSubscriptionLoading = false, canSubscribe = true) }
        compose.onNodeWithText("重试获取收藏状态").assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(clicked); state.value = state.value.copy(isSubscribed = true) }
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("取消收藏专辑").assertIsEnabled()
    }
    @Test fun modelCategoryExposesFixedChoicesAndUpdatesTheCurrentPageImmediately() {
        val state = mutableStateOf(state())
        show {
            AlbumDetailMoreButton(state.value, actions().copy(
                onArtworkModeChange = { state.value = state.value.copy(artworkMode = it) },
                onModelModeChange = { state.value = state.value.copy(modelMode = it) },
            ))
        }
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("封面展示 · 自动").performClick()
        compose.onNodeWithText("CD／DVD 光盘").assertDoesNotExist()
        compose.onNodeWithText("年代模型").performClick()
        compose.onNodeWithText("自动判断").assertIsSelected()
        compose.onNodeWithText("CD／DVD 光盘").performClick()
        compose.runOnIdle {
            assertEquals(AlbumModelMode.CD, state.value.modelMode)
            assertEquals(AlbumArtworkMode.Model, state.value.artworkMode)
        }
        compose.onNodeWithText("专辑封面展示").assertDoesNotExist()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("封面展示 · 年代模型 · CD／DVD 光盘").assertIsDisplayed()
    }

    @Test fun artworkChoiceUsesTheSharedModeAndClosesAfterSelection() {
        val state = mutableStateOf(state())
        show { AlbumDetailMoreButton(state.value, actions().copy(onArtworkModeChange = { state.value = state.value.copy(artworkMode = it) })) }
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("封面展示 · 自动").performClick()
        compose.onNodeWithText("静态封面").performClick()
        compose.runOnIdle { assertEquals(AlbumArtworkMode.Static, state.value.artworkMode) }
        compose.onNodeWithText("专辑封面展示").assertDoesNotExist()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("封面展示 · 静态封面").assertIsDisplayed()
    }
}
