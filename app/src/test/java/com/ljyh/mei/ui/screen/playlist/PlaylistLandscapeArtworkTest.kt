package com.ljyh.mei.ui.screen.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.PlaylistHeaderStyle
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1280dp-h800dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class PlaylistLandscapeArtworkTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun likedPlaylistUsesTheThreeCoverStageWithFloatingActions() {
        val covers = listOf("ic_menu_gallery", "ic_menu_camera", "ic_media_play", "ic_media_next")
            .map { "android.resource://android/drawable/$it" }
        val playlist = UiPlaylist(
            id = 42,
            title = "我喜欢的音乐",
            count = covers.size,
            subscriberCount = 0,
            cover = "",
            coverList = covers,
            creatorName = "",
            tracks = emptyList(),
            headerStyle = PlaylistHeaderStyle.Liked,
        )
        var played = false
        TabletContentTestActivity.content = {
            MaterialTheme {
                Box(Modifier.size(width = 780.dp, height = 320.dp)) {
                    CompositionLocalProvider(
                        LocalPageStartInset provides 72.dp,
                        LocalPlayerAwareWindowInsets provides WindowInsets(bottom = 16.dp),
                        LocalMeiGlassEnabled provides false,
                    ) {
                        PlaylistContent(
                            state = PlaylistUiState(
                                uiData = playlist,
                                isLoading = false,
                                headerActionIcon = Icons.Rounded.Favorite,
                                headerActionLabel = "收藏",
                            ),
                            actions = PlaylistActions(
                                onPlayAll = { played = true },
                                onHeaderAction = {},
                                onTrackClick = { _, _ -> },
                                onBack = {},
                            ),
                        )
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val allCovers = compose.onAllNodesWithContentDescription("歌曲封面", useUnmergedTree = true)
                .assertCountEquals(3)
            val frontCover = requireNotNull(allCovers[2].fetchSemanticsNode().layoutInfo.parentInfo)
                .coordinates.boundsInRoot()
            val play = compose.onNodeWithText("播放全部").assertIsDisplayed()
            val more = compose.onNodeWithContentDescription("更多操作").assertIsDisplayed()
            val playCenter = play.fetchSemanticsNode().boundsInRoot.center
            val moreCenter = more.fetchSemanticsNode().boundsInRoot.center
            assertTrue(frontCover.contains(playCenter))
            assertTrue(frontCover.contains(moreCenter))
            assertTrue(playCenter.y > frontCover.center.y)
            play.performClick()
            compose.runOnIdle { assertTrue(played) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
