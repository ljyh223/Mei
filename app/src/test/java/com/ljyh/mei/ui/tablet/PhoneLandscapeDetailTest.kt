package com.ljyh.mei.ui.tablet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.domain.AlbumOverview
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalPageHorizontalSafePadding
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.album.AdaptiveAlbumContent
import com.ljyh.mei.ui.screen.album.AlbumDetailActions
import com.ljyh.mei.ui.screen.album.AlbumDetailUiState
import com.ljyh.mei.ui.screen.artist.AdaptiveArtistContent
import com.ljyh.mei.ui.screen.artist.ArtistDetailActions
import com.ljyh.mei.ui.screen.artist.ArtistDetailUiState
import org.junit.Assert.assertEquals
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
class PhoneLandscapeDetailTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val song = MediaMetadata(1, "第一首歌曲", "", emptyList(), 60000, MediaMetadata.Album(2, "测试专辑"))

    @Test fun artistPlaybackAndTrackStayVisibleInShortLandscape() {
        var operation = ""
        showDetail {
            AdaptiveArtistContent(
                ArtistDetailUiState(
                    id = "1", artist = ArtistDetail.Data.Artist(name = "测试歌手", musicSize = 1),
                    expertIdentities = emptyList(), detailLoading = false, detailError = null,
                    songs = listOf(song), songsLoading = false, songsError = null,
                    albums = emptyList(), albumsLoading = false, albumsError = null,
                    coverColor = null, canPlay = true,
                ),
                ArtistDetailActions(
                    onBack = {}, onBrowseSongs = {}, onBrowseAlbums = {},
                    onPlayHotSongs = { operation = "play" },
                    onTrackClick = { track, _ -> operation = track.title },
                    onTrackMore = { _, _ -> }, onAlbumClick = {},
                    onRetryDetail = {}, onRetrySongs = {}, onRetryAlbums = {},
                ),
            )
        }
        val play = compose.onNodeWithText("播放热门歌曲").assertIsDisplayed()
        val track = compose.onNodeWithText(song.title).assertIsDisplayed()
        assertInSeparatePanes(play, track)
        assertNoDetailToolbar("测试歌手")
        compose.onNodeWithText("热门单曲").assertIsDisplayed()
        play.performClick()
        compose.runOnIdle { assertEquals("play", operation) }
        track.performClick()
        compose.runOnIdle { assertEquals(song.title, operation) }
    }

    @Test fun albumShowsVinylWithPlaybackAndAlbumActionsBesideTheTrackList() {
        var operation = ""
        showDetail {
            AdaptiveAlbumContent(
                AlbumDetailUiState(
                    album = AlbumOverview(2, "测试专辑", "", "", 1, emptyList()),
                    coverColor = null, songs = listOf(song), displayedSongs = listOf(song),
                    isSubscribed = false, isSubscriptionLoading = false, canSubscribe = true, canPlay = true,
                    searchQuery = "", isSearchActive = false,
                ),
                AlbumDetailActions(
                    onSearchQueryChange = {}, onSearchActiveChange = {}, onBack = {}, onArtistClick = {},
                    onPlayAll = { operation = "play" }, onSubscribe = { operation = "subscribe" },
                    onDownload = { operation = "download" },
                    onTrackClick = { track, _ -> operation = track.title }, onTrackMore = { _, _ -> },
                ),
            )
        }
        val play = compose.onNodeWithText("播放全部").assertIsDisplayed()
        val track = compose.onNodeWithText(song.title).assertIsDisplayed()
        assertInSeparatePanes(play, track)
        assertNoDetailToolbar("测试专辑")
        compose.onNodeWithText("歌曲 · 1").assertIsDisplayed()
        val more = compose.onNodeWithContentDescription("更多操作").assertIsDisplayed()
        assertInSeparatePanes(more, track)
        val artwork = compose.onNodeWithContentDescription("专辑封面").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(VinylAlbumArtworkRatio, artwork.width / artwork.height, 0.01f)
        assertTrue(artwork.contains(play.fetchSemanticsNode().boundsInRoot.center))
        assertTrue(artwork.contains(more.fetchSemanticsNode().boundsInRoot.center))
        play.performClick()
        compose.runOnIdle { assertEquals("play", operation) }
        more.performClick()
        compose.onNodeWithText("收藏专辑").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("subscribe", operation) }
        more.performClick()
        compose.onNodeWithText("下载专辑").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("download", operation) }
        more.performClick()
        compose.onNodeWithText("搜索专辑歌曲").assertIsDisplayed()
        compose.onNodeWithText("专辑信息").assertIsDisplayed()
    }

    private fun assertNoDetailToolbar(title: String) {
        compose.onNodeWithContentDescription("返回").assertDoesNotExist()
        compose.onNodeWithText(title).assertDoesNotExist()
    }
    private fun assertInSeparatePanes(action: SemanticsNodeInteraction, track: SemanticsNodeInteraction) {
        val actionBounds = action.fetchSemanticsNode().boundsInRoot
        val trackBounds = track.fetchSemanticsNode().boundsInRoot
        assertTrue(actionBounds.right < trackBounds.left)
        assertTrue(trackBounds.top < actionBounds.top)
    }

    private fun showDetail(content: @Composable () -> Unit) {
        TabletContentTestActivity.content = {
            MaterialTheme {
                Box(Modifier.size(width = 668.dp, height = 300.dp)) {
                    CompositionLocalProvider(
                        LocalAppWindowLayout provides AppWindowLayout.Compact,
                        LocalPhoneLandscape provides true,
                        LocalPageStartInset provides 72.dp,
                        LocalPageHorizontalSafePadding provides PaddingValues(end = 40.dp),
                        LocalPlayerAwareWindowInsets provides WindowInsets(bottom = 16.dp),
                        LocalMeiGlassEnabled provides false,
                        content = content,
                    )
                }
            }
        }
        controller = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
    }

    private var controller: org.robolectric.android.controller.ActivityController<TabletContentTestActivity>? = null

    @org.junit.After
    fun tearDown() {
        controller?.pause()?.stop()?.destroy()
        TabletContentTestActivity.content = {}
    }
}
