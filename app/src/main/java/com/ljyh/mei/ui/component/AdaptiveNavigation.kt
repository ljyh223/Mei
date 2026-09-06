package com.ljyh.mei.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.ljyh.mei.ui.motion.PlayerMotionSpec
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
fun AdaptiveMainNavigationRail(
    useSidebar: Boolean,
    shouldShow: Boolean,
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
    sidebarModifier: Modifier = Modifier,
) {
    if (useSidebar) {
        AnimatedVisibility(
            visible = shouldShow,
            modifier = sidebarModifier,
            enter = slideInHorizontally(
                animationSpec = PlayerMotionSpec.tween(),
                initialOffsetX = { -it },
            ) + fadeIn(
                animationSpec = PlayerMotionSpec.tween(),
            ),
            exit = slideOutHorizontally(
                animationSpec = PlayerMotionSpec.tween(),
                targetOffsetX = { -it },
            ) + fadeOut(
                animationSpec = PlayerMotionSpec.tween(),
            ),
        ) {
            TabletNavigationRail(
                selectedRoute = selectedRoute,
                onTabSelect = onTabSelect,
            )
        }
    }
}
