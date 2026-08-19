package com.ljyh.mei.ui.screen.main.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.UserAvatarUrlKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.constants.UserNicknameKey
import com.ljyh.mei.constants.UserPhotoKey
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.model.toAlbum
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
    val account by viewModel.account.collectAsState()
    val profileUi by viewModel.profileUi.collectAsState()
    val photoAlbum by viewModel.photoAlbum.collectAsState()
    val localPlaylists by viewModel.localPlaylists.collectAsState()
    val albumList by viewModel.albumList.collectAsState()

    // Preferences
    val (userId, setUserId) = rememberPreference(UserIdKey, "")
    val (userNickname, setUserNickname) = rememberPreference(UserNicknameKey, "")
    val (userAvatarUrl, setUserAvatarUrl) = rememberPreference(UserAvatarUrlKey, "")
    val (userPhoto, setUserPhoto) = rememberPreference(UserPhotoKey, "")
    val cookie by rememberPreference(CookieKey, defaultValue = "")

    // State
    var showPhotoPicker by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("创建歌单", "收藏歌单", "收藏专辑")
    val accountProfile = (account as? Resource.Success)?.data?.profile
    val profileSignature = accountProfile?.signature.orEmpty()
    val albums = (albumList as? Resource.Success)?.data?.data?.map { it.toAlbum() }.orEmpty()

    val (createdPlaylists, collectedPlaylists) = remember(localPlaylists, userId) {
        if (userId.isEmpty()) Pair(emptyList(), emptyList())
        else {
            val (created, collected) = localPlaylists.partition { it.author == userId }
            val now = System.currentTimeMillis()
            Pair(
                created.sortedForLibrary(now),
                collected.sortedForLibrary(now),
            )
        }
    }

    // --- 数据同步逻辑 ---
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.loadLibrary(userId)
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

        if (userId.isNotEmpty()) {
            if (device.isTablet && device.isLandscape) {
                // 平板布局
                val tabletState = LibraryTabletUiState(
                    profile = profileUi ?: LibraryProfileUi(
                        userId = userId,
                        nickname = userNickname,
                        avatarUrl = userAvatarUrl,
                        signature = profileSignature,
                    ),
                    section = LibrarySection.entries[selectedTabIndex],
                    createdPlaylists = createdPlaylists,
                    collectedPlaylists = collectedPlaylists,
                    albums = albums,
                )
                LibraryTabletLayout(
                    state = tabletState,
                    onEvent = { event ->
                        when (event) {
                            is LibraryTabletEvent.SelectSection -> selectedTabIndex = event.section.ordinal
                            is LibraryTabletEvent.OpenPlaylist -> Screen.PlayList.navigate(navController) { addPath(event.id) }
                            is LibraryTabletEvent.OpenAlbum -> Screen.Album.navigate(navController) { addPath(event.id) }
                            LibraryTabletEvent.ChangeProfilePhoto -> {
                                viewModel.getPhotoAlbum(userId)
                                showPhotoPicker = true
                            }
                            LibraryTabletEvent.OpenHistory -> Screen.History.navigate(navController)
                            LibraryTabletEvent.OpenLocalMusic -> Screen.LocalMusic.navigate(navController)
                            LibraryTabletEvent.OpenDownloads -> Screen.DownloadManage.navigate(navController)
                        }
                    },
                )
            } else {
                // 手机布局 (保持你原来的代码逻辑)
                LibraryMobileLayout(
                    userNickname = userNickname,
                    userAvatarUrl = userAvatarUrl,
                    userPhoto = userPhoto,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelect = {selectedTabIndex = it},
                    tabTitles = tabTitles,
                    createdPlaylists = createdPlaylists,
                    collectedPlaylists = collectedPlaylists,
                    albums = albums,
                    onAvatarClick = {
                        viewModel.getPhotoAlbum(userId)
                        showPhotoPicker = true
                    },
                    onPlaylistClick = { id->
                        Screen.PlayList.navigate(navController) { addPath(id) }
                    },
                    onAlbumClick = { id->
                        Screen.Album.navigate(navController) { addPath(id) }
                    }
                )
            }

            if (showPhotoPicker) {
                PhotoPickerSheet(
                    photoAlbum = photoAlbum,
                    onSelect = { setUserPhoto(it); showPhotoPicker = false },
                    onDismiss = { showPhotoPicker = false }
                )
            }
        } else {
            // 未登录逻辑
            EmptyLoginState(navController)
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

private fun List<Playlist>.sortedForLibrary(now: Long): List<Playlist> {
    val maxLocalPlayCount = maxOfOrNull { it.localPlayCount } ?: 0
    val maxPlayCount = maxOfOrNull { it.playCount } ?: 0L
    return sortedByDescending {
        it.sortScore(maxLocalPlayCount, maxPlayCount, now)
    }
}
