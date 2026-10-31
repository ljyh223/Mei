package com.ljyh.mei.ui.screen.main.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import com.ljyh.mei.ui.tablet.library.TabletLibraryContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Gestures on the actual layouts; no screenshots or network artwork. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
@OptIn(ExperimentalMaterial3Api::class)
class LibraryRefreshInteractionTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test @Config(qualifiers = "w400dp-h800dp")
    fun phonePullRefreshesOnlyAtTopAndDoesNotOverlapRefreshes() = exercise(wide = false)

    @Test @Config(qualifiers = "w1280dp-h800dp")
    fun tabletPullRefreshesOnlyAtTopAndDoesNotOverlapRefreshes() = exercise(wide = true)

    private fun exercise(wide: Boolean) {
        var requests = 0
        var refreshing by mutableStateOf(false)
        val data = LibraryContentUiState(
            LibraryProfileUi("owner", "", "", ""), LibrarySection.Created,
            (1..13).map { Playlist("$it", "自建 $it", "", "owner", "", "", 5) },
            (14..24).map { Playlist("$it", "收藏 $it", "", "other", "", "", 5) },
            emptyList(),
        )
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalPlayerAwareWindowInsets provides WindowInsets(0, 0, 0, 0),
                    LocalMeiGlassEnabled provides false,
                ) {
                    Box(Modifier.fillMaxSize().testTag("library")) {
                        LibraryRefreshBox(refreshing, true, { requests++; refreshing = true }) {
                            val state = LibraryPageState(LibraryUiState.Content(data), null, null, true,
                                TopAppBarDefaults.pinnedScrollBehavior(), false, false)
                            val actions = LibraryPageActions({}, {}, {}, {}, {}, {})
                            if (wide) TabletLibraryContent(state, actions) else LibraryPageContent(state, actions)
                        }
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            val list = if (wide) compose.onAllNodes(hasScrollToIndexAction()).onLast()
                else compose.onAllNodes(hasScrollToIndexAction()).onFirst()
            list.performScrollToIndex(4)
            compose.onNodeWithTag("library").performTouchInput {
                swipe(start = androidx.compose.ui.geometry.Offset(width * .8f, height * .3f),
                    end = androidx.compose.ui.geometry.Offset(width * .8f, height * .45f))
            }
            compose.runOnIdle { assertEquals(0, requests) }
            list.performScrollToIndex(0)
            compose.onNodeWithTag("library").performTouchInput {
                swipe(start = androidx.compose.ui.geometry.Offset(width * .8f, height * .2f),
                    end = androidx.compose.ui.geometry.Offset(width * .8f, height * .85f))
            }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(1, requests) }
            compose.onNodeWithTag("library").performTouchInput { swipeDown() }
            compose.runOnIdle { assertEquals(1, requests) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
