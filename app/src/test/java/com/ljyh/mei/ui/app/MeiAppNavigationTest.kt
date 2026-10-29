package com.ljyh.mei.ui.app

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
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
}
