package com.ljyh.music.ui.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.util.fastAny
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ljyh.music.ui.component.player.Test
import com.ljyh.music.ui.screen.index.home.HomeScreen
import com.ljyh.music.ui.screen.index.library.LibraryScreen
import com.ljyh.music.ui.screen.playlist.PlaylistScreen
import com.ljyh.music.ui.screen.setting.AppearanceSettings
import com.ljyh.music.ui.screen.setting.ContentsSetting
import com.ljyh.music.ui.screen.setting.SettingScreen


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
}


fun NavController.backToMain() {
    while (!Screen.MainScreens.fastAny { it.route == currentBackStackEntry?.destination?.route }) {
        navigateUp()
    }
}