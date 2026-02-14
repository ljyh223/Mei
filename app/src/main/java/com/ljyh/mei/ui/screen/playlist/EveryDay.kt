package com.ljyh.mei.ui.screen.playlist

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.extensions.mediaItems
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.UiPlaylist

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EveryDay(
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val everyDaySongs by viewModel.everyDay.collectAsState()
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isLoading = everyDaySongs is Resource.Loading
    LaunchedEffect(true) {
        viewModel.getEveryDayRecommendSongs()
    }

    val uiData = remember(everyDaySongs) {
        if (everyDaySongs is Resource.Success) {
            val data = (everyDaySongs as Resource.Success).data.data.dailySongs
            UiPlaylist(
                id = "-1",
                title = "每日推荐",
                count = data.size,
                subscriberCount = -1,
                cover = data[0].al.picUrl,
                coverList = data.take(6).map { it.al.picUrl },
                creatorName = "网易云音乐",
                isCreator = false,
                description = "根据你的音乐口味生成，每天6:00更新",
                tracks = data.map { it.toMediaMetadata() },
                playCount = -1,
                isSubscribed = false
            )
        } else {
            UiPlaylist(
                id = "", title = "", cover = "", coverList = emptyList(), creatorName = "", tracks = emptyList(),
                count = 0,
                subscriberCount = 0,
                isCreator = false,
                description = "",
                trackCount = 0,
                playCount = 0,
                isSubscribed = false
            )
        }
    }

    if (everyDaySongs is Resource.Error) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${(everyDaySongs as Resource.Error).message}")
        }
    } else {
        CommonSongListScreen(
            uiData = uiData,
            pagingItems = null,
            isLoading = isLoading,
            onPlayAll = {
                val allIds = (everyDaySongs as Resource.Success).data.data.dailySongs.map {
                    it.id.toString() to it.toMediaMetadata().toMediaItem()
                }
                playerConnection.playQueue(
                    ListQueue(
                        id = "playlist_${uiData.id}",
                        title = uiData.title,
                        items = allIds,
                        startIndex = 0
                    )
                )
            },
            headerActionIcon = Icons.Default.FavoriteBorder,
            headerActionLabel = "收藏",
            onTrackClick = { mediaMetadata, index ->

                playerConnection.onTrackClicked(
                    trackId = mediaMetadata.id.toString(),
                    buildQueue = {
                        if (everyDaySongs is Resource.Success) {
                            val allIds =
                                (everyDaySongs as Resource.Success).data.data.dailySongs.map {
                                    it.id.toString() to it.toMediaMetadata().toMediaItem()
                                }
                            ListQueue(
                                id = "dailySongs",
                                title = uiData.title,
                                items = allIds,
                                startIndex = index
                            )
                        } else {
                            null
                        }
                    }
                )
            },
            onBack = {
                navController.popBackStack()
            },
            onHeaderAction = {
                Toast.makeText(context, "不能收藏每日推荐歌单", Toast.LENGTH_SHORT).show()
            }
        )
    }
}