package com.ljyh.mei.ui.screen

import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import androidx.navigation.navArgument
import com.ljyh.mei.ui.app.MainNavigationPresentation
import com.ljyh.mei.ui.app.resolveMainShellState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class CollectionNavigationTest {
    private fun controller(): NavHostController = NavHostController(RuntimeEnvironment.getApplication()).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        setViewModelStore(ViewModelStore())
        graph = createGraph(startDestination = Screen.FindMusic.route) {
            pageDestination(Screen.FindMusic.route) { }
            pageDestination(
                "${Screen.DiscoverPlaylists.route}?category={category}",
                listOf(navArgument("category") { type = NavType.StringType; defaultValue = "全部" }),
            ) { }
            pageDestination(Screen.DiscoverAlbums.route) { }
            pageDestination(Screen.DiscoverCharts.route) { }
            pageDestination("${Screen.PlayList.route}/{id}", listOf(navArgument("id") { type = NavType.LongType })) { }
        }
    }

    @Test
    fun collectionAndDetailPopsRestoreTheSameBackStackEntries() {
        val nav = controller()
        val overview = nav.currentBackStackEntry
        nav.navigate(discoverPlaylistsRoute("华语"))
        val collection = nav.currentBackStackEntry!!
        collection.savedStateHandle["scroll_marker"] = 24
        nav.navigate("${Screen.PlayList.route}/42")
        assertTrue(nav.popBackStack())
        assertSame(collection, nav.currentBackStackEntry)
        assertEquals(24, nav.currentBackStackEntry!!.savedStateHandle.get<Int>("scroll_marker"))
        assertTrue(nav.popBackStack())
        assertSame(overview, nav.currentBackStackEntry)
    }

    @Test
    fun categoryArgumentsSurviveReservedCharactersWithoutDoubleDecoding() {
        val nav = controller()
        for (category in listOf("R&B/Soul", "另类/独立", "夜晚 #1+2", "100%")) {
            nav.navigate(discoverPlaylistsRoute(category))
            assertEquals(category, nav.currentBackStackEntry!!.arguments!!.getString("category"))
            assertTrue(nav.popBackStack())
        }
    }

    @Test
    fun independentCollectionRoutesHideRootNavigationAndReturnToDiscovery() {
        val nav = controller()
        for (route in listOf(Screen.DiscoverAlbums.route, Screen.DiscoverCharts.route, Screen.DiscoverPlaylists.route)) {
            nav.navigate(route)
            val shell = resolveMainShellState(nav.currentDestination!!.route, false, false)
            assertEquals(MainNavigationPresentation.Hidden, shell.navigation)
            assertFalse(shell.isMainDestination)
            assertTrue(nav.popBackStack())
            assertEquals(Screen.FindMusic.route, nav.currentDestination!!.route)
        }
    }
}
