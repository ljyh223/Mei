package com.ljyh.mei.ui.screen.main.library

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.screen.main.library.component.BentoDashboard
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.UserAvatarUrlKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.constants.UserNicknameKey
import com.ljyh.mei.constants.UserPhotoKey
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.main.library.component.ImmersiveBackground
import com.ljyh.mei.ui.screen.main.library.component.LibraryMobileLayout
import com.ljyh.mei.ui.screen.main.library.component.LibraryTabletLayout
import com.ljyh.mei.ui.screen.main.library.component.PhotoPickerSheet
import com.ljyh.mei.utils.preferences.rememberPreference
import com.ljyh.mei.utils.preferences.dataStore

@Composable
fun LibraryScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val device = rememberDeviceInfo()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val libraryUiState by viewModel.libraryUiState.collectAsStateWithLifecycle()
    val photoAlbum by viewModel.photoAlbum.collectAsStateWithLifecycle()

    // Preferences
    val (userId, setUserId) = rememberPreference(UserIdKey, "")
    val (userNickname, setUserNickname) = rememberPreference(UserNicknameKey, "")
    val (userAvatarUrl, setUserAvatarUrl) = rememberPreference(UserAvatarUrlKey, "")
    val (userPhoto, setUserPhoto) = rememberPreference(UserPhotoKey, "")
    val cookie by rememberPreference(CookieKey, defaultValue = "")

    // State
    var showPhotoPicker by remember { mutableStateOf(false) }
    val tabTitles = listOf("创建歌单", "收藏歌单", "收藏专辑")
    val accountProfile = (account as? Resource.Success)?.data?.profile
    val profileSignature = accountProfile?.signature.orEmpty()
    val profileSeed = LibraryProfileUi(
        userId = accountProfile?.userId?.toString() ?: userId,
        nickname = accountProfile?.nickname ?: userNickname,
        avatarUrl = accountProfile?.avatarUrl ?: userAvatarUrl,
        signature = profileSignature,
    )

    // --- 数据同步逻辑 ---
    LaunchedEffect(profileSeed) {
        if (profileSeed.userId.isNotEmpty()) {
            viewModel.loadLibrary(profileSeed)
        }
    }

    LaunchedEffect(photoAlbum) {
        val defaultPhoto = (photoAlbum as? Resource.Success)
            ?.data?.data?.records?.firstOrNull()?.imageUrl.orEmpty()
        if (defaultPhoto.isNotBlank()) {
            context.dataStore.edit { preferences ->
                if (preferences[UserPhotoKey].isNullOrBlank()) {
                    preferences[UserPhotoKey] = defaultPhoto
                }
            }
        }
    }
    LaunchedEffect(cookie) {
        viewModel.onCookieAvailable(cookie)
    }
    LaunchedEffect(account) {
        (account as? Resource.Success)
            ?.data?.profile
            ?.let { profile ->
                setUserId(profile.userId.toString())
                setUserNickname(profile.nickname)
                setUserAvatarUrl(profile.avatarUrl)
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {

        if ((!device.isTablet || !device.isLandscape) && userId.isNotEmpty() && userPhoto.isNotEmpty()) {
            ImmersiveBackground(userPhoto)
        }

        when {
            cookie.isNotEmpty() && libraryUiState is LibraryUiState.Error -> LibraryErrorState(
                onRetry = { viewModel.getUserAccount(force = true) },
            )
            cookie.isNotEmpty() && userId.isEmpty() -> LibraryLoadingState()
            userId.isEmpty() -> EmptyLoginState(navController)
            libraryUiState is LibraryUiState.Loading -> LibraryLoadingState()
            libraryUiState is LibraryUiState.Error -> LibraryErrorState(
                onRetry = {
                    viewModel.getUserAccount(force = true)
                    viewModel.loadLibrary(profileSeed)
                },
            )
            libraryUiState is LibraryUiState.Content -> {
                val contentState = (libraryUiState as LibraryUiState.Content).data
                if (device.isTablet && device.isLandscape) {
                    LibraryTabletLayout(
                        state = contentState,
                        backgroundUrl = userPhoto,
                        onEvent = { event ->
                            when (event) {
                                is LibraryEvent.SelectSection -> viewModel.selectSection(event.section)
                                is LibraryEvent.OpenPlaylist -> Screen.PlayList.navigate(navController) {
                                    addPath(event.id)
                                }
                                is LibraryEvent.OpenAlbum -> Screen.Album.navigate(navController) {
                                    addPath(event.id)
                                }
                                LibraryEvent.ChangeProfilePhoto -> {
                                    viewModel.refreshPhotoAlbum(userId)
                                    showPhotoPicker = true
                                }
                                LibraryEvent.OpenHistory -> Screen.History.navigate(navController)
                                LibraryEvent.OpenLocalMusic -> Screen.LocalMusic.navigate(navController)
                                LibraryEvent.OpenDownloads -> Screen.DownloadManage.navigate(navController)
                            }
                        },
                    )
                } else {
                    LibraryMobileLayout(
                        userNickname = userNickname,
                        userAvatarUrl = userAvatarUrl,
                        userPhoto = userPhoto,
                        selectedTabIndex = contentState.section.ordinal,
                        onTabSelect = { viewModel.selectSection(LibrarySection.entries[it]) },
                        tabTitles = tabTitles,
                        createdPlaylists = contentState.createdPlaylists,
                        collectedPlaylists = contentState.collectedPlaylists,
                        albums = contentState.albums,
                        onAvatarClick = {
                            viewModel.refreshPhotoAlbum(userId)
                            showPhotoPicker = true
                        },
                        onPlaylistClick = { id ->
                            Screen.PlayList.navigate(navController) { addPath(id) }
                        },
                        onAlbumClick = { id ->
                            Screen.Album.navigate(navController) { addPath(id) }
                        },
                    )
                }

                if (showPhotoPicker) {
                    PhotoPickerSheet(
                        photoAlbum = photoAlbum,
                        onSelect = {
                            setUserPhoto(it)
                            showPhotoPicker = false
                        },
                        onDismiss = { showPhotoPicker = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryLoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LibraryErrorState(onRetry: () -> Unit) {
    val navController = LocalNavController.current
    Box(
        Modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
        )), contentAlignment = Alignment.Center,
    ) {
        MeiStatePanel(
            title = "暂时无法同步音乐库",
            message = "你仍然可以收听本地音乐。",
            icon = Icons.Rounded.CloudOff,
            actionLabel = "重试",
            onAction = onRetry,
            secondaryLabel = "本地音乐",
            onSecondaryAction = { Screen.LocalMusic.navigate(navController) },
        )
    }
}

@Composable
fun EmptyLoginState(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ))
            .verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("音乐库", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 20.dp))
        BentoDashboard(
            onHistoryClick = { Screen.History.navigate(navController) },
            onLocalClick = { Screen.LocalMusic.navigate(navController) },
            onDownloadClick = { Screen.DownloadManage.navigate(navController) },
        )
        MeiStatePanel(
            title = "收藏与歌单",
            message = "登录网易云音乐，同步你的歌单与收藏。",
            icon = Icons.Rounded.LibraryMusic,
            actionLabel = "登录账号",
            onAction = { Screen.ContentSettings.navigate(navController) },
        )
    }
}
