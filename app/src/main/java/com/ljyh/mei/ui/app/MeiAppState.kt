package com.ljyh.mei.ui.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.component.navigation.isMainDestination
import com.ljyh.mei.ui.component.navigation.selectMainDestination
import com.ljyh.mei.ui.component.sheet.dismissedAnchor
import com.ljyh.mei.ui.screen.Index
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.main.library.LibrarySearchState
import com.ljyh.mei.ui.screen.search.recordSearchHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Shared navigation and user intent; each form factor owns its layout and player-sheet geometry. */
@Stable
internal class MeiAppState(
    val navController: NavHostController,
    val playerConnection: PlayerConnection?,
    val librarySearchState: LibrarySearchState,
    val startDestination: String,
    activeSearch: MutableState<Boolean>,
    searchQuery: MutableState<TextFieldValue>,
    selectedRoute: MutableState<String>,
    private val context: Context,
    private val scope: CoroutineScope,
    val playerSheetAnchor: MutableIntState = mutableIntStateOf(dismissedAnchor),
    val pageHost: @Composable (MeiAppPagesSpec) -> Unit = { MeiAppPageHost(it) },
) {
    var searchActive by activeSearch
    var query by searchQuery
    var selectedMainRoute by selectedRoute
        private set

    fun onDestinationChanged(route: String?) {
        if (route.isMainDestination()) {
            selectedMainRoute = requireNotNull(route)
            if (!searchActive) query = TextFieldValue()
        }
    }

    fun selectMainDestination(destination: Index) {
        searchActive = false
        selectedMainRoute = destination.route
        navController.selectMainDestination(destination)
    }

    fun submitSearch(text: String, type: Int) {
        if (text.isBlank()) return
        scope.launch { context.recordSearchHistory(text) }
        searchActive = false
        Screen.SearchResult.navigate(navController) {
            addPath(text)
            addPath(type.toString())
        }
    }
}

@Composable
internal fun rememberMeiAppState(playerConnection: PlayerConnection?, startInLibrary: Boolean): MeiAppState {
    val navController = rememberNavController()
    val librarySearch = remember { LibrarySearchState() }
    val active = rememberSaveable { mutableStateOf(false) }
    val query = rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val start = if (startInLibrary) Screen.Library.route else Screen.Home.route
    val selected = rememberSaveable { mutableStateOf(start) }
    val playerAnchor = rememberSaveable { mutableIntStateOf(dismissedAnchor) }
    val pageHost = rememberMeiPageHost()
    val state = remember(navController, playerConnection, librarySearch, active, query, selected, start, context, scope, playerAnchor, pageHost) {
        MeiAppState(navController, playerConnection, librarySearch, start, active, query, selected, context, scope, playerAnchor, pageHost)
    }
    val entry by navController.currentBackStackEntryAsState()
    LaunchedEffect(entry) { state.onDestinationChanged(entry?.destination?.route) }
    return state
}
