package com.ljyh.mei.ui.screen.album

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.ui.screen.playlist.CommonSongListScreen

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    id: Long,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    // 1. 请求初始数据
    LaunchedEffect(id) {
        viewModel.getAlbumDetail(id.toString())
        viewModel.isSubscribe(id)
    }

    val context = LocalContext.current
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val albumDetail by viewModel.albumDetail.collectAsState()
    val subscribeState by viewModel.subscribeAlbum.collectAsState()
    val unSubscribeState by viewModel.unSubscribeAlbum.collectAsState()
    val isSubscribeState by viewModel.isSubscribe.collectAsState()


    // 3. 本地收藏状态 (乐观更新核心)
    var isSubscribed by remember { mutableStateOf(false) }

    // 处理收藏失败的回滚逻辑
    LaunchedEffect(subscribeState) {
        if (subscribeState is Resource.Error) {
            isSubscribed = false // 回滚
            Toast.makeText(context, "收藏失败: ${(subscribeState as Resource.Error).message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(unSubscribeState) {
        when(val result= unSubscribeState){
            is Resource.Success->{
                isSubscribed = false // 回滚
                viewModel.deleteAlbum(id)
                Toast.makeText(context, "取消收藏成功", Toast.LENGTH_SHORT).show()
            }
            is Resource.Error->{
                isSubscribed = true // 回滚
                Toast.makeText(context, "取消收藏失败: ${result.message}", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
        if (unSubscribeState is Resource.Error) {

        }
    }

    LaunchedEffect(isSubscribeState) {
        isSubscribed=isSubscribeState
    }

    // 4. 构建 UI 数据模型
    val uiData = remember(albumDetail) {
        if (albumDetail is Resource.Success) {
            val album = (albumDetail as Resource.Success).data.album
            val songs = (albumDetail as Resource.Success).data.songs
            UiPlaylist(
                id = album.id,
                title = album.name,
                count = album.size,
                subscriberCount = -1, // 专辑通常没有订阅人数，或者在 dynamicInfo 中
                cover = album.picUrl,
                coverList = listOf(album.picUrl),
                creatorName = album.artists.joinToString(", ") { it.name },
                isCreator = false,
                description = album.description,
                tracks = songs.map { it.toMediaMetadata().copy(coverUrl = album.picUrl) },
                playCount = -1,
                isSubscribed = isSubscribeState
            )
        } else {
            UiPlaylist(
                id = 0L, title = "", cover = "", coverList = emptyList(), creatorName = "", tracks = emptyList(),
                count = 0, subscriberCount = 0, isCreator = false, description = "",
                trackCount = 0, playCount = 0, isSubscribed = false
            )
        }
    }

    // 5. 提取播放队列构建逻辑 (避免重复)
    fun buildListQueue(startIndex: Int = 0): ListQueue? {
        val detail = albumDetail
        if (detail is Resource.Success) {
            val items = detail.data.songs.map { song ->
                Pair(
                    song.id.toString(),
                    song.toMediaMetadata().copy(coverUrl = detail.data.album.picUrl).toMediaItem()
                )
            }
            return ListQueue(
                id = "album_${uiData.id}",
                title = uiData.title,
                items = items,
                startIndex = startIndex
            )
        }
        return null
    }

    // 6. UI 渲染
    if (albumDetail is Resource.Error) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${(albumDetail as Resource.Error).message}")
        }
    } else {
        CommonSongListScreen(
            uiData = uiData,
            pagingItems = null, // 专辑通常一次性加载，不需要 paging
            isLoading = albumDetail is Resource.Loading,

            // 头部按钮逻辑
            headerActionIcon = if (isSubscribed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            headerActionLabel = if (isSubscribed) "取消收藏" else "收藏",
            onHeaderAction = {
                // 乐观更新
                val newState = !isSubscribed
                isSubscribed = newState

                // 发起请求
                if (newState) {
                    viewModel.subscribeAlbum(uiData.id.toString())
                } else {
                    viewModel.unSubscribeAlbum(uiData.id.toString())
                }
            },

            // 播放全部
            onPlayAll = {
                buildListQueue(0)?.let { queue ->
                    playerConnection.playQueue(queue)
                }
            },

            // 点击单曲
            onTrackClick = { mediaMetadata, index ->
                playerConnection.onTrackClicked(
                    trackId = mediaMetadata.id.toString(),
                    buildQueue = { buildListQueue(index) }
                )
            },

            onBack = { navController.popBackStack() }
        )
    }
}