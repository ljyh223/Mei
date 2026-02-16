package com.ljyh.mei.ui.screen.main.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.UserAvatarUrlKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.constants.UserNicknameKey
import com.ljyh.mei.constants.UserPhotoKey
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
fun LibraryScreen(viewModel: LibraryViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    val device = rememberDeviceInfo()
    val account by viewModel.account.collectAsState()
    val photoAlbum by viewModel.photoAlbum.collectAsState()
    val localPlaylists by viewModel.localPlaylists.collectAsState()
    val albumList by viewModel.albumList.collectAsState()
    val userSubcount by viewModel.userSubcount.collectAsState()

    // Preferences
    val (userId, setUserId) = rememberPreference(UserIdKey, "")
    val (userNickname, setUserNickname) = rememberPreference(UserNicknameKey, "")
    val (userAvatarUrl, setUserAvatarUrl) = rememberPreference(UserAvatarUrlKey, "")
    val (userPhoto, setUserPhoto) = rememberPreference(UserPhotoKey, "")
    val cookie by rememberPreference(CookieKey, defaultValue = "")

    // State
    var showPhotoPicker by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var subPlaylistCount by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("创建歌单", "收藏歌单", "收藏专辑")
    val listState = rememberLazyListState()

    val (createdPlaylists, collectedPlaylists) = remember(localPlaylists, userId) {
        if (userId.isEmpty()) Pair(emptyList(), emptyList())
        else localPlaylists.partition { it.author == userId }
    }

    // --- 数据同步逻辑 ---
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.syncUserPlaylists(userId)
            viewModel.getPhotoAlbum(userId)
            viewModel.getAlbumList()
            viewModel.getUserSubcount()
        }
    }

    LaunchedEffect(photoAlbum) {
        if (userPhoto.isEmpty() && photoAlbum is Resource.Success) {
            (photoAlbum as Resource.Success).data.data.records.firstOrNull()?.imageUrl?.let {
                setUserPhoto(it)
            }
        }
    }
    LaunchedEffect(cookie, account) {
        if (cookie.isNotEmpty() && account !is Resource.Success) viewModel.getUserAccount()
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

    LaunchedEffect(userSubcount) {
        if (userSubcount is Resource.Success) {

        }
    }

    LaunchedEffect(subPlaylistCount) {
        if (userId.isNotEmpty() && localPlaylists.size != subPlaylistCount && subPlaylistCount != 0)
            viewModel.syncUserPlaylists(userId, subPlaylistCount)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. 通用背景
        if (userId.isNotEmpty() && userPhoto.isNotEmpty()) {
            ImmersiveBackground(userPhoto)
        }

        if (userId.isNotEmpty()) {
            if (device.isTablet && device.isLandscape) {
                // 平板布局
                LibraryTabletLayout(
                    userNickname = userNickname,
                    userAvatarUrl = userAvatarUrl,
                    userPhoto = userPhoto,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelect = {selectedTabIndex = it},
                    onAvatarClick = {
                        viewModel.getPhotoAlbum(userId)
                        showPhotoPicker = true
                    },
                    createdPlaylists = createdPlaylists,
                    collectedPlaylists = collectedPlaylists,
                    albums = if (albumList is Resource.Success) (albumList as Resource.Success).data.data.map { it.toAlbum() } else emptyList(),
                    onAlbumClick = { id->
                        Screen.Album.navigate(navController) { addPath(id) }
                    },
                    onPlaylistClick = { id->
                        Screen.PlayList.navigate(navController) { addPath(id) }
                    }
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
                    albums = if (albumList is Resource.Success) (albumList as Resource.Success).data.data.map { it.toAlbum() } else emptyList(),
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
