package com.ljyh.mei.ui.screen.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.SearchAlbum
import com.ljyh.mei.data.model.domain.SearchArtist
import com.ljyh.mei.data.model.domain.SearchPlaylist
import com.ljyh.mei.data.model.domain.SearchResults
import com.ljyh.mei.ui.app.PhoneSideBarWidth
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.MeiPageLayout
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.MeiDestinationSurface
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1000dp-h480dp", manifest = Config.NONE)
class LandscapeSearchResultsTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun phoneArtworkCategoriesUseCollectionGridAndKeepTheirClickTargets() {
        val type = mutableStateOf(SearchType.Artist)
        val width = mutableStateOf(690.dp)
        var density = 1f
        var opened: Pair<SearchType, Long>? = null
        val actions = SearchActions(
            onSong = { _, _, _ -> },
            onMore = { _, _ -> },
            onArtist = { opened = SearchType.Artist to it.id },
            onAlbum = { opened = SearchType.Album to it.id },
            onPlaylist = { opened = SearchType.Playlist to it.id },
            onType = {},
            playingId = null,
        )
        show {
            density = LocalDensity.current.density
            val category = type.value
            val data = when (category) {
                SearchType.Artist -> SearchResults(artists = (1L..10L).map { SearchArtist(it, "结果 $it", null, emptyList()) })
                SearchType.Album -> SearchResults(albums = (1L..10L).map { SearchAlbum(it, "结果 $it", "", 2, emptyList()) })
                SearchType.Playlist -> SearchResults(playlists = (1L..10L).map { SearchPlaylist(it, "结果 $it", "", 100, "创建者", "", 3) })
                else -> error("Expected an artwork category")
            }
            LandscapeViewport(width.value) {
                SearchResultPage(resultState(category, data, actions))
            }
        }.use {
            for (category in listOf(SearchType.Artist, SearchType.Album, SearchType.Playlist)) {
                for ((windowWidth, columns) in listOf(690.dp to 4, 800.dp to 5)) {
                    compose.runOnIdle {
                        type.value = category
                        width.value = windowWidth
                        opened = null
                    }
                    val first = bounds("结果 1")
                    val lastInRow = compose.onNodeWithText("结果 $columns").assertIsDisplayed()
                    assertEquals(category.name, first.top, lastInRow.fetchSemanticsNode().boundsInRoot.top, 1f)
                    assertTrue(category.name, bounds("结果 ${columns + 1}").top > first.top)
                    assertEquals(12f * density, bounds("结果 2").left - first.right, 1f)
                    val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
                    val start = 40.dp + PhoneSideBarWidth + MeiPageLayout.PhoneGutter
                    assertEquals(start.value * density, first.left - viewport.left, 1f)
                    lastInRow.performClick()
                    compose.runOnIdle { assertEquals(category to columns.toLong(), opened) }
                }
            }
        }
    }

    @Test fun songResultsKeepPlaybackIndexAndHideTableColumnsWhenTheWindowNarrows() {
        val width = mutableStateOf(800.dp)
        val songs = (1L..3L).map { MediaMetadata(it, "歌曲 $it", "", emptyList(), 90000, MediaMetadata.Album(it, "专辑 $it")) }
        var playedIndex: Int? = null
        val actions = SearchActions(
            onSong = { queue, index, section ->
                assertEquals(songs, queue)
                assertEquals("songs", section)
                playedIndex = index
            },
            onMore = { _, _ -> },
            onArtist = {},
            onAlbum = {},
            onPlaylist = {},
            onType = {},
            playingId = null,
        )
        show {
            LandscapeViewport(width.value) {
                SearchResultPage(resultState(SearchType.Song, SearchResults(songs = songs), actions))
            }
        }.use {
            compose.onNodeWithText("时长").assertIsDisplayed()
            compose.onNodeWithText("歌曲 2").performClick()
            compose.runOnIdle {
                assertEquals(1, playedIndex)
                width.value = 650.dp
            }
            compose.onNodeWithText("时长").assertDoesNotExist()
            compose.onNodeWithText("歌曲 2").assertIsDisplayed()
        }
    }

    private fun resultState(type: SearchType, data: SearchResults, actions: SearchActions) =
        SearchResultContentState(SearchPageState(data = data, loading = false), type, "结果", actions, {}, {}, 56.dp)

    private fun bounds(title: String) = compose.onNodeWithText(title).assertIsDisplayed().fetchSemanticsNode().boundsInRoot

    @Composable
    private fun LandscapeViewport(width: Dp, content: @Composable () -> Unit) {
        Box(Modifier.size(width, 420.dp).testTag("viewport")) {
            CompositionLocalProvider(
                LocalAppWindowLayout provides AppWindowLayout.Compact,
                LocalPhoneLandscape provides true,
                LocalPageStartInset provides PhoneSideBarWidth,
                LocalPlayerAwareWindowInsets provides WindowInsets(0),
            ) {
                MeiDestinationSurface(
                    drawsBehindSideBar = true,
                    horizontalSafeInsets = WindowInsets(left = 40.dp),
                    content = content,
                )
            }
        }
    }

    private fun show(content: @Composable () -> Unit): AutoCloseable {
        TabletContentTestActivity.content = { MaterialTheme { content() } }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        return AutoCloseable {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
