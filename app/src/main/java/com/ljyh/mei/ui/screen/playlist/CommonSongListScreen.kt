package com.ljyh.mei.ui.screen.playlist

import com.ljyh.mei.playback.queue.QueueSource

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.ljyh.mei.ui.component.glass.MeiHazeTopBar
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.player.overlay.OverlayState
import com.ljyh.mei.ui.screen.playlist.component.PlaylistBackground
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.ui.screen.playlist.component.PlaylistActionOverlay
import com.ljyh.mei.ui.screen.playlist.component.PlaylistHeader
import com.ljyh.mei.ui.screen.playlist.component.PlaylistTrackList
import com.ljyh.mei.ui.component.glass.MeiGlassIconButton
import com.ljyh.mei.ui.component.layout.meiPageGutter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonSongListScreen(
    uiData: UiPlaylist,
    pagingItems: LazyPagingItems<MediaMetadata>? = null,
    isLoading: Boolean,
    // 头部操作
    onPlayAll: () -> Unit,
    onHeaderAction: () -> Unit,
    onDownload: () -> Unit = {},
    headerActionIcon: ImageVector,
    headerActionLabel: String,
    customHeader: (@Composable () -> Unit)? = null,

    // 列表操作
    onTrackClick: (MediaMetadata, Int) -> Unit,
    onTrackDownload: ((MediaMetadata) -> Unit)? = null,
    onBack: () -> Unit,
    playlistSearchQuery: String = "",
    isPlaylistSearchActive: Boolean = false,
    onPlaylistSearchQueryChange: ((String) -> Unit)? = null,
    onPlaylistSearchActiveChange: (Boolean) -> Unit = {},
    reorderItems: List<PlaylistOrderItem>? = null,
    reorderSaving: Boolean = false,
    onMoveTrack: (Int, Int) -> Unit = { _, _ -> },
    onReorderFinished: () -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val device = rememberDeviceInfo()

    var menuTargetTrack by remember { mutableStateOf<MediaMetadata?>(null) }


    val allMePlaylist by viewModel.playlist.collectAsState()
    var currentOverlay by remember { mutableStateOf<OverlayState>(OverlayState.None) }

    val showTopBarTitle by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }
    val supportsPlaylistSearch = onPlaylistSearchQueryChange != null
    val layoutDirection = LocalLayoutDirection.current
    val collapseDistance = with(LocalDensity.current) { 56.dp.toPx() }
    val backgroundProgress by remember(lazyListState, collapseDistance) {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) 1f
            else (lazyListState.firstVisibleItemScrollOffset / collapseDistance).coerceIn(0f, 1f)
        }
    }
    val titleProgress by animateFloatAsState(
        targetValue = if (isPlaylistSearchActive || showTopBarTitle) 1f else 0f,
        label = "playlistTopBarTitle",
    )


    Box(modifier = Modifier.fillMaxSize()) {

        val pageHaze = rememberHazeState()
        Scaffold(
            containerColor = if (uiData.cover.isBlank()) MaterialTheme.colorScheme.surface else Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                MeiHazeTopBar(
                    state = pageHaze,
                    title = uiData.title,
                    progress = titleProgress,
                    backgroundProgress = if (isPlaylistSearchActive) 1f else backgroundProgress,
                    onNavigateBack = {
                        if (isPlaylistSearchActive) onPlaylistSearchActiveChange(false) else onBack()
                    },
                    actions = {
                        if (supportsPlaylistSearch && !isLoading) {
                            MeiGlassIconButton(onClick = {
                                if (isPlaylistSearchActive) {
                                    onPlaylistSearchQueryChange("")
                                    onPlaylistSearchActiveChange(false)
                                } else {
                                    onPlaylistSearchActiveChange(true)
                                }
                            }) {
                                Icon(
                                    imageVector = if (isPlaylistSearchActive) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = if (isPlaylistSearchActive) "关闭歌单搜索" else "搜索歌单"
                                )
                            }
                        }
                    },
                    titleContent = if (isPlaylistSearchActive && !(device.isTablet && device.isLandscape)) {
                        {
                            TextField(
                                value = playlistSearchQuery,
                                onValueChange = { onPlaylistSearchQueryChange?.invoke(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("搜索歌名、歌手或专辑") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                            )
                        }
                    } else null,
                )
            }
        ) { paddingValues ->
            SkeletonShimmerHost(
                enabled = isLoading,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(pageHaze)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                    if (!isLoading && uiData.cover.isNotEmpty()) {
                        PlaylistBackground(coverUrl = uiData.cover)
                    }
                    if (device.isTablet && device.isLandscape) {
                        // --- 平板布局：左右并排 ---
                        Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            Box(modifier = Modifier.weight(0.4f).align(Alignment.CenterVertically)) {
                                if (customHeader != null) customHeader() else PlaylistHeader(
                                    title = uiData.title,
                                    cover = uiData.cover,
                                    coverList = uiData.coverList,
                                    creator = uiData.creatorName,
                                    onPlayAll = onPlayAll,
                                    onDownload = onDownload,
                                    actionIcon = headerActionIcon,
                                    actionLabel = headerActionLabel,
                                    count = uiData.count,
                                    playCount = uiData.playCount ?: 0L,
                                    subscribeCount = uiData.subscriberCount,
                                    isSubscribed = uiData.isSubscribed,
                                    isLoading = isLoading,
                                    onSubscribed = {
                                        onHeaderAction()
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(0.6f)) {
                                if (!isLoading && isPlaylistSearchActive) {
                                    TextField(
                                        value = playlistSearchQuery,
                                        onValueChange = { onPlaylistSearchQueryChange?.invoke(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = meiPageGutter(), vertical = 12.dp),
                                        placeholder = { Text("搜索歌名、歌手或专辑") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Search, contentDescription = null)
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(28.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        )
                                    )
                                }
                                PlaylistTrackList(
                                    modifier = Modifier.weight(1f),
                                    pagingItems = pagingItems,
                                    staticTracks = uiData.tracks,
                                    isTablet = true,
                                    lazyListState = lazyListState,
                                    onTrackClick = onTrackClick,
                                    onMoreClick = { track, anchor -> currentOverlay = OverlayState.TrackActionMenu(track, anchor) },
                                    onTrackDownload = onTrackDownload,
                                    reorderItems = reorderItems.takeIf { !isPlaylistSearchActive },
                                    reorderSaving = reorderSaving,
                                    onMoveTrack = onMoveTrack,
                                    onReorderFinished = onReorderFinished,
                                    emptyMessage = playlistSearchQuery.takeIf { it.isNotBlank() }
                                        ?.let { "未找到匹配的歌曲" },
                                    isLoading = isLoading,
                                    loadingItemCount = 10,
                                )
                            }
                        }
                    }else{
                        PlaylistTrackList(
                            pagingItems = pagingItems,
                            staticTracks = uiData.tracks,
                            isTablet = false,
                            lazyListState = lazyListState,
                            // 关键：把 Header 作为参数传进去
                            headerContent = {
                                if (customHeader != null) customHeader() else PlaylistHeader(
                                    title = uiData.title,
                                    cover = uiData.cover,
                                    coverList = uiData.coverList,
                                    creator = uiData.creatorName,
                                    onPlayAll = onPlayAll,
                                    onDownload = onDownload,
                                    actionIcon = headerActionIcon,
                                    actionLabel = headerActionLabel,
                                    count = uiData.count,
                                    playCount = uiData.playCount ?: 0L,
                                    subscribeCount = uiData.subscriberCount,
                                    isSubscribed = uiData.isSubscribed,
                                    isLoading = isLoading,
                                    onSubscribed = { onHeaderAction() }
                                )
                            },
                            onTrackClick = onTrackClick,
                            onMoreClick = { track, anchor -> currentOverlay = OverlayState.TrackActionMenu(track, anchor) },
                            onTrackDownload = onTrackDownload,
                            reorderItems = reorderItems.takeIf { !isPlaylistSearchActive },
                            reorderSaving = reorderSaving,
                            onMoveTrack = onMoveTrack,
                            onReorderFinished = onReorderFinished,
                            emptyMessage = playlistSearchQuery.takeIf { it.isNotBlank() }
                                ?.let { "未找到匹配的歌曲" },
                            // 手机端需要考虑底部播放器的高度
                            contentPadding = PaddingValues(
                                start = paddingValues.calculateStartPadding(layoutDirection),
                                end = paddingValues.calculateEndPadding(layoutDirection),
                                top = paddingValues.calculateTopPadding(),
                                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                            ),
                            isLoading = isLoading,
                        )
                    }
            }

            PlaylistActionOverlay(
                overlay = currentOverlay,
                isCreator = uiData.isCreator,
                playlistId = uiData.id,
                queueSource = if (uiData.id == -1L) QueueSource(QueueSource.Type.DAILY, "dailySongs", uiData.title)
                    else QueueSource(QueueSource.Type.PLAYLIST, uiData.id.toString(), uiData.title),
                allMePlaylist = allMePlaylist,
                onDismiss = { currentOverlay = OverlayState.None },
                onUpdateOverlay = { currentOverlay = it },
                onDownloadTrack = onTrackDownload,
                viewModel = viewModel
            )
        }


    }


}
