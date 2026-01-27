package com.ljyh.mei.ui.screen.index.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.UserAvatarUrlKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.constants.UserNicknameKey
import com.ljyh.mei.constants.UserPhotoKey
import com.ljyh.mei.data.model.AlbumPhoto
import com.ljyh.mei.data.model.toAlbum
import com.ljyh.mei.data.model.toAlbumEntity
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.SingleImagePickerSheet
import com.ljyh.mei.ui.component.item.AlbumItem
import com.ljyh.mei.ui.component.item.PlaylistItem
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
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
        if (account is Resource.Success) {
            val data = (account as Resource.Success).data.profile
            setUserId(data.userId.toString())
            setUserNickname(data.nickname)
            setUserAvatarUrl(data.avatarUrl)
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

    // --- 界面主体 ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. 沉浸式背景 (如果已登录且有背景图)
        if (userId.isNotEmpty() && userPhoto.isNotEmpty()) {
            ImmersiveBackground(userPhoto)
        }

        if (userId.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    // 顶部避让状态栏，并增加额外间距
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                    bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                        .calculateBottomPadding() + 80.dp
                )
            ) {
                // 2. 杂志风格大标题
                item(key = "header") {
                    BigUserHeader(
                        nickname = userNickname,
                        avatarUrl = userAvatarUrl, // 使用头像而不是背景图
                        onAvatarClick = {
                            viewModel.getPhotoAlbum(userId)
                            showPhotoPicker = true
                        }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // 3. 不对称 Bento 仪表盘
                item(key = "dashboard") {
                    BentoDashboard(
                        // 暂时用背景图演示最近播放封面，之后建议从 HistoryViewModel 获取
                        lastPlayedCover = userPhoto,
                        onHistoryClick = {
                            Screen.History.navigate(navController)
                        },
                        onLocalClick = { /* TODO */ },
                        onCloudClick = { /* TODO */ },
                        onDownloadClick = { /* TODO */ }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                stickyHeader(key = "tabs") {
                    // 外层 Box：负责定位和留白，背景透明
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp) // 上下留出空隙
                            .padding(horizontal = 16.dp),       // 核心：左右留白，不切断背景
                        contentAlignment = Alignment.Center
                    ) {
                        // 内层 Surface：负责营造“玻璃”质感
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp), //稍微增加高度以容纳 Tab
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(50), // 巨大的圆角，做成胶囊形状
                            shadowElevation = 4.dp, // 添加一点阴影，增强悬浮感
                            tonalElevation = 2.dp
                        ) {
                            // 居中对齐 Tab
                            Box(contentAlignment = Alignment.Center) {
                                ModernCapsuleTabs(
                                    tabs = tabTitles,
                                    selectedIndex = selectedTabIndex,
                                    onTabClick = { selectedTabIndex = it }
                                )
                            }
                        }
                    }
                }

                // 5. 列表内容
                when (selectedTabIndex) {
                    0 -> { // 创建歌单
                        if (createdPlaylists.isEmpty()) item { EmptyState("暂无创建歌单") }
                        else items(createdPlaylists, key = { it.id }) { playlist ->
                            PlaylistItem(playlist) {
                                Screen.PlayList.navigate(navController) { addPath(playlist.id) }
                            }
                        }
                    }

                    1 -> { // 收藏歌单
                        if (collectedPlaylists.isEmpty()) item { EmptyState("暂无收藏歌单") }
                        else items(collectedPlaylists, key = { it.id }) { playlist ->
                            PlaylistItem(playlist) {
                                Screen.PlayList.navigate(navController) { addPath(playlist.id) }
                            }
                        }
                    }

                    2 -> { // 收藏专辑
                        val albums = (albumList as? Resource.Success)?.data?.data ?: emptyList()

                        if (albums.isEmpty()) item { EmptyState("暂无收藏专辑") }
                        else{
                            albums.map { al->
                                al.toAlbumEntity().let {
                                    viewModel.insertAlbum(it.first, it.second)
                                }
                            }
                            items(albums, key = { it.id }) { album ->
                                AlbumItem(album.toAlbum()) {
                                    Screen.Album.navigate(navController) { addPath(album.id.toString()) }
                                }
                            }
                        }
                    }
                }
            }
        } else if (cookie.isEmpty()) {
            // 未登录状态
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyLoginState(navController)
            }
        }
    }

    // 图片选择器逻辑保持不变
    if (showPhotoPicker) {
        PhotoPickerSheet(
            photoAlbum = photoAlbum,
            onSelect = { setUserPhoto(it); showPhotoPicker = false },
            onDismiss = { showPhotoPicker = false }
        )
    }
}

// --- 组件部分 ---

@Composable
fun ImmersiveBackground(imageUrl: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 80.dp) // 强力模糊
                .alpha(0.5f), // 降低透明度
            contentScale = ContentScale.Crop
        )
        // 叠加暗色遮罩，确保前台内容可读性
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun BigUserHeader(nickname: String, avatarUrl: String, onAvatarClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // 大圆角头像
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .size(80.dp)
                .clickable { onAvatarClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(
                text = "My Library",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Text(
                text = nickname.ifEmpty { "Music Lover" },
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BentoDashboard(
    lastPlayedCover: String?,
    onHistoryClick: () -> Unit,
    onLocalClick: () -> Unit,
    onCloudClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val cardHeight = 190.dp // 仪表盘总高度

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 左侧：巨大的最近播放入口 (占 60% 宽度) ---
        Card(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
                .clickable { onHistoryClick() },
            shape = RoundedCornerShape(28.dp),
            // 使用 SurfaceVariant 色调，避免和背景混淆
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 封面图背景
                if (!lastPlayedCover.isNullOrEmpty()) {
                    AsyncImage(
                        model = lastPlayedCover,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.8f),
                        contentScale = ContentScale.Crop
                    )
                    // 黑色渐变，保证文字清晰
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    ),
                                    startY = 100f
                                )
                            )
                    )
                }

                // 内容
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "最近播放",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "继续聆听", // 以后可以换成具体的歌名
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 装饰图标
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                )
            }
        }

        // --- 右侧：功能堆叠 (占 40% 宽度) ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 右上：本地音乐
            DashboardSmallCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Folder,
                label = "本地音乐",
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                onClick = onLocalClick
            )

            // 右下：下载/云盘 (两个小按钮放在一行)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardSmallCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Cloud,
                    label = "云盘",
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                    onClick = onCloudClick,
                    hideLabel = true
                )
                DashboardSmallCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Download,
                    label = "下载",
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    onClick = onDownloadClick,
                    hideLabel = true
                )
            }
        }
    }
}

@Composable
fun DashboardSmallCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    hideLabel: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (hideLabel) 32.dp else 28.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (!hideLabel) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ModernCapsuleTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabClick: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp), // 容器内部的内边距
        horizontalArrangement = Arrangement.SpaceEvenly,   // 均匀分布，或者用 spacedBy
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tabs.size) { index ->
            val isSelected = selectedIndex == index

            // 选中态：使用主色，实心
            // 未选中态：透明背景，只显示文字
            val containerColor =
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val contentColor =
                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

            // 增加点击区域的 Surface
            Surface(
                onClick = { onTabClick(index) },
                shape = CircleShape,
                color = containerColor,
                // 稍微调整高度，让它比外面的“灵动岛”小一圈
                modifier = Modifier.height(40.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabs[index],
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = fontWeight),
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyLoginState(navController: NavController) {
    Button(
        onClick = { Screen.ContentSettings.navigate(navController) },
    ) {
        Text("去填写 Cookie 以同步数据")
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