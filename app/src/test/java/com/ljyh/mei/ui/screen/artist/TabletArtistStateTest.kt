package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import com.ljyh.mei.data.repository.artwork.AppleArtistImage
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.Album
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
@Config(sdk = [36], qualifiers = "w1280dp-h800dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class TabletArtistStateTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val window = mutableStateOf(AppWindowLayout.Wide)
    private val phoneLandscape = mutableStateOf(false)
    private var operation = ""
    private val vertical = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    private val state = ArtistDetailUiState(
        id = "1", artist = ArtistDetail.Data.Artist(name = "测试歌手"), expertIdentities = emptyList(),
        detailLoading = false, detailError = null,
        songs = (1L..12L).map { MediaMetadata(it, "歌曲 $it", "", emptyList(), 60000, MediaMetadata.Album(1, "曲目专辑")) },
        songsLoading = false, songsError = null,
        albums = (1L..6L).map { Album(it, "专辑 $it", "", 10, emptyList(), "专辑", 10000 - it) } +
            Album(100, "最新单曲", "", 1, emptyList(), "Single", 20000),
        albumsLoading = false, albumsError = null, coverColor = null, canPlay = true,
    )
    private val actions = ArtistDetailActions(
        onBack = {}, onBrowseSongs = { operation = "songs" }, onBrowseAlbums = { operation = "albums" },
        onPlayHotSongs = { operation = "play" }, onTrackClick = { song, index -> operation = "song:${song.id}:$index" },
        onTrackMore = { song, _ -> operation = "more:${song.id}" }, onAlbumClick = { operation = "album:$it" },
        onRetryDetail = {}, onRetrySongs = {}, onRetryAlbums = {}, onToggleSubscription = { operation = "favorite" },
        onBrowseAlbumReleases = { operation = "album-category" }, onBrowseSingles = { operation = "single-category" },
    )

    @Test fun tabletUsesOneVerticalFeedAndPreservesPlaybackAndAlbumActions() {
        show().use {
            compose.onAllNodes(vertical).assertCountEquals(1)
            compose.onNodeWithContentDescription("播放热门歌曲").performClick()
            compose.runOnIdle { assertEquals("play", operation) }
            compose.onNodeWithContentDescription("收藏歌手").performClick()
            compose.runOnIdle { assertEquals("favorite", operation) }
            compose.onNode(vertical).performScrollToNode(hasText("歌曲 1"))
            compose.onNodeWithText("歌曲 1").performClick()
            compose.runOnIdle { assertEquals("song:1:0", operation) }
            compose.onNodeWithText("歌曲 2").performClick()
            compose.runOnIdle { assertEquals("song:2:1", operation) }
            compose.onNodeWithContentDescription("歌曲 2的更多操作").performClick()
            compose.runOnIdle { assertEquals("more:2", operation) }
            compose.onNode(vertical).performScrollToNode(hasText("专辑 1"))
            compose.onNodeWithText("专辑 1").performClick()
            compose.runOnIdle { assertEquals("album:1", operation) }
            compose.onAllNodes(vertical).assertCountEquals(1)
        }
    }

    @Test fun overviewPlacesLatestReleaseBesideSongsAndCategoryActionsStaySeparate() {
        show().use {
            compose.onNode(vertical).performScrollToNode(hasText("Latest Release"))
            val latest = compose.onNodeWithText("Latest Release").fetchSemanticsNode().boundsInRoot
            val songs = compose.onNodeWithText("热门单曲").fetchSemanticsNode().boundsInRoot
            assertTrue(latest.left < songs.left)
            assertEquals(latest.top, songs.top, 1f)
            val releaseBounds = compose.onNodeWithText("最新单曲").fetchSemanticsNode().boundsInRoot
            val horizontal = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
            val songsBounds = compose.onNode(horizontal and hasAnyDescendant(hasText("歌曲 1")))
                .fetchSemanticsNode().boundsInRoot
            assertEquals(releaseBounds.top, songsBounds.top, 1f)
            assertEquals(releaseBounds.bottom, songsBounds.bottom, 1f)
            val releaseImage = compose.onNodeWithContentDescription("最新发行封面", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            val releaseTitle = compose.onNodeWithText("最新单曲", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            assertTrue(releaseImage.right < releaseTitle.left)
            val dateBounds = compose.onNodeWithText("1970/01/01", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val titleBounds = compose.onNodeWithText("最新单曲", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val countBounds = compose.onNodeWithText("1 首歌曲", useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue(dateBounds.bottom <= titleBounds.top)
            assertTrue(titleBounds.bottom <= countBounds.top)
            val fullSongsBounds = compose.onNode(horizontal and hasAnyDescendant(hasText("歌曲 1")))
                .getUnclippedBoundsInRoot()
            val fullImageBounds = compose.onNodeWithContentDescription("最新发行封面", useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            assertEquals(fullSongsBounds.top.value, fullImageBounds.top.value, 1f)
            assertEquals(fullSongsBounds.bottom.value, fullImageBounds.bottom.value, 1f)
            assertEquals((fullImageBounds.right - fullImageBounds.left).value,
                (fullImageBounds.bottom - fullImageBounds.top).value, 1f)
            val firstSong = compose.onNodeWithText("歌曲 1").getUnclippedBoundsInRoot()
            val secondSong = compose.onNodeWithText("歌曲 2").getUnclippedBoundsInRoot()
            assertEquals((firstSong.bottom - firstSong.top).value * 3f,
                (fullSongsBounds.bottom - fullSongsBounds.top).value, 1f)
            assertEquals(firstSong.bottom.value, secondSong.top.value, 1f)
            compose.onNodeWithText("最新单曲").performClick()
            compose.runOnIdle { assertEquals("album:100", operation) }
            compose.onNode(vertical).performScrollToNode(hasText("单曲与 EP"))
            // The first All action is Top Songs, then Albums, then Singles and EPs.
            compose.onAllNodesWithText("全部").onLast().performClick()
            compose.runOnIdle { assertEquals("single-category", operation) }
            compose.onAllNodes(vertical).assertCountEquals(1)
        }
    }

    @Test fun narrowLargeFontOverviewStacksWithoutChangingTheSongQueueIndex() {
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, 1.6f)) {
                    Box(Modifier.requiredSize(600.dp, 700.dp)) {
                        TabletArtistOverview(state, artistDiscography(state.albums).latest, actions)
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val latest = compose.onNodeWithText("Latest Release").fetchSemanticsNode().boundsInRoot
            val songs = compose.onNodeWithText("热门单曲").fetchSemanticsNode().boundsInRoot
            assertTrue(latest.bottom < songs.top)
            assertEquals(latest.left, songs.left, 1f)
            val horizontal = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
            compose.onNode(horizontal).performScrollToNode(hasText("歌曲 11"))
            compose.onNodeWithText("歌曲 11").performClick()
            compose.runOnIdle { assertEquals("song:11:10", operation) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    @Test fun tabletScrollPositionReturnsAfterSwitchingToTheCompactLayout() {
        show().use {
            compose.onNode(vertical).performScrollToNode(hasText("单曲与 EP"))
            compose.waitForIdle()
            fun position() = compose.onNode(vertical).fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange].value()
            val previous = position()
            assertTrue(previous > 0f)
            compose.runOnIdle { window.value = AppWindowLayout.Compact }
            compose.waitForIdle()
            compose.runOnIdle { window.value = AppWindowLayout.Wide }
            compose.waitForIdle()
            assertEquals(previous, position(), 0.001f)
        }
    }

    @Test fun phoneLandscapeKeepsTheExistingAlbumGrid() {
        window.value = AppWindowLayout.Compact
        phoneLandscape.value = true
        show().use {
            compose.onNodeWithContentDescription("播放热门歌曲").performClick()
            compose.runOnIdle { assertEquals("play", operation) }
            compose.onNode(vertical).performScrollToNode(hasText("专辑 1"))
            compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
                .assertCountEquals(0)
            compose.onNodeWithText("专辑 1").performClick()
            compose.runOnIdle { assertEquals("album:1", operation) }
        }
    }

    @Test fun portraitHeaderKeepsThePhotoNameAndActionsAtLargeFontWithoutAWordmark() {
        val artwork = AppleArtistArtwork("1", "测试歌手", AppleArtistImage("", 1000, 1000))
        TabletContentTestActivity.content = {
            MaterialTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, 1.8f)) {
                    Box(Modifier.requiredSize(600.dp, 720.dp)) {
                        TabletArtistHeader(state, artwork,
                            ArtistArtworkPresentation(photo = ArtistArtworkLoadState.Success,
                                tabletPhotoKind = TabletArtistPhotoKind.Portrait),
                            ColorPainter(Color.Blue), 480.dp, 80.dp, actions,
                            onInfo = { operation = "info" }, onWordmarkChanged = {}, artworkModifier = Modifier)
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.onNodeWithContentDescription("歌手照片").assertIsDisplayed()
            compose.onNodeWithContentDescription("测试歌手").assertIsDisplayed()
            compose.onNodeWithContentDescription("播放热门歌曲").performClick()
            compose.runOnIdle { assertEquals("play", operation) }
            compose.onNodeWithContentDescription("歌手简介").performClick()
            compose.runOnIdle { assertEquals("info", operation) }
            compose.onNodeWithContentDescription("收藏歌手").performClick()
            compose.runOnIdle { assertEquals("favorite", operation) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }

    private fun show(): AutoCloseable {
        TabletContentTestActivity.content = {
            MaterialTheme {
                Box(Modifier.requiredSize(if (phoneLandscape.value) 668.dp else 1000.dp,
                    if (phoneLandscape.value) 300.dp else 720.dp)) {
                    CompositionLocalProvider(
                        LocalAppWindowLayout provides window.value,
                        LocalPhoneLandscape provides phoneLandscape.value,
                        LocalPageStartInset provides if (phoneLandscape.value) 72.dp else 0.dp,
                        LocalMeiGlassEnabled provides false,
                        LocalPlayerAwareWindowInsets provides WindowInsets(0),
                    ) { AdaptiveArtistContent(state, actions) }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        compose.waitForIdle()
        return AutoCloseable {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
