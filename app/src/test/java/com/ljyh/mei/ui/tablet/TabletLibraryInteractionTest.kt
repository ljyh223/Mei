package com.ljyh.mei.ui.tablet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalPageHorizontalSafePadding
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.main.library.*
import com.ljyh.mei.ui.screen.main.library.component.AdaptiveLibraryContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
                    AdaptiveLibraryContent(
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

    @Test fun phoneLandscapeShowsThreeLikedCoversAndKeepsPlaybackAndCreatedPlaylistsVisible() {
        val data = LibraryContentUiState(
            LibraryProfileUi("owner", "", "", ""), LibrarySection.Created,
            listOf("liked" to "我喜欢的音乐", "created" to "自建歌单").map { (id, title) ->
                Playlist(id, title, "", "owner", "", "", if (id == "liked") 5 else 7)
            },
            emptyList(), emptyList(),
        )
        var played = ""
        var opened = ""
        TabletContentTestActivity.content = {
            MaterialTheme {
                Box(Modifier.size(width = 780.dp, height = 320.dp)) {
                    CompositionLocalProvider(
                        LocalAppWindowLayout provides AppWindowLayout.Compact,
                        LocalPhoneLandscape provides true,
                        LocalPageStartInset provides 72.dp,
                        LocalPageHorizontalSafePadding provides PaddingValues(end = 40.dp),
                        LocalPlayerAwareWindowInsets provides WindowInsets(bottom = 16.dp),
                        LocalMeiGlassEnabled provides false,
                    ) {
                        LibraryPageContent(
                            LibraryPageState(
                                LibraryUiState.Content(data), null, "", true,
                                TopAppBarDefaults.pinnedScrollBehavior(), false, true,
                                likedArtworks = listOf("ic_menu_gallery", "ic_menu_camera", "ic_media_play").map {
                                    "android.resource://android/drawable/$it"
                                },
                            ),
                            LibraryPageActions({ played = it }, { opened = it }, {}, {}, {}, {}),
                        )
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val play = compose.onNodeWithContentDescription("播放我喜欢的音乐")
                .assertIsDisplayed()
                .assertTextEquals("播放全部")
                .assertHeightIsAtLeast(48.dp)
            val created = compose.onNodeWithText("自建歌单").assertIsDisplayed()
            val covers = compose.onAllNodesWithContentDescription("歌曲封面", useUnmergedTree = true).assertCountEquals(3)
            val liked = compose.onNodeWithContentDescription("打开我喜欢的音乐").assertIsDisplayed()
            val profile = compose.onNodeWithContentDescription("个人中心").assertIsDisplayed()
            val section = compose.onNodeWithText("我创建的").assertIsDisplayed()
            val frontCover = covers.onLast().assertHeightIsAtLeast(125.dp)
            assertEquals(section.fetchSemanticsNode().boundsInRoot.center.y,
                profile.fetchSemanticsNode().boundsInRoot.center.y, 1f)
            compose.onNodeWithText("音乐库").assertDoesNotExist()
            // The cover frame stays in place while the image loads and animates inside it.
            val coverFrame = requireNotNull(frontCover.fetchSemanticsNode().layoutInfo.parentInfo)
            val coverBounds = coverFrame.coordinates.boundsInRoot()
            val playBounds = play.fetchSemanticsNode().boundsInRoot
            val overlap = coverBounds.intersect(playBounds)
            assertTrue("Play button ${playBounds} should sit on the lower part of ${coverBounds}",
                coverBounds.contains(playBounds.center) && playBounds.center.y > coverBounds.center.y)
            assertTrue("Most of the play button should overlap the front cover",
                overlap.width * overlap.height >= 0.8f * playBounds.width * playBounds.height)
            assertTrue(playBounds.right < created.fetchSemanticsNode().boundsInRoot.left)
            compose.onNodeWithText("我喜欢的音乐").assertDoesNotExist()
            compose.onNodeWithText("5 首").assertDoesNotExist()
            liked.performClick()
            compose.runOnIdle { assertEquals("liked", opened) }
            play.performClick()
            created.performClick()
            compose.runOnIdle {
                assertEquals("liked", played)
                assertEquals("created", opened)
            }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
