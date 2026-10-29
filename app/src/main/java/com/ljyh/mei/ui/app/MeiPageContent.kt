package com.ljyh.mei.ui.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ljyh.mei.ui.screen.album.AlbumDetailScreen
import com.ljyh.mei.ui.screen.artist.ArtistAlbumsScreen
import com.ljyh.mei.ui.screen.artist.ArtistScreen
import com.ljyh.mei.ui.screen.artist.ArtistSongsScreen
import com.ljyh.mei.ui.screen.main.findmusic.FindMusicScreen
import com.ljyh.mei.ui.screen.main.home.HomeScreen
import com.ljyh.mei.ui.screen.main.library.LibraryCollection
import com.ljyh.mei.ui.screen.main.library.LibraryCollectionScreen
import com.ljyh.mei.ui.screen.main.library.LibraryScreen
import com.ljyh.mei.ui.screen.main.library.LibraryViewModel
import com.ljyh.mei.ui.screen.playlist.PlaylistScreen
import com.ljyh.mei.ui.screen.setting.SettingScreen

/**
 * Page rendering boundary, independent of route parsing and shell geometry.
 * Each shell chooses a page set. Replacements use the same route structure and navigation
 * entry scope; page-local state restoration remains the responsibility of its owner.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal interface MeiPageContent {
    @Composable fun Home(scrollBehavior: TopAppBarScrollBehavior)
    @Composable fun Discovery(scrollBehavior: TopAppBarScrollBehavior)
    @Composable fun Library(scrollBehavior: TopAppBarScrollBehavior)
    @Composable fun Settings(scrollBehavior: TopAppBarScrollBehavior)
    @Composable fun PageHost(navController: NavHostController, modifier: Modifier, content: @Composable (Modifier) -> Unit) { content(modifier) }
    fun useImmediateTransition(navController: NavHostController, from: String?, to: String?): Boolean = false
    @Composable fun Collection(collection: LibraryCollection, viewModel: LibraryViewModel)
    @Composable fun Playlist(id: Long)
    @Composable fun Album(id: Long)
    @Composable fun Artist(id: String)
    @Composable fun ArtistSongs(id: String)
    @Composable fun ArtistAlbums(id: String)
}

/** Existing responsive pages remain the default until a form factor needs its own presentation. */
@OptIn(ExperimentalMaterial3Api::class)
internal object SharedMeiPageContent : MeiPageContent {
    @Composable override fun Home(scrollBehavior: TopAppBarScrollBehavior) = HomeScreen(scrollBehavior = scrollBehavior)
    @Composable override fun Discovery(scrollBehavior: TopAppBarScrollBehavior) = FindMusicScreen(scrollBehavior = scrollBehavior)
    @Composable override fun Library(scrollBehavior: TopAppBarScrollBehavior) = LibraryScreen(scrollBehavior = scrollBehavior)
    @Composable override fun Settings(scrollBehavior: TopAppBarScrollBehavior) = SettingScreen(scrollBehavior)
    @Composable override fun Collection(collection: LibraryCollection, viewModel: LibraryViewModel) =
        LibraryCollectionScreen(collection = collection, viewModel = viewModel)
    @Composable override fun Playlist(id: Long) = PlaylistScreen(id = id)
    @Composable override fun Album(id: Long) = AlbumDetailScreen(id = id)
    @Composable override fun Artist(id: String) = ArtistScreen(id = id)
    @Composable override fun ArtistSongs(id: String) = ArtistSongsScreen(id = id)
    @Composable override fun ArtistAlbums(id: String) = ArtistAlbumsScreen(id = id)
}
