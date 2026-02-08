package com.ljyh.mei.ui.screen.artist

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.item.AlbumItem
import com.ljyh.mei.ui.component.item.Track
import com.ljyh.mei.ui.component.shimmer.ListItemPlaceHolder
import com.ljyh.mei.ui.component.shimmer.ShimmerHost
import com.ljyh.mei.ui.component.shimmer.TextPlaceholder
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.screen.Screen
import java.util.UUID


@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    id: String,
    viewModel: ArtistViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val artistDetail by viewModel.artistDetail.collectAsState()
    val artistAlbums by viewModel.artistAlbums.collectAsState()
    val artistSongs by viewModel.artistSongs.collectAsState()

    // 用于 TopBar 的滚动联动
    val scrollState = rememberLazyListState()
    val headerHeightPx = with(LocalDensity.current) { 320.dp.toPx() }

    val showTopBarTitle by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 ||
                    scrollState.firstVisibleItemScrollOffset > headerHeightPx * 0.6f
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()


    LaunchedEffect(id) {
        viewModel.getArtistDetail(id)
        viewModel.getArtistAlbums(id)
        viewModel.getArtistSongs(id)
    }

    val backIconBg =
        if (!showTopBarTitle) Color.Black.copy(alpha = 0.25f)
        else Color.Transparent

    val backIconTint =
        if (!showTopBarTitle) Color.White
        else MaterialTheme.colorScheme.onSurface


    val bgColor by animateColorAsState(
        targetValue = backIconBg,
        label = "backBg"
    )

    val iconTint by animateColorAsState(
        targetValue = backIconTint,
        label = "backTint"
    )


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showTopBarTitle,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val name =
                            (artistDetail as? Resource.Success)?.data?.data?.artist?.name ?: ""
                        Text(
                            name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(bgColor, CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = iconTint
                        )
                    }

                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            // 内容直接顶到最上方，让 TopBar 悬浮在 Header 上
            contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding()),
        ) {
            // --- 1. Header Section ---
            item {
                when (val detail = artistDetail) {
                    is Resource.Success -> {
                        ArtistHeader(
                            artist = detail.data.data.artist
                        )
                    }

                    is Resource.Loading -> ArtistHeaderShimmer()
                    is Resource.Error -> ErrorItem(detail.message)
                }
            }

            // --- 2. Hot Songs Section ---
            item { SectionTitle("Hot Songs") }

            when (val songsResource = artistSongs) {
                is Resource.Success -> {
                    val songs = songsResource.data.hotSongs
                    items(songs.take(10)) { song ->
                        Track(
                            track = song.toMediaMetadata(),
                            onClick = {
                                val allIds = songs.map {
                                    it.id.toString() to it.toMediaMetadata().toMediaItem()
                                }
                                playerConnection.onTrackClicked(
                                    trackId = song.id.toString(),
                                    buildQueue = {
                                        ListQueue(
                                            UUID.randomUUID().toString(),
                                            "Hot Songs",
                                            allIds,
                                            songs.indexOf(song)
                                        )
                                    }
                                )
                            },
                            onMoreClick = {}
                        )
                    }
                }

                is Resource.Loading -> {
                    items(5) { ShimmerHost { ListItemPlaceHolder() } }
                }

                is Resource.Error -> item { ErrorItem(songsResource.message) }
            }

            // --- 3. Albums Section ---
            item { SectionTitle("Albums") }

            when (val albumsResource = artistAlbums) {
                is Resource.Success -> {
                    val albums = albumsResource.data.hotAlbums
                    items(albums) { hotAlbum ->
                        AlbumItem(
                            album = Album(
                                id = hotAlbum.id.toLong(),
                                title = hotAlbum.name,
                                cover = hotAlbum.picUrl,
                                artist = hotAlbum.artists.map {
                                    Album.Artist(
                                        it.id.toLong(),
                                        it.name
                                    )
                                },
                                size = hotAlbum.size
                            ),
                            onClick = { navController.navigate("${Screen.Album.route}/${it}") }
                        )
                    }
                }

                is Resource.Loading -> {
                    items(3) { ShimmerHost { ListItemPlaceHolder() } }
                }

                is Resource.Error -> item { ErrorItem(albumsResource.message) }
            }
        }
    }
}

@Composable
fun ArtistHeader(artist: ArtistDetail.Data.Artist) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(320.dp)) {
        // 背景图：回归第一版清晰效果
        AsyncImage(
            model = artist.cover,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 渐变蒙层：确保底部文字可读，且平滑过渡到列表
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // 内容区：头像 + 名字 + 统计信息
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = artist.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .alpha(0.85f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 28.sp
                )
                Text(
                    text = "${artist.musicSize} songs • ${artist.albumSize} albums",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

            }
        }
    }
}

/**
 * Header 专用骨架屏：高度 320dp，位置 1:1 对齐
 */
@Composable
fun ArtistHeaderShimmer() {
    ShimmerHost {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 模拟 64dp 的圆头像
                Spacer(modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    TextPlaceholder(Modifier
                        .width(150.dp)
                        .height(24.dp))
                    TextPlaceholder(Modifier
                        .width(100.dp)
                        .height(16.dp))
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun ErrorItem(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(16.dp),
        color = MaterialTheme.colorScheme.error
    )
}