package com.ljyh.mei.ui.tablet

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.main.library.*
import com.ljyh.mei.ui.tablet.library.TabletLibraryContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Exercise real controls without screenshots, network artwork or playback services. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1280dp-h800dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
@OptIn(ExperimentalMaterial3Api::class)
class TabletLibraryInteractionTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun likedPlaybackAndCreatedCollectionKeepTheirOriginalTargets() {
        fun playlist(id: String, title: String) = Playlist(id, title, "", "owner", "", "", 5)
        val liked = playlist("liked", "我喜欢的音乐")
        val data = LibraryContentUiState(
            LibraryProfileUi("owner", "", "", ""), LibrarySection.Created,
            listOf(liked) + (1..13).map { playlist("created-$it", "自建 $it") },
            emptyList(), emptyList(),
        )
        var artworkLoading by mutableStateOf(true)
        var played = ""
        var opened = ""
        var collection: LibraryCollection? = null
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalPlayerAwareWindowInsets provides WindowInsets(0, 0, 0, 0),
                    LocalMeiGlassEnabled provides false,
                ) {
                    TabletLibraryContent(
                        LibraryPageState(LibraryUiState.Content(data), null, "最近喜欢的歌", true,
                            TopAppBarDefaults.pinnedScrollBehavior(), false, true, likedArtworkLoading = artworkLoading),
                        LibraryPageActions({ played = it }, { opened = it }, {}, { collection = it }, {}, {}),
                    )
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.onAllNodesWithText("我喜欢的音乐").assertCountEquals(1)
            compose.onNodeWithText("5 首 · 最近喜欢《最近喜欢的歌》").assertExists()
            compose.onNodeWithText("我喜欢的音乐", useUnmergedTree = true).performClick()
            compose.runOnIdle { assertEquals("liked", opened) }
            compose.onNodeWithContentDescription("播放我喜欢的音乐").performClick()
            compose.runOnIdle { assertEquals("liked", played) }
            compose.runOnIdle { artworkLoading = false }
            compose.waitForIdle()
            compose.onNodeWithText("自建 1").performClick()
            compose.runOnIdle { assertEquals("created-1", opened) }
            compose.onNodeWithText("全部").performClick()
            compose.runOnIdle { assertEquals(LibraryCollection.Created, collection) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
