package com.ljyh.mei.ui.app

import android.os.Build
import android.app.ActivityManager
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ljyh.mei.constants.AlwaysExpandMiniPlayerKey
import com.ljyh.mei.constants.AppBarHeight
import com.ljyh.mei.constants.LiquidGlassEnabledKey
import com.ljyh.mei.constants.DynamicThemeKey
import com.ljyh.mei.constants.DefaultThemeColor
import com.ljyh.mei.constants.PageTransitionStyle
import com.ljyh.mei.constants.PageTransitionStyleKey
import com.ljyh.mei.constants.PageTransitionSpeed
import com.ljyh.mei.constants.PageTransitionSpeedKey
import com.ljyh.mei.constants.ThemeColorKey
import com.ljyh.mei.data.model.domain.UserData
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.repository.ColorRepository
import com.ljyh.mei.ui.component.navigation.AdaptiveMainNavigationRail
import com.ljyh.mei.ui.screen.main.library.LibrarySearchState
import com.ljyh.mei.ui.screen.main.library.LocalLibrarySearchState
import com.ljyh.mei.ui.component.navigation.AppBottomNavigationBar
import com.ljyh.mei.ui.component.navigation.compactPlayerVerticalDrop
import com.ljyh.mei.ui.component.navigation.TabletNavigationRailWidth
import com.ljyh.mei.ui.component.navigation.selectMainDestination
import com.ljyh.mei.ui.component.player.BottomSheetPlayer
import com.ljyh.mei.ui.component.player.state.SyncPlayerSheetVisibility
import com.ljyh.mei.ui.component.player.state.rememberPlayerConnection
import com.ljyh.mei.ui.component.sheet.rememberBottomSheetState
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalDatabase
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.local.LocalUserData
import com.ljyh.mei.ui.motion.PlayerMotionSpec
import com.ljyh.mei.ui.motion.PageNavigationMotion
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.screen.navigationBuilder
import com.ljyh.mei.ui.screen.search.recordSearchHistory
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.glass.LocalMeiHazeState
import com.ljyh.mei.ui.component.glass.LocalMeiGlassAppearance
import com.ljyh.mei.utils.preferences.rememberGlassAppearance
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.ljyh.mei.ui.component.utils.rememberBatteryIntensiveFeaturesAllowed
import com.ljyh.mei.ui.theme.MusicTheme
import com.ljyh.mei.ui.theme.rememberPlayerThemeColor
import com.ljyh.mei.utils.preferences.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class NavigationTab { Home, Library }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeiApp(
    database: AppDatabase,
    colorRepository: ColorRepository,
    userData: UserData,
    startInLibrary: Boolean,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val librarySearchState = remember { LibrarySearchState() }
    val glassEnabled by rememberPreference(LiquidGlassEnabledKey, defaultValue = true)
    val alwaysExpandMiniPlayer by rememberPreference(AlwaysExpandMiniPlayerKey, defaultValue = false)
    val glassAppearance by rememberGlassAppearance()
    val pageTransitionStyle by rememberEnumPreference(PageTransitionStyleKey, PageTransitionStyle.Slide)
    val pageTransitionSpeed by rememberEnumPreference(PageTransitionSpeedKey, PageTransitionSpeed.Standard)
    val pageMotion = remember(pageTransitionStyle, pageTransitionSpeed) {
        PageNavigationMotion(pageTransitionStyle, pageTransitionSpeed)
    }
    val dynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
    val themeColor by rememberPreference(ThemeColorKey, defaultValue = DefaultThemeColor)
    val staticThemeColor = remember(themeColor) {
        runCatching { Color(android.graphics.Color.parseColor(themeColor)) }
            .getOrDefault(Color(0xFF0061A4))
    }
    val playerConnection = rememberPlayerConnection(context, database)

    val targetThemeColor = rememberPlayerThemeColor(
        context = context,
        playerConnection = playerConnection,
        dynamicTheme = dynamicTheme,
        colorRepository = colorRepository,
    )
    MusicTheme(
        seedColor = if (dynamicTheme) targetThemeColor else staticThemeColor,
        isDark = isSystemInDarkTheme(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            val focusManager = LocalFocusManager.current
            val density = LocalDensity.current
            val device = rememberDeviceInfo()
            val useTabletSidebar = device.isTablet && device.isLandscape
            val systemBars = WindowInsets.systemBars
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val route = navBackStackEntry?.destination?.route
            val bottomInset by remember {
                derivedStateOf {
                    with(density) { systemBars.getBottom(density).toDp() }
                }
            }
            val shellState = resolveMainShellState(
                route = route,
                isSearchActive = searchActive,
                useTabletSidebar = useTabletSidebar,
            )
            val navigationStartPadding by animateDpAsState(
                targetValue = if (shellState.showTabletSidebar) {
                    TabletNavigationRailWidth
                } else {
                    0.dp
                },
                animationSpec = PlayerMotionSpec.tween(),
                label = "mainNavigationStartPadding",
            )
            val playerBottomSheetState = rememberBottomSheetState(
                dismissedBound = 0.dp,
                collapsedBound = collapsedPlayerBound(
                    systemBottomInset = bottomInset,
                ),
                expandedBound = maxHeight,
                allowDismissGesture = false,
            )
            var navigationExpanded by rememberSaveable { mutableStateOf(true) }
            val compactNavigationProgress by animateFloatAsState(
                targetValue = if (alwaysExpandMiniPlayer || navigationExpanded) 0f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "compactNavigation",
            )
            val compactNavigationAllowed = rememberUpdatedState(
                !alwaysExpandMiniPlayer && shellState.showBottomNavigation && !searchActive && !device.isLandscape &&
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
            LaunchedEffect(device.isLandscape, alwaysExpandMiniPlayer) {
                if (device.isLandscape || alwaysExpandMiniPlayer) navigationExpanded = true
            }
            val activity = context as? Activity
            val requestedOrientation = if (device.isTablet || playerBottomSheetState.isExpanded) {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            SideEffect {
                if (activity != null && activity.requestedOrientation != requestedOrientation) {
                    activity.requestedOrientation = requestedOrientation
                }
            }
            val routeBottomChromeAnimationSpec = remember {
                PlayerMotionSpec.tween<Float>()
            }
            val showBottomBar = shellState.showBottomNavigation ||
                (searchActive && !useTabletSidebar)
            val routeBottomNavigationProgress by animateFloatAsState(
                targetValue = if (showBottomBar) 1f else 0f,
                animationSpec = routeBottomChromeAnimationSpec,
                label = "routeBottomNavigationProgress",
            )
            val playerBottomNavigationProgress =
                PlayerMotionSpec.BottomNavigationExit.reverse(playerBottomSheetState.progress)
            val bottomNavigationProgress =
                routeBottomNavigationProgress * playerBottomNavigationProgress
            val canScrollAppBar = {
                route?.startsWith("search_result/") == false &&
                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
            }
            val searchBarScrollBehavior = appBarScrollBehavior(canScroll = canScrollAppBar)
            val topAppBarScrollBehavior = appBarScrollBehavior(canScroll = canScrollAppBar)
            val searchBarFocusRequester = remember { FocusRequester() }
            val bottomSearchFocusRequester = remember { FocusRequester() }
            val (query, onQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue())
            }
            val onActiveChange: (Boolean) -> Unit = { active ->
                searchActive = active
                if (!active) focusManager.clearFocus()
            }
            val librarySearch = route == Screen.Library.route
            val searchScope = rememberCoroutineScope()
            val submitBottomSearch: (String, Int) -> Unit = { searchQuery, type ->
                if (librarySearch) {
                    focusManager.clearFocus()
                } else if (searchQuery.isNotBlank()) {
                    searchScope.launch { context.recordSearchHistory(searchQuery) }
                    onActiveChange(false)
                    Screen.SearchResult.navigate(navController) {
                        addPath(android.net.Uri.encode(searchQuery.trim()))
                        addPath(type.toString())
                    }
                }
            }
            val topLevelScreens = remember {
                setOf(
                    Screen.Home.route,
                    Screen.FindMusic.route,
                    Screen.Library.route,
                    Screen.Setting.route,
                )
            }
            val startTab = remember(startInLibrary) {
                if (startInLibrary) {
                    NavigationTab.Library
                } else {
                    NavigationTab.Home
                }
            }

            LaunchedEffect(navBackStackEntry) {
                searchBarScrollBehavior.state.resetHeightOffset()
                topAppBarScrollBehavior.state.resetHeightOffset()
                if (route in topLevelScreens && !searchActive) onQueryChange(TextFieldValue())
            }
            LaunchedEffect(searchActive) {
                if (searchActive) {
                    searchBarScrollBehavior.state.resetHeightOffset()
                    topAppBarScrollBehavior.state.resetHeightOffset()
                    navigationExpanded = true
                    if (!useTabletSidebar) {
                        runCatching { bottomSearchFocusRequester.requestFocus() }
                    }
                }
            }

            val pageHazeState = rememberHazeState()
            val searchHazeState = rememberHazeState()
            val batteryAllowsGlass = rememberBatteryIntensiveFeaturesAllowed()
            val lowRam = remember(context) {
                context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
            }
            val useGlass = glassEnabled && batteryAllowsGlass && !lowRam && Build.VERSION.SDK_INT >= 33
            SyncPlayerSheetVisibility(playerConnection, playerBottomSheetState)
            CompositionLocalProvider(
                LocalMeiHazeState provides pageHazeState,
                LocalMeiGlassEnabled provides useGlass,
                LocalMeiGlassAppearance provides glassAppearance,
            ) {
                Scaffold(
                    // One owner resizes the search viewport and consumes IME insets for its children.
                    modifier = Modifier.fillMaxSize()
                        .windowInsetsPadding(if (searchActive && !useTabletSidebar) WindowInsets.ime else WindowInsets(0))
                        .nestedScroll(navigationScrollConnection),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    contentWindowInsets = WindowInsets(0),
                    bottomBar = {
                        if (!useTabletSidebar) {
                            // Search chrome samples its own page, never the hidden browsing content.
                            CompositionLocalProvider(
                                LocalMeiHazeState provides if (searchActive) searchHazeState else pageHazeState,
                            ) {
                                AppBottomNavigationBar(
                                    visibilityProgress = bottomNavigationProgress,
                                    alphaProgress = routeBottomNavigationProgress,
                                    interactive = showBottomBar && playerBottomNavigationProgress >= 1f,
                                    selectedRoute = route,
                                    onTabSelect = navController::selectMainDestination,
                                    compactProgress = compactNavigationProgress,
                                    onExpand = { navigationExpanded = true },
                                    searchActive = searchActive,
                                    searchPlaceholder = if (librarySearch) "搜索歌单、专辑" else "搜索歌曲、歌手、专辑",
                                    searchQuery = query,
                                    onSearchQueryChange = onQueryChange,
                                    onSearchActivate = { onActiveChange(true) },
                                    onSearchClose = { onActiveChange(false) },
                                    onSearchSubmit = { submitBottomSearch(it, com.ljyh.mei.ui.screen.search.SearchType.All.type) },
                                    searchFocusRequester = bottomSearchFocusRequester,
                                )
                            }
                        }
                    },
                ) {
                    val scaffoldBottomInset = it.calculateBottomPadding()
                    val playerNavigationOffset = playerBottomNavigationOffset(
                        systemBottomInset = bottomInset,
                        scaffoldBottomInset = scaffoldBottomInset,
                        visibilityProgress = routeBottomNavigationProgress,
                    )
                    val playerAwareWindowInsets = remember(
                        bottomInset,
                        playerNavigationOffset,
                        playerBottomSheetState.isDismissed,
                    ) {
                        val bottom = playerAwareBottomInset(
                            systemBottomInset = bottomInset,
                            navigationOffset = playerNavigationOffset,
                            showMiniPlayer = !playerBottomSheetState.isDismissed,
                        )
                        systemBars
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                    }

                    CompositionLocalProvider(
                        LocalLibrarySearchState provides librarySearchState,
                        LocalDatabase provides database,
                        LocalNavController provides navController,
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalUserData provides userData,
                    ) {
                        NavHost(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = navigationStartPadding)
                                .then(if (useGlass) Modifier.hazeSource(pageHazeState) else Modifier)
                                .background(MaterialTheme.colorScheme.background),
                            navController = navController,
                            enterTransition = { pageMotion.enter(this) },
                            exitTransition = { pageMotion.exit(this) },
                            popEnterTransition = { pageMotion.enter(this, isPop = true) },
                            popExitTransition = { pageMotion.exit(this, isPop = true) },
                            startDestination = when (startTab) {
                                NavigationTab.Home -> Screen.Home.route
                                NavigationTab.Library -> Screen.Library.route
                            },
                        ) {
                            navigationBuilder(navController, topAppBarScrollBehavior)
                        }

                        if (useTabletSidebar) {
                            AppSearchOverlay(
                                shellState = shellState,
                                librarySearch = librarySearch,
                                query = query,
                                onQueryChange = onQueryChange,
                                onActiveChange = onActiveChange,
                                onSubmit = { searchQuery, type ->
                                    if (librarySearch) {
                                        focusManager.clearFocus()
                                    } else if (searchQuery.isNotEmpty()) {
                                        onActiveChange(false)
                                        Screen.SearchResult.navigate(navController) {
                                            addPath(android.net.Uri.encode(searchQuery.trim()))
                                            addPath(type.toString())
                                        }
                                    }
                                },
                                onNavigateUp = { navController.navigateUp() },
                                onBackToMain = { navController.backToMain() },
                                onOpenSettings = { navController.navigate(Screen.Setting.route) },
                                onOpenAudioMatch = {
                                    onActiveChange(false)
                                    Screen.AudioMatch.navigate(navController)
                                },
                                isTopLevelRoute = route in topLevelScreens,
                                scrollBehavior = searchBarScrollBehavior,
                                focusRequester = searchBarFocusRequester,
                            )
                        }

                        AdaptiveMainNavigationRail(
                            useSidebar = useTabletSidebar,
                            shouldShow = shellState.showMainNavigation,
                            selectedRoute = route,
                            onTabSelect = navController::selectMainDestination,
                            sidebarModifier = Modifier.align(Alignment.CenterStart),
                        )
                        BottomSheetPlayer(
                            state = playerBottomSheetState,
                            collapsedBottomOffset = (playerNavigationOffset - compactPlayerVerticalDrop(density.fontScale) * compactNavigationProgress)
                                .coerceAtLeast(0.dp),
                            systemBottomInset = bottomInset,
                            bottomNavigationVisibilityProgress = routeBottomNavigationProgress,
                            compactProgress = compactNavigationProgress,
                        )
                        BottomSearchOverlay(
                            active = searchActive && !useTabletSidebar,
                            hazeState = searchHazeState,
                            librarySearch = librarySearch,
                            bottomBarClearance = scaffoldBottomInset,
                            query = query,
                            onQueryChange = onQueryChange,
                            onSubmit = submitBottomSearch,
                            onDismiss = { onActiveChange(false) },
                        )
                    }
                }
            }
        }
    }
}
