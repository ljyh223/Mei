package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
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
        albums = (1L..6L).map { Album(it, "专辑 $it", "", 10, emptyList()) },
        albumsLoading = false, albumsError = null, coverColor = null, canPlay = true,
    )
    private val actions = ArtistDetailActions(
        onBack = {}, onBrowseSongs = { operation = "songs" }, onBrowseAlbums = { operation = "albums" },
        onPlayHotSongs = { operation = "play" }, onTrackClick = { song, index -> operation = "song:${song.id}:$index" },
        onTrackMore = { song, _ -> operation = "more:${song.id}" }, onAlbumClick = { operation = "album:$it" },
        onRetryDetail = {}, onRetrySongs = {}, onRetryAlbums = {}, onToggleSubscription = { operation = "favorite" },
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

    @Test fun tabletScrollPositionReturnsAfterSwitchingToTheCompactLayout() {
        show().use {
            compose.onNode(vertical).performScrollToNode(hasText("歌曲 11"))
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
