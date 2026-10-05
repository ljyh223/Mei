package com.ljyh.mei.ui.app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.ljyh.mei.constants.AppBarHeight
import com.ljyh.mei.constants.DynamicThemeKey
import com.ljyh.mei.constants.ImageCacheLimitMbKey
import com.ljyh.mei.data.model.UserData
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.repository.ColorRepository
import com.ljyh.mei.ui.component.AdaptiveMainNavigationRail
import com.ljyh.mei.ui.component.AppBottomNavigationBar
import com.ljyh.mei.ui.component.TabletNavigationRailWidth
import com.ljyh.mei.ui.component.player.BottomSheetPlayer
import com.ljyh.mei.ui.component.player.SyncPlayerSheetVisibility
import com.ljyh.mei.ui.component.player.rememberPlayerConnection
import com.ljyh.mei.ui.component.selectMainDestination
import com.ljyh.mei.ui.component.sheet.rememberBottomSheetState
import com.ljyh.mei.ui.component.utils.appBarScrollBehavior
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.component.utils.rememberPlayerThemeColor
import com.ljyh.mei.ui.component.utils.resetHeightOffset
import com.ljyh.mei.ui.local.LocalDatabase
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.local.LocalUserData
import com.ljyh.mei.ui.motion.PlayerMotionSpec
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.screen.navigationBuilder
import com.ljyh.mei.ui.theme.MusicTheme
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.rememberPreference
import okhttp3.OkHttpClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

private enum class NavigationTab { Home, Library }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeiApp(
    database: AppDatabase,
    colorRepository: ColorRepository,
    userData: UserData,
    startInLibrary: Boolean,
    imageClient: OkHttpClient,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val dynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
    val playerConnection = rememberPlayerConnection(context, database)

    setSingletonImageLoaderFactory {
        val imageLimitMb = runBlocking {
            context.dataStore.data.first()[ImageCacheLimitMbKey] ?: 250
        }
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(imageClient))
                add(AnimatedImageDecoder.Factory())
            }
            .crossfade(true)
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "image_cache"))
                    .maxSizeBytes(imageLimitMb.coerceIn(50, 2048) * 1024L * 1024L)
                    .build()
            }
            .build()
    }

    val targetThemeColor = rememberPlayerThemeColor(
        context = context,
        playerConnection = playerConnection,
        dynamicTheme = dynamicTheme,
        colorRepository = colorRepository,
    )
    MusicTheme(seedColor = targetThemeColor, isDark = isSystemInDarkTheme()) {
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
            val routeBottomChromeAnimationSpec = remember {
                PlayerMotionSpec.tween<Float>()
            }
            val routeBottomNavigationProgress by animateFloatAsState(
                targetValue = if (shellState.showBottomNavigation) 1f else 0f,
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
            val (query, onQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue())
            }
            val onActiveChange: (Boolean) -> Unit = { active ->
                searchActive = active
                if (!active) focusManager.clearFocus()
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
                }
            }

            SyncPlayerSheetVisibility(playerConnection, playerBottomSheetState)
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    if (!useTabletSidebar) {
                        AppBottomNavigationBar(
                            visibilityProgress = bottomNavigationProgress,
                            alphaProgress = routeBottomNavigationProgress,
                            interactive = shellState.showBottomNavigation &&
                                playerBottomNavigationProgress >= 1f,
                            selectedRoute = route,
                            onTabSelect = navController::selectMainDestination,
                        )
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
                    LocalDatabase provides database,
                    LocalNavController provides navController,
                    LocalPlayerConnection provides playerConnection,
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                    LocalUserData provides userData,
                ) {
                    NavHost(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = navigationStartPadding),
                        navController = navController,
                        startDestination = when (startTab) {
                            NavigationTab.Home -> Screen.Home.route
                            NavigationTab.Library -> Screen.Library.route
                        },
                    ) {
                        navigationBuilder(navController, topAppBarScrollBehavior)
                    }

                    AppSearchOverlay(
                        shellState = shellState,
                        query = query,
                        onQueryChange = onQueryChange,
                        onActiveChange = onActiveChange,
                        onSubmit = { searchQuery, type ->
                            if (searchQuery.isNotEmpty()) {
                                onActiveChange(false)
                                Screen.SearchResult.navigate(navController) {
                                    addPath(searchQuery)
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

                    AdaptiveMainNavigationRail(
                        useSidebar = useTabletSidebar,
                        shouldShow = shellState.showMainNavigation,
                        selectedRoute = route,
                        onTabSelect = navController::selectMainDestination,
                        sidebarModifier = Modifier.align(Alignment.CenterStart),
                    )
                    BottomSheetPlayer(
                        state = playerBottomSheetState,
                        collapsedBottomOffset = playerNavigationOffset,
                        systemBottomInset = bottomInset,
                        bottomNavigationVisibilityProgress = routeBottomNavigationProgress,
                    )
                }
            }
        }
    }
}
