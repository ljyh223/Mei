package com.ljyh.mei.ui.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.ljyh.mei.ui.motion.PageNavigationMotion
import com.ljyh.mei.ui.screen.navigationBuilder

/** Inputs change with the chrome; the movable host retains its own page composition. */
@OptIn(ExperimentalMaterial3Api::class)
internal data class MeiAppPagesSpec(
    val navController: NavHostController,
    val startDestination: String,
    val pageMotion: PageNavigationMotion,
    val scrollBehavior: TopAppBarScrollBehavior,
    val pages: MeiPageContent,
    val modifier: Modifier,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeiAppPages(
    appState: MeiAppState,
    pageMotion: PageNavigationMotion,
    scrollBehavior: TopAppBarScrollBehavior,
    pages: MeiPageContent,
    modifier: Modifier = Modifier,
) {
    appState.pageHost(MeiAppPagesSpec(appState.navController, appState.startDestination,
        pageMotion, scrollBehavior, pages, modifier))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeiAppPageHost(spec: MeiAppPagesSpec) = with(spec) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
        enterTransition = { pageMotion.enter(this) },
        exitTransition = { pageMotion.exit(this) },
        popEnterTransition = { pageMotion.enter(this, isPop = true) },
        popExitTransition = { pageMotion.exit(this, isPop = true) },
        builder = { navigationBuilder(navController, scrollBehavior, pages) },
    )
}

/** A single owner for page composition when its chrome container moves across window layouts. */
@Composable
internal fun rememberMeiPageHost(
    renderer: @Composable (MeiAppPagesSpec) -> Unit = { MeiAppPageHost(it) },
): @Composable (MeiAppPagesSpec) -> Unit {
    val current by rememberUpdatedState(renderer)
    return remember { movableContentOf<MeiAppPagesSpec> { current(it) } }
}
