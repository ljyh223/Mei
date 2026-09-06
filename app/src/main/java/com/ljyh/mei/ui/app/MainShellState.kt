package com.ljyh.mei.ui.app

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.MiniPlayerBarHeight
import com.ljyh.mei.ui.component.isMainDestination
import com.ljyh.mei.ui.screen.Screen

enum class MainNavigationPresentation { Hidden, Sidebar, BottomBar }

@Immutable
data class MainShellState(
    val route: String?,
    val isSearchActive: Boolean,
    val navigation: MainNavigationPresentation,
    val isMainDestination: Boolean,
    val showSearchBar: Boolean,
) {
    val showMainNavigation: Boolean
        get() = navigation != MainNavigationPresentation.Hidden

    val showTabletSidebar: Boolean
        get() = navigation == MainNavigationPresentation.Sidebar

    val showBottomNavigation: Boolean
        get() = navigation == MainNavigationPresentation.BottomBar
}

fun resolveMainShellState(
    route: String?,
    isSearchActive: Boolean,
    useTabletSidebar: Boolean,
): MainShellState {
    val isMainDestination = route.isMainDestination()
    val navigation = when {
        isSearchActive || !isMainDestination -> MainNavigationPresentation.Hidden
        useTabletSidebar -> MainNavigationPresentation.Sidebar
        else -> MainNavigationPresentation.BottomBar
    }
    return MainShellState(
        route = route,
        isSearchActive = isSearchActive,
        navigation = navigation,
        isMainDestination = isMainDestination,
        showSearchBar = isSearchActive ||
            route == Screen.Home.route ||
            route?.startsWith("search_result/") == true,
    )
}

/**
 * The player's collapsed anchor must not include transient navigation chrome.
 * Bottom-navigation clearance is animated separately so it cannot be interpreted as sheet
 * expansion progress when destinations change.
 */
fun collapsedPlayerBound(
    systemBottomInset: Dp,
): Dp = systemBottomInset +
    MiniPlayerBarHeight

fun playerAwareBottomInset(
    systemBottomInset: Dp,
    navigationOffset: Dp,
    showMiniPlayer: Boolean,
): Dp = systemBottomInset + navigationOffset.coerceAtLeast(0.dp) +
    (if (showMiniPlayer) MiniPlayerBarHeight else 0.dp)

fun playerBottomNavigationOffset(
    systemBottomInset: Dp,
    scaffoldBottomInset: Dp,
    visibilityProgress: Float = 1f,
): Dp = (scaffoldBottomInset - systemBottomInset).coerceAtLeast(0.dp) *
    visibilityProgress.coerceIn(0f, 1f)
