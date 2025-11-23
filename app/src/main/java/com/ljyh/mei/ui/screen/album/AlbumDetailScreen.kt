package com.ljyh.mei.ui.screen.album

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.MiniAlbumDetail
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.model.toMiniAlbumDetail
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.extensions.mediaItems
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.playlist.PlaylistBackground
import com.ljyh.mei.ui.component.playlist.Track
import com.ljyh.mei.ui.component.shimmer.ButtonPlaceholder
import com.ljyh.mei.ui.component.shimmer.ShimmerHost
import com.ljyh.mei.ui.component.shimmer.TextPlaceholder
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.playlist.ActionButton
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.utils.TimeUtils.timestampToDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    id: Long,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    // 请求数据
    LaunchedEffect(id) {
        viewModel.getAlbumDetail(id.toString())
    }

    val albumDetail by viewModel.albumDetail.collectAsState()
    val playerConnection = LocalPlayerConnection.current ?: return
    val navController = LocalNavController.current
    val listState = rememberLazyListState()

    // 只有滑动超过头部后，才显示 TopBar 的标题和背景
    val showTopBarTitle by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Box(Modifier.fillMaxSize()) {

        // 1. 背景层
        val coverUrl = (albumDetail as? Resource.Success)?.data?.album?.picUrl
        PlaylistBackground(coverUrl = coverUrl)

        // 2. 内容层
        LazyColumn(
            // 移除 horizontal padding，让 Track 的波纹撑满屏幕
            contentPadding = PaddingValues(
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 16.dp
            ),
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            when (val res = albumDetail) {
                Resource.Loading -> {
                    item {
                        // 适配骨架屏位置
                        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                        Spacer(Modifier.height(64.dp))
                        AlbumShimmer()
                    }
                }

                is Resource.Error -> {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("加载失败: ${res.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                is Resource.Success -> {
                    val album = res.data
                    val miniAlbum = album.toMiniAlbumDetail()

                    item {
                        // 状态栏适配
                        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                        // 预留 TopBar 高度
                        Spacer(Modifier.height(64.dp))

                        AlbumHeaderInfo(
                            album = miniAlbum,
                            playAll = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        id = "album_${album.album.id}",
                                        title = album.album.name,
                                        items = album.songs.map { it.id.toString() }
                                    )
                                )
                            },
                            onCollect = {
//                                viewModel.toggleCollect(album.album.id)
                            }
                        )
                    }

                    items(album.songs.size) { i ->
                        val track = album.songs[i]
                        Track(
                            viewModel = playlistViewModel,
                            // 专辑内单曲通常没有封面，或者封面就是专辑封面，这里传入专辑封面
                            track = track.toMediaMetadata().copy(coverUrl = album.album.picUrl),
                            isPlaying = playerConnection.mediaMetadata.collectAsState().value?.id == track.id
                        ) {
                            // 播放单曲逻辑优化
                            val player = playerConnection.player
                            val currentItems = player.mediaItems
                            val index = currentItems.indexOfFirst { it.mediaId == track.id.toString() }

                            // 如果当前播放列表就是这张专辑，且包含了这首歌，直接 seek
                            if (index != -1 && playerConnection.queueTitle.value == album.album.name) {
                                player.seekToDefaultPosition(index)
                                player.play()
                            } else {
                                playerConnection.playQueue(
                                    ListQueue(
                                        id = "album_${album.album.id}",
                                        title = album.album.name,
                                        items = album.songs.map { it.id.toString() },
                                        startIndex = i
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. 沉浸式 TopAppBar
        val titleText = (albumDetail as? Resource.Success)?.data?.album?.name ?: "专辑详情"
        CenterAlignedTopAppBar(
            title = {
                AnimatedVisibility(visible = showTopBarTitle, enter = fadeIn(), exit = fadeOut()) {
                    Text(titleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = if (showTopBarTitle) MaterialTheme.colorScheme.surface else Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun AlbumHeaderInfo(
    album: MiniAlbumDetail,
    playAll: () -> Unit,
    onCollect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // 1. 专辑封面 (带阴影)
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.size(220.dp)
        ) {
            AsyncImage(
                model = album.picUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(24.dp))

        // 2. 标题
        Text(
            text = album.name,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        // 3. 歌手 · 年份
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = album.artist.joinToString(" / ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = " · ${timestampToDate(album.publishTime)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))

        // 4. 按钮组 (收藏 - 播放 - 分享)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 收藏
            ActionButton(
                icon = if (album.collected) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                text = if (album.collected) "已收藏" else "收藏",
                color = if (album.collected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onCollect
            )

            // 播放全部
            Button(
                onClick = playAll,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("播放全部")
            }

            // 分享 (模拟功能)
            ActionButton(
                icon = Icons.Filled.Share,
                text = "分享",
                onClick = { /* Share Logic */ }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Composable
fun AlbumShimmer() {
    ShimmerHost {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 封面
            TextPlaceholder(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 标题
            TextPlaceholder(
                modifier = Modifier
                    .height(32.dp)
                    .width(180.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 歌手信息
            TextPlaceholder(
                modifier = Modifier
                    .height(16.dp)
                    .width(120.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 按钮组
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

            // 列表
            repeat(6) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextPlaceholder(Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        TextPlaceholder(Modifier.height(16.dp).fillMaxWidth(0.6f))
                        Spacer(Modifier.height(8.dp))
                        TextPlaceholder(Modifier.height(12.dp).fillMaxWidth(0.4f))
                    }
                    Spacer(Modifier.width(16.dp))
                    ButtonPlaceholder(Modifier.size(24.dp))
                }
            }
        }
    }
}