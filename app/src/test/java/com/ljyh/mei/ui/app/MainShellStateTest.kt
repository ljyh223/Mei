package com.ljyh.mei.ui.app

import com.ljyh.mei.ui.screen.Screen
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainShellStateTest {
    @Test
    fun tabletMainDestinationUsesSidebar() {
        val state = resolveMainShellState(
            route = Screen.Library.route,
            isSearchActive = false,
            useTabletSidebar = true,
        )

        assertEquals(MainNavigationPresentation.Sidebar, state.navigation)
        assertTrue(state.showTabletSidebar)
        assertFalse(state.showBottomNavigation)
    }

    @Test
    fun secondaryDestinationHidesNavigation() {
        val state = resolveMainShellState(
            route = Screen.PlayList.route,
            isSearchActive = false,
            useTabletSidebar = true,
        )

        assertEquals(MainNavigationPresentation.Hidden, state.navigation)
    }

    @Test
    fun activeSearchHidesNavigationButKeepsSearchBar() {
        val state = resolveMainShellState(
            route = Screen.Home.route,
            isSearchActive = true,
            useTabletSidebar = false,
        )

        assertEquals(MainNavigationPresentation.Hidden, state.navigation)
        assertTrue(state.showSearchBar)
    }

    @Test
    fun playerBoundsAlwaysIncludeSystemGestureInset() {
        val systemInset = 24.dp
        val scaffoldInset = 104.dp
        val navigationOffset = 80.dp

        assertTrue(collapsedPlayerBound(systemInset) > systemInset)
        assertTrue(playerAwareBottomInset(systemInset, 0.dp, true) > systemInset)
        assertEquals(systemInset, playerAwareBottomInset(systemInset, 0.dp, false))
        assertEquals(scaffoldInset, playerAwareBottomInset(systemInset, navigationOffset, false))
        assertEquals(navigationOffset, playerBottomNavigationOffset(systemInset, scaffoldInset))
    }

    @Test
    fun navigationProgressMovesPlayerBetweenMainAndSecondaryPositions() {
        val systemInset = 24.dp
        val scaffoldInset = 104.dp

        assertEquals(
            80.dp,
            playerBottomNavigationOffset(systemInset, scaffoldInset, visibilityProgress = 1f),
        )
        assertEquals(
            40.dp,
            playerBottomNavigationOffset(systemInset, scaffoldInset, visibilityProgress = 0.5f),
        )
        assertEquals(
            0.dp,
            playerBottomNavigationOffset(systemInset, scaffoldInset, visibilityProgress = 0f),
        )
    }
}
