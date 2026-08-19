package com.ljyh.mei.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@Composable
fun AdaptiveMainNavigation(
    useSidebar: Boolean,
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
    sidebarModifier: Modifier = Modifier,
    bottomBarModifier: Modifier = Modifier,
) {
    if (useSidebar) {
        TabletNavigationRail(
            selectedRoute = selectedRoute,
            onTabSelect = onTabSelect,
            modifier = sidebarModifier,
        )
    } else {
        FloatingCapsuleNavigationBar(
            shouldShow = true,
            selectedRoute = selectedRoute,
            onTabSelect = onTabSelect,
            modifier = bottomBarModifier,
        )
    }
}
