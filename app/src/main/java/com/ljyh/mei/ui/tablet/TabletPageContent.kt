package com.ljyh.mei.ui.tablet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import com.ljyh.mei.ui.app.MeiPageContent
import com.ljyh.mei.ui.app.SplitSettingsPageContent
import com.ljyh.mei.ui.tablet.search.TabletSearchResultScreen
import com.ljyh.mei.ui.screen.main.library.LibraryScreen
import com.ljyh.mei.ui.screen.artist.ArtistScreen
import com.ljyh.mei.ui.screen.album.AlbumDetailScreen
import com.ljyh.mei.ui.screen.main.library.component.AdaptiveLibraryContent
import com.ljyh.mei.ui.tablet.artist.TabletArtistContent
import com.ljyh.mei.ui.tablet.album.TabletAlbumContent

/** One route and business owner per page, with separate wide-window presentation. */
@OptIn(ExperimentalMaterial3Api::class)
internal object TabletPageContent : MeiPageContent by SplitSettingsPageContent {
    @Composable override fun Search(query: String, type: Int) { TabletSearchResultScreen(query, type) }
    @Composable override fun PersonalCenter(viewModel: com.ljyh.mei.ui.screen.main.library.LibraryViewModel) {
        com.ljyh.mei.ui.screen.profile.PersonalCenterScreen(viewModel, content = { state, actions ->
            com.ljyh.mei.ui.tablet.profile.TabletPersonalCenterContent(state, actions)
        })
    }
    @Composable override fun Library(scrollBehavior: TopAppBarScrollBehavior) {
        LibraryScreen(scrollBehavior = scrollBehavior, content = { state, actions -> AdaptiveLibraryContent(state, actions) })
    }
    @Composable override fun Artist(id: String) {
        ArtistScreen(id = id, content = { state, actions -> TabletArtistContent(state, actions) })
    }
    @Composable override fun Album(id: Long) {
        AlbumDetailScreen(id = id, content = { state, actions -> TabletAlbumContent(state, actions) })
    }
}
