package com.ljyh.mei.ui.screen.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.domain.SearchAlbum
import com.ljyh.mei.data.model.domain.SearchArtist
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
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
@Config(sdk = [36], qualifiers = "w600dp-h1000dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class SearchAlbumRowLayoutTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun phoneRowsReserveTheDiscWidthAndKeepTextAndNavigationAvailableAtLargeFonts() {
        val album = SearchAlbum(7, "Album title", "", 10, listOf(SearchArtist(8, "Artist", null, emptyList())))
        var width by mutableStateOf(360.dp)
        var best by mutableStateOf(false)
        var scale by mutableFloatStateOf(1f)
        var density = 1f
        var opened = 0L
        TabletContentTestActivity.content = {
            MaterialTheme {
                val currentDensity = LocalDensity.current
                SideEffect { density = currentDensity.density }
                CompositionLocalProvider(LocalAppWindowLayout provides AppWindowLayout.Compact,
                    LocalDensity provides Density(currentDensity.density, scale)) {
                    Box(Modifier.width(width).height(500.dp)) {
                        SearchAlbumRow(album, listOf("Album"), { opened = it.id }, bestMatch = best)
                    }
                }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            for (size in listOf(320.dp, 360.dp, 412.dp)) for (font in listOf(1f, 1.5f, 2f)) for (match in listOf(false, true)) {
                compose.runOnIdle { width = size; scale = font; best = match }
                compose.waitForIdle()
                val title = compose.onNodeWithText(album.title, useUnmergedTree = true)
                val bounds = title.fetchSemanticsNode().boundsInRoot
                val coverSize = if (match) 64f else 56f
                assertTrue(bounds.left >= (20f + coverSize * 1.2f + 16f) * density - 1f)
                assertTrue(bounds.width > 0f)
                assertTrue(bounds.right <= (size.value - 20f) * density + 1f)
                title.performClick()
                compose.runOnIdle { assertEquals(7L, opened); opened = 0L }
            }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
