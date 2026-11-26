package com.ljyh.mei.ui.component.player.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
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
@Config(sdk = [36], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class PlayerProgressSliderCompatibilityTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun fluidTrackFollowsPlaybackAndSeeksOnceAfterDragging() = verify(fluid = true)
    @Test fun wavyTrackFollowsPlaybackAndSeeksOnceAfterDragging() = verify(fluid = false)

    private fun verify(fluid: Boolean) {
        val position = mutableLongStateOf(25_000)
        val duration = mutableLongStateOf(100_000)
        val previews = mutableListOf<Long>()
        val seeks = mutableListOf<Long>()
        val seek: (Long) -> Unit = { seeks += it; position.longValue = it }
        TabletContentTestActivity.content = {
            MaterialTheme {
                Box(Modifier.width(360.dp)) {
                    if (fluid) FluidProgressSlider(position.longValue, duration.longValue, false,
                        seek, { previews += it }, {}, false, showMusicQuality = false)
                    else PlayerProgressSlider(position.longValue, duration.longValue, false,
                        seek, { previews += it }, {}, false, showMusicQuality = false)
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        fun range() = slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        try {
            assertEquals(ProgressBarRangeInfo(25_000f, 0f..100_000f), range())
            compose.runOnIdle { duration.longValue = 200_000; position.longValue = 150_000 }
            compose.waitForIdle()
            assertEquals(ProgressBarRangeInfo(150_000f, 0f..200_000f), range())
            slider.performTouchInput {
                swipe(Offset(width * .3f, centerY), Offset(width * .8f, centerY), 300)
            }
            compose.runOnIdle {
                assertTrue(previews.isNotEmpty())
                assertEquals(1, seeks.size)
                assertTrue(seeks.single() in 140_000..180_000)
            }
            compose.runOnIdle { position.longValue = 125_000 }
            compose.waitForIdle()
            assertEquals(125_000f, range().current)
            compose.runOnIdle { duration.longValue = 0; position.longValue = 0 }
            slider.assertIsNotEnabled()
            assertEquals(ProgressBarRangeInfo(0f, 0f..1f), range())
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
