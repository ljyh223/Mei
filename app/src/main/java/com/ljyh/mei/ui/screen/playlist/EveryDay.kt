package com.ljyh.mei.ui.screen.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ljyh.mei.data.model.MiniPlaylistDetail
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.extensions.mediaItems
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.playlist.PlaylistBackground
import com.ljyh.mei.ui.component.playlist.Track
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EveryDay(
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val everyDaySongs by viewModel.everyDay.collectAsState()
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current ?: return

    // 滚动状态管理
    val lazyListState = rememberLazyListState()
    // 仅当列表滑动一段距离后才显示 TopBar 标题
    val showTopBarTitle by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }

    LaunchedEffect(true) {
        viewModel.getEveryDayRecommendSongs()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val result = everyDaySongs) {
            is Resource.Success -> {
                val songs = result.data.data.dailySongs
                // 获取第一张图作为背景模糊图
                val firstCover = songs.firstOrNull()?.al?.picUrl

                // 1. 复用背景模糊组件 (假设你已将 PlaylistBackground 放在同一包或公共组件中)
                PlaylistBackground(coverUrl = firstCover)

                LazyColumn(
                    // 移除 horizontal padding，让 Item 铺满宽度
                    contentPadding = PaddingValues(
                        bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 16.dp
                    ),
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        // 状态栏适配
                        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                        // 预留 TopBar 高度
                        Spacer(Modifier.height(64.dp))

                        // 构造虚拟的歌单详情对象用于 Header 展示
                        val dateStr = try {
                            LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd"))
                        } catch (e: Exception) {
                            "今日"
                        }

                        // 2. 复用头部组件
                        PlaylistHeaderInfo(
                            playlistDetail = MiniPlaylistDetail(
                                id = -1L, // 每日推荐通常没有固定的 ID
                                name = "每日推荐",
                                cover = songs.take(5).map { it.al.picUrl }, // 取前5张做拼贴封面
                                description = "根据你的音乐口味生成，每天6:00更新",
                                tracks = songs.map { it.toMediaMetadata() },
                                trackIds = songs.map { it.id },
                                creatorUserId = 0,
                                createUserName = "Mei Music · $dateStr", // 显示日期更有感觉
                                count = songs.size,
                                playCount = 0,
                                subscribedCount = 0,
                                subscribed = false // 每日推荐通常不可收藏歌单本身
                            ),
                            viewModel = viewModel,
                            onPlayAll = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        id = "everyday",
                                        title = "每日推荐",
                                        items = songs.map { it.id.toString() }
                                    )
                                )
                            }
                        )
                    }

                    // 3. 复用新的 Track 组件
                    items(songs.size) { index ->
                        val track = songs[index]
                        Track(
                            viewModel = viewModel,
                            track = track.toMediaMetadata(),
                            isPlaying = playerConnection.mediaMetadata.collectAsState().value?.id == track.id,
                            onClick = {
                                // 播放逻辑
                                val currentMediaItems = playerConnection.player.mediaItems
                                val foundIndex = currentMediaItems.indexOfFirst { it.mediaId == track.id.toString() }

                                if (foundIndex != -1 && playerConnection.queueTitle.value == "每日推荐") {
                                    playerConnection.player.seekToDefaultPosition(foundIndex)
                                    playerConnection.player.play()
                                } else {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            id = "everyday",
                                            title = "每日推荐",
                                            items = songs.map { it.id.toString() },
                                            startIndex = index
                                        )
                                    )
                                }
                            }
                        )
                    }
                }

                // 4. 沉浸式 TopBar
                CenterAlignedTopAppBar(
                    title = {
                        AnimatedVisibility(visible = showTopBarTitle, enter = fadeIn(), exit = fadeOut()) {
                            Text("每日推荐", maxLines = 1, overflow = TextOverflow.Ellipsis)
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

            is Resource.Error -> {
                // 错误页需要适配 Scaffold 确保有返回按钮
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("每日推荐") },
                            navigationIcon = {
                                IconButton(onClick = { navController.navigateUp() }) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "加载失败: ${result.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Resource.Loading -> {
                // 加载骨架屏
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {},
                            navigationIcon = {
                                IconButton(onClick = { navController.navigateUp() }) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        PlaylistShimmer()
                    }
                }
            }
        }
    }
}