package com.ljyh.mei.ui.screen.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.MiniPlaylistDetail
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.Track
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.utils.rearrangeArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EveryDay(
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val everyDaySongs by viewModel.everyDay.collectAsState()
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(true) {
        viewModel.getEveryDayRecommendSongs()
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            Modifier.padding(horizontal = 16.dp),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            state = lazyListState,
        ) {
            when (val result = everyDaySongs) {
                is Resource.Error -> {
                    item {
                        Text(text = "Error: ${result.message}")
                    }
                }

                Resource.Loading -> {
                    item {
                        PlaylistShimmer()
                    }
                }

                is Resource.Success -> {
                    item {
                        PlaylistInfo(
                            playlistDetail = MiniPlaylistDetail(
                                id = result.data.data.dailySongs[0].id,
                                name = "每日推荐",
                                cover = result.data.data.dailySongs.take(5).map { it.al.picUrl },
                                description = "",
                                tracks = result.data.data.dailySongs.map { it.toMediaMetadata() },
                                trackIds = result.data.data.dailySongs.map { it.id },
                                creatorUserId = result.data.data.dailySongs[0].ar[0].id,
                                createUserName = result.data.data.dailySongs[0].ar[0].name,
                                count = result.data.data.dailySongs.size,
                                playCount = 0,
                                subscribedCount = 0
                            ), viewModel = viewModel
                        ) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = "每日推荐",
                                    items = result.data.data.dailySongs.map { it.id.toString() }
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(result.data.data.dailySongs.size) { index ->
                        val track = result.data.data.dailySongs[index]
                        Track(viewModel = viewModel, track = track.toMediaMetadata()) {
                            val trackIds = result.data.data.dailySongs.map { it.id.toString() }
                            playerConnection.playQueue(
                                ListQueue(
                                    title = "每日推荐",
                                    items = rearrangeArray(index, trackIds)
                                )
                            )
                        }
                    }
                }
            }
        }

        TopAppBar(
            title = { Text("每日推荐") },
            navigationIcon = {
                IconButton(onClick = navController::navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            scrollBehavior = scrollBehavior
        )
    }
}