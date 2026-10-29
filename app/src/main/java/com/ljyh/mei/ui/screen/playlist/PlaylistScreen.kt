package com.ljyh.mei.ui.screen.playlist

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import com.ljyh.mei.constants.DownloadPathKey
import com.ljyh.mei.constants.DownloadQuality
import com.ljyh.mei.constants.DownloadQualityKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.download.DownloadManager
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.ui.component.dialog.DownloadConfirmDialog
import com.ljyh.mei.ui.component.player.overlay.OverlayState
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.ui.model.headerStyle
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.playlist.component.playlistHeaderStyleCache
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference
import timber.log.Timber

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    id: Long,
    viewModel: PlaylistViewModel = hiltViewModel(),
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
    var loadingHeaderStyle by remember(id) { mutableStateOf(playlistHeaderStyleCache.get(id)) }
    LaunchedEffect(id, playlistDetail) {
        val detail = (playlistDetail as? Resource.Success)?.data
        if (detail != null && detail.id == id) {
            val style = detail.headerStyle()
            playlistHeaderStyleCache.put(id, style)
            loadingHeaderStyle = style
        }
    }
    val removedTrackIds by viewModel.removedTrackIds.collectAsState()
    val subscriberState by viewModel.subscribePlaylist.collectAsState()
    val unSubscriberState by viewModel.unSubscribePlaylist.collectAsState()

    // 3. Paging 数据
    val isPlaylistSearchActive by viewModel.searchActive.collectAsState()
    val playlistSearchQuery by viewModel.searchQuery.collectAsState()
    val orderState by viewModel.orderEditor.state.collectAsState()
    val orderItems = orderState.items
    val isPreparingOrder = orderState.loading
    val isSavingOrder = orderState.saving
    val allMePlaylist by viewModel.playlist.collectAsState()
    var overlay by remember(id) { mutableStateOf<OverlayState>(OverlayState.None) }
    val menu = rememberPlaylistMenuActions(viewModel, id)
    LaunchedEffect(viewModel) {
        viewModel.orderEditor.feedback.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    val pagingFlow = remember(id, playlistDetail, playlistSearchQuery, removedTrackIds) {
        viewModel.searchPlaylistTracks(playlistDetail, playlistSearchQuery, removedTrackIds)
    }
    val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()

    val downloadState by viewModel.downloads.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.downloads.feedback.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    val (downloadPath) = rememberPreference(DownloadPathKey, DownloadManager.getDefaultDownloadPath())
    val (downloadQuality) = rememberEnumPreference(DownloadQualityKey, DownloadQuality.EXHIGH)

    // 4. 管理收藏状态 (乐观更新核心)
    // 默认 false，等待数据加载后同步
    var isSubscribed by remember { mutableStateOf(false) }

    // 当网络数据(playlistDetail)加载成功时，同步初始状态
    LaunchedEffect(playlistDetail) {
        if (playlistDetail is Resource.Success) {
            isSubscribed = (playlistDetail as Resource.Success).data.subscribed
        }
    }

    LaunchedEffect(subscriberState) {
        when(val result=subscriberState){
            is Resource.Success ->{
                if(result.data.code!=200){
                    isSubscribed = false // 回滚为未收藏
                    Toast.makeText(context, "收藏失败: 错误码:${result.data.code}", Toast.LENGTH_SHORT).show()
                }else{
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
    val uiData = remember(playlistDetail, userId, removedTrackIds) {
        if (playlistDetail is Resource.Success) {
            val data = (playlistDetail as Resource.Success).data
            val visibleTrackCount = (data.count - removedTrackIds.size).coerceAtLeast(0)
            UiPlaylist(
                id = data.id,
                title = data.name,
                count = visibleTrackCount,
                subscriberCount = data.subscribedCount,
                cover = data.coverUrl.ifBlank { data.cover.firstOrNull().orEmpty() },
                coverList = data.cover,
                creatorName = data.createUserName,
                isCreator = data.creatorUserId.toString() == userId,
                description = data.description,
                tracks = data.tracks.filterNot { it.id in removedTrackIds },
                trackCount = visibleTrackCount,
                playCount = data.playCount,
                isSubscribed = data.subscribed,
                headerStyle = data.headerStyle(),
                updateTime = data.updateTime,
            )
        } else {
            UiPlaylist(
                id = 0L, title = "", cover = "", coverList = emptyList(), creatorName = "", tracks = emptyList(),
                count = 0, subscriberCount = 0, isCreator = false, description = "",
                trackCount = 0, playCount = 0, isSubscribed = false
            )
        }
    }

    val currentDetail = (playlistDetail as? Resource.Success)?.data
    LaunchedEffect(id, uiData.isCreator, currentDetail?.trackIds, removedTrackIds, isSavingOrder) {
        if (uiData.isCreator && currentDetail != null) {
            viewModel.orderEditor.prepare(id, currentDetail.trackIds.filterNot { it in removedTrackIds })
        }
    }

    // 6. 提取构建播放队列的逻辑 (避免重复代码)
    fun buildListQueue(startTrackId: Long? = null): ListQueue? {
        val detail = playlistDetail
        if (detail is Resource.Success) {
            val playlist = detail.data
            // 优化：在此处构建 map 可能会比较耗时，如果列表很大，建议放到 ViewModel 或 IO 线程处理
            // 但对于点击事件，直接处理通常也能接受
            val mediaItemsMap = (playlist.tracks + orderItems.orEmpty().mapNotNull(PlaylistOrderItem::track))
                .associate { it.id.toString() to it.toMediaItem() }
            // 保持原始顺序
            val allPairs = (orderItems?.map(PlaylistOrderItem::id) ?: playlist.trackIds)
                .filterNot { it in removedTrackIds }
                .mapNotNull { trackId ->
                val tid = trackId.toString()
                mediaItemsMap[tid]?.let { Pair(tid, it) }
            }

            return ListQueue(
                id = "playlist_${uiData.id}",
                title = uiData.title,
                source = QueueSource(QueueSource.Type.PLAYLIST, uiData.id.toString(), uiData.title),
                items = allPairs,
                startIndex = startTrackId
                    ?.let { trackId -> allPairs.indexOfFirst { it.first == trackId.toString() } }
                    ?.takeIf { it >= 0 }
                    ?: 0
            )
        }
        return null
    }

    fun handleDownload() {
        val detail = currentDetail ?: return
        val selection = PlaylistDownloadSelection(id, detail.name, detail.trackIds.filterNot { it in removedTrackIds })
        viewModel.downloads.begin(selection, detail.tracks)
    }

    fun handleTrackDownload(track: MediaMetadata) {
        viewModel.downloads.begin(PlaylistDownloadSelection(id, uiData.title, listOf(track.id)), listOf(track))
    }

    if (downloadState.phase == PlaylistDownloadPhase.Confirming) {
        DownloadConfirmDialog(
            currentQuality = downloadQuality,
            downloadPath = downloadPath,
            recoveryNote = if (downloadState.uncertainSubmission)
                "上次下载提交结果尚未确认，请先查看下载管理，避免重复提交。" else null,
            onDismiss = viewModel.downloads::dismiss,
            onConfirm = { separateLyrics ->
                viewModel.downloads.confirm(PlaylistDownloadOptions(downloadQuality, downloadPath, separateLyrics))
            },
            onGoToSettings = {
                viewModel.downloads.dismiss()
                Screen.DownloadSettings.navigate(navController)
            },
            onGoToDownloadManage = {
                viewModel.downloads.dismiss()
                Screen.DownloadManage.navigate(navController)
            },
        )
    }

    // 8. UI 渲染
    if (playlistDetail is Resource.Error) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${(playlistDetail as Resource.Error).message}")
        }
    } else {
        PlaylistContent(
            state = PlaylistUiState(
                uiData = uiData.copy(isSubscribed = isSubscribed),
                playlistId = id,
                trackIds = currentDetail?.trackIds.orEmpty().filterNot { it in removedTrackIds },
                playlists = allMePlaylist,
                overlay = overlay,
                pagingItems = lazyPagingItems,
                isLoading = playlistDetail is Resource.Loading,
                loadingHeaderStyle = loadingHeaderStyle,
                headerActionIcon = if (isSubscribed) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                headerActionLabel = if (isSubscribed) "取消收藏" else "收藏",
                playlistSearchQuery = playlistSearchQuery,
                isPlaylistSearchActive = isPlaylistSearchActive,
                reorderItems = orderItems.takeIf {
                    uiData.isCreator && !isPreparingOrder
                },
                reorderSaving = isSavingOrder,
            ),
            actions = PlaylistActions(
                onOverlayChange = { overlay = it },
                menu = menu,
                onPlayAll = {
                    buildListQueue()?.let { queue ->
                        playerConnection.playQueue(queue)
                        viewModel.recordPlaylistPlayback(id.toString())
                    }
                },
                onHeaderAction = headerAction@{
                    if(uiData.isCreator){
                        Toast.makeText(context, "不能收藏自己创建的歌单", Toast.LENGTH_SHORT).show()
                        return@headerAction
                    }
                    val newState = !isSubscribed
                    isSubscribed = newState
                    if (newState) {
                        viewModel.subscribePlaylist(uiData.id.toString())
                    } else {
                        viewModel.unsubscribePlaylist(uiData.id.toString())
                    }
                },
                onDownload = { handleDownload() },
                onTrackClick = { mediaMetadata, index ->
                    playerConnection.onTrackClicked(
                        trackId = mediaMetadata.id.toString(),
                        buildQueue = { buildListQueue(mediaMetadata.id) },
                        fallbackSource = QueueSource(QueueSource.Type.PLAYLIST, uiData.id.toString(), uiData.title)
                    )
                    viewModel.recordPlaylistPlayback(id.toString())
                },
                onTrackDownload = { track -> handleTrackDownload(track) },
                onBack = { navController.popBackStack() },
                onPlaylistSearchQueryChange = viewModel::setSearchQuery,
                onPlaylistSearchActiveChange = viewModel::setSearchActive,
                onMoveTrack = viewModel.orderEditor::move,
                onReorderFinished = viewModel.orderEditor::finish,
            ),
        )
    }
}
