package com.ljyh.mei

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
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
import com.ljyh.mei.constants.DeviceIdKey
import com.ljyh.mei.constants.DynamicThemeKey
import com.ljyh.mei.constants.FirstLaunchKey
import com.ljyh.mei.constants.FloatingCapsuleBottomMargin
import com.ljyh.mei.constants.FloatingCapsuleMiniPlayerHeight
import com.ljyh.mei.constants.FloatingCapsuleNavHeight
import com.ljyh.mei.constants.UserAgent
import com.ljyh.mei.data.model.UserData
import com.ljyh.mei.extensions.togglePlayPause
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.repository.ColorRepository
import com.ljyh.mei.playback.MusicService
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.component.AdaptiveMainNavigation
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.SearchBar
import com.ljyh.mei.ui.component.TabletNavigationRailWidth
import com.ljyh.mei.ui.component.isMainDestination
import com.ljyh.mei.ui.component.selectMainDestination
import com.ljyh.mei.ui.component.player.BottomSheetPlayer
import com.ljyh.mei.ui.component.player.SyncPlayerSheetVisibility
import com.ljyh.mei.ui.component.sheet.rememberBottomSheetState
import com.ljyh.mei.ui.component.utils.appBarScrollBehavior
import com.ljyh.mei.ui.component.utils.resetHeightOffset
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalDatabase
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.local.LocalUserData
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.screen.navigationBuilder
import com.ljyh.mei.ui.screen.search.SearchScreen
import com.ljyh.mei.ui.theme.MusicTheme
import com.ljyh.mei.utils.log.CrashHandler
import com.ljyh.mei.utils.cache.preloadImage
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.get
import com.ljyh.mei.utils.log.FileLoggingTree
import com.ljyh.mei.utils.netease.NeteaseUtils.getAndroidId
import com.ljyh.mei.utils.rememberPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var colorRepository: ColorRepository
    private var userData by mutableStateOf(UserData.VISITOR)


    @androidx.annotation.OptIn(UnstableApi::class)
    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CrashHandler.init(this)
        if (BuildConfig.DEBUG) {
            // 开发模式：既输出到 Logcat，也输出到文件
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileLoggingTree(this))
        } else {
            // Release 模式：主要是植入文件记录器
            Timber.plant(FileLoggingTree(this))
        }
        val headerInterceptor = Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("User-Agent", UserAgent)
                .build()
            chain.proceed(newRequest)
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .build()

        setContent {
            val context = this@MainActivity
            val lifecycleOwner = LocalLifecycleOwner.current
            val navController = rememberNavController()
            var active by rememberSaveable {
                mutableStateOf(false)
            }
            val dynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            var playerConnection by remember { mutableStateOf<PlayerConnection?>(null) }

            DisposableEffect(Unit) {
                val intent = Intent(context, MusicService::class.java)

                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        Timber.tag("MainActivity").d("Service Connected") // 添加日志
                        if (service is MusicService.MusicBinder) {
                            // 更新 State，触发 Recomposition
                            playerConnection = PlayerConnection(
                                context,
                                service,
                                database,
                                lifecycleOwner.lifecycleScope
                            )
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        Timber.tag("MainActivity").d("Service Disconnected")
                        playerConnection?.dispose() // 假设你有 dispose 方法清理资源
                        playerConnection = null
                    }
                }

                // 启动并绑定服务
                context.startService(intent)
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

                onDispose {
                    // Compose 销毁时解绑
                    context.unbindService(connection)
                    playerConnection = null
                }
            }
            setSingletonImageLoaderFactory {
                ImageLoader.Builder(this)
                    .components {
                        add(OkHttpNetworkFetcherFactory(okHttpClient))
                        add(AnimatedImageDecoder.Factory())
                    }
                    .crossfade(true)
                    .diskCache {
                        DiskCache.Builder()
                            .directory(File(this@MainActivity.cacheDir, "image_cache"))
                            .maxSizePercent(0.1)
                            .build()
                    }
                    .build()
            }
            var targetThemeColor by remember { mutableStateOf(Color.Black) }

            LaunchedEffect(playerConnection) {
                Timber.tag("MainActivity").d("playerConnection: $playerConnection")
                val playerConnection = playerConnection ?: return@LaunchedEffect
                val player = playerConnection.service.player
                playerConnection.service.currentMediaMetadata.collect { song->
                    if (dynamicTheme && song != null) {
                        val context = this@MainActivity
                        launch {
                            Timber.tag("MainActivity").d("获取当前歌曲颜色: $song")
                            val color = colorRepository.getColorOrExtract(context, song.coverUrl)
                            targetThemeColor = color
                        }
                        Timber.tag("MainActivity").d("获取歌曲颜色: $targetThemeColor")

                        val nextIndex = player.nextMediaItemIndex
                        if (nextIndex != C.INDEX_UNSET) {
                            val nextUrl = player.getMediaItemAt(nextIndex).mediaMetadata.artworkUri?.toString()
                            if (!nextUrl.isNullOrEmpty()) {
                                Timber.tag("MainActivity").d("获取下一首歌曲颜色: $nextUrl")
                                launch(Dispatchers.IO) {
                                    colorRepository.getColorOrExtract(context, nextUrl)
                                    preloadImage(context, nextUrl)
                                }
                            }
                        }
                    }
                }

            }
            MusicTheme(
                seedColor = targetThemeColor,
                isDark = isSystemInDarkTheme()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val device = rememberDeviceInfo()
                    val useTabletSidebar = device.isTablet && device.isLandscape
                    val windowsInsets = WindowInsets.systemBars
                    val navBackStackEntry by navController.currentBackStackEntryAsState()

                    val bottomInset by remember {
                        derivedStateOf {
                            with(density) {
                                windowsInsets.getBottom(density).toDp()
                            }
                        }
                    }

                    val isMainDestination = navBackStackEntry?.destination?.route.isMainDestination()
                    val shouldShowMainNavigation = !active && isMainDestination
                    val shouldShowTabletSidebar = shouldShowMainNavigation && useTabletSidebar
                    val shouldShowNavigationBar = shouldShowMainNavigation && !useTabletSidebar

                    val searchBarFocusRequester = remember { FocusRequester() }
                    val shouldShowSearchBar = remember(active, navBackStackEntry) {
                        active || navBackStackEntry?.destination?.route == Screen.Home.route ||
                                navBackStackEntry?.destination?.route?.startsWith("search_result/") == true
                    }

                    val collapsedBound = remember(bottomInset, shouldShowNavigationBar) {
                        derivedStateOf {
                            bottomInset +
                                    FloatingCapsuleMiniPlayerHeight +
                                    if (shouldShowNavigationBar) {
                                        FloatingCapsuleNavHeight + 8.dp
                                    } else {
                                        0.dp
                                    }
                        }
                    }

                    val playerBottomSheetState = rememberBottomSheetState(
                        dismissedBound = 0.dp,
                        collapsedBound = collapsedBound.value,
                        expandedBound = maxHeight,
                    )
                    val searchBarScrollBehavior = appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search_result/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )
                    val topAppBarScrollBehavior = appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search_result/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )
                    val (query, onQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                        mutableStateOf(TextFieldValue())
                    }
                    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                        }
                    }

                    val onSearch: (String) -> Unit = {
                        if (it.isNotEmpty()) {
                            onActiveChange(false)
                            Screen.SearchResult.navigate(navController){
                                addPath(query.text)
                                addPath("1") // 默认所搜单曲
                            }
                        }
                    }

                    val playerAwareWindowInsets = remember(
                        bottomInset,
                        shouldShowNavigationBar,
                        playerBottomSheetState.isDismissed
                    ) {
                        var bottom = bottomInset
                        if (shouldShowNavigationBar) {
                            bottom += FloatingCapsuleBottomMargin + FloatingCapsuleNavHeight
                        }
                        if (!playerBottomSheetState.isDismissed) {
                            bottom += FloatingCapsuleBottomMargin + FloatingCapsuleMiniPlayerHeight
                        }
                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                    }
                    val defaultOpenTab = remember {
                        NavigationTab.HOME
                    }
                    val tabOpenedFromShortcut = remember {
                        when (intent?.action) {
                            ACTION_LIBRARY -> NavigationTab.Library
                            else -> null
                        }
                    }
                    val topLevelScreens = listOf(
                        Screen.Home.route,
                        Screen.FindMusic.route,
                        Screen.Library.route,
                        Screen.Setting.route
                    )


                    LaunchedEffect(Unit) {
                        lifecycleScope.launch {
                            getAndroidId(this@MainActivity)
                            if (dataStore.get(FirstLaunchKey, true)) {
                                dataStore.edit { settings ->
                                    settings[FirstLaunchKey] = false
                                    settings[DeviceIdKey] = com.ljyh.mei.utils.getDeviceId()
                                }
                            }
                        }
                    }
                    LaunchedEffect(navBackStackEntry) {
                        searchBarScrollBehavior.state.resetHeightOffset()
                        topAppBarScrollBehavior.state.resetHeightOffset()
                        if (navBackStackEntry?.destination?.route in topLevelScreens && !active) {
                            onQueryChange(TextFieldValue())
                        }
                    }
                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }
                    }

                    SyncPlayerSheetVisibility(playerConnection, playerBottomSheetState)
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
                                .padding(start = if (shouldShowTabletSidebar) TabletNavigationRailWidth else 0.dp),
                            navController = navController,
                            startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                NavigationTab.HOME -> Screen.Home
                                NavigationTab.FindMusic -> Screen.FindMusic
                                NavigationTab.Library -> Screen.Library
                            }.route,
                        ) {
                            navigationBuilder(
                                navController = navController,
                                scrollBehavior = topAppBarScrollBehavior,
                            )
                        }

                        AnimatedVisibility(
                            visible = shouldShowSearchBar,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            SearchBar(
                                query = query,
                                onQueryChange = onQueryChange,
                                onSearch = onSearch,
                                active = active,
                                onActiveChange = onActiveChange,
                                scrollBehavior = searchBarScrollBehavior,
                                placeholder = {
                                    Text("搜索")
                                },
                                leadingIcon = {
                                    IconButton(
                                        onClick = {
                                            when {
                                                active -> onActiveChange(false)
                                                !isMainDestination -> {
                                                    navController.navigateUp()
                                                }

                                                else -> onActiveChange(true)
                                            }
                                        },
                                        onLongClick = {
                                            when {
                                                active -> {}
                                                !isMainDestination -> {
                                                    navController.backToMain()
                                                }
                                                else -> {}
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (active || !isMainDestination) {
                                                Icons.AutoMirrored.Rounded.ArrowBack
                                            } else {
                                                Icons.Rounded.Search
                                            },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            contentDescription = null
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (active) {
                                        if (query.text.isNotEmpty()) {
                                            IconButton(
                                                onClick = { onQueryChange(TextFieldValue("")) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                onSearch(query.text)
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    R.drawable.cloud
                                                ),
                                                contentDescription = "neteasecloud"
                                            )
                                        }
                                    } else if (navBackStackEntry?.destination?.route in topLevelScreens) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .padding(end = 4.dp)
                                                .clickable {
                                                    navController.navigate(Screen.Setting.route)
                                                }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Settings,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                },
                                focusRequester = searchBarFocusRequester,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(start = if (shouldShowTabletSidebar) TabletNavigationRailWidth else 0.dp),
                            ) {

                                SearchScreen(
                                    query = query.text,
                                    onQueryChange = onQueryChange,
                                    onSearch = { query, type ->
                                        onActiveChange(false)
                                        Screen.SearchResult.navigate(navController){
                                            addPath(query)
                                            addPath(type.toString())
                                        }
                                    },
                                    onDismiss = {
                                        onActiveChange(false)
                                    }
                                )

                            }
                        }
                        val capsuleBottom = bottomInset + FloatingCapsuleBottomMargin

                        if (shouldShowMainNavigation) AdaptiveMainNavigation(
                            useSidebar = useTabletSidebar,
                            selectedRoute = navBackStackEntry?.destination?.route,
                            onTabSelect = navController::selectMainDestination,
                            sidebarModifier = Modifier.align(Alignment.CenterStart),
                            bottomBarModifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = capsuleBottom),
                        )

                        BottomSheetPlayer(
                            state = playerBottomSheetState,
                        )
                    }
                }

            }

        }
    }

    override fun onStart() {
        super.onStart()
    }
    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        const val ACTION_LIBRARY = "com.ljyh.mei.action.LIBRARY"
    }

}

enum class NavigationTab {
    HOME,FindMusic, Library
}
