package com.ljyh.mei.ui.screen.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.component.playlist.PlaylistBackground
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.ui.screen.playlist.component.PlaylistActionOverlay
import com.ljyh.mei.ui.screen.playlist.component.PlaylistHeader
import com.ljyh.mei.ui.screen.playlist.component.PlaylistShimmer
import com.ljyh.mei.ui.screen.playlist.component.PlaylistTrackList


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

    // 列表操作
    onTrackClick: (MediaMetadata, Int) -> Unit,
    onTrackDownload: ((MediaMetadata) -> Unit)? = null,
    onBack: () -> Unit,
    playlistSearchQuery: String = "",
    isPlaylistSearchActive: Boolean = false,
    onPlaylistSearchQueryChange: ((String) -> Unit)? = null,
    onPlaylistSearchActiveChange: (Boolean) -> Unit = {},
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


    LaunchedEffect(Unit) {
        if (uiData.title.endsWith("喜欢的音乐")) {
            viewModel.updateAllLike(uiData.tracks.map { Like(it.id.toString()) })
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (!isLoading && uiData.cover.isNotEmpty()) {
            PlaylistBackground(
                coverUrl = uiData.cover,
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        if (isPlaylistSearchActive && !(device.isTablet && device.isLandscape)) {
                            TextField(
                                value = playlistSearchQuery,
                                onValueChange = { onPlaylistSearchQueryChange?.invoke(it) },
                                modifier = Modifier.fillMaxSize(),
                                placeholder = { Text("搜索歌名、歌手或专辑") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            AnimatedVisibility(
                                visible = !isLoading &&
                                    showTopBarTitle &&
                                    !(device.isTablet && device.isLandscape),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Text(
                                    text = if (device.isTablet) {
                                        uiData.title
                                    } else {
                                        uiData.title.take(6).let { shortTitle ->
                                            if (shortTitle.length < uiData.title.length) "$shortTitle…" else shortTitle
                                        }
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isPlaylistSearchActive) onPlaylistSearchActiveChange(false) else onBack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                // 确保图标在背景上可见，通常用 OnSurface 或者纯白
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        if (supportsPlaylistSearch) {
                            IconButton(onClick = {
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                if (isLoading) {
                    PlaylistShimmer()
                } else {
                    if (device.isTablet && device.isLandscape) {
                        // --- 平板布局：左右并排 ---
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(0.4f).align(Alignment.CenterVertically)) {
                                PlaylistHeader(
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
                                    onSubscribed = {
                                        onHeaderAction()
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(0.6f)) {
                                if (isPlaylistSearchActive) {
                                    TextField(
                                        value = playlistSearchQuery,
                                        onValueChange = { onPlaylistSearchQueryChange?.invoke(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 12.dp),
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
                                    onMoreClick = { currentOverlay = OverlayState.TrackActionMenu(it) },
                                    onTrackDownload = onTrackDownload,
                                    emptyMessage = playlistSearchQuery.takeIf { it.isNotBlank() }
                                        ?.let { "未找到匹配的歌曲" }
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
                                PlaylistHeader(
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
                                    onSubscribed = { onHeaderAction() }
                                )
                            },
                            onTrackClick = onTrackClick,
                            onMoreClick = { currentOverlay = OverlayState.TrackActionMenu(it) },
                            onTrackDownload = onTrackDownload,
                            emptyMessage = playlistSearchQuery.takeIf { it.isNotBlank() }
                                ?.let { "未找到匹配的歌曲" },
                            // 手机端需要考虑底部播放器的高度
                            contentPadding = PaddingValues(
                                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
                            )
                        )
                    }
                }

            }

            PlaylistActionOverlay(
                overlay = currentOverlay,
                isCreator = uiData.isCreator,
                playlistId = uiData.id,
                allMePlaylist = allMePlaylist,
                onDismiss = { currentOverlay = OverlayState.None },
                onUpdateOverlay = { currentOverlay = it },
                onDownloadTrack = onTrackDownload,
                viewModel = viewModel
            )
        }


    }


}


@Composable
fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(contentColor = color)
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(28.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
    }
}
