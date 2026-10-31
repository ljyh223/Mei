package com.ljyh.mei.ui.component.navigation

import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.ljyh.mei.ui.screen.Index
import com.ljyh.mei.ui.screen.Screen

fun String?.isMainDestination(): Boolean = Screen.MainScreens.any { it.route == this }

fun NavHostController.selectMainDestination(destination: Index) {
    if (currentBackStackEntry?.destination?.hierarchy?.any { it.route == destination.route } == true) {
        currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
    } else {
        navigate(destination.route) {
            popUpTo(graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}
