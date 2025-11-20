package com.ljyh.mei.ui.screen.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.MiniAlbumDetail
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.model.toMiniAlbumDetail
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.extensions.mediaItems
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.playlist.Track
import com.ljyh.mei.ui.component.shimmer.ListItemPlaceHolder
import com.ljyh.mei.ui.component.shimmer.ShimmerHost
import com.ljyh.mei.ui.component.shimmer.TextPlaceholder
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.utils.TimeUtils.timestampToDate
import com.ljyh.mei.utils.smallImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    id: Long,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel= hiltViewModel()
) {
    // 请求数据
    LaunchedEffect(id) {
        viewModel.getAlbumDetail(id.toString())
    }

    val albumDetail by viewModel.albumDetail.collectAsState()


    val playerConnection = LocalPlayerConnection.current ?: return
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()

    val showTitle by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            state = listState
        ) {
            when (val res = albumDetail) {

                Resource.Loading -> {
                    item { AlbumShimmer() }
                }

                is Resource.Error -> {
                    item {
                        Text("加载失败: ${res.message}")
                    }
                }

                is Resource.Success -> {
                    val album = res.data

                    item {
                        AlbumInfo(
                            album = album.toMiniAlbumDetail(),
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
                                //viewModel.toggleCollect(album.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(album.songs.size) { i ->
                        val track = album.songs[i]
                        Track(
                            viewModel = playlistViewModel,
                            track = track.toMediaMetadata(),
                            isPlaying = playerConnection.mediaMetadata.collectAsState().value?.id == track.id
                        ) {
                            // 播放单曲
                            val player = playerConnection.player
                            val index = player.mediaItems.indexOfFirst {
                                it.mediaId == track.id.toString()
                            }
                            if (index != -1) {
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

        TopAppBar(
            title = {
                if (showTitle) Text("专辑详情")
            },
            navigationIcon = {
                IconButton(onClick = navController::navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null
                    )
                }
            },
            scrollBehavior = scrollBehavior
        )
    }
}


@Composable
fun AlbumInfo(
    album: MiniAlbumDetail,
    playAll: () -> Unit,
    onCollect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {

        AsyncImage(
            model = album.picUrl.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(170.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )

        Spacer(Modifier.height(4.dp))
        Text(
            text = "歌手: ${album.artist.joinToString(", ") { it.name }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        Text(
            text = "发行时间: ${timestampToDate(album.publishTime)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            Button(onClick = playAll) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
            }

            Button(onClick = onCollect) {
                Icon(
                    if (album.collected) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null
                )
            }
        }
    }
}


@Composable
fun AlbumShimmer() {
    ShimmerHost {
        Row(
            modifier = Modifier.padding(8.dp)
        ) {
            TextPlaceholder(
                Modifier
                    .size(144.dp)
                    .padding(end = 16.dp)
            )
            Column {
                repeat(3) { TextPlaceholder() }
            }
        }
        repeat(10) { ListItemPlaceHolder() }
    }
}
