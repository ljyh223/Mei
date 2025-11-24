package com.ljyh.mei.ui.screen.playlist

import android.app.Activity
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.google.gson.Gson
import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.MiniPlaylistDetail
import com.ljyh.mei.data.model.SimplePlaylist
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.model.toMiniPlaylistDetail
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.extensions.mediaItems
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.ConfirmationDialog
import com.ljyh.mei.ui.component.playlist.FinalPerfectCollage
import com.ljyh.mei.ui.component.playlist.PlaylistBackground
import com.ljyh.mei.ui.component.playlist.Track
import com.ljyh.mei.ui.component.shimmer.ButtonPlaceholder
import com.ljyh.mei.ui.component.shimmer.ShimmerHost
import com.ljyh.mei.ui.component.shimmer.TextPlaceholder
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.utils.PermissionsUtils.checkAndRequestFilesPermissions
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    id: Long,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    LaunchedEffect(key1 = id) {
        viewModel.getPlaylistDetail(id.toString())
    }

    val userId by rememberPreference(UserIdKey, "")
    val playlistDetail by viewModel.playlistDetail.collectAsState()

    // Paging Flow logic
    val pagingFlow = remember(id, playlistDetail) {
        if (playlistDetail is Resource.Success) {
            viewModel.getPlaylistTracks(id.toString(), userId, playlistDetail)
        } else {
            flowOf(PagingData.empty())
        }
    }
    val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()

    val lazyListState = rememberLazyListState()
    val playerConnection = LocalPlayerConnection.current ?: return
    val navController = LocalNavController.current

    // Show title in TopBar only when scrolled past the header
    val showTopBarTitle by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val result = playlistDetail) {
            is Resource.Success -> {
                val detail = result.data.toMiniPlaylistDetail()
                // Auto-like logic moved here purely as side effect
                LaunchedEffect(Unit) {
                    if (result.data.playlist.name.endsWith("喜欢的音乐")) {
                        viewModel.updateAllLike(result.data.playlist.trackIds.map { Like(it.id.toString()) })
                    }
                }

                // Blurred Background
                PlaylistBackground(coverUrl = detail.cover.firstOrNull())

                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                            .calculateBottomPadding() + 16.dp
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Item
                    item {
                        // Padding for status bar
                        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                        // Top Bar Placeholder height
                        Spacer(Modifier.height(64.dp))

                        PlaylistHeaderInfo(
                            playlistDetail = detail,
                            viewModel = viewModel,
                            onPlayAll = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        id = "playlist_${result.data.playlist.Id}",
                                        title = result.data.playlist.name,
                                        items = result.data.playlist.trackIds.map { it.id.toString() }
                                    )
                                )
                            }
                        )
                    }

                    // Track List
                    items(lazyPagingItems.itemCount) { index ->
                        val track = lazyPagingItems[index]
                        if (track != null) {
                            Track(
                                viewModel = viewModel,
                                track = track.toMediaMetadata(),
                                isPlaying = playerConnection.mediaMetadata.collectAsState().value?.id == track.id,
                                onClick = {
                                    // Optimized seek logic
                                    val currentMediaItems = playerConnection.player.mediaItems
                                    val foundIndex =
                                        currentMediaItems.indexOfFirst { it.mediaId == track.id.toString() }

                                    if (foundIndex != -1) {
                                        playerConnection.player.seekToDefaultPosition(foundIndex)
                                        playerConnection.player.play()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                id = "playlist_${result.data.playlist.Id}",
                                                title = result.data.playlist.name,
                                                items = lazyPagingItems.itemSnapshotList.items.map { it.id.toString() },
                                                startIndex = index
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Custom TopAppBar
                CenterAlignedTopAppBar(
                    title = {
                        AnimatedVisibility(
                            visible = showTopBarTitle,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(text = detail.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (showTopBarTitle) MaterialTheme.colorScheme.surface else Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            is Resource.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${result.message}")
                }
            }

            Resource.Loading -> {
                Scaffold(
                    topBar = {
                        TopAppBar(title = {}, navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        })
                    }
                ) { padding ->
                    Box(Modifier.padding(padding)) { PlaylistShimmer() }
                }
            }
        }
    }
}


@Composable
fun PlaylistHeaderInfo(
    playlistDetail: MiniPlaylistDetail,
    viewModel: PlaylistViewModel,
    onPlayAll: () -> Unit
) {
    val context = LocalContext.current
    val userId by rememberPreference(UserIdKey, "")
    val scope = rememberCoroutineScope()
    var isSubscribed by remember(playlistDetail.subscribed) {
        mutableStateOf(playlistDetail.subscribed)
    }

    val showDownloadDialog = remember { mutableStateOf(false) }
    val downloadCount = remember { mutableIntStateOf(0) }
    val downloadIds = remember { mutableStateOf("") }
    val subscriberState by viewModel.subscribePlaylist.collectAsState()
    val unSubscriberState by viewModel.unSubscribePlaylist.collectAsState()

    LaunchedEffect(subscriberState) {
        if (subscriberState is Resource.Error) {
            Toast.makeText(context, "收藏失败: ${(subscriberState as Resource.Error).message}", Toast.LENGTH_SHORT).show()
            // 可选：如果失败了，把状态改回去
            isSubscribed = false
        }
    }

    LaunchedEffect(unSubscriberState) {
        if (unSubscriberState is Resource.Error) {
            Toast.makeText(context, "取消收藏失败: ${(unSubscriberState as Resource.Error).message}", Toast.LENGTH_SHORT).show()
            // 可选：如果失败了，把状态改回去
            isSubscribed = true
        }
    }



    ConfirmationDialog(
        title = "确认下载",
        text = "共计下载 ${downloadCount.intValue} 首歌曲",
        onConfirm = { prepare(downloadIds.value, scope, playlistDetail, viewModel) },
        onDismiss = { /* Do nothing */ },
        openDialog = showDownloadDialog
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Cover Image with Shadow
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.size(220.dp)
        ) {
            if (playlistDetail.cover.size < 5) {
                AsyncImage(
                    model = playlistDetail.cover.firstOrNull(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                FinalPerfectCollage(
                    imageUrls = playlistDetail.cover,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Title & Metadata
        Text(
            text = playlistDetail.name,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "By ${playlistDetail.createUserName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape
            ) {
                Text(
                    text = "${playlistDetail.count} 首",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            text = "播放 ${playlistDetail.playCount} · 收藏 ${playlistDetail.subscribedCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subscribe Button
            val isCreator = userId == playlistDetail.creatorUserId.toString()


            ActionButton(
                icon = if (isSubscribed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                text = if (isSubscribed) "已收藏" else "收藏",
                color = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = {
                    if (isCreator) {
                        Toast.makeText(context, "不能收藏自己创建的歌单", Toast.LENGTH_SHORT).show()
                    } else {
                        isSubscribed = !isSubscribed
                        if (isSubscribed) {
                            viewModel.subscribePlaylist(playlistDetail.id.toString())
                        } else {
                            viewModel.unsubscribePlaylist(playlistDetail.id.toString())
                        }
                    }
                }
            )


            // Play All Button (Prominent)
            Button(
                onClick = onPlayAll,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("播放全部")
            }

            // Download Button
            ActionButton(
                icon = Icons.Filled.Download,
                text = "下载",
                onClick = {
                    handleDownloadClick(
                        context = context,
                        isCreator = isCreator,
                        playlistDetail = playlistDetail,
                        onShowDialog = { count, ids ->
                            downloadCount.intValue = count
                            downloadIds.value = ids
                            showDownloadDialog.value = true
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(contentColor = color)
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(28.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
    }
}

// Extracted logic for download click to keep UI clean
fun handleDownloadClick(
    context: android.content.Context,
    isCreator: Boolean,
    playlistDetail: MiniPlaylistDetail,
    onShowDialog: (Int, String) -> Unit
) {
    if (!isCreator) {
        // Logic for non-creators (add your logic if needed, current code empty in original)
        return
    }

    if (playlistDetail.count > 500) {
        Toast.makeText(context, "歌曲数量大于500", Toast.LENGTH_SHORT).show()
        return
    }

    if (!checkAndRequestFilesPermissions(context as Activity)) {
        Toast.makeText(context, "没有权限", Toast.LENGTH_SHORT).show()
        return
    }

    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    val file = downloadDir.listFiles()?.find { it.isFile && it.name == "${playlistDetail.id}.json" }

    if (file != null) {
        try {
            val json = file.readText()
            val playlist = Gson().fromJson(json, SimplePlaylist::class.java)
            if (playlist.songs.size < playlistDetail.count) {
                val difference = playlistDetail.trackIds.filterNot { a ->
                    playlist.songs.any { s -> s.id == a.toString() }
                }
                if (difference.isNotEmpty()) {
                    onShowDialog(difference.size, difference.joinToString(","))
                } else {
                    Toast.makeText(context, "没有需要下载的", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "没有需要下载的", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("PlaylistScreen", "Error reading local json", e)
        }
    } else {
        onShowDialog(playlistDetail.count, playlistDetail.trackIds.joinToString(","))
    }
}

// Kept the original prepare function logic
fun prepare(
    ids: String,
    scope: CoroutineScope,
    playlistDetail: MiniPlaylistDetail,
    viewModel: PlaylistViewModel,
) {
    val context = AppContext.instance
    // val notificationHelper = NotificationHelper(context)
    // Uncomment when NotificationHelper is available
    Log.d("download", ids)
    Toast.makeText(context, "正在建设中: $ids", Toast.LENGTH_SHORT).show()
    // Original commented-out implementation...
}

@Composable
fun PlaylistShimmer() {
    ShimmerHost {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 对应 Header 的 padding
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 模拟 TopBar 和状态栏的高度占位
            Spacer(modifier = Modifier.height(80.dp))

            // 2. 居中的大封面 (对应 Card size 220dp)
            TextPlaceholder(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. 居中的标题 (模拟 HeadlineSmall)
            TextPlaceholder(
                modifier = Modifier
                    .height(32.dp)
                    .width(180.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. 居中的次要信息 (作者、数量等)
            TextPlaceholder(
                modifier = Modifier
                    .height(16.dp)
                    .width(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. 按钮栏 (收藏 - 播放全部 - 下载)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 模拟收藏按钮 (圆形)
                ButtonPlaceholder(modifier = Modifier.size(48.dp))

                // 模拟"播放全部"按钮 (长胶囊形)
                // 这里用 TextPlaceholder 模拟长按钮，圆角设大一点
                TextPlaceholder(
                    modifier = Modifier
                        .height(48.dp)
                        .width(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                )

                // 模拟下载按钮 (圆形)
                ButtonPlaceholder(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 6. 列表项
            // 这里我们手动写 Item 的结构，以匹配新的 Track 组件 (50dp 图片 + 垂直间距)
            repeat(6) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp), // 对应 Track 的 vertical padding
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 小封面
                    TextPlaceholder(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // 文字区域
                    Column(modifier = Modifier.weight(1f)) {
                        TextPlaceholder(
                            modifier = Modifier
                                .height(16.dp)
                                .fillMaxWidth(0.6f) // 标题长度随机感
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextPlaceholder(
                            modifier = Modifier
                                .height(12.dp)
                                .fillMaxWidth(0.4f) // 副标题更短
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 右侧更多按钮占位
                    ButtonPlaceholder(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}