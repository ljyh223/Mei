package com.ljyh.mei.ui.app

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.collectAsState
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ljyh.mei.constants.AlwaysExpandMiniPlayerKey
import com.ljyh.mei.constants.AppBarHeight
import com.ljyh.mei.ui.component.navigation.AppBottomNavigationBar
import com.ljyh.mei.ui.component.navigation.compactPlayerVerticalDrop
import com.ljyh.mei.ui.component.player.BottomSheetPlayer
import com.ljyh.mei.ui.component.player.state.SyncPlayerSheetVisibility
import com.ljyh.mei.ui.component.sheet.rememberBottomSheetState
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.motion.PlayerMotionSpec
import com.ljyh.mei.ui.motion.PageNavigationMotion
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.search.SearchType
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.glass.LocalMeiHazeState
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.screen.setting.canShowPhoneSettingsSplit
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.ljyh.mei.utils.preferences.rememberPreference
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun PhoneApp(appState: MeiAppState, pageMotion: PageNavigationMotion) {
    val navController = appState.navController
    val playerConnection = appState.playerConnection
    val searchActive = appState.searchActive
    val useGlass = LocalMeiGlassEnabled.current
    val alwaysExpandMiniPlayer by rememberPreference(AlwaysExpandMiniPlayerKey, false)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val focusManager = LocalFocusManager.current
        val density = LocalDensity.current
        // A keyboard can make a portrait viewport wider than it is tall.
        val configuration = LocalConfiguration.current
        val sideBar = usePhoneSideBar(configuration.screenWidthDp.toFloat(), configuration.screenHeightDp.toFloat())
        val pages = if (sideBar) {
            val direction = LocalLayoutDirection.current
            val safe = WindowInsets.safeDrawing.asPaddingValues()
            val contentWidth = maxWidth - safe.calculateLeftPadding(direction) - safe.calculateRightPadding(direction) - PhoneSideBarWidth
            if (canShowPhoneSettingsSplit(contentWidth, density.fontScale)) SplitSettingsPageContent
            else SharedMeiPageContent
        } else SharedMeiPageContent
        val systemBars = WindowInsets.systemBars
        val windowInsets = if (sideBar) systemBars.union(WindowInsets.displayCutout) else systemBars
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val route = navBackStackEntry?.destination?.route
        val bottomInset by remember(density, windowInsets) {
            derivedStateOf {
                with(density) { windowInsets.getBottom(density).toDp() }
            }
        }
        val shellState = resolveMainShellState(
            route = route,
            isSearchActive = searchActive,
        )
        val playerBottomSheetState = rememberBottomSheetState(
            dismissedBound = 0.dp,
            collapsedBound = collapsedPlayerBound(
                systemBottomInset = bottomInset,
            ).coerceAtMost(maxHeight / 2),
            expandedBound = maxHeight,
            allowDismissGesture = false,
            savedAnchor = appState.playerSheetAnchor,
        )
        var navigationExpanded by rememberSaveable { mutableStateOf(true) }
        val compactNavigationProgress by animateFloatAsState(
            targetValue = if (sideBar || alwaysExpandMiniPlayer || navigationExpanded) 0f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "compactNavigation",
        )
        val compactNavigationAllowed = rememberUpdatedState(
            !alwaysExpandMiniPlayer && shellState.showBottomNavigation && !searchActive && !sideBar &&
                (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed),
        )
        val navigationScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (!compactNavigationAllowed.value) return Offset.Zero
                    if (abs(available.y) <= abs(available.x) || abs(available.y) < 10f) {
                        return Offset.Zero
                    }
                    navigationExpanded = available.y > 0f
                    return Offset.Zero
                }
            }
        }
        LaunchedEffect(route) {
            navigationExpanded = true
        }
        LaunchedEffect(sideBar, alwaysExpandMiniPlayer) {
            if (sideBar || alwaysExpandMiniPlayer) navigationExpanded = true
        }
        val routeBottomChromeAnimationSpec = remember {
            PlayerMotionSpec.tween<Float>()
        }
        val showBottomBar = !sideBar && (shellState.showBottomNavigation || searchActive)
        val routeBottomNavigationProgress by animateFloatAsState(
            targetValue = if (showBottomBar) 1f else 0f,
            animationSpec = routeBottomChromeAnimationSpec,
            label = "routeBottomNavigationProgress",
        )
        val playerBottomNavigationProgress =
            PlayerMotionSpec.BottomNavigationExit.reverse(playerBottomSheetState.progress)
        val bottomNavigationProgress =
            if (searchActive) 1f else routeBottomNavigationProgress * playerBottomNavigationProgress
        val canScrollAppBar = {
            route?.startsWith("search_result/") == false &&
                (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
        }
        val searchBarScrollBehavior = appBarScrollBehavior(canScroll = canScrollAppBar)
        val topAppBarScrollBehavior = appBarScrollBehavior(canScroll = canScrollAppBar)
        val bottomSearchFocusRequester = remember { FocusRequester() }
        val query = appState.query
        val onQueryChange: (TextFieldValue) -> Unit = { appState.query = it }
        val onActiveChange: (Boolean) -> Unit = { active ->
            appState.searchActive = active
            if (!active) focusManager.clearFocus()
        }
        val librarySearch = route == Screen.Library.route
        val submitBottomSearch: (String, Int) -> Unit = { searchQuery, type ->
            if (librarySearch) focusManager.clearFocus()
            else {
                appState.submitSearch(searchQuery, type)
                if (!appState.searchActive) focusManager.clearFocus()
            }
        }
        LaunchedEffect(navBackStackEntry) {
            searchBarScrollBehavior.state.resetHeightOffset()
            topAppBarScrollBehavior.state.resetHeightOffset()
        }
        LaunchedEffect(searchActive, sideBar) {
            if (searchActive) {
                searchBarScrollBehavior.state.resetHeightOffset()
                topAppBarScrollBehavior.state.resetHeightOffset()
                navigationExpanded = true
                runCatching { bottomSearchFocusRequester.requestFocus() }
            }
        }

        val pageHazeState = rememberHazeState()
        val searchHazeState = rememberHazeState()
        SyncPlayerSheetVisibility(playerConnection, playerBottomSheetState)
        CompositionLocalProvider(
            LocalMeiHazeState provides pageHazeState,
        ) {
            Scaffold(
                // One owner resizes the search viewport and consumes IME insets for its children.
                modifier = Modifier.fillMaxSize()
                    .windowInsetsPadding(if (searchActive && !sideBar) WindowInsets.ime else WindowInsets(0))
                    .nestedScroll(navigationScrollConnection),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    // Search chrome samples its own page, never the hidden browsing content.
                    if (!sideBar) CompositionLocalProvider(
                        LocalMeiHazeState provides if (searchActive) searchHazeState else pageHazeState,
                    ) {
                        AppBottomNavigationBar(
                            visibilityProgress = bottomNavigationProgress,
                            alphaProgress = if (searchActive) 1f else routeBottomNavigationProgress,
                            interactive = showBottomBar && (searchActive || playerBottomNavigationProgress >= 1f),
                            selectedRoute = route,
                            onTabSelect = appState::selectMainDestination,
                            compactProgress = compactNavigationProgress,
                            onExpand = { navigationExpanded = true },
                            searchActive = searchActive,
                            searchPlaceholder = if (librarySearch) "搜索歌单、专辑" else "搜索歌曲、歌手、专辑",
                            searchQuery = query,
                            onSearchQueryChange = onQueryChange,
                            onSearchActivate = { onActiveChange(true) },
                            onSearchClose = { onActiveChange(false) },
                            onSearchSubmit = { submitBottomSearch(it, SearchType.All.type) },
                            searchFocusRequester = bottomSearchFocusRequester,
                        )
                    }
                },
            ) { scaffoldPadding ->
                val scaffoldBottomInset = scaffoldPadding.calculateBottomPadding()
                val playerNavigationOffset = playerBottomNavigationOffset(
                    systemBottomInset = bottomInset,
                    scaffoldBottomInset = scaffoldBottomInset,
                    visibilityProgress = routeBottomNavigationProgress,
                )
                val playerAwareWindowInsets = remember(
                    sideBar,
                    windowInsets,
                    bottomInset,
                    playerNavigationOffset,
                    playerBottomSheetState.isDismissed,
                ) {
                    val bottom = if (sideBar) bottomInset else playerAwareBottomInset(
                        systemBottomInset = bottomInset,
                        navigationOffset = playerNavigationOffset,
                        showMiniPlayer = !playerBottomSheetState.isDismissed,
                    )
                    windowInsets
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                }

                val hasPlayer = !playerBottomSheetState.isDismissed && playerConnection != null
                val imeInsets = if (searchActive && sideBar) WindowInsets.ime else WindowInsets(0)
                CompositionLocalProvider(
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        CompositionLocalProvider(
                            LocalPageStartInset provides if (sideBar) PhoneSideBarWidth else 0.dp,
                            LocalPhoneLandscape provides sideBar,
                        ) {
                            MeiAppPages(
                                appState = appState,
                                pageMotion = pageMotion,
                                scrollBehavior = topAppBarScrollBehavior,
                                pages = pages,
                                modifier = Modifier.fillMaxSize()
                                    .then(if (useGlass) Modifier.hazeSource(pageHazeState) else Modifier)
                                    .background(MaterialTheme.colorScheme.background),
                            )
                            BottomSearchOverlay(
                                active = searchActive,
                                hazeState = searchHazeState,
                                librarySearch = librarySearch,
                                bottomBarClearance = if (sideBar) bottomInset else scaffoldBottomInset,
                                query = query,
                                onQueryChange = onQueryChange,
                                onSubmit = submitBottomSearch,
                                onDismiss = { onActiveChange(false) },
                                topSearchFocus = bottomSearchFocusRequester.takeIf { sideBar },
                                modifier = Modifier.windowInsetsPadding(imeInsets)
                                    .zIndex(if (searchActive && !sideBar) 1f else 0f),
                            )
                        }
                        if (sideBar) {
                            val song = playerConnection?.mediaMetadata?.collectAsState()?.value
                            Box(Modifier.windowInsetsPadding(windowInsets.only(
                                WindowInsetsSides.Start + WindowInsetsSides.Vertical,
                            ))) {
                                CompositionLocalProvider(
                                    LocalMeiHazeState provides if (searchActive) searchHazeState else pageHazeState,
                                ) {
                                    PhoneSideBar(
                                        selectedRoute = appState.selectedMainRoute,
                                        showPlayer = hasPlayer,
                                        coverUrl = song?.coverUrl,
                                        onSelect = appState::selectMainDestination,
                                        onBack = if (appState.canNavigateBackFromDetail(route)) {
                                            {
                                                if (appState.searchActive) onActiveChange(false)
                                                else appState.navigateBackFromDetail()
                                            }
                                        } else null,
                                        onSearch = { onActiveChange(!searchActive) },
                                        onOpenPlayer = {
                                            if (searchActive) onActiveChange(false)
                                            playerBottomSheetState.expandSoft()
                                        },
                                        visibility = playerBottomNavigationProgress,
                                    )
                                }
                            }
                        }
                        BottomSheetPlayer(
                            state = playerBottomSheetState,
                            wideContent = sideBar,
                            collapsedBottomOffset = (playerNavigationOffset - compactPlayerVerticalDrop(density.fontScale) * compactNavigationProgress)
                                .coerceAtLeast(0.dp),
                            systemBottomInset = bottomInset,
                            bottomNavigationVisibilityProgress = routeBottomNavigationProgress,
                            compactProgress = compactNavigationProgress,
                        )
                    }
                }
            }
        }
    }
}
