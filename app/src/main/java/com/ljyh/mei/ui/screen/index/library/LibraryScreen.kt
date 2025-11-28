package com.ljyh.mei.ui.screen.index.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.AlbumThumbnailSize
import com.ljyh.mei.constants.CommonImageRadius
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.UserAvatarUrlKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.constants.UserNicknameKey
import com.ljyh.mei.constants.UserPhotoKey
import com.ljyh.mei.data.model.AlbumPhoto
import com.ljyh.mei.data.model.UserAlbumList
import com.ljyh.mei.data.model.toAlbum
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.SingleImagePickerSheet
import com.ljyh.mei.ui.component.home.PlaylistItem
import com.ljyh.mei.ui.component.utils.fadingEdge
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.rememberPreference
import com.ljyh.mei.utils.smallImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    val account by viewModel.account.collectAsState()
    val photoAlbum by viewModel.photoAlbum.collectAsState()
    val localPlaylists by viewModel.localPlaylists.collectAsState()
    val albumList by viewModel.albumList.collectAsState()
    val networkState by viewModel.networkPlaylistsState.collectAsState()

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

    val (createdPlaylists, collectedPlaylists) = remember(localPlaylists, userId) {
        if (userId.isEmpty()) Pair(emptyList(), emptyList())
        else localPlaylists.partition { it.author == userId }
    }

    // Initial Sync Logic
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.syncUserPlaylists(userId)
            viewModel.getPhotoAlbum(userId)
            viewModel.getAlbumList()
        }
    }

    // Default Photo Logic
    LaunchedEffect(photoAlbum) {
        if (userPhoto.isEmpty() && photoAlbum is Resource.Success) {
            (photoAlbum as Resource.Success).data.data.records.firstOrNull()?.imageUrl?.let {
                setUserPhoto(it)
            }
        }
    }

    // Account Logic
    LaunchedEffect(cookie, account) {
        if (cookie.isNotEmpty() && account !is Resource.Success) {
            viewModel.getUserAccount()
        }
    }

    // Handle Account Success
    LaunchedEffect(account) {
        if (account is Resource.Success) {
            val data = (account as Resource.Success).data.profile
            setUserId(data.userId.toString())
            setUserNickname(data.nickname)
            setUserAvatarUrl(data.avatarUrl)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (userId.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                            .calculateBottomPadding()
                    )
            ) {
                // 1. 头部区域 (作为一个普通的 Item)
                item(key = "header") {
                    UserHeader(
                        userNickname = userNickname,
                        userPhoto = userPhoto,
                        height = 320.dp,
                        onPhotoChangeRequest = {
                            viewModel.getPhotoAlbum(userId)
                            showPhotoPicker = true
                        }
                    )
                }

                // 2. Tab 栏 (使用 stickyHeader 实现吸顶)
                stickyHeader(key = "tabs") {
                    // 这里的 Surface 很重要，吸顶时防止内容透视
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        CustomTabRow(
                            tabs = tabTitles,
                            selectedIndex = selectedTabIndex,
                            onTabClick = { index -> selectedTabIndex = index }
                        )
                    }
                }

                // 3. 根据选中的 Tab 动态渲染下方的内容
                // 注意：这里不需要 HorizontalPager，直接切换 items
                when (selectedTabIndex) {
                    0 -> { // 创建歌单
                        if (createdPlaylists.isEmpty()) {
                            item { EmptyState("暂无创建歌单") }
                        } else {
                            items(createdPlaylists, key = { it.id }) { playlist ->
                                PlaylistItem(playlist) {
                                    Screen.PlayList.navigate(navController) {
                                        addPath(playlist.id)
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // 收藏歌单
                        if (collectedPlaylists.isEmpty()) {
                            item { EmptyState("暂无收藏歌单") }
                        } else {
                            items(collectedPlaylists, key = { it.id }) { playlist ->
                                PlaylistItem(playlist) {
                                    Screen.PlayList.navigate(navController) {
                                        addPath(playlist.id)
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // 收藏专辑
                        val albums = (albumList as? Resource.Success)?.data?.data ?: emptyList()
                        if (albums.isEmpty()) {
                            item { EmptyState("暂无收藏专辑") }
                        } else {
                            items(albums, key = { it.id }) { album ->
                                AlbumItem(album.toAlbum()) {
                                    Screen.Album.navigate(navController) {
                                        addPath(album.id.toString())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(
                    LocalPlayerAwareWindowInsets.current.asPaddingValues()
                        .calculateBottomPadding()
                )
            )
        } else if (cookie.isEmpty()) {
            EmptyLoginState(navController)
        }
    }

    // Photo Picker Sheet
    if (showPhotoPicker) {
        PhotoPickerSheet(
            photoAlbum = photoAlbum,
            onSelect = { setUserPhoto(it); showPhotoPicker = false },
            onDismiss = { showPhotoPicker = false }
        )
    }
}

@Composable
fun CustomTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabClick: (Int) -> Unit
) {
    SecondaryTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = true),
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = selectedIndex == index
            Tab(
                selected = selected,
                onClick = { onTabClick(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(50.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserHeader(
    userNickname: String,
    userPhoto: String,
    height: Dp,
    onPhotoChangeRequest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        AsyncImage(
            model = userPhoto,
            contentDescription = "背景",
            modifier = Modifier
                .fillMaxSize()
                .fadingEdge(
                    top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding(),
                    bottom = 64.dp
                )
                .combinedClickable(onClick = {}, onLongClick = onPhotoChangeRequest),
            contentScale = ContentScale.Crop
        )

        // 渐变遮罩，让文字更清晰
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        startY = 300f
                    )
                )
        )

        Text(
            text = userNickname,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp)
                .clickable(onClick = onPhotoChangeRequest),
            style = TextStyle(
                shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(2f, 2f), 4f)
            )
        )
    }
}

@Composable
fun AlbumItem(album: Album, onClick: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(album.id) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = album.cover.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(AlbumThumbnailSize)
                .clip(RoundedCornerShape(CommonImageRadius)),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${album.artist.joinToString(" / ") { it.name }} · ${album.size}首",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            onClick = { Screen.ContentSettings.navigate(navController) },
        ) {
            Text("去填写 Cookie 以同步数据")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerSheet(
    photoAlbum: Resource<AlbumPhoto>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        when (photoAlbum) {
            is Resource.Success -> {
                SingleImagePickerSheet(
                    images = photoAlbum.data.data.records.map { it.imageUrl },
                    onSelect = onSelect,
                    onDismiss = onDismiss
                )
            }

            else -> {
                Box(Modifier
                    .fillMaxWidth()
                    .height(200.dp), contentAlignment = Alignment.Center) {
                    if (photoAlbum is Resource.Loading) CircularProgressIndicator()
                    else Text("无法加载图片")
                }
            }
        }
    }
}