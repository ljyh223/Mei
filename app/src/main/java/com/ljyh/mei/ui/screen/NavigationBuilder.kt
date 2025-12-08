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
import com.ljyh.mei.ui.screen.album.AlbumScreen
import com.ljyh.mei.ui.screen.history.HistoryScreen
import com.ljyh.mei.ui.screen.index.home.HomeScreen
import com.ljyh.mei.ui.screen.index.library.LibraryScreen
import com.ljyh.mei.ui.screen.playlist.EveryDay
import com.ljyh.mei.ui.screen.playlist.PlaylistScreen
import com.ljyh.mei.ui.screen.search.SearchResultScreen
import com.ljyh.mei.ui.screen.setting.AppearanceSettings
import com.ljyh.mei.ui.screen.artist.ArtistScreen
import com.ljyh.mei.ui.screen.setting.ContentsSetting
import com.ljyh.mei.ui.screen.setting.SettingScreen


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

    composable(Screen.EveryDay.route){
        EveryDay()
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
        AlbumScreen(id = it.arguments!!.getLong("id"))
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