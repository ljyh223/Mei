package com.ljyh.mei.ui.tablet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ljyh.mei.ui.app.MeiPageContent
import com.ljyh.mei.ui.app.SharedMeiPageContent
import com.ljyh.mei.ui.tablet.search.TabletSearchResultScreen
import com.ljyh.mei.ui.screen.main.library.LibraryScreen
import com.ljyh.mei.ui.screen.artist.ArtistScreen
import com.ljyh.mei.ui.screen.album.AlbumDetailScreen
import com.ljyh.mei.ui.tablet.library.TabletLibraryContent
import com.ljyh.mei.ui.tablet.artist.TabletArtistContent
import com.ljyh.mei.ui.tablet.album.TabletAlbumContent

/** One route and business owner per page, with separate wide-window presentation. */
@OptIn(ExperimentalMaterial3Api::class)
internal object TabletPageContent : MeiPageContent by SharedMeiPageContent {
    @Composable override fun Search(query: String, type: Int) { TabletSearchResultScreen(query, type) }
    @Composable override fun Settings(scrollBehavior: TopAppBarScrollBehavior) {
        TabletSettingsHome(scrollBehavior)
    }
    override fun useImmediateTransition(navController: NavHostController, from: String?, to: String?): Boolean =
        navController.isSettingsDetailTransition(from, to)

    @Composable override fun PageHost(navController: NavHostController, modifier: Modifier, content: @Composable (Modifier) -> Unit) {
        TabletSettingsHost(navController, modifier, content)
    }

    @Composable override fun Library(scrollBehavior: TopAppBarScrollBehavior) {
        LibraryScreen(scrollBehavior = scrollBehavior, content = { state, actions -> TabletLibraryContent(state, actions) })
    }
    @Composable override fun Artist(id: String) {
        ArtistScreen(id = id, content = { state, actions -> TabletArtistContent(state, actions) })
    }
    @Composable override fun Album(id: Long) {
        AlbumDetailScreen(id = id, content = { state, actions -> TabletAlbumContent(state, actions) })
    }
}
