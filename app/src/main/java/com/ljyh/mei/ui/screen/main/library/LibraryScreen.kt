package com.ljyh.mei.ui.screen.main.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.Button
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
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.main.library.component.ImmersiveBackground
import com.ljyh.mei.ui.screen.main.library.component.LibraryMobileLayout
import com.ljyh.mei.ui.screen.main.library.component.LibraryTabletLayout
import com.ljyh.mei.ui.screen.main.library.component.PhotoPickerSheet
import com.ljyh.mei.utils.rememberPreference

@Composable
fun LibraryScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
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
        userId = userId,
        nickname = userNickname,
        avatarUrl = userAvatarUrl,
        signature = profileSignature,
    )

    // --- 数据同步逻辑 ---
    LaunchedEffect(profileSeed) {
        if (userId.isNotEmpty()) {
            viewModel.loadLibrary(profileSeed)
        }
    }

    LaunchedEffect(photoAlbum) {
        if (userPhoto.isEmpty() && photoAlbum is Resource.Success) {
            (photoAlbum as Resource.Success).data.data.records.firstOrNull()?.imageUrl?.let {
                setUserPhoto(it)
            }
        }
    }
    LaunchedEffect(cookie) {
        if (cookie.isNotEmpty()) viewModel.getUserAccount()
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
                message = (libraryUiState as LibraryUiState.Error).message,
                onRetry = { viewModel.getUserAccount() },
            )
            cookie.isNotEmpty() && userId.isEmpty() -> LibraryLoadingState()
            userId.isEmpty() -> EmptyLoginState(navController)
            libraryUiState is LibraryUiState.Loading -> LibraryLoadingState()
            libraryUiState is LibraryUiState.Error -> LibraryErrorState(
                message = (libraryUiState as LibraryUiState.Error).message,
                onRetry = {
                    viewModel.getUserAccount()
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
private fun LibraryErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onRetry) {
            Text("加载失败：$message，点击重试")
        }
    }
}

@Composable
fun EmptyLoginState(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = { Screen.ContentSettings.navigate(navController) }
        ) {
            Text("去填写 Cookie 以同步数据")
        }
    }
}
