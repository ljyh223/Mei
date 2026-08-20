package com.ljyh.mei.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.kyant.backdrop.Backdrop
import com.ljyh.mei.ui.screen.Index
import com.ljyh.mei.ui.screen.Screen

const val TabletNavigationAnimationDurationMillis = 240

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
    shouldShow: Boolean,
    backdrop: Backdrop? = null,
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
    sidebarModifier: Modifier = Modifier,
    bottomBarModifier: Modifier = Modifier,
) {
    if (useSidebar) {
        AnimatedVisibility(
            visible = shouldShow,
            modifier = sidebarModifier,
            enter = slideInHorizontally(
                animationSpec = tween(
                    durationMillis = TabletNavigationAnimationDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
                initialOffsetX = { -it },
            ) + fadeIn(
                animationSpec = tween(TabletNavigationAnimationDurationMillis),
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = TabletNavigationAnimationDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
                targetOffsetX = { -it },
            ) + fadeOut(
                animationSpec = tween(TabletNavigationAnimationDurationMillis),
            ),
        ) {
            TabletNavigationRail(
                selectedRoute = selectedRoute,
                onTabSelect = onTabSelect,
            )
        }
    } else {
        FloatingCapsuleNavigationBar(
            shouldShow = shouldShow,
            backdrop = backdrop,
            selectedRoute = selectedRoute,
            onTabSelect = onTabSelect,
            modifier = bottomBarModifier,
        )
    }
}
