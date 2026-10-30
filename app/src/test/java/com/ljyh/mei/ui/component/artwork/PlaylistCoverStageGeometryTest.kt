package com.ljyh.mei.ui.component.artwork

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Measure loading/loaded layout only: no screenshots, bitmap requests or visual rendering checks. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1000dp-h800dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class PlaylistCoverStageGeometryTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun reservedArtworkSizeSurvivesLoadingAndWindowChangesForSingleAndTripleCovers() {
        var loading by mutableStateOf(true)
        var liked by mutableStateOf(true)
        var count by mutableIntStateOf(3)
        var width by mutableStateOf(240.dp)
        var measured = IntSize.Zero
        var expectedWidth = 0
        TabletContentTestActivity.content = {
            MaterialTheme {
                val density = LocalDensity.current
                SideEffect { expectedWidth = with(density) { width.roundToPx() } }
                Box(Modifier.width(width).height(600.dp)) {
                    PlaylistCoverStage(
                        covers = if (loading) emptyList() else List(count) { "" },
                        liked = liked, onCoverClick = {}, isLoading = loading,
                        placeholderCoverCount = count, animateEntrance = false,
                        modifier = Modifier.fillMaxWidth().onGloballyPositioned { measured = it.size },
                    )
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            for (isLiked in listOf(true, false)) for (covers in listOf(1, 3)) for (stageWidth in listOf(240.dp, 360.dp)) {
                compose.runOnIdle { liked = isLiked; count = covers; width = stageWidth; loading = true }
                compose.waitForIdle()
                val skeletonSize = measured
                compose.runOnIdle { loading = false }
                compose.waitForIdle()
                assertEquals("liked=$isLiked count=$covers width=$stageWidth", skeletonSize, measured)
                assertEquals(expectedWidth, measured.width)
            }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun entranceWaitsForDecodedArtworkAndDoesNotReplayOnMetadataRecomposition() {
        compose.mainClock.autoAdvance = false
        var ready by mutableStateOf(false)
        var imageKey by mutableStateOf("first")
        var metadataVersion by mutableIntStateOf(0)
        var observed: CoverEntranceState? = null
        TabletContentTestActivity.content = {
            val entrance = rememberCoverEntranceState(imageKey, ready, true, 0)
            androidx.compose.foundation.text.BasicText(metadataVersion.toString())
            SideEffect { observed = entrance }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.mainClock.advanceTimeBy(500)
            compose.waitForIdle()
            assertEquals(0f, observed!!.progress.value, 0.001f)
            assertFalse(observed!!.settled)
            compose.runOnIdle { ready = true }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(250)
            compose.waitForIdle()
            assertTrue("Decoded artwork must start entering: running=${observed!!.progress.isRunning}, settled=${observed!!.settled}", observed!!.progress.value > 0f)
            val animation = observed!!.progress
            compose.runOnIdle { metadataVersion++ }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            assertSame(animation, observed!!.progress)
            compose.mainClock.advanceTimeBy(3000)
            compose.waitForIdle()
            assertEquals(1f, observed!!.progress.value, 0.001f)
            assertTrue(observed!!.settled)
            compose.runOnIdle { metadataVersion++ }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            assertSame(animation, observed!!.progress)
            assertEquals(1f, observed!!.progress.value, 0.001f)
            compose.runOnIdle { imageKey = "next"; ready = false }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            assertEquals(0f, observed!!.progress.value, 0.001f)
            assertFalse(observed!!.settled)
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

}
