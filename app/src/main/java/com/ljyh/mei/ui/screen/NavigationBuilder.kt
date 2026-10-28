package com.ljyh.mei.ui.screen

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.util.fastAny
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import java.net.URLDecoder
import com.ljyh.mei.ui.screen.about.AboutScreen
import com.ljyh.mei.ui.screen.album.AlbumDetailScreen
import com.ljyh.mei.ui.screen.history.HistoryScreen
import com.ljyh.mei.ui.screen.local.LocalMusicScreen
import com.ljyh.mei.ui.screen.local.LocalSongListScreen
import com.ljyh.mei.ui.screen.main.home.HomeScreen
import com.ljyh.mei.ui.screen.main.library.LibraryScreen
import com.ljyh.mei.ui.screen.main.library.LibraryCollection
import com.ljyh.mei.ui.screen.main.library.LibraryCollectionScreen
import com.ljyh.mei.ui.screen.playlist.EveryDay
import com.ljyh.mei.ui.screen.playlist.PlaylistScreen
import com.ljyh.mei.ui.screen.search.SearchResultScreen
import com.ljyh.mei.ui.screen.setting.GlassSettingsScreen
import com.ljyh.mei.ui.screen.setting.AppearanceSettingsScreen
import com.ljyh.mei.ui.screen.setting.BetaSettingsScreen
import com.ljyh.mei.ui.screen.setting.LyricsSettingsScreen
import com.ljyh.mei.ui.screen.artist.ArtistScreen
import com.ljyh.mei.ui.screen.artist.ArtistSongsScreen
import com.ljyh.mei.ui.screen.artist.ArtistAlbumsScreen
import com.ljyh.mei.ui.screen.main.findmusic.DiscoverPlaylistsScreen
import com.ljyh.mei.ui.screen.main.findmusic.DiscoverAlbumsScreen
import com.ljyh.mei.ui.screen.main.findmusic.DiscoverChartsScreen
import com.ljyh.mei.ui.screen.main.findmusic.FindMusicScreen
import com.ljyh.mei.ui.screen.main.findmusic.AudioMatchScreen
import com.ljyh.mei.ui.screen.setting.ContentsSettingScreen
import com.ljyh.mei.ui.screen.setting.AudioOutputSettingsScreen
import com.ljyh.mei.ui.screen.setting.SourceAndLyricsSettingsScreen
import com.ljyh.mei.ui.screen.setting.DownloadManageScreen
import com.ljyh.mei.ui.screen.setting.DownloadSettingScreen
import com.ljyh.mei.ui.screen.setting.CacheSettingsScreen
import com.ljyh.mei.ui.screen.setting.UsbExclusiveSettingsScreen
import com.ljyh.mei.ui.screen.setting.PlaySettingScreen
import com.ljyh.mei.ui.screen.setting.EqualizerSettingScreen
import com.ljyh.mei.ui.screen.setting.ReverbSettingScreen
import com.ljyh.mei.ui.screen.setting.LxSourcesScreen
import com.ljyh.mei.ui.screen.setting.SettingScreen
import com.ljyh.mei.ui.screen.setting.DeveloperSettingsScreen
import com.ljyh.mei.ui.screen.log.LogScreen
import com.ljyh.mei.ui.screen.comment.CommentScreen


@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    pageDestination(Screen.Home.route) {
        HomeScreen(scrollBehavior = scrollBehavior)
    }

    pageDestination(Screen.Library.route) {
        LibraryScreen(scrollBehavior = scrollBehavior)
    }

    pageDestination(
        "${Screen.LibraryCollection.route}/{collection}",
        arguments = listOf(navArgument("collection") { type = NavType.StringType }),
    ) { entry ->
        val parentEntry = remember(entry) { navController.getBackStackEntry(Screen.Library.route) }
        val collection = LibraryCollection.entries.firstOrNull {
            it.name == entry.arguments?.getString("collection")
        } ?: LibraryCollection.Created
        LibraryCollectionScreen(collection = collection, viewModel = hiltViewModel(parentEntry))
    }

    pageDestination(Screen.FindMusic.route) {
        FindMusicScreen(scrollBehavior = scrollBehavior)
    }

    pageDestination(
        route = "${Screen.DiscoverPlaylists.route}?category={category}",
        arguments = listOf(navArgument("category") { type = NavType.StringType; defaultValue = "全部" }),
    ) { DiscoverPlaylistsScreen() }
    pageDestination(Screen.DiscoverAlbums.route) { DiscoverAlbumsScreen() }
    pageDestination(Screen.DiscoverCharts.route) { DiscoverChartsScreen() }

    pageDestination(Screen.AudioMatch.route) {
        AudioMatchScreen(scrollBehavior = scrollBehavior)
    }

    pageDestination(Screen.Test.route) {
        Test()
    }

    pageDestination(Screen.Setting.route) {
        SettingScreen(scrollBehavior)
    }

    pageDestination(Screen.GlassSettings.route) {
        GlassSettingsScreen()
    }
    pageDestination(Screen.AppearanceSettings.route) {
        AppearanceSettingsScreen(scrollBehavior)
    }

    pageDestination(Screen.BetaSettings.route) {
        BetaSettingsScreen(scrollBehavior)
    }

    pageDestination(Screen.LyricsSettings.route) {
        LyricsSettingsScreen(scrollBehavior)
    }

    pageDestination(Screen.LxSources.route) { LxSourcesScreen(scrollBehavior) }

    pageDestination(Screen.ContentSettings.route) {
        ContentsSettingScreen(scrollBehavior)
    }
    pageDestination(Screen.SourceAndLyricsSettings.route) { SourceAndLyricsSettingsScreen() }
    pageDestination(Screen.AudioOutputSettings.route) { AudioOutputSettingsScreen() }
    pageDestination(Screen.UsbExclusiveSettings.route) {
        UsbExclusiveSettingsScreen(scrollBehavior)
    }
    pageDestination(Screen.PlaySettings.route){
        PlaySettingScreen(scrollBehavior)
    }

    pageDestination(Screen.DeveloperSettings.route) {
        DeveloperSettingsScreen(scrollBehavior)
    }

    pageDestination(Screen.Equalizer.route) {
        EqualizerSettingScreen(scrollBehavior)
    }

    pageDestination(Screen.ReverbSettings.route) {
        ReverbSettingScreen(scrollBehavior)
    }

    pageDestination(Screen.DownloadSettings.route) {
        DownloadSettingScreen(scrollBehavior)
    }

    pageDestination(Screen.CacheSettings.route) {
        CacheSettingsScreen(scrollBehavior)
    }

    pageDestination(Screen.DownloadManage.route) {
        DownloadManageScreen(scrollBehavior)
    }

    pageDestination(Screen.LocalMusic.route) {
        LocalMusicScreen(scrollBehavior)
    }

    pageDestination(
        route = "${Screen.LocalSongList.route}/{type}/{name}",
        arguments = listOf(
            navArgument("type") { type = NavType.StringType },
            navArgument("name") { type = NavType.StringType }
        )
    ) {
        val type = it.arguments?.getString("type") ?: "all"
        val name = it.arguments?.getString("name") ?: ""
        val context = LocalContext.current

        val filterValue: String
        val title: String

        when (type) {
            "folder" -> {
                filterValue = URLDecoder.decode(name, "UTF-8")
                title = filterValue.substringAfterLast('/').ifEmpty { filterValue.substringAfterLast(":") }
            }
            "artist" -> {
                filterValue = name
                title = name
            }
            "album" -> {
                filterValue = name
                title = name
            }
            else -> {
                filterValue = name
                title = "全部歌曲"
            }
        }

        LocalSongListScreen(
            filterType = when (type) {
                "folder" -> "folder"
                else -> type
            },
            filterValue = filterValue,
            title = title,
            scrollBehavior = scrollBehavior
        )
    }

    pageDestination(Screen.EveryDay.route){
        EveryDay()
    }
    pageDestination(Screen.About.route) {
        AboutScreen()
    }
    pageDestination(Screen.Log.route) {
        LogScreen()
    }
    pageDestination(
        route = "${Screen.SearchResult.route}/{query}/{type}",
        arguments = listOf(
            navArgument("query") {
                type = NavType.StringType
            }
            ,
            navArgument("type") {
                type = NavType.IntType
            }
        ),
    ) {
        SearchResultScreen(
            query = it.arguments!!.getString("query")!!,
            type= it.arguments!!.getInt("type"),
        )
    }
    pageDestination(
        route = "${Screen.PlayList.route}/{id}",
        arguments = listOf(
            navArgument("id") {
                type = NavType.LongType
            }
        )
    ) {
        PlaylistScreen(id = it.arguments!!.getLong("id"))
    }


    pageDestination(
        route = "${Screen.Album.route}/{id}",
        arguments = listOf(
            navArgument("id") {
                type = NavType.LongType
            }
        )
    ) {
        AlbumDetailScreen(id = it.arguments!!.getLong("id"))
    }
    
    pageDestination(
        route = "${Screen.Artist.route}/{id}",
        arguments = listOf(
            navArgument("id") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistScreen(id = it.arguments!!.getString("id")!!)
    }

    pageDestination(
        route = "${Screen.ArtistSongs.route}/{id}",
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) {
        ArtistSongsScreen(id = it.arguments!!.getString("id")!!)
    }

    pageDestination(
        route = "${Screen.ArtistAlbums.route}/{id}",
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) {
        ArtistAlbumsScreen(id = it.arguments!!.getString("id")!!)
    }

    pageDestination(Screen.History.route) {
        HistoryScreen()
    }

    pageDestination(
        route = "${Screen.Comment.route}/{songId}",
        arguments = listOf(
            navArgument("songId") { type = NavType.StringType }
        )
    ) {
        CommentScreen(
            songId = it.arguments!!.getString("songId")!!
        )
    }
}


fun NavController.backToMain() {
    while (!Screen.MainScreens.fastAny { it.route == currentBackStackEntry?.destination?.route }) {
        navigateUp()
    }
}
