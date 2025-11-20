package com.ljyh.mei.ui.screen.index.library

import android.R.attr.onClick
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.UserAvatarUrlKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.constants.UserNicknameKey
import com.ljyh.mei.constants.UserPhotoKey
import com.ljyh.mei.data.model.UserAlbumList
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.SingleImagePickerSheet
import com.ljyh.mei.ui.component.home.PlaylistItem
import com.ljyh.mei.ui.component.utils.fadingEdge
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.rememberPreference
import com.ljyh.mei.utils.smallImage
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account by viewModel.account.collectAsState()

    val navController = LocalNavController.current
    val photoAlbum by viewModel.photoAlbum.collectAsState()
    val localPlaylists by viewModel.localPlaylists.collectAsState()
    val albumList by viewModel.albumList.collectAsState()
    val networkState by viewModel.networkPlaylistsState.collectAsState()
    val (userId, setUserId) = rememberPreference(UserIdKey, "")
    val (userNickname, setUserNickname) = rememberPreference(UserNicknameKey, "")
    val (userAvatarUrl, setUserAvatarUrl) = rememberPreference(UserAvatarUrlKey, "")
    val (userPhoto, setUserPhoto) = rememberPreference(UserPhotoKey, "")
    val cookie by rememberPreference(CookieKey, defaultValue = "")

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    val scrollState = rememberScrollState()
    var showPhotoPicker by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val tabTitles = listOf("创建的歌单", "收藏的歌单", "收藏的专辑")
    val (createdPlaylists, collectedPlaylists) = remember(localPlaylists, userId) {
        if (userId.isEmpty()) {
            Pair(emptyList(), emptyList())
        } else {
            localPlaylists.partition { it.author == userId }
        }
    }
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            scrollState.animateScrollTo(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    LaunchedEffect(key1 = userId) {
        if (userId.isNotEmpty()) {
            Log.d("libraryScreen", "Syncing user playlist for userId: $userId")
            viewModel.syncUserPlaylists(userId) // 调用新的同步函数
            viewModel.getPhotoAlbum(userId)
            viewModel.getAlbumList()
        }
    }
    LaunchedEffect(photoAlbum) {
        if (userPhoto.isEmpty() && photoAlbum is Resource.Success) {
            val photos = (photoAlbum as Resource.Success).data.data.records
            if (photos.isNotEmpty()) {
                setUserPhoto(photos.first().imageUrl)
            }
        }
    }





    LaunchedEffect(key1 = cookie, key2 = account) {
        if (cookie.isNotEmpty() && account !is Resource.Success) {
            Log.d("libraryScreen", "Loading user account with cookie")
            viewModel.getUserAccount()
        }
    }

    when (val result = account) {
        is Resource.Error -> {
            Log.d("libraryScreen", result.toString())
        }

        Resource.Loading -> {
            Log.d("libraryScreen", result.toString())
        }

        is Resource.Success -> {
            //异步写入datastore
            setUserId(result.data.profile.userId.toString())
            setUserNickname(result.data.profile.nickname)
            setUserAvatarUrl(result.data.profile.avatarUrl)
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        if (userId.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                User(
                    userNickname = userNickname,
                    userPhoto = userPhoto,
                    onPhotoChangeRequest = {
                        viewModel.getPhotoAlbum(userId)
                        showPhotoPicker = true
                    }
                )
                Spacer(Modifier.height(10.dp))
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(text = title) }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    when (page) {
                        0 -> {
                            val currentList = createdPlaylists
                            PlaylistPage(
                                playlists = currentList,
                                isLoading = networkState is Resource.Loading,
                                navController = navController
                            )
                        }
                        1 -> {
                            val currentList = collectedPlaylists
                            PlaylistPage(
                                playlists = currentList,
                                isLoading = networkState is Resource.Loading,
                                navController = navController
                            )
                        }
                        2 -> {
                            AlbumPage(
                                albumList = albumList,
                                isLoading = albumList is Resource.Loading,
                                navController = navController
                            )
                        }
                    }
                }

                Spacer(
                    Modifier.height(
                        LocalPlayerAwareWindowInsets.current.asPaddingValues()
                            .calculateBottomPadding()
                    )
                )
            }

        } else if (cookie == "") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        Screen.ContentSettings.navigate(navController)
                    },
                ) {
                    Text("去填写cookie")
                }
            }
        }
    }

    if (showPhotoPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()

        val hideSheet = {
            scope.launch {
                sheetState.hide()
                showPhotoPicker = false
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showPhotoPicker = false },
            sheetState = sheetState
        ) {
            when (val result = photoAlbum) {
                is Resource.Success -> {
                    SingleImagePickerSheet(
                        images = result.data.data.records.map { it.imageUrl },
                        onSelect = { selectedImageUrl ->
                            setUserPhoto(selectedImageUrl) // 更新状态并保存
                            hideSheet()
                        },
                        onDismiss = {
                            hideSheet()
                        }
                    )
                }

                is Resource.Error -> {
                    // 可以显示错误提示
                    Text("加载图片失败，请重试。")
                }

                Resource.Loading -> {
                }
            }
        }
    }
}

@Composable
fun PlaylistPage(playlists: List<Playlist>,isLoading: Boolean, navController: NavController) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (playlists.isNotEmpty()) {
            playlists.forEach { playlist ->
                PlaylistItem(playlist) {
                    Screen.PlayList.navigate(navController) {
                        addPath(playlist.id)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = "这里什么都没有哦",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

    }
}


@Composable
fun AlbumPage(
    albumList: Resource<UserAlbumList>, // 如果你是 List 就改成 List
    isLoading: Boolean,
    navController: NavController
) {
    when (albumList) {
        is Resource.Success -> {
            val albums = albumList.data.data // ← 按照你接口结构修改

            if (albums.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "这里什么都没有哦",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    albums.forEach { album ->
                        AlbumItem(album) { id ->
                            Screen.Album.navigate(navController) {
                                addPath(id.toString())
                            }
                        }
                    }
                }
            }
        }
        is Resource.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("加载失败，请下拉刷新")
            }
        }
        Resource.Loading -> {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}


@Composable
fun AlbumItem(album: UserAlbumList.Data, onClick: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .height(48.dp)
            .clickable { onClick(album.id) },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,

        ) {
        AsyncImage(
            model = album.picUrl.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = album.name,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
//            Text(
//                text = "${playlist.count} • ${playlist.authorName}",
//                color = MaterialTheme.colorScheme.secondary,
//                fontSize = 12.sp,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
        }

    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun User(
    userNickname: String,
    userPhoto: String,
    onPhotoChangeRequest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3)
    ) {
        // 图片展示
        AsyncImage(
            model = userPhoto,
            contentDescription = "用户背景图片",
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .fadingEdge(
                    top = WindowInsets.systemBars
                        .asPaddingValues()
                        .calculateTopPadding(),
                    bottom = 64.dp
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = onPhotoChangeRequest // 长按时触发图片更换请求
                ),
            contentScale = ContentScale.Crop
        )

        // 用户昵称
        Text(
            text = userNickname,
            fontSize = 32.sp,
            fontWeight = FontWeight.W900,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(vertical = 32.dp)
                .clickable(onClick = onPhotoChangeRequest),
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(4f, 4f),
                    blurRadius = 8f
                )
            )
        )
    }
}