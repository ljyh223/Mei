package com.ljyh.mei.ui.screen.playlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.glass.GlassAppearance
import com.ljyh.mei.ui.component.glass.LocalMeiGlassAppearance
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.glass.MeiTopBarBackground
import com.ljyh.mei.ui.component.glass.meiArtworkTopBarBackground
import com.ljyh.mei.ui.component.glass.resolveMeiTopBarBackground
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.ui.screen.playlist.layout.TwoPanePlaylistLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** State and layout-coordinate assertions only: no artwork, screenshots or visual inspection. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1000dp-h700dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class PlaylistScrollStateTest {
    @get:Rule val compose = createEmptyComposeRule()

    class HostActivity : ComponentActivity() {
        companion object { var content: @Composable () -> Unit = {} }
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent { content() }
        }
    }

    @Test
    fun sampledRowsInsideTopPaddingDoNotReplaceTheReadingAnchorAcrossLayoutAndActivityChanges() {
        val tracks = (1L..100L).map { MediaMetadata(it, "Song $it", "", emptyList(), 1000, MediaMetadata.Album(0, "")) }
        val input = PlaylistUiState(
            uiData = UiPlaylist(42, "歌单", tracks.size, 0, "", emptyList(), "", tracks = tracks),
            isLoading = false,
            headerActionIcon = Icons.Rounded.Favorite,
            headerActionLabel = "收藏",
        )
        var split by mutableStateOf(false)
        var list: LazyListState? = null
        var scope: CoroutineScope? = null
        HostActivity.content = {
            val current = rememberPlaylistScrollState(input, split)
            val currentScope = rememberCoroutineScope()
            SideEffect { list = current; scope = currentScope }
            LazyColumn(
                state = current,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 120.dp, bottom = 70.dp),
            ) {
                item(key = if (split) PlaylistToolsKey else PlaylistHeaderKey) {
                    Box(Modifier.fillMaxWidth().height(if (split) 80.dp else 400.dp))
                }
                items(tracks, key = { it.id }) { Box(Modifier.fillMaxWidth().height(40.dp)) }
            }
        }
        val activity = Robolectric.buildActivity(HostActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.runOnIdle { scope!!.launch { list!!.scrollToItem(6, 15) } }
            compose.waitForIdle()
            assertEquals(6, list!!.firstVisibleItemIndex)
            assertEquals(15, list!!.firstVisibleItemScrollOffset)
            assertTrue("Previous rows remain visible in the toolbar sampling area",
                list!!.layoutInfo.visibleItemsInfo.first().index < list!!.firstVisibleItemIndex)
            compose.runOnIdle { split = true }
            compose.waitForIdle()
            assertEquals(6, list!!.firstVisibleItemIndex)
            assertEquals(15, list!!.firstVisibleItemScrollOffset)
            activity.recreate()
            compose.waitForIdle()
            assertEquals(6, list!!.firstVisibleItemIndex)
            assertEquals(15, list!!.firstVisibleItemScrollOffset)
            compose.runOnIdle { split = false }
            compose.waitForIdle()
            assertEquals(6, list!!.firstVisibleItemIndex)
            assertEquals(15, list!!.firstVisibleItemScrollOffset)
        } finally {
            activity.pause().stop().destroy()
            HostActivity.content = {}
        }
    }

    @Test
    fun firstTrackClearsGlassFadeAndArtworkBackgroundStaysTransparentWhileScrolling() {
        var glass by mutableStateOf(true)
        var reduced by mutableStateOf(false)
        var toolbarBottomPx = 0
        var fadeBottomPx = 0
        var topBarBackground = MeiTopBarBackground.Auto
        var scope: CoroutineScope? = null
        val list = LazyListState()
        val input = PlaylistUiState(
            uiData = UiPlaylist(42, "歌单", 0, 0, "", emptyList(), "", tracks = emptyList()),
            isLoading = true,
            headerActionIcon = Icons.Rounded.Favorite,
            headerActionLabel = "收藏",
        )
        HostActivity.content = {
            val density = LocalDensity.current
            val currentScope = rememberCoroutineScope()
            SideEffect {
                scope = currentScope
                toolbarBottomPx = with(density) { 80.dp.roundToPx() }
                fadeBottomPx = with(density) { (80.dp + 34.dp).roundToPx() }
            }
            CompositionLocalProvider(
                LocalMeiGlassEnabled provides glass,
                LocalMeiGlassAppearance provides GlassAppearance(reduceTransparency = reduced),
                LocalPlayerAwareWindowInsets provides WindowInsets(0),
            ) {
                val policy = meiArtworkTopBarBackground()
                SideEffect { topBarBackground = policy }
                TwoPanePlaylistLayout(PlaylistLayoutScope(
                    state = input,
                    actions = PlaylistActions(onPlayAll = {}, onHeaderAction = {}, onTrackClick = { _, _ -> }, onBack = {}),
                    listState = list,
                    padding = PaddingValues(top = 80.dp),
                    detailPaneWidth = 336.dp,
                    onTrackMore = { _, _ -> },
                    header = {},
                ))
            }
        }
        fun firstTrackTop(): Int {
            val info = list.layoutInfo
            return info.visibleItemsInfo.first { it.key == "playlist_track_placeholder_0" }.offset - info.viewportStartOffset
        }
        val activity = Robolectric.buildActivity(HostActivity::class.java).setup()
        try {
            compose.waitForIdle()
            assertEquals("First row must clear the entire glass fade", fadeBottomPx, firstTrackTop())
            compose.runOnIdle { reduced = true }
            compose.waitForIdle()
            assertEquals(toolbarBottomPx, firstTrackTop())
            compose.runOnIdle { reduced = false; glass = false }
            compose.waitForIdle()
            assertEquals(toolbarBottomPx, firstTrackTop())
            assertEquals(MeiTopBarBackground.Transparent, topBarBackground)
            assertEquals(0f, resolveMeiTopBarBackground(topBarBackground, glass, reduced).opacity, 0f)
            compose.runOnIdle { scope!!.launch { list.scrollToItem(1, 14) } }
            compose.waitForIdle()
            assertEquals("Scrolling must not cover the artwork with a theme-color background",
                0f, resolveMeiTopBarBackground(topBarBackground, glass, reduced, glassProgress = 0.25f).opacity, 0f)
            compose.runOnIdle { scope!!.launch { list.scrollToItem(5) } }
            compose.waitForIdle()
            assertEquals(0f, resolveMeiTopBarBackground(topBarBackground, glass, reduced, glassProgress = 1f).opacity, 0f)
            compose.runOnIdle { reduced = true }
            compose.waitForIdle()
            assertEquals(1f, resolveMeiTopBarBackground(topBarBackground, glass, reduced).opacity, 0f)
        } finally {
            activity.pause().stop().destroy()
            HostActivity.content = {}
        }
    }

}
