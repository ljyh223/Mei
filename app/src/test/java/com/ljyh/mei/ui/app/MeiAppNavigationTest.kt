package com.ljyh.mei.ui.app

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import com.ljyh.mei.ui.component.navigation.selectMainDestination
import com.ljyh.mei.ui.screen.Index
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.main.library.LibraryCollection
import com.ljyh.mei.ui.screen.main.library.LibrarySearchState
import com.ljyh.mei.ui.screen.navigationBuilder
import com.ljyh.mei.ui.tablet.TabletPageContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class MeiAppNavigationTest {
    // These tests construct the production graph without composing page contents.
    private val scrollBehavior = object : TopAppBarScrollBehavior {
        override val state: TopAppBarState get() = error("Pages are not composed in navigation tests")
        override val isPinned = true
        override val snapAnimationSpec: AnimationSpec<Float>? = null
        override val flingAnimationSpec: DecayAnimationSpec<Float>? = null
        override val nestedScrollConnection = object : NestedScrollConnection {}
    }

    private fun controller(pages: MeiPageContent) = NavHostController(RuntimeEnvironment.getApplication()).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        setViewModelStore(ViewModelStore())
        installPages(pages)
    }

    private fun NavHostController.installPages(pages: MeiPageContent) {
        graph = createGraph(startDestination = Screen.Home.route) {
            navigationBuilder(this@installPages, scrollBehavior, pages)
        }
    }

    private fun state(nav: NavHostController) = MeiAppState(
        navController = nav,
        playerConnection = null,
        librarySearchState = LibrarySearchState(),
        startDestination = Screen.Home.route,
        activeSearch = mutableStateOf(false),
        searchQuery = mutableStateOf(TextFieldValue()),
        selectedRoute = mutableStateOf(Screen.Home.route),
        context = RuntimeEnvironment.getApplication(),
        scope = CoroutineScope(Dispatchers.Unconfined),
    )

    @Test
    fun bothPageSetsKeepCollectionParentAndDetailArguments() {
        for (pages in listOf(SharedMeiPageContent, TabletPageContent)) {
            val nav = controller(pages)
            nav.navigate(Screen.Library.route)
            val library = nav.currentBackStackEntry!!
            nav.navigate("${Screen.LibraryCollection.route}/${LibraryCollection.Created.name}")
            val collection = nav.currentBackStackEntry!!
            assertSame(library, nav.getBackStackEntry(Screen.Library.route))
            nav.navigate("${Screen.PlayList.route}/42")
            assertEquals(42L, nav.currentBackStackEntry!!.arguments!!.getLong("id"))
            assertTrue(nav.popBackStack())
            assertSame(collection, nav.currentBackStackEntry)
            assertTrue(nav.popBackStack())
            assertSame(library, nav.currentBackStackEntry)
        }
    }

    @Test
    fun changingPageSetPreservesCurrentEntryAndSavedState() {
        val nav = controller(SharedMeiPageContent)
        nav.navigate(Screen.Library.route)
        val library = nav.currentBackStackEntry!!
        nav.navigate("${Screen.Artist.route}/55")
        val artist = nav.currentBackStackEntry!!
        artist.savedStateHandle["scroll_marker"] = 24
        nav.installPages(TabletPageContent)
        assertSame(artist, nav.currentBackStackEntry)
        assertEquals("55", nav.currentBackStackEntry!!.arguments!!.getString("id"))
        assertEquals(24, nav.currentBackStackEntry!!.savedStateHandle.get<Int>("scroll_marker"))
        assertTrue(nav.popBackStack())
        assertSame(library, nav.currentBackStackEntry)
    }

    @Test
    fun restoringTabDetailAlsoRestoresItsSelectedNavigationItem() {
        val nav = controller(TabletPageContent)
        val state = state(nav)
        state.selectMainDestination(Index.Library)
        nav.navigate("${Screen.Artist.route}/55")
        val artist = nav.currentBackStackEntry!!
        artist.savedStateHandle["scroll_marker"] = 12
        state.selectMainDestination(Index.Home)
        state.selectMainDestination(Index.Library)
        state.onDestinationChanged(nav.currentDestination?.route)
        assertEquals("${Screen.Artist.route}/{id}", nav.currentDestination!!.route)
        assertEquals(artist.id, nav.currentBackStackEntry!!.id)
        assertEquals(12, nav.currentBackStackEntry!!.savedStateHandle.get<Int>("scroll_marker"))
        assertEquals(Screen.Library.route, state.selectedMainRoute)
    }

    @Test
    fun sidebarBackPopsOneEntryAndStopsAtTheOriginatingTab() {
        val nav = controller(SharedMeiPageContent)
        val state = state(nav)
        val home = nav.currentBackStackEntry!!
        assertFalse(state.canNavigateBackFromDetail())
        nav.navigate("${Screen.PlayList.route}/42")
        assertTrue(state.canNavigateBackFromDetail())
        assertEquals(Index.Home.route, state.selectedMainRoute)
        state.navigateBackFromDetail()
        assertSame(home, nav.currentBackStackEntry)

        state.selectMainDestination(Index.Library)
        val library = nav.currentBackStackEntry!!
        nav.navigate("${Screen.PlayList.route}/55")
        val playlist = nav.currentBackStackEntry!!
        playlist.savedStateHandle["scroll_marker"] = 24
        nav.navigate("${Screen.Album.route}/66")
        val album = nav.currentBackStackEntry!!

        state.selectMainDestination(Index.Home)
        state.selectMainDestination(Index.Library)
        assertEquals(album.id, nav.currentBackStackEntry!!.id)
        assertEquals(Index.Library.route, state.selectedMainRoute)
        assertTrue(state.canNavigateBackFromDetail())
        state.navigateBackFromDetail()
        assertEquals(playlist.id, nav.currentBackStackEntry!!.id)
        assertEquals(24, nav.currentBackStackEntry!!.savedStateHandle.get<Int>("scroll_marker"))
        assertTrue(state.canNavigateBackFromDetail())
        state.navigateBackFromDetail()
        assertEquals(library.id, nav.currentBackStackEntry!!.id)
        assertFalse(state.canNavigateBackFromDetail())

        state.navigateBackFromDetail()
        assertEquals(library.id, nav.currentBackStackEntry!!.id)
    }

    @Test
    fun sidebarBackLetsThePageCloseSearchBeforePoppingTheRoute() {
        val nav = controller(SharedMeiPageContent)
        val state = state(nav)
        val home = nav.currentBackStackEntry
        nav.navigate("${Screen.ArtistSongs.route}/42")
        val catalog = nav.currentBackStackEntry
        var searchActive = true
        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                searchActive = false
                isEnabled = false
            }
        })

        state.navigateBackFromDetail(dispatcher)
        assertFalse(searchActive)
        assertSame(catalog, nav.currentBackStackEntry)

        state.navigateBackFromDetail(dispatcher)
        assertSame(home, nav.currentBackStackEntry)
    }

    @Test
    fun detailNavigationPreservesQueryAndActiveSearchIsNotClearedByRootNavigation() {
        val state = state(controller(TabletPageContent))
        state.onDestinationChanged(Screen.Library.route)
        state.query = TextFieldValue("歌单")
        state.onDestinationChanged("${Screen.PlayList.route}/{id}")
        assertEquals(Screen.Library.route, state.selectedMainRoute)
        assertEquals("歌单", state.query.text)
        state.searchActive = true
        state.onDestinationChanged(Screen.Home.route)
        assertEquals("歌单", state.query.text)
        state.searchActive = false
        state.onDestinationChanged(Screen.Library.route)
        assertEquals("", state.query.text)
    }

    @Test
    fun sharedSearchSubmissionDefaultsToOverviewAndPreservesSpecialCharacters() {
        for (pages in listOf(SharedMeiPageContent, TabletPageContent)) {
            val nav = controller(pages)
            val state = state(nav)
            state.searchActive = true
            state.submitSearch(" AC/DC & A+B ? ")
            val args = nav.currentBackStackEntry!!.arguments!!
            assertEquals("AC/DC & A+B ?", args.getString("query"))
            assertEquals(1018, args.getInt("type"))
            assertFalse(state.searchActive)
        }
    }
    class HomeStateMarker : ViewModel() {
        var loadedPages = 3
    }

    @Test
    fun homeStateAndOwnerSurviveTabSwitchAndDetailReturn() {
        for (pages in listOf(SharedMeiPageContent, TabletPageContent)) {
            val nav = controller(pages)
            val home = nav.currentBackStackEntry!!
            val factory = ViewModelProvider.NewInstanceFactory()
            val model = ViewModelProvider(home, factory)[HomeStateMarker::class.java]
            home.savedStateHandle["home_scroll_marker"] = 12

            repeat(2) {
                nav.selectMainDestination(Index.FindMusic)
                nav.selectMainDestination(Index.Home)
                val returned = nav.currentBackStackEntry!!
                assertSame(model, ViewModelProvider(returned, factory)[HomeStateMarker::class.java])
                assertEquals(3, model.loadedPages)
                assertEquals(12, returned.savedStateHandle.get<Int>("home_scroll_marker"))
                nav.navigate("${Screen.PlayList.route}/42")
                assertTrue(nav.popBackStack())
                assertSame(model, ViewModelProvider(nav.currentBackStackEntry!!, factory)[HomeStateMarker::class.java])
                assertEquals(12, nav.currentBackStackEntry!!.savedStateHandle.get<Int>("home_scroll_marker"))
            }
        }
    }

    @Test fun personalCenterSharesLibraryParentAndReturnsToSameEntry() {
        for (pages in listOf(SharedMeiPageContent, TabletPageContent)) {
            val nav = controller(pages)
            nav.selectMainDestination(Index.Library)
            val library = nav.currentBackStackEntry!!
            library.savedStateHandle["library_scroll_marker"] = 9
            nav.navigate(Screen.PersonalCenter.route)
            assertEquals(Screen.PersonalCenter.route, nav.currentDestination!!.route)
            assertSame(library, nav.getBackStackEntry(Screen.Library.route))
            nav.navigate(Screen.ContentSettings.route)
            assertTrue(nav.popBackStack())
            assertEquals(Screen.PersonalCenter.route, nav.currentDestination!!.route)
            assertTrue(nav.popBackStack())
            assertSame(library, nav.currentBackStackEntry)
            assertEquals(9, library.savedStateHandle.get<Int>("library_scroll_marker"))
        }
    }

}
