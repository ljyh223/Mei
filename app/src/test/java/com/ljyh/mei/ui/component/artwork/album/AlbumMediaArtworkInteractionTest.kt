package com.ljyh.mei.ui.component.artwork.album

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Layout/semantics and button actions only; no device, images, screenshots or network. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w600dp-h1000dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class AlbumMediaArtworkInteractionTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun changingFixedStyleImmediatelyReplacesTheMechanismAndResetsItsOpenState() {
        val style = mutableStateOf(AlbumMediaStyle.CD)
        TabletContentTestActivity.content = {
            MaterialTheme { Box(Modifier.width(380.dp).height(380.dp)) {
                AlbumMediaArtwork(null, null, "固定造型", interactive = true, style = style.value)
            } }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        fun mechanism() = compose.onNodeWithContentDescription("专辑《固定造型》封面")
        fun description() = mechanism().fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        try {
            compose.waitForIdle()
            assertEquals("盒盖已合上", description())
            mechanism().performClick()
            assertEquals("盒盖已打开", description())
            compose.runOnIdle { style.value = AlbumMediaStyle.Cassette }
            assertEquals("包装已合上", description())
            mechanism().performClick()
            assertEquals("包装已打开，磁带在托盘内", description())
            compose.runOnIdle { style.value = AlbumMediaStyle.Vinyl }
            assertEquals("唱片在封套内", description())
            mechanism().performClick()
            assertTrue(description().contains("唱片正在抽出") || description().contains("唱片已抽出"))
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun openLinerCanReachTheLastTrackAndShrinkingMetadataClampsTheSelectedPage() {
        val tracks = mutableStateOf((1..17).map { AlbumPackageTrack(it, "曲目 $it", 180_000) })
        val date = LocalDate.of(1983, 1, 1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        TabletContentTestActivity.content = {
            MaterialTheme { Box(Modifier.width(380.dp).height(380.dp)) {
                AlbumMediaArtwork(null, date, "测试专辑", interactive = true, tracks = tracks.value)
            } }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("下一页曲目").assertDoesNotExist()
            compose.onNodeWithContentDescription("专辑《测试专辑》封面").performClick()
            compose.onNodeWithText("1 / 3").assertIsDisplayed()
            val stage = compose.onNodeWithContentDescription("专辑《测试专辑》封面").fetchSemanticsNode().boundsInRoot
            val pager = compose.onNodeWithContentDescription("上一页曲目").fetchSemanticsNode().boundsInRoot
            assertTrue("Pager should stay at the top instead of moving down for sampling", pager.top - stage.top < stage.height * .05f)
            compose.onNodeWithContentDescription("上一页曲目").assertIsNotEnabled()
            compose.onNodeWithContentDescription("下一页曲目").performClick()
            compose.onNodeWithContentDescription("下一页曲目").performClick()
            compose.onNodeWithText("3 / 3").assertIsDisplayed()
            compose.onNodeWithContentDescription("下一页曲目").assertIsNotEnabled()
            val state = compose.onNodeWithContentDescription("专辑《测试专辑》封面").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            assertTrue(state.contains("17. 曲目 17"))
            assertFalse(state.contains("16. 曲目 16"))
            compose.runOnIdle { tracks.value = tracks.value.take(2) }
            compose.waitForIdle()
            compose.onNodeWithContentDescription("下一页曲目").assertDoesNotExist()
            val updated = compose.onNodeWithContentDescription("专辑《测试专辑》封面").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            assertTrue(updated.contains("1. 曲目 1"))
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
    @Test fun cdLinerShowsSixTracksPerPageAndKeepsItsPagingActions() {
        val tracks = (1..13).map { AlbumPackageTrack(it, "曲目 $it", 180_000) }
        val date = LocalDate.of(1995, 1, 1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        TabletContentTestActivity.content = {
            MaterialTheme { Box(Modifier.width(380.dp).height(380.dp)) {
                AlbumMediaArtwork(null, date, "测试 CD", interactive = true, tracks = tracks)
            } }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("专辑《测试 CD》封面").performClick()
            compose.onNodeWithText("1 / 3").assertIsDisplayed()
            val first = compose.onNodeWithContentDescription("专辑《测试 CD》封面").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            assertTrue(first.contains("6. 曲目 6"))
            assertFalse(first.contains("7. 曲目 7"))
            compose.onNodeWithContentDescription("下一页曲目").performClick()
            compose.onNodeWithContentDescription("下一页曲目").performClick()
            val last = compose.onNodeWithContentDescription("专辑《测试 CD》封面").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            assertTrue(last.contains("13. 曲目 13"))
            compose.onNodeWithContentDescription("下一页曲目").assertIsNotEnabled()
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun vinylCanBePutAwayAfterGrabbingItMidwayThroughExtraction() {
        val date = LocalDate.of(1975, 1, 1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        TabletContentTestActivity.content = {
            MaterialTheme { Box(Modifier.width(380.dp).height(380.dp)) {
                AlbumMediaArtwork(null, date, "黑胶交互", interactive = true)
            } }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.mainClock.autoAdvance = false
            val model = compose.onNodeWithContentDescription("专辑《黑胶交互》封面")
            model.performTouchInput { click(center) }
            compose.mainClock.advanceTimeBy(700)
            model.performTouchInput { swipe(center, center + Offset(80f, 0f), 180) }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            assertEquals("收回唱片", model.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
            model.performTouchInput { click(center) }
            compose.mainClock.advanceTimeBy(2700)
            compose.waitForIdle()
            assertEquals("抽出唱片", model.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
            assertEquals("唱片在封套内", model.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        } finally {
            compose.mainClock.autoAdvance = true
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun cassetteCanReturnToItsPackageAfterOpeningExtractingAndDragging() {
        val date = LocalDate.of(1983, 1, 1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        TabletContentTestActivity.content = {
            MaterialTheme { Box(Modifier.width(380.dp).height(380.dp)) {
                AlbumMediaArtwork(null, date, "磁带交互", interactive = true)
            } }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.mainClock.autoAdvance = false
            val model = compose.onNodeWithContentDescription("专辑《磁带交互》封面")
            model.performTouchInput { click(center) }
            compose.mainClock.advanceTimeBy(1800)
            assertEquals("取出磁带", model.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
            model.performTouchInput { click(center) }
            compose.mainClock.advanceTimeBy(1800)
            model.performTouchInput { swipe(center, center + Offset(80f, 0f), 180) }
            compose.mainClock.advanceTimeByFrame()
            assertEquals("装回包装", model.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
            model.performTouchInput { click(center) }
            compose.mainClock.advanceTimeBy(1800)
            assertEquals("打开包装", model.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
            model.performTouchInput { click(center) }
            compose.mainClock.advanceTimeBy(1800)
            assertEquals("取出磁带", model.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        } finally {
            compose.mainClock.autoAdvance = true
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

}
