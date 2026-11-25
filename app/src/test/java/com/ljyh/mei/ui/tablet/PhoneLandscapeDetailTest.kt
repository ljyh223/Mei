package com.ljyh.mei.ui.tablet

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.domain.AlbumOverview
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.constants.AlbumArtworkMode
import com.ljyh.mei.constants.AlbumModelMode
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
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1280dp-h800dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class PhoneLandscapeDetailTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val song = MediaMetadata(1, "第一首歌曲", "", emptyList(), 60000, MediaMetadata.Album(2, "测试专辑"))

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun landscapeArtistPhotoFillsTheLeftPaneEvenWhenTheSourceIsWide() {
        val photo = File.createTempFile("mei-artist-portrait", ".png")
        val bitmap = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        try {
            photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
        var operation = ""
        try {
            showDetail {
                AdaptiveArtistContent(
                    ArtistDetailUiState(
                        id = "1",
                        artist = ArtistDetail.Data.Artist(
                            name = "测试歌手",
                            cover = photo.toURI().toString(),
                            transNames = listOf("艺名"),
                            identities = listOf("入驻艺人"),
                            briefDesc = "这是一段简介",
                            musicSize = 1,
                        ),
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
            val artwork = compose.onNodeWithContentDescription("歌手照片")
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.waitForIdle()
                compose.onAllNodesWithContentDescription("歌手照片").fetchSemanticsNodes().size == 1
            }
            val picture = artwork.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            artwork.assertWidthIsEqualTo(232.dp).assertHeightIsAtLeast(240.dp)
            val name = compose.onNodeWithText("测试歌手").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(name.left in picture.left..picture.center.x)
            assertTrue(name.top >= picture.center.y && name.bottom <= picture.bottom)
            compose.onNodeWithText("艺名").assertDoesNotExist()
            compose.onNodeWithText("入驻艺人").assertDoesNotExist()
            compose.onNodeWithText("这是一段简介").assertDoesNotExist()
            compose.onNodeWithText("1 首歌曲 · 0 张专辑").assertDoesNotExist()
            compose.onNodeWithContentDescription("返回").assertDoesNotExist()

            val play = compose.onNodeWithContentDescription("播放热门歌曲").assertIsDisplayed()
            val track = compose.onNodeWithText(song.title).assertIsDisplayed()
            assertTrue(play.fetchSemanticsNode().boundsInRoot.center.x in picture.left..picture.right)
            assertTrue(play.fetchSemanticsNode().boundsInRoot.top >= picture.bottom)
            compose.onNodeWithContentDescription("歌手简介").assertIsDisplayed()
            compose.onNodeWithContentDescription("收藏歌手").assertIsDisplayed()
            compose.onNodeWithContentDescription("更多操作").assertIsDisplayed()
            assertTrue(track.fetchSemanticsNode().boundsInRoot.left > picture.right)
            play.performClick()
            compose.runOnIdle { assertEquals("play", operation) }
            track.performClick()
            compose.runOnIdle { assertEquals(song.title, operation) }
        } finally {
            photo.delete()
        }
    }

    @Test fun albumShowsLargeStillModelWithOnlyPlayAndMoreOverTheArtwork() {
        var operation = ""
        showDetail {
            AdaptiveAlbumContent(
                AlbumDetailUiState(
                    album = AlbumOverview(2, "测试专辑", "", "", 1, emptyList()),
                    coverColor = null, songs = listOf(song), displayedSongs = listOf(song),
                    isSubscribed = false, isSubscriptionLoading = false, canSubscribe = true, canPlay = true,
                    searchQuery = "", isSearchActive = false,
                    artworkMode = AlbumArtworkMode.Model, modelMode = AlbumModelMode.Vinyl,
                ),
                AlbumDetailActions(
                    onSearchQueryChange = {}, onSearchActiveChange = {}, onBack = {}, onArtistClick = {},
                    onPlayAll = { operation = "play" }, onShuffle = { operation = "shuffle" }, onSubscribe = { operation = "subscribe" },
                    onDownload = { operation = "download" },
                    onTrackClick = { track, _ -> operation = track.title }, onTrackMore = { _, _ -> },
                ),
            )
        }
        val play = compose.onNodeWithText("播放").assertIsDisplayed()
        val track = compose.onNodeWithText(song.title).assertIsDisplayed()
        assertInSeparatePanes(play, track)
        assertNoDetailToolbar("测试专辑")
        compose.onNodeWithText("歌曲 · 1").assertIsDisplayed()
        val more = compose.onNodeWithContentDescription("更多操作").assertIsDisplayed()
        assertTrue(more.fetchSemanticsNode().boundsInRoot.left > play.fetchSemanticsNode().boundsInRoot.right)
        val artwork = compose.onNodeWithContentDescription("专辑封面").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("detail-viewport").fetchSemanticsNode().boundsInRoot
        assertTrue("Model uses the pane height instead of a short fixed-ratio stage", artwork.height >= viewport.height * .75f)
        assertTrue(artwork.contains(play.fetchSemanticsNode().boundsInRoot.center))
        assertTrue(artwork.contains(more.fetchSemanticsNode().boundsInRoot.center))
        compose.onNodeWithContentDescription("随机播放专辑").assertDoesNotExist()
        compose.onNodeWithContentDescription("下载专辑").assertDoesNotExist()
        play.performClick()
        compose.runOnIdle { assertEquals("play", operation) }
        more.performClick()
        compose.onNodeWithText("随机播放专辑").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("shuffle", operation) }
        more.performClick()
        compose.onNodeWithText("下载专辑").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("download", operation) }
        more.performClick()
        compose.onNodeWithText("收藏专辑").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("subscribe", operation) }
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
                Box(Modifier.size(width = 668.dp, height = 300.dp).testTag("detail-viewport")) {
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
