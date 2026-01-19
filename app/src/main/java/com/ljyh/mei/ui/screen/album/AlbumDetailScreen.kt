package com.ljyh.mei.ui.screen.album

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.extensions.mediaItems
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.ui.screen.playlist.CommonSongListScreen
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel

@androidx.annotation.OptIn(UnstableApi::class)
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
    val context = LocalContext.current

    val albumDetail by viewModel.albumDetail.collectAsState()
    val allMePlaylist by playlistViewModel.playlist.collectAsState()

    val playerConnection = LocalPlayerConnection.current ?: return
    val navController = LocalNavController.current

    val isLoading = albumDetail is Resource.Loading


    val uiData = remember(albumDetail) {
        if (albumDetail is Resource.Success) {
            val album = (albumDetail as Resource.Success).data.album
            val songs = (albumDetail as Resource.Success).data.songs
            UiPlaylist(
                id = album.id.toString(),
                title = album.name,
                count = album.size,
                subscriberCount = -1,
                coverUrl = listOf(album.picUrl),
                creatorName = album.artists.joinToString(", ") { it.name },
                isCreate = false,
                description = album.description,
                tracks = songs.map { it.toMediaMetadata().copy(coverUrl = album.picUrl) },
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

    if (albumDetail is Resource.Error) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${(albumDetail as Resource.Error).message}")
        }
    } else {
        CommonSongListScreen(
            uiData = uiData,
            pagingItems = null,
            isLoading = isLoading,
            onPlayAll = {
                val allIds = (albumDetail as Resource.Success).data.songs.map {
                    Pair(
                        it.id.toString(),
                        it.toMediaMetadata().toMediaItem()
                    )
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

            headerActionIcon = Icons.Default.Favorite,
            headerActionLabel = "取消收藏",
            onTrackClick = { mediaMetadata, index ->
                playerConnection.onTrackClicked(
                    trackId = mediaMetadata.id.toString(),
                    buildQueue = {
                        if (albumDetail is Resource.Success) {
                            val allIds = (albumDetail as Resource.Success).data.songs.map {
                                Pair(
                                    it.id.toString(),
                                    it.toMediaMetadata().toMediaItem()
                                )
                            }
                            ListQueue(
                                id = "playlist_${uiData.id}",
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
                Toast.makeText(context, "暂未实现", Toast.LENGTH_SHORT).show()
            }
        )
    }
}