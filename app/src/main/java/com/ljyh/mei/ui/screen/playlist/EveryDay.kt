package com.ljyh.mei.ui.screen.playlist

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.extensions.mediaItems
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.UiPlaylist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EveryDay(
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val everyDaySongs by viewModel.everyDay.collectAsState()
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current ?: return


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
                coverUrl = data.take(6).map { it.al.picUrl },
                creatorName = "网易云音乐",
                isCreate = false,
                description = "根据你的音乐口味生成，每天6:00更新",
                tracks = data.map { it.toMediaMetadata() },
                playCount = -1,
                isSubscribed = false
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

    if (everyDaySongs is Resource.Error) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${(everyDaySongs as Resource.Error).message}")
        }
    }else{
        CommonSongListScreen(
            uiData = uiData,
            pagingItems = null,
            isLoading = false,
            onPlayAll = {
                val allIds = (everyDaySongs as Resource.Success).data.data.dailySongs.map { it.id.toString() }
                playerConnection.playQueue(
                    ListQueue(
                        id = "playlist_${uiData.id}",
                        title = uiData.title,
                        items = allIds,
                        startIndex = 0
                    )
                )
            },

            headerActionIcon = Icons.Default.Favorite,
            headerActionLabel ="取消收藏",
            onTrackClick = { mediaMetadata, index ->
                val currentMediaItems = playerConnection.player.mediaItems
                val foundIndex =
                    currentMediaItems.indexOfFirst { it.mediaId == mediaMetadata.id.toString() }

                if (foundIndex != -1) {
                    playerConnection.player.seekToDefaultPosition(foundIndex)
                    playerConnection.player.play()
                } else {
                    if (everyDaySongs is Resource.Success) {
                        val allIds = (everyDaySongs as Resource.Success).data.data.dailySongs.map { it.id.toString() }
                        playerConnection.playQueue(
                            ListQueue(
                                id = "dailySongs",
                                title = uiData.title,
                                items = allIds,
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
                Toast.makeText(context, "无法操作", Toast.LENGTH_SHORT).show()
            }
        )
    }
}