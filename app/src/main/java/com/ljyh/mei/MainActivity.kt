package com.ljyh.mei

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.ljyh.mei.constants.AppBarHeight
import com.ljyh.mei.constants.DeviceIdKey
import com.ljyh.mei.constants.DynamicThemeKey
import com.ljyh.mei.constants.FirstLaunchKey
import com.ljyh.mei.constants.MiniPlayerHeight
import com.ljyh.mei.constants.NavigationBarAnimationSpec
import com.ljyh.mei.constants.NavigationBarHeight
import com.ljyh.mei.constants.UserAgent
import com.ljyh.mei.data.model.UserData
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.ColorRepository
import com.ljyh.mei.playback.MusicService
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.component.ConfirmationDialog
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.SearchBar
import com.ljyh.mei.ui.component.player.BottomSheetPlayer
import com.ljyh.mei.ui.component.sheet.rememberBottomSheetState
import com.ljyh.mei.ui.component.utils.appBarScrollBehavior
import com.ljyh.mei.ui.component.utils.resetHeightOffset
import com.ljyh.mei.ui.local.LocalDatabase
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.local.LocalUserData
import com.ljyh.mei.ui.screen.Index
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.screen.navigationBuilder
import com.ljyh.mei.ui.screen.search.SearchScreen
import com.ljyh.mei.ui.theme.MusicTheme
import com.ljyh.mei.utils.log.CrashHandler
import com.ljyh.mei.utils.MusicUtils
import com.ljyh.mei.utils.PermissionsUtils.checkAndRequestFilesPermissions
import com.ljyh.mei.utils.PermissionsUtils.checkFilesPermissions
import com.ljyh.mei.utils.cache.preloadImage
import com.ljyh.mei.utils.checkAndRequestNotificationPermission
import com.ljyh.mei.utils.createNotificationChannel
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.get
import com.ljyh.mei.utils.log.FileLoggingTree
import com.ljyh.mei.utils.netease.NeteaseUtils.getAndroidId
import com.ljyh.mei.utils.rememberPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

            var isMeasured by remember { mutableStateOf(false) }
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
                        .onSizeChanged {
                            isMeasured = true
                        }
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val bottomInset by remember {
                        derivedStateOf {
                            with(density) {
                                windowsInsets.getBottom(density).toDp()
                            }
                        }
                    }

                    val navigationItems = remember { Screen.MainScreens }
                    val showDialog = remember { mutableStateOf(false) }
                    val shouldShowNavigationBar = remember(navBackStackEntry, active) {
                        navBackStackEntry?.destination?.route == null ||
                                navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } && !active
                    }


                    val searchBarFocusRequester = remember { FocusRequester() }
                    val shouldShowSearchBar = remember(active, navBackStackEntry) {
                        active || navBackStackEntry?.destination?.route == Screen.Home.route ||
                                navBackStackEntry?.destination?.route?.startsWith("search_result/") == true
                    }

                    val collapsedBound = remember(shouldShowNavigationBar) {
                        derivedStateOf {
                            bottomInset + (if (shouldShowNavigationBar) NavigationBarHeight else 0.dp) + MiniPlayerHeight
                        }
                    }

                    val playerBottomSheetState = rememberBottomSheetState(
                        dismissedBound = 0.dp,
                        collapsedBound = collapsedBound.value,
                        expandedBound = maxHeight,
                    )
                    val navigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar) NavigationBarHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = ""
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
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
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
                        if (shouldShowNavigationBar) bottom += NavigationBarHeight
                        if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
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
                    ConfirmationDialog(
                        title = "申请文件访问权限",
                        text = "用于下载音乐",
                        onConfirm = {
                            createNotificationChannel(this@MainActivity)
                            checkAndRequestNotificationPermission(this@MainActivity)
                            checkAndRequestFilesPermissions(this@MainActivity)
                        },
                        onDismiss = {
                            Toast.makeText(
                                this@MainActivity,
                                "已取消",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        openDialog = showDialog
                    )

                    LaunchedEffect(key1 = showDialog.value) {
                        if (!checkFilesPermissions(this@MainActivity)) {
                            showDialog.value = true
                        } else {

                            Timber.tag("MainActivity").d("permission granted")
                            lifecycleScope.launch {
                                getAndroidId(this@MainActivity)
                                if (dataStore.get(FirstLaunchKey, true)) {
                                    dataStore.edit { settings ->
                                        settings[FirstLaunchKey] = false
                                        settings[DeviceIdKey] = com.ljyh.mei.utils.getDeviceId()
                                    }
                                    Timber.tag("MainActivity").d("load local music")
                                    withContext(Dispatchers.IO) {
                                        val localSongs = MusicUtils.getLocalMusic()
                                        database.songDao().insertSongs(localSongs)
                                    }
                                }
                            }
                        }

                    }
                    LaunchedEffect(isMeasured, playerConnection) {
                        if (isMeasured && playerConnection?.player?.currentMediaItem != null) {
                            playerBottomSheetState.collapseSoft()
                        }
                    }

                    LaunchedEffect(navBackStackEntry) {
                        searchBarScrollBehavior.state.resetHeightOffset()
                        topAppBarScrollBehavior.state.resetHeightOffset()
                    }
                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }
                    }

                    LaunchedEffect(playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else {
                            if (playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.collapseSoft()
                            }
                        }
                    }


                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player =
                            playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener = object : Player.Listener {
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && mediaItem != null && playerBottomSheetState.isDismissed) {
                                    playerBottomSheetState.collapseSoft()
                                }
                            }
                        }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }
                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalNavController provides navController,
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalUserData provides userData,
                    ) {

                        NavHost(
                            modifier = Modifier.fillMaxSize(),
                            navController = navController,
                            startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                NavigationTab.HOME -> Screen.Home
                                NavigationTab.FindMusic -> Screen.FindMusic
                                NavigationTab.Library -> Screen.Library
                            }.route,
                        ) {
                            navigationBuilder(navController, topAppBarScrollBehavior)
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
                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                    navController.navigateUp()
                                                }

                                                else -> onActiveChange(true)
                                            }
                                        },
                                        onLongClick = {
                                            when {
                                                active -> {}
                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                    navController.backToMain()
                                                }
                                                else -> {}
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (active || !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
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
                                modifier = Modifier.align(Alignment.TopCenter),
                            ) {

                                SearchScreen(
                                    query = query.text,
                                    onQueryChange = onQueryChange,
                                    onSearch = { query, type ->
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
                        BottomSheetPlayer(
                            state = playerBottomSheetState,
                        )

                        NavigationBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset {
                                    if (navigationBarHeight == 0.dp) {
                                        IntOffset(
                                            x = 0,
                                            y = (bottomInset + NavigationBarHeight).roundToPx()
                                        )
                                    } else {
                                        val slideOffset =
                                            (bottomInset + NavigationBarHeight) * playerBottomSheetState.progress.coerceIn(
                                                0f,
                                                1f
                                            )
                                        val hideOffset =
                                            (bottomInset + NavigationBarHeight) * (1 - navigationBarHeight / NavigationBarHeight)
                                        IntOffset(
                                            x = 0,
                                            y = (slideOffset + hideOffset).roundToPx()
                                        )
                                    }
                                }
                        ) {
                            Index.entries.fastForEach { screen ->

                                NavigationBarItem(
                                    selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true,

                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        if (navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true) {
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                "scrollToTop",
                                                true
                                            )
                                        } else {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
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