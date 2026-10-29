package com.ljyh.mei.ui.tablet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ljyh.mei.constants.AppBarHeight
import com.ljyh.mei.ui.app.BottomSearchOverlay
import com.ljyh.mei.ui.app.MeiAppPages
import com.ljyh.mei.ui.app.MeiAppState
import com.ljyh.mei.ui.app.collapsedPlayerBound
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.glass.LocalMeiHazeState
import com.ljyh.mei.ui.component.player.state.SyncPlayerSheetVisibility
import com.ljyh.mei.ui.component.sheet.rememberBottomSheetState
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.motion.PageNavigationMotion
import com.ljyh.mei.ui.motion.PlayerMotionSpec
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.tablet.component.TabletBottomBar
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Tablet browsing shell. Mobile compacting, side rails and mini-player offsets do not enter here. */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TabletApp(appState: MeiAppState, pageMotion: PageNavigationMotion) {
    val focus = LocalFocusManager.current
    val density = LocalDensity.current
    val backStack by appState.navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val selectedRoute = appState.selectedMainRoute
    val connection = appState.playerConnection
    val metadata = connection?.mediaMetadata?.collectAsState()?.value
    val playing = connection?.isPlaying?.collectAsState()?.value ?: false
    val canNext = connection?.canSkipNext?.collectAsState()?.value ?: false
    val searchActive = appState.searchActive
    val librarySearch = route == Screen.Library.route
    val searchFocus = remember { FocusRequester() }
    val pageHaze = rememberHazeState()
    val searchHaze = rememberHazeState()
    val glass = LocalMeiGlassEnabled.current
    val topBarScroll = TopAppBarDefaults.pinnedScrollBehavior()
    val submit: (String, Int) -> Unit = { query, type ->
        if (!librarySearch) appState.submitSearch(query, type)
        focus.clearFocus()
    }
    LaunchedEffect(searchActive) {
        if (searchActive) runCatching { searchFocus.requestFocus() }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val bars = WindowInsets.systemBars
        val bottom = with(density) { bars.getBottom(this).toDp() }
        val sheet = rememberBottomSheetState(
            dismissedBound = 0.dp,
            collapsedBound = collapsedPlayerBound(bottom),
            expandedBound = maxHeight,
            allowDismissGesture = false,
            savedAnchor = appState.playerSheetAnchor,
        )
        SyncPlayerSheetVisibility(appState.playerConnection, sheet)
        var barHeight by remember {
            mutableStateOf(tabletBarLayout(maxWidth.value, density.fontScale).controlHeight + 24.dp + bottom)
        }
        val contentInsets = bars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            .add(WindowInsets(top = AppBarHeight, bottom = barHeight))
        CompositionLocalProvider(
            LocalPlayerAwareWindowInsets provides contentInsets,
            LocalMeiHazeState provides pageHaze,
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize()
                    .windowInsetsPadding(if (searchActive) WindowInsets.ime else WindowInsets(0)),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    Box(Modifier.onSizeChanged { barHeight = with(density) { it.height.toDp() } }) {
                        CompositionLocalProvider(LocalMeiHazeState provides if (searchActive) searchHaze else pageHaze) {
                            TabletBottomBar(
                                selectedRoute = selectedRoute,
                                metadata = metadata,
                                playing = playing,
                                canSkipNext = canNext,
                                visibility = PlayerMotionSpec.BottomNavigationExit.reverse(sheet.progress),
                                searchActive = searchActive,
                                query = appState.query,
                                searchPlaceholder = if (librarySearch) "搜索歌单、专辑" else "搜索歌曲、歌手、专辑",
                                searchFocusRequester = searchFocus,
                                onSelect = { tab ->
                                    appState.selectMainDestination(tab)
                                    focus.clearFocus()
                                },
                                onOpenPlayer = { focus.clearFocus(); sheet.expandSoft() },
                                onPlayPause = {
                                    val player = connection?.player ?: return@TabletBottomBar
                                    if (player.playbackState == Player.STATE_ENDED) {
                                        player.seekTo(0, 0)
                                        player.playWhenReady = true
                                    } else if (player.isPlaying) player.pause() else player.play()
                                },
                                onNext = { connection?.seekToNext() },
                                onSearchToggle = {
                                    appState.searchActive = !searchActive
                                    if (!appState.searchActive) focus.clearFocus()
                                },
                                onQueryChange = { appState.query = it },
                                onSubmit = { submit(it, 1) },
                            )
                        }
                    }
                },
            ) { padding ->
                MeiAppPages(
                    appState = appState,
                    pageMotion = pageMotion,
                    scrollBehavior = topBarScroll,
                    pages = TabletPageContent,
                    modifier = Modifier.fillMaxSize().then(if (glass) Modifier.hazeSource(pageHaze) else Modifier),
                )
                BottomSearchOverlay(
                    active = searchActive,
                    librarySearch = librarySearch,
                    hazeState = searchHaze,
                    bottomBarClearance = padding.calculateBottomPadding(),
                    query = appState.query,
                    onQueryChange = { appState.query = it },
                    onSubmit = submit,
                    onDismiss = { appState.searchActive = false; focus.clearFocus() },
                )
            }
            TabletPlayerHost(sheet)
        }
    }
}
