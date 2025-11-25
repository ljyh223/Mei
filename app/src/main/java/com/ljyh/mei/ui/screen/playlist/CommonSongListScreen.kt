package com.ljyh.mei.ui.screen.playlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.ui.component.playlist.AddToPlaylistDialog
import com.ljyh.mei.ui.component.playlist.FinalPerfectCollage
import com.ljyh.mei.ui.component.playlist.PlaylistBackground
import com.ljyh.mei.ui.component.playlist.Track
import com.ljyh.mei.ui.component.playlist.TrackActionMenu
import com.ljyh.mei.ui.component.shimmer.ButtonPlaceholder
import com.ljyh.mei.ui.component.shimmer.ShimmerHost
import com.ljyh.mei.ui.component.shimmer.TextPlaceholder
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.UiPlaylist


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonSongListScreen(
    uiData: UiPlaylist,
    pagingItems: LazyPagingItems<MediaMetadata>? = null,
    isLoading: Boolean,
    // 头部操作
    onPlayAll: () -> Unit,
    onHeaderAction: () -> Unit,
    headerActionIcon: ImageVector,
    headerActionLabel: String,

    // 列表操作
    onTrackClick: (MediaMetadata, Int) -> Unit,
    onBack: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()

    var menuTargetTrack by remember { mutableStateOf<MediaMetadata?>(null) }
    var trackToAdd by remember { mutableStateOf<MediaMetadata?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }


    val allMePlaylist by viewModel.playlist.collectAsState()

    val showTopBarTitle by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }


    LaunchedEffect(Unit) {
        if (uiData.title.endsWith("喜欢的音乐")) {
            viewModel.updateAllLike(uiData.tracks.map { Like(it.id.toString()) })
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isLoading && uiData.coverUrl.isNotEmpty()) {
            PlaylistBackground(
                coverUrl = uiData.coverUrl[0],
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        AnimatedVisibility(
                            visible = !isLoading && showTopBarTitle,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = uiData.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                // 确保标题文字在深色背景下可见，或者跟随主题
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                // 确保图标在背景上可见，通常用 OnSurface 或者纯白
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        // 1. 初始状态：完全透明，直接显示下方的模糊大图
                        containerColor = Color.Transparent,

                        // 2. 滚动状态：使用半透明的 Surface 颜色 (Glassmorphism 效果)
                        // 这样既能区分头部，又不会产生割裂感
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),

                        // 确保图标和标题颜色正确
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    PlaylistShimmer()
                } else {

                    LazyColumn(
                        state = lazyListState,
                        contentPadding = PaddingValues(
                            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                                .calculateBottomPadding() + 16.dp
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. 通用头部
                        item {
                            GenericHeader(
                                title = uiData.title,
                                cover = uiData.coverUrl,
                                creator = uiData.creatorName,
                                onPlayAll = onPlayAll,
                                actionIcon = headerActionIcon,
                                actionLabel = headerActionLabel,
                                count = uiData.count,
                                playCount = uiData.playCount ?: 0L,
                                subscribeCount = uiData.subscriberCount,
                                isSubscribed = uiData.isSubscribed,
                                onSubscribed = {
                                    onHeaderAction()
                                }
                            )
                        }

                        // 2. 歌曲列表
                        if (pagingItems != null) {
                            // === 分支 A: 使用 Paging 3 ===
                            items(
                                count = pagingItems.itemCount,
                                key = pagingItems.itemKey { it.id }, // 使用 Paging 扩展的 key
                                contentType = pagingItems.itemContentType { "Track" }
                            ) { index ->
                                val track = pagingItems[index]
                                if (track != null) {
                                    Track(
                                        track = track,
                                        onClick = { onTrackClick(track, index) }, // index 对分页也很重要
                                        onMoreClick = { menuTargetTrack = track },
                                        viewModel = viewModel
                                    )
                                } else {
                                    // 可选：如果不禁用 placeholder，这里渲染一个简单的占位条
                                }
                            }

                            // Paging 3 的底部加载状态处理
                            when (pagingItems.loadState.append) {
                                is LoadState.Loading -> {
                                    item {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(Modifier.size(24.dp))
                                        }
                                    }
                                }

                                is LoadState.Error -> {
                                    item { Text("加载更多失败，点击重试") }
                                }

                                else -> {}
                            }

                        } else {
                            itemsIndexed(
                                uiData.tracks,
                                key = { _, item -> item.id }) { index, track ->
                                Track(
                                    track = track,
                                    onClick = { onTrackClick(track, index) },
                                    onMoreClick = { menuTargetTrack = track },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }


                    if (menuTargetTrack != null) {
                        TrackActionMenu(
                            targetTrack = menuTargetTrack,
                            isCreator = uiData.isCreate,
                            onDismiss = { menuTargetTrack = null },
                            onAddToPlaylist = {
                                trackToAdd = menuTargetTrack
                                menuTargetTrack = null
                                showAddToPlaylistDialog = true
                                viewModel.getAllMePlaylist()
                            },
                            onDelete = {
                                viewModel.deleteSongFromPlaylist(
                                    uiData.id,
                                    menuTargetTrack!!.id.toString()
                                )
                                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                menuTargetTrack = null
                            },
                            onCopyId = {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "id",
                                        menuTargetTrack?.id.toString()
                                    )
                                )
                                Toast.makeText(context, "ID 已复制", Toast.LENGTH_SHORT).show()
                            },
                            onCopyName = {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "id",
                                        menuTargetTrack?.id.toString()
                                    )
                                )
                                Toast.makeText(context, "Name 已复制", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    AddToPlaylistDialog(
                        isVisible = showAddToPlaylistDialog,
                        playlists = allMePlaylist,
                        onDismiss = { showAddToPlaylistDialog = false },
                        onSelectPlaylist = { selectedPlaylist ->
                            // 执行添加逻辑
                            if (trackToAdd != null) {
                                viewModel.addSongToPlaylist(
                                    pid = selectedPlaylist.id,
                                    trackIds = trackToAdd!!.id.toString()
                                )
                                Toast.makeText(
                                    context,
                                    "已添加到 ${selectedPlaylist.title}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.d(
                                    "Playlist",
                                    "Added ${trackToAdd?.title} to ${selectedPlaylist.title}"
                                )
                            }
                            showAddToPlaylistDialog = false
                            trackToAdd = null //清理状态
                        }
                    )


                }
            }
        }

    }


}


@Composable
fun GenericHeader(
    title: String,
    count: Int,
    playCount: Long,
    subscribeCount: Long,
    cover: List<String>,
    creator: String,
    isSubscribed: Boolean,
    onPlayAll: () -> Unit,
    onSubscribed: (Boolean) -> Unit,
    actionIcon: ImageVector,
    actionLabel: String,
) {


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.size(220.dp)
        ) {
            if (cover.size < 5) {
                AsyncImage(
                    model = cover.firstOrNull(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                FinalPerfectCollage(
                    imageUrls = cover,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Title & Metadata
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "By $creator",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape
            ) {
                Text(
                    text = "$count 首",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            text = "播放 $playCount · 收藏 $subscribeCount",
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


            ActionButton(
                icon = actionIcon,
                text = actionLabel,
                color = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = {
                    onSubscribed(isSubscribed)
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
//                    handleDownloadClick(
//                        context = context,
//                        isCreator = isCreator,
//                        playlistDetail = playlistDetail,
//                        onShowDialog = { count, ids ->
//                            downloadCount.intValue = count
//                            downloadIds.value = ids
//                            showDownloadDialog.value = true
//                        }
//                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Composable
fun PlaylistShimmer() {
    ShimmerHost {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            TextPlaceholder(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))
            TextPlaceholder(
                modifier = Modifier
                    .height(32.dp)
                    .width(180.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextPlaceholder(
                modifier = Modifier
                    .height(16.dp)
                    .width(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ButtonPlaceholder(modifier = Modifier.size(48.dp))
                TextPlaceholder(
                    modifier = Modifier
                        .height(48.dp)
                        .width(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                ButtonPlaceholder(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
            repeat(6) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextPlaceholder(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(16.dp))
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
                    ButtonPlaceholder(modifier = Modifier.size(24.dp))
                }
            }
        }
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