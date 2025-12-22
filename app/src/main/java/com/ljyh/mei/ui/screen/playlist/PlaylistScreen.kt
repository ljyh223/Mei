package com.ljyh.mei.ui.screen.playlist

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.paging.compose.collectAsLazyPagingItems
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.extensions.mediaItems
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.utils.rememberPreference

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
    val context = LocalContext.current

    val userId by rememberPreference(UserIdKey, "")
    val playlistDetail by viewModel.playlistDetail.collectAsState()
    val allMePlaylist by viewModel.playlist.collectAsState()
    val subscriberState by viewModel.subscribePlaylist.collectAsState()
    val unSubscriberState by viewModel.unSubscribePlaylist.collectAsState()


    val pagingFlow = remember(id, playlistDetail) {
        viewModel.getPlaylistTracks(playlistDetail)
    }
    val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()

    val isLoading = playlistDetail is Resource.Loading
    val playerConnection = LocalPlayerConnection.current ?: return
    val navController = LocalNavController.current


    var isSubscribed by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(subscriberState) {
        if (subscriberState is Resource.Error) {
            Toast.makeText(
                context,
                "收藏失败: ${(subscriberState as Resource.Error).message}",
                Toast.LENGTH_SHORT
            ).show()
            // 可选：如果失败了，把状态改回去
            isSubscribed = false
        }
    }

    LaunchedEffect(unSubscriberState) {
        if (unSubscriberState is Resource.Error) {
            Toast.makeText(
                context,
                "取消收藏失败: ${(unSubscriberState as Resource.Error).message}",
                Toast.LENGTH_SHORT
            ).show()
            isSubscribed = true
        }
    }
    val uiData = remember(playlistDetail, userId) {
        if (playlistDetail is Resource.Success) {
            val data = (playlistDetail as Resource.Success).data.playlist
            isSubscribed = data.subscribed
            data.tracks.map {
                Log.d("PlaylistScreen", it.ar.toString())
            }
            UiPlaylist(
                id = data.Id.toString(),
                title = data.name,
                count = data.trackCount,
                subscriberCount = data.subscribedCount,
                coverUrl = data.tracks.take(6).map { it.al.picUrl },
                creatorName = data.creator.nickname,
                isCreate = data.creator.userId.toString() == userId,
                description = data.description,
                tracks = data.tracks.filter { it.ar[0].Id != 0L && it.al.Id != 0L }
                    .map { it.toMediaMetadata() },
                trackCount = data.trackCount,
                playCount = data.playCount,
                isSubscribed = data.subscribed
            )
        } else {
            UiPlaylist(
                id = "", title = "", coverUrl = emptyList(), creatorName = "", tracks = emptyList(),
                count = 0,
                subscriberCount = 0,
                isCreate = false,
                description = "",
                trackCount = 0,
                playCount = 0,
                isSubscribed = false
            )
        }
    }

    if (playlistDetail is Resource.Error) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${(playlistDetail as Resource.Error).message}")
        }
    } else {
        CommonSongListScreen(
            uiData = uiData,
            pagingItems = lazyPagingItems,
            isLoading = isLoading,
            onPlayAll = {
                val playlist = (playlistDetail as Resource.Success).data.playlist
                val mediaItemsMap = playlist.tracks.associate {
                    it.id.toString() to it.toMediaMetadata().toMediaItem()
                }

                val allPairs = playlist.trackIds.map { trackId ->
                    val id = trackId.id.toString()
                    Pair(id, mediaItemsMap[id])
                }
                playerConnection.playQueue(
                    ListQueue(
                        id = "playlist_${uiData.id}",
                        title = uiData.title,
                        items = allPairs,
                        startIndex = 0
                    )
                )
            },

            headerActionIcon =
                if (isSubscribed) {
                    Icons.Default.Favorite
                } else {
                    Icons.Default.FavoriteBorder
                },
            headerActionLabel =
                if (isSubscribed) {
                    "取消收藏"
                } else {
                    "收藏"
                },
            onTrackClick = { mediaMetadata, index ->
                val currentMediaItems = playerConnection.player.mediaItems
                val foundIndex =
                    currentMediaItems.indexOfFirst { it.mediaId == mediaMetadata.id.toString() }

                if (foundIndex != -1) {
                    playerConnection.player.seekToDefaultPosition(foundIndex)
                    playerConnection.player.play()
                } else {
                    if (playlistDetail is Resource.Success) {
                        val playlist = (playlistDetail as Resource.Success).data.playlist
                        val mediaItemsMap = playlist.tracks.associate {
                            it.id.toString() to it.toMediaMetadata().toMediaItem()
                        }

                        val allPairs = playlist.trackIds.map { trackId ->
                            val id = trackId.id.toString()
                            Pair(id, mediaItemsMap[id])
                        }
                        playerConnection.playQueue(
                            ListQueue(
                                id = "playlist_${id}",
                                title = uiData.title,
                                items = allPairs,
                                startIndex = index
                            )
                        )
                    }
                }
            },
            onBack = {
                navController.popBackStack()
            },
            onHeaderAction = {
                if (isSubscribed) {
                    viewModel.unsubscribePlaylist(uiData.id)
                } else {
                    viewModel.subscribePlaylist(uiData.id)
                }
            }
        )
    }


}