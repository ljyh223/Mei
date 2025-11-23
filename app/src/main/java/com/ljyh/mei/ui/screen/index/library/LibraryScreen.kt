package com.ljyh.mei.ui.screen.index.library

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.*
import com.ljyh.mei.data.model.AlbumPhoto
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
import kotlin.math.roundToInt

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

    // Data processing (Moved derived state here)
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

    // --- UI Layout ---
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            if (userId.isNotEmpty()) {
                CollapsingProfileContent(
                    userNickname = userNickname,
                    userPhoto = userPhoto,
                    onPhotoChangeRequest = {
                        viewModel.getPhotoAlbum(userId)
                        showPhotoPicker = true
                    },
                    tabTitles = listOf("创建歌单", "收藏歌单", "收藏专辑"),
                    pagerContent = { pageIndex ->
                        when (pageIndex) {
                            0 -> PlaylistLazyPage(createdPlaylists, networkState is Resource.Loading, navController)
                            1 -> PlaylistLazyPage(collectedPlaylists, networkState is Resource.Loading, navController)
                            2 -> AlbumLazyPage(albumList, albumList is Resource.Loading, navController)
                        }
                    }
                )
            } else if (cookie.isEmpty()) {
                EmptyLoginState(navController)
            }
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

/**
 * 核心组件：实现折叠头部效果
 * 使用 NestedScrollConnection 监听滑动，动态改变 Header 的偏移量
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollapsingProfileContent(
    userNickname: String,
    userPhoto: String,
    onPhotoChangeRequest: () -> Unit,
    tabTitles: List<String>,
    pagerContent: @Composable (Int) -> Unit
) {
    val density = LocalDensity.current
    // 头部高度配置
    val headerHeight = 320.dp
    val tabRowHeight = 48.dp
    val headerHeightPx = with(density) { headerHeight.toPx() }

    // 当前 Header 的偏移量 (负数表示向上移出屏幕)
    var headerOffsetHeightPx by remember { mutableFloatStateOf(0f) }

    // 嵌套滑动连接器
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = headerOffsetHeightPx + delta
                // 限制偏移量在 -Header高度 到 0 之间
                headerOffsetHeightPx = newOffset.coerceIn(-headerHeightPx, 0f)
                return Offset.Zero // 不消耗滑动，传给 LazyColumn
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // 1. 内容区域 (Pager)
        // 这里的 padding 是关键：内容实际上是从顶部开始的，但我们给它一个顶部的 padding，把位置留给 Header
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true // 允许左右滑动
        ) { page ->
            // 给 LazyColumn 加一个 contentPadding，高度等于 Header + TabRow
            // 这样初始状态下，List 的第一个 Item 就在 TabRow 下方
            Box(modifier = Modifier.fillMaxSize()) {
                pagerContent(page)
            }
        }

        // 2. 头部区域 (Header + Tabs)
        // 使用 graphicsLayer 来做平移，性能更好
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight + tabRowHeight) // 总高度
                .graphicsLayer {
                    translationY = headerOffsetHeightPx
                }
                .background(MaterialTheme.colorScheme.surface) // 防止透视
        ) {
            // 用户信息 Header
            UserHeader(
                userNickname = userNickname,
                userPhoto = userPhoto,
                height = headerHeight,
                onPhotoChangeRequest = onPhotoChangeRequest
            )

            // TabRow
            CustomTabRow(
                tabs = tabTitles,
                pagerState = pagerState,
                onTabClick = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            )
        }
    }
}

/**
 * 美化后的 TabRow
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTabRow(
    tabs: List<String>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onTabClick: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = { /* 去掉默认分割线，更现代 */ },
        indicator = { tabPositions ->
            if (pagerState.currentPage < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier
                        .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                        .padding(horizontal = 20.dp) // 指示器短一点
                        .clip(CircleShape), // 圆角指示器
                    height = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = pagerState.currentPage == index
            Tab(
                selected = selected,
                onClick = { onTabClick(index) },
                text = {
                    Text(
                        text = title,
                        fontSize = if (selected) 16.sp else 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

/**
 * 用户头部组件 (拆分出来)
 */
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
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        startY = 300f
                    )
                )
        )

        Text(
            text = userNickname,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
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

/**
 * 优化后的播放列表页 (使用 LazyColumn)
 */
@Composable
fun PlaylistLazyPage(
    playlists: List<Playlist>,
    isLoading: Boolean,
    navController: NavController
) {
    val density = LocalDensity.current
    // 计算顶部 padding: Header高度 + Tab高度
    // 注意：这里必须硬编码或传递高度，因为在 Collapsing Layout 中，List 是铺满全屏的
    val topPadding = 320.dp + 48.dp
    val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    if (playlists.isEmpty() && !isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无歌单",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding + 16.dp)
        ) {
            if (isLoading && playlists.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            items(playlists, key = { it.id }) { playlist ->
                PlaylistItem(playlist) {
                    Screen.PlayList.navigate(navController) {
                        addPath(playlist.id)
                    }
                }
            }
        }
    }
}

/**
 * 优化后的专辑页 (使用 LazyColumn)
 */
@Composable
fun AlbumLazyPage(
    albumList: Resource<UserAlbumList>,
    isLoading: Boolean,
    navController: NavController
) {
    val topPadding = 320.dp + 48.dp
    val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding + 16.dp)
    ) {
        when (albumList) {
            is Resource.Success -> {
                val albums = albumList.data.data
                if (albums.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) {
                            Text("这里什么都没有哦", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(albums, key = { it.id }) { album ->
                        AlbumItem(album) { id ->
                            Screen.Album.navigate(navController) { addPath(id.toString()) }
                        }
                    }
                }
            }
            is Resource.Error -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) {
                        Text("加载失败")
                    }
                }
            }
            Resource.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumItem(album: UserAlbumList.Data, onClick: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(album.id) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = album.picUrl.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp) // 稍微大一点好看
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${album.artists.joinToString(" / ") { it.name }} · ${album.size}首",
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
    photoAlbum: Resource<AlbumPhoto>, // 假设复用类型，如果是不同类型请修改
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        // 这里沿用你原来的逻辑，稍微解耦
        when (photoAlbum) {
            is Resource.Success -> {
                SingleImagePickerSheet(
                    images = photoAlbum.data.data.records.map { it.imageUrl },
                    onSelect = onSelect,
                    onDismiss = onDismiss
                )
            }
            else -> {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    if (photoAlbum is Resource.Loading) CircularProgressIndicator()
                    else Text("无法加载图片")
                }
            }
        }
    }
}