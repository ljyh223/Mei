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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
                        AnimatedVisibility(
                            visible = !isLoading && showTopBarTitle,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = uiData.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                // 确保标题文字在深色背景下可见，或者跟随主题
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                // 确保图标在背景上可见，通常用 OnSurface 或者纯白
                                tint = MaterialTheme.colorScheme.onSurface
                            )
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
                            PlaylistTrackList(
                                modifier = Modifier.weight(0.6f),
                                pagingItems = pagingItems,
                                staticTracks = uiData.tracks,
                                isTablet = true,
                                onTrackClick = onTrackClick,
                                onMoreClick = { currentOverlay = OverlayState.TrackActionMenu(it) },
                                onTrackDownload = onTrackDownload
                            )
                        }
                    }else{
                        PlaylistTrackList(
                            pagingItems = pagingItems,
                            staticTracks = uiData.tracks,
                            isTablet = false,
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