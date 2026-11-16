package com.ljyh.mei.ui.screen.daily

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.screen.MeiDestinationSurface
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.PlaylistHeaderStyle
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.ui.screen.playlist.PlaylistActions
import com.ljyh.mei.ui.screen.playlist.PlaylistContent
import com.ljyh.mei.ui.screen.playlist.PlaylistUiState
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1000dp-h600dp", manifest = Config.NONE)
class DailyRecommendationLayoutTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun landscapeDailyAndPlaylistUseTheSameToolbarAndCoverActions() {
        val tracks = (1L..33L).map {
            MediaMetadata(it, "Song $it", "", emptyList(), 1000, MediaMetadata.Album(0, ""))
        }
        val state = PlaylistUiState(
            uiData = UiPlaylist(-1, "每日推荐", tracks.size, 0, "", emptyList(), "", tracks = tracks),
            isLoading = false,
            headerActionIcon = Icons.Rounded.FavoriteBorder,
            headerActionLabel = "收藏",
        )
        val playlist = state.copy(
            uiData = state.uiData.copy(
                id = 42L,
                title = "私人雷达",
                creatorName = "创建者",
                description = "旧头部的说明",
                headerStyle = PlaylistHeaderStyle.Radar,
            ),
            playlistId = 42L,
        )
        val showDaily = mutableStateOf(true)
        var plays = 0
        var downloads = 0
        val actions = PlaylistActions(
            onPlayAll = { plays++ },
            onHeaderAction = {},
            onTrackClick = { _, _ -> },
            onBack = {},
            onDownload = { downloads++ },
            onPlaylistSearchQueryChange = {},
        )
        TabletContentTestActivity.content = {
            MaterialTheme {
                Box(Modifier.size(width = 790.dp, height = 350.dp)) {
                    CompositionLocalProvider(
                        LocalAppWindowLayout provides AppWindowLayout.Compact,
                        LocalPhoneLandscape provides true,
                        LocalPageStartInset provides 72.dp,
                        LocalPlayerAwareWindowInsets provides WindowInsets(0),
                    ) {
                        MeiDestinationSurface(
                            drawsBehindSideBar = true,
                            drawsToHorizontalEdgesInLandscape = true,
                            horizontalSafeInsets = WindowInsets(right = 40.dp),
                        ) {
                            PlaylistContent(
                                state = if (showDaily.value) state else playlist,
                                actions = actions,
                                landscapeArtwork = if (showDaily.value) { modifier ->
                                    DailyCoverWall(emptyList(), LocalDate.of(2026, 1, 1), modifier.testTag("daily-artwork"))
                                } else null,
                            )
                        }
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val title = compose.onNodeWithText("每日推荐")
            val song = compose.onNodeWithText("Song 1", useUnmergedTree = true)
            val more = compose.onNodeWithContentDescription("更多操作")
            val play = compose.onNodeWithText("播放全部")
            compose.onAllNodesWithText("每日推荐").assertCountEquals(1)
            val artwork = compose.onNodeWithTag("daily-artwork").fetchSemanticsNode().boundsInRoot
            val initialSong = song.fetchSemanticsNode().boundsInRoot
            val initialTitle = title.fetchSemanticsNode().boundsInRoot
            val initialPlay = play.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val initialMore = more.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(initialTitle.bottom < artwork.top)
            assertTrue(
                "The first song title should sit near the top of the cover: song=$initialSong cover=$artwork",
                initialSong.top >= artwork.top && initialSong.top < artwork.top + artwork.height / 3,
            )
            assertTrue(initialPlay.left > artwork.left && initialPlay.center.x < artwork.center.x)
            assertTrue(initialPlay.center.y > artwork.center.y && initialPlay.bottom < artwork.bottom)
            assertTrue(initialMore.left >= initialPlay.right && initialMore.right < artwork.right)
            play.performClick()

            compose.runOnIdle { showDaily.value = false }
            compose.onAllNodesWithText("私人雷达").assertCountEquals(1)
            compose.onNodeWithText("旧头部的说明").assertDoesNotExist()
            compose.onNodeWithText("创建者").assertDoesNotExist()
            assertEquals(initialTitle.top, compose.onNodeWithText("私人雷达").fetchSemanticsNode().boundsInRoot.top, 1f)
            assertEquals(initialSong.top, song.fetchSemanticsNode().boundsInRoot.top, 1f)
            assertEquals(initialPlay, play.fetchSemanticsNode().boundsInRoot)
            assertEquals(initialMore, more.fetchSemanticsNode().boundsInRoot)
            play.performClick()
            more.performClick()
            compose.onNodeWithText("收藏").assertIsDisplayed()
            compose.onNodeWithText("下载歌单").assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(2, plays); assertEquals(1, downloads) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
