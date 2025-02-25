package com.ljyh.music

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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.CoilUtils
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.ljyh.music.constants.AppBarHeight
import com.ljyh.music.constants.FirstLaunchKey
import com.ljyh.music.constants.MiniPlayerHeight
import com.ljyh.music.constants.NavigationBarAnimationSpec
import com.ljyh.music.constants.NavigationBarHeight
import com.ljyh.music.data.model.UserData
import com.ljyh.music.data.model.room.Color
import com.ljyh.music.di.AppDatabase
import com.ljyh.music.playback.MusicService
import com.ljyh.music.playback.PlayerConnection
import com.ljyh.music.ui.component.ConfirmationDialog
import com.ljyh.music.ui.component.player.BottomSheetPlayer
import com.ljyh.music.ui.component.rememberBottomSheetState
import com.ljyh.music.ui.local.LocalDatabase
import com.ljyh.music.ui.local.LocalNavController
import com.ljyh.music.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.music.ui.local.LocalPlayerConnection
import com.ljyh.music.ui.local.LocalUserData
import com.ljyh.music.ui.screen.Index
import com.ljyh.music.ui.screen.Screen
import com.ljyh.music.ui.screen.navigationBuilder
import com.ljyh.music.ui.theme.MusicTheme
import com.ljyh.music.utils.MusicUtils
import com.ljyh.music.utils.appBarScrollBehavior
import com.ljyh.music.utils.checkAndRequestFilesPermissions
import com.ljyh.music.utils.checkAndRequestNotificationPermission
import com.ljyh.music.utils.checkFilesPermissions
import com.ljyh.music.utils.createNotificationChannel
import com.ljyh.music.utils.dataStore
import com.ljyh.music.utils.get
import com.ljyh.music.utils.resetHeightOffset
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var database: AppDatabase
    private var userData by mutableStateOf(UserData.VISITOR)
    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicBinder) {
                playerConnection =
                    PlayerConnection(this@MainActivity, service, database, lifecycleScope)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
        }
    }


    override fun onStart() {
        super.onStart()
        startService(Intent(this, MusicService::class.java))
        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @kotlin.OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {
            val navController = rememberNavController()
            val active by rememberSaveable {
                mutableStateOf(false)
            }

            //根据图片加载主题色
            val loader = rememberNetworkLoader()
            val dominantColorState = rememberDominantColorState(loader)
            val coverUrl = remember { mutableStateOf("") }
            val isColorLoaded = remember { mutableStateOf(false) } // 记录颜色是否已从数据库加载
            LaunchedEffect(playerConnection) {
                val playerConnection = playerConnection ?: return@LaunchedEffect
                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    if (song != null) {
                        coverUrl.value = song.coverUrl
                        withContext(Dispatchers.IO) {
                            val cachedColor = database.colorDao().getColor(song.coverUrl)
                            if (cachedColor != null) {
                                // 不能直接赋值 color，改用 updateFrom 触发更新
                                withContext(Dispatchers.Main) {
                                    dominantColorState.updateFrom(Url(song.coverUrl))
                                    isColorLoaded.value = true // 颜色已加载，避免重复存储
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    val demoImageUrl = Url(song.coverUrl)
                                    loader.load(demoImageUrl)
                                    dominantColorState.updateFrom(demoImageUrl)
                                    isColorLoaded.value = false // 颜色未加载，将在下一个 LaunchedEffect 存入数据库
                                }
                            }
                        }

                    }
                }
            }
            // 颜色计算完成后存入数据库
            LaunchedEffect(dominantColorState.color) {
                if (coverUrl.value.isNotEmpty() && !isColorLoaded.value) {
                    withContext(Dispatchers.IO) {
                        database.colorDao().insertColor(
                            Color(
                                url = coverUrl.value,
                                color = dominantColorState.color.toArgb()
                            )
                        )
                    }
                    isColorLoaded.value = true
                }
            }


            MusicTheme(
                seedColor = dominantColorState.color,
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                    val navigationItems = remember { Screen.MainScreens }
                    val showDialog = remember { mutableStateOf(false) }
                    val shouldShowNavigationBar = remember(navBackStackEntry, active) {
                        navBackStackEntry?.destination?.route == null ||
                                navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } && !active
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
                    val topAppBarScrollBehavior = appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

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
                            Log.d("MainActivity", "permission granted")
                            lifecycleScope.launch {
                                if (dataStore.get(FirstLaunchKey, true)) {
                                    dataStore.edit { settings ->
                                        settings[FirstLaunchKey] = false
                                    }
                                    Log.d("MainActivity", "load local music")
                                    withContext(Dispatchers.IO) {
                                        val localSongs = MusicUtils.getLocalMusic()
                                        database.songDao().insertSongs(localSongs)
                                    }
                                }
                            }
                        }

                    }
                    LaunchedEffect(navBackStackEntry) {
//                        searchBarScrollBehavior.state.resetHeightOffset()
                        topAppBarScrollBehavior.state.resetHeightOffset()
                    }
                    LaunchedEffect(active) {
                        if (active) {
//                            searchBarScrollBehavior.state.resetHeightOffset()
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
                                NavigationTab.Library -> Screen.Library
                            }.route,
                        ) {
                            navigationBuilder(navController, topAppBarScrollBehavior)
                        }
                        BottomSheetPlayer(
                            state = playerBottomSheetState,
                            navController = navController
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

    companion object {
        const val ACTION_LIBRARY = "com.ljyh.music.action.LIBRARY"
    }

}

enum class NavigationTab {
    HOME, Library
}

