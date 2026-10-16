package com.ljyh.mei.ui.screen.playlist

import com.ljyh.mei.playback.queue.QueueSource

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.ui.screen.playlist.component.DailyRecommendationHeader

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EveryDay(
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val everyDaySongs by viewModel.everyDay.collectAsState()
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current ?: return
    var isDailySearchActive by remember { mutableStateOf(false) }
    var dailySearchQuery by remember { mutableStateOf("") }

    val isLoading = everyDaySongs is Resource.Loading
    LaunchedEffect(true) {
        viewModel.getEveryDayRecommendSongs()
    }

    val uiData = remember(everyDaySongs) {
        if (everyDaySongs is Resource.Success) {
            val data = (everyDaySongs as Resource.Success).data
            UiPlaylist(
                id = -1L,
                title = "每日推荐",
                count = data.size,
                subscriberCount = 0,
                cover = "",
                coverList = emptyList(),
                creatorName = "",
                isCreator = false,
                description = null,
                tracks = data,
                isSubscribed = false
            )
        } else {
            UiPlaylist(
                id = 0L, title = "", cover = "", coverList = emptyList(), creatorName = "", tracks = emptyList(),
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
    val displayedUiData = remember(uiData, dailySearchQuery) {
        if (dailySearchQuery.isBlank()) uiData else uiData.copy(
            tracks = uiData.tracks.filter { it.matchesPlaylistSearch(dailySearchQuery) }
        )
    }
    val allSongs = (everyDaySongs as? Resource.Success)?.data.orEmpty()
    val playAll: () -> Unit = {
        if (allSongs.isNotEmpty()) {
            playerConnection.playQueue(
                ListQueue(
                    id = "dailySongs",
                    title = uiData.title,
                    source = QueueSource(QueueSource.Type.DAILY, "dailySongs", uiData.title),
                    items = allSongs.map { it.id.toString() to it.toMediaItem() },
                    startIndex = 0,
                )
            )
        }
    }

    if (everyDaySongs is Resource.Error) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${(everyDaySongs as Resource.Error).message}")
        }
    } else {
        CommonSongListScreen(
            uiData = displayedUiData,
            pagingItems = null,
            isLoading = isLoading,
            onPlayAll = playAll,
            customHeader = {
                DailyRecommendationHeader(tracks = uiData.tracks, isLoading = isLoading, onPlayAll = playAll)
            },
            headerActionIcon = Icons.Default.FavoriteBorder,
            headerActionLabel = "收藏",
            onTrackClick = { mediaMetadata, index ->

                playerConnection.onTrackClicked(
                    trackId = mediaMetadata.id.toString(),
                    fallbackSource = QueueSource(QueueSource.Type.DAILY, "dailySongs", uiData.title),
                    buildQueue = {
                        if (everyDaySongs is Resource.Success) {
                            val allIds =
                                (everyDaySongs as Resource.Success).data.map {
                                    it.id.toString() to it.toMediaItem()
                                }
                            ListQueue(
                                id = "dailySongs",
                                title = uiData.title,
                                source = QueueSource(QueueSource.Type.DAILY, "dailySongs", uiData.title),
                                items = allIds,
                                startIndex = uiData.tracks.indexOfFirst { it.id == mediaMetadata.id }
                                    .takeIf { it >= 0 } ?: index
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
            onHeaderAction = {},
            playlistSearchQuery = dailySearchQuery,
            isPlaylistSearchActive = isDailySearchActive,
            onPlaylistSearchQueryChange = { dailySearchQuery = it },
            onPlaylistSearchActiveChange = { active ->
                isDailySearchActive = active
                if (!active) dailySearchQuery = ""
            }
        )
    }
}
