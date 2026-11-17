package com.ljyh.mei.ui.screen.main.findmusic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.domain.DiscoverAlbum
import com.ljyh.mei.data.model.domain.DiscoverPlaylist
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.app.PhoneSideBarWidth
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.collection.MeiCollectionPage
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.MeiPageLayout
import com.ljyh.mei.ui.component.track.Track
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
@Config(sdk = [36], qualifiers = "w1000dp-h400dp", manifest = Config.NONE)
class LandscapeCollectionLayoutTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun artworkCollectionsShareTheSameColumnsAndCellWidths() {
        val samples = listOf(
            ArtworkCollection("全部歌单", GridCells.Adaptive(164.dp), albums = false),
            ArtworkCollection("新碟上架", GridCells.Adaptive(164.dp * VinylAlbumArtworkRatio), albums = true),
            ArtworkCollection("我的歌单", GridCells.Adaptive(168.dp), albums = false, edgeToEdge = true),
            ArtworkCollection("收藏专辑", GridCells.Adaptive(168.dp * VinylAlbumArtworkRatio), albums = true, edgeToEdge = true),
            ArtworkCollection("全部专辑", GridCells.Adaptive(148.dp * VinylAlbumArtworkRatio), albums = true),
            ArtworkCollection("手机竖屏网格", GridCells.Fixed(2), albums = false),
        )
        val width = mutableStateOf(690.dp)
        val collection = mutableStateOf(samples.first())
        var density = 1f
        show {
            density = LocalDensity.current.density
            val sample = collection.value
            LandscapeViewport(width.value, edgeToEdge = sample.edgeToEdge) {
                MeiCollectionPage(
                    title = sample.title,
                    itemCount = 10,
                    itemKey = { it },
                    onNavigateBack = {},
                    onRetry = {},
                    artworkColumns = sample.columns,
                ) { index ->
                    val card = Modifier.fillMaxWidth().testTag("cover-$index")
                    if (sample.albums) {
                        DiscoverAlbumCard(DiscoverAlbum(index.toLong(), "专辑", "", "歌手"), card) {}
                    } else {
                        DiscoverPlaylistCard(DiscoverPlaylist(index.toLong(), "歌单", "", 100), card, {}, {})
                    }
                }
            }
        }.use {
            for ((viewportWidth, columns) in listOf(690.dp to 4, 800.dp to 5)) {
                val bodyWidth = viewportWidth - 40.dp - PhoneSideBarWidth - MeiPageLayout.PhoneGutter * 2
                val expectedWidth = (bodyWidth - 12.dp * (columns - 1)) / columns
                for (sample in samples) {
                    compose.runOnIdle {
                        collection.value = sample
                        width.value = viewportWidth
                    }
                    val first = bounds("cover-0")
                    assertEquals(sample.title, first.top, bounds("cover-${columns - 1}").top, 1f)
                    assertTrue(sample.title, bounds("cover-$columns").top > first.top)
                    assertEquals(sample.title, expectedWidth.value * density, first.width, 1f)
                    assertEquals(sample.title, 12f * density, bounds("cover-1").left - first.right, 1f)
                }
            }
        }
    }

    private data class ArtworkCollection(
        val title: String,
        val columns: GridCells,
        val albums: Boolean,
        val edgeToEdge: Boolean = false,
    )

    @Test fun artistTrackReservesTheSidebarAndCutoutOnce() {
        var density = 1f
        show {
            density = LocalDensity.current.density
            LandscapeViewport(690.dp, edgeToEdge = true) {
                MeiCollectionPage(
                    title = "全部歌曲",
                    itemCount = 1,
                    itemKey = { it },
                    onNavigateBack = {},
                    onRetry = {},
                    horizontalContentPadding = 0.dp,
                    verticalItemSpacing = 0.dp,
                ) {
                    Track(
                        track = MediaMetadata(1, "歌曲", "", emptyList(), 0, MediaMetadata.Album(1, "专辑")),
                        onClick = {},
                        onMoreClick = {},
                    )
                }
            }
        }.use {
            val cover = compose.onNodeWithContentDescription("Album Art", useUnmergedTree = true)
                .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val expectedStart = 40.dp + PhoneSideBarWidth + MeiPageLayout.PhoneGutter
            assertEquals(expectedStart.value * density, cover.left - bounds("viewport").left, 1f)
        }
    }

    @Composable
    private fun LandscapeViewport(
        width: Dp,
        edgeToEdge: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        Box(Modifier.size(width, 320.dp).testTag("viewport")) {
            CompositionLocalProvider(
                LocalAppWindowLayout provides AppWindowLayout.Compact,
                LocalPhoneLandscape provides true,
                LocalPageStartInset provides PhoneSideBarWidth,
                LocalPlayerAwareWindowInsets provides WindowInsets(0),
            ) {
                MeiDestinationSurface(
                    drawsBehindSideBar = true,
                    drawsToHorizontalEdgesInLandscape = edgeToEdge,
                    horizontalSafeInsets = WindowInsets(left = 40.dp),
                    content = content,
                )
            }
        }
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot

    private fun show(content: @Composable () -> Unit): AutoCloseable {
        TabletContentTestActivity.content = { MaterialTheme { content() } }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        return AutoCloseable {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
