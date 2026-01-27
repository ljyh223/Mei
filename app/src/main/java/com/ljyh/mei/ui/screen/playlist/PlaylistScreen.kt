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
import com.ljyh.mei.data.model.PlaylistDetail
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.utils.rememberPreference
import timber.log.Timber

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    id: Long,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    // 1. 初始化数据请求
    LaunchedEffect(key1 = id) {
        viewModel.getPlaylistDetail(id.toString())
    }

    val context = LocalContext.current
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current ?: return

    // 2. 状态收集
    val userId by rememberPreference(UserIdKey, "")
    val playlistDetail by viewModel.playlistDetail.collectAsState()
    val subscriberState by viewModel.subscribePlaylist.collectAsState()
    val unSubscriberState by viewModel.unSubscribePlaylist.collectAsState()

    // 3. Paging 数据
    val pagingFlow = remember(id, playlistDetail) {
        viewModel.getPlaylistTracks(playlistDetail)
    }
    val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()

    // 4. 管理收藏状态 (乐观更新核心)
    // 默认 false，等待数据加载后同步
    var isSubscribed by remember { mutableStateOf(false) }

    // 当网络数据(playlistDetail)加载成功时，同步初始状态
    LaunchedEffect(playlistDetail) {
        if (playlistDetail is Resource.Success) {
            isSubscribed = (playlistDetail as Resource.Success).data.playlist.subscribed
        }
    }

    LaunchedEffect(subscriberState) {
        when(val result=subscriberState){
            is Resource.Success ->{
                if(result.data.code!=200){
                    isSubscribed = false // 回滚为未收藏
                    Toast.makeText(context, "收藏失败: 错误码:${result.data.code}", Toast.LENGTH_SHORT).show()
                }else{
                    viewModel
                    Toast.makeText(context, "收藏成功", Toast.LENGTH_SHORT).show()

                }
                Timber.tag("PlaylistScreen").d(result.data.toString())
            }
            is Resource.Error->{
                isSubscribed = false // 回滚为未收藏
                Toast.makeText(context, "收藏失败: ${(subscriberState as Resource.Error).message}", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    LaunchedEffect(unSubscriberState) {
        when(val result=unSubscriberState){
            is Resource.Success ->{
                Timber.tag("PlaylistScreen").d(result.data.toString())
                if(result.data.code!=200){
                    isSubscribed = false // 回滚为未收藏
                    Toast.makeText(context, "取消收藏失败: 错误码:${result.data.code}", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(context, "取消收藏成功", Toast.LENGTH_SHORT).show()
                }
            }
            is Resource.Error->{
                isSubscribed = false // 回滚为未收藏
                Toast.makeText(context, "取消收藏失败: ${result.message}", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    // 5. 构建 UI 模型 (移除副作用和内部状态修改)
    val uiData = remember(playlistDetail, userId) {
        if (playlistDetail is Resource.Success) {
            val data = (playlistDetail as Resource.Success).data.playlist
            UiPlaylist(
                id = data.Id.toString(),
                title = data.name,
                count = data.trackCount,
                subscriberCount = data.subscribedCount,
                coverUrl = data.tracks.take(6).map { it.al.picUrl },
                creatorName = data.creator.nickname,
                isCreator = data.creator.userId.toString() == userId,
                description = data.description,
                tracks = data.tracks.map { it.toMediaMetadata() },
                trackCount = data.trackCount,
                playCount = data.playCount,
                isSubscribed = data.subscribed // 注意：这里仅用于 UI 初始化，后续由 isSubscribed 状态变量控制
            )
        } else {
            UiPlaylist(
                id = "", title = "", coverUrl = emptyList(), creatorName = "", tracks = emptyList(),
                count = 0, subscriberCount = 0, isCreator = false, description = "",
                trackCount = 0, playCount = 0, isSubscribed = false
            )
        }
    }

    // 6. 提取构建播放队列的逻辑 (避免重复代码)
    fun buildListQueue(index: Int = 0): ListQueue? {
        val detail = playlistDetail
        if (detail is Resource.Success) {
            val playlist = detail.data.playlist
            // 优化：在此处构建 map 可能会比较耗时，如果列表很大，建议放到 ViewModel 或 IO 线程处理
            // 但对于点击事件，直接处理通常也能接受
            val mediaItemsMap = playlist.tracks.associate {
                it.id.toString() to it.toMediaMetadata().toMediaItem()
            }
            // 保持原始顺序
            val allPairs = playlist.trackIds.mapNotNull { trackId ->
                val tid = trackId.id.toString()
                mediaItemsMap[tid]?.let { Pair(tid, it) }
            }

            return ListQueue(
                id = "playlist_${uiData.id}",
                title = uiData.title,
                items = allPairs,
                startIndex = index
            )
        }
        return null
    }

    // 7. UI 渲染
    if (playlistDetail is Resource.Error) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${(playlistDetail as Resource.Error).message}")
        }
    } else {
        CommonSongListScreen(
            uiData = uiData,
            pagingItems = lazyPagingItems,
            isLoading = playlistDetail is Resource.Loading,

            // 收藏按钮逻辑
            headerActionIcon = if (isSubscribed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            headerActionLabel = if (isSubscribed) "取消收藏" else "收藏",
            onHeaderAction = {
                if(uiData.isCreator){
                    Toast.makeText(context, "不能收藏自己创建的歌单", Toast.LENGTH_SHORT).show()
                    return@CommonSongListScreen
                }
                // 乐观更新：立即改变 UI 状态
                val newState = !isSubscribed
                isSubscribed = newState

                // 发起网络请求
                if (newState) {
                    viewModel.subscribePlaylist(uiData.id)
                } else {
                    viewModel.unsubscribePlaylist(uiData.id)
                }
            },

            // 播放全部
            onPlayAll = {
                buildListQueue(0)?.let { queue ->
                    playerConnection.playQueue(queue)
                }
            },

            // 点击单曲播放
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