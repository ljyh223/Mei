package com.ljyh.mei.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.util.fastAny
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.ljyh.mei.di.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.ljyh.mei.ui.screen.about.AboutScreen
import com.ljyh.mei.ui.screen.album.AlbumDetailScreen
import com.ljyh.mei.ui.screen.history.HistoryScreen
import com.ljyh.mei.ui.screen.local.LocalMusicScreen
import com.ljyh.mei.ui.screen.local.LocalSongListScreen
import com.ljyh.mei.ui.screen.main.home.HomeScreen
import com.ljyh.mei.ui.screen.main.library.LibraryScreen
import com.ljyh.mei.ui.screen.playlist.EveryDay
import com.ljyh.mei.ui.screen.playlist.PlaylistScreen
import com.ljyh.mei.ui.screen.search.SearchResultScreen
import com.ljyh.mei.ui.screen.setting.AppearanceSettings
import com.ljyh.mei.ui.screen.artist.ArtistScreen
import com.ljyh.mei.ui.screen.main.findmusic.FindMusicScreen
import com.ljyh.mei.ui.screen.setting.ContentsSetting
import com.ljyh.mei.ui.screen.setting.DownloadManageScreen
import com.ljyh.mei.ui.screen.setting.DownloadSetting
import com.ljyh.mei.ui.screen.setting.PlaySetting
import com.ljyh.mei.ui.screen.setting.SettingScreen
import com.ljyh.mei.ui.screen.log.LogScreen


@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    composable(Screen.Home.route) {
        HomeScreen()
    }

    composable(Screen.Library.route) {
        LibraryScreen()
    }

    composable(Screen.FindMusic.route) {
        FindMusicScreen()
    }

    composable(Screen.Test.route) {
        Test()
    }

    composable(Screen.Setting.route) {
        SettingScreen(scrollBehavior)
    }

    composable(Screen.AppearanceSettings.route) {
        AppearanceSettings(scrollBehavior)
    }

    composable(Screen.ContentSettings.route) {
        ContentsSetting(scrollBehavior)
    }
    composable(Screen.PlaySettings.route){
        PlaySetting(scrollBehavior)
    }

    composable(Screen.DownloadSettings.route) {
        DownloadSetting(scrollBehavior)
    }

    composable(Screen.DownloadManage.route) {
        DownloadManageScreen(scrollBehavior)
    }

    composable(Screen.LocalMusic.route) {
        LocalMusicScreen(scrollBehavior)
    }

    composable(
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
            "folder_id" -> {
                val folderId = name.toLongOrNull()
                val folder = if (folderId != null) {
                    runBlocking { AppDatabase.getDatabase(context).scanFolderDao().getAll().first().find { f -> f.id == folderId } }
                } else null
                filterValue = folder?.path ?: name
                title = filterValue.substringAfterLast('/').ifEmpty { filterValue.substringAfterLast(":") }
            }
            "folder" -> {
                filterValue = name
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
                "folder_id", "folder" -> "folder"
                else -> type
            },
            filterValue = filterValue,
            title = title,
            scrollBehavior = scrollBehavior
        )
    }

    composable(Screen.EveryDay.route){
        EveryDay()
    }
    composable(Screen.About.route) {
        AboutScreen()
    }
    composable(Screen.Log.route) {
        LogScreen()
    }
    composable(
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
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        }
    ) {
        SearchResultScreen(
            query = it.arguments!!.getString("query")!!,
            type= it.arguments!!.getInt("type"),
        )
    }
    composable(
        route = "${Screen.PlayList.route}/{id}",
        arguments = listOf(
            navArgument("id") {
                type = NavType.LongType
            }
        )
    ) {
        PlaylistScreen(id = it.arguments!!.getLong("id"))
    }


    composable(
        route = "${Screen.Album.route}/{id}",
        arguments = listOf(
            navArgument("id") {
                type = NavType.LongType
            }
        )
    ) {
        AlbumDetailScreen(id = it.arguments!!.getLong("id"))
    }
    
    composable(
        route = "${Screen.Artist.route}/{id}",
        arguments = listOf(
            navArgument("id") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistScreen(id = it.arguments!!.getString("id")!!)
    }

    composable(Screen.History.route) {
        HistoryScreen()
    }
}


fun NavController.backToMain() {
    while (!Screen.MainScreens.fastAny { it.route == currentBackStackEntry?.destination?.route }) {
        navigateUp()
    }
}