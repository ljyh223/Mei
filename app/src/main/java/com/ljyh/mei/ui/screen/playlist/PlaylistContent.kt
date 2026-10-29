package com.ljyh.mei.ui.screen.playlist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.ui.component.glass.MeiHazeTopBar
import com.ljyh.mei.ui.component.menu.MeiDetailMoreButton
import com.ljyh.mei.ui.component.player.overlay.OverlayState
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.model.PlaylistHeaderStyle
import com.ljyh.mei.ui.screen.playlist.component.PlaylistActionMenu
import com.ljyh.mei.ui.screen.playlist.component.PlaylistBackground
import com.ljyh.mei.ui.screen.playlist.component.PlaylistDetailHeader
import com.ljyh.mei.ui.screen.playlist.layout.PlaylistLayout
import com.ljyh.mei.ui.screen.playlist.layout.SinglePanePlaylistLayout
import com.ljyh.mei.ui.screen.playlist.layout.TwoPanePlaylistLayout
import com.ljyh.mei.ui.screen.playlist.layout.playlistLayoutMetrics
import com.ljyh.mei.utils.share.MediaShareLink
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Width-adaptive presentation with no navigation, player-service or ViewModel dependency. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistContent(
    state: PlaylistUiState,
    actions: PlaylistActions,
    customHeader: (@Composable () -> Unit)? = null,
) {
    val uiData = state.uiData
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val metrics = playlistLayoutMetrics(maxWidth.value, LocalDensity.current.fontScale)
        val splitPane = metrics.layout == PlaylistLayout.TwoPane
        val listState = rememberPlaylistScrollState(state, splitPane)
        val searchInTopBar = !splitPane
        val showTitle by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
        val collapseDistance = with(LocalDensity.current) { 56.dp.toPx() }
        val backgroundProgress by remember(listState, collapseDistance) {
            derivedStateOf {
                if (listState.firstVisibleItemIndex > 0) 1f
                else (listState.firstVisibleItemScrollOffset / collapseDistance).coerceIn(0f, 1f)
            }
        }
        val titleProgress by animateFloatAsState(
            targetValue = if (state.isPlaylistSearchActive || showTitle) 1f else 0f,
            label = "playlistTopBarTitle",
        )
        val pageHaze = rememberHazeState()
        Scaffold(
            containerColor = if (uiData.cover.isBlank()) MaterialTheme.colorScheme.surface else Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                MeiHazeTopBar(
                    state = pageHaze,
                    title = uiData.title,
                    progress = titleProgress,
                    backgroundProgress = if (state.isPlaylistSearchActive) 1f else backgroundProgress,
                    onNavigateBack = {
                        if (state.isPlaylistSearchActive) actions.onPlaylistSearchActiveChange(false)
                        else actions.onBack()
                    },
                    actions = {
                        if (!state.isLoading) {
                            MeiDetailMoreButton(
                                shareContent = MediaShareLink.playlist(uiData.id, uiData.title, uiData.creatorName),
                                playlistCoverUrl = uiData.cover,
                                searchActive = state.isPlaylistSearchActive,
                                searchLabel = "搜索歌单",
                                onSearchActiveChange = actions.onPlaylistSearchQueryChange?.let { changeQuery ->
                                    { active ->
                                        if (!active) changeQuery("")
                                        actions.onPlaylistSearchActiveChange(active)
                                    }
                                },
                            )
                        }
                    },
                    titleContent = if (state.isPlaylistSearchActive && searchInTopBar) {
                        {
                            TextField(
                                value = state.playlistSearchQuery,
                                onValueChange = { actions.onPlaylistSearchQueryChange?.invoke(it) },
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
            },
        ) { padding ->
            SkeletonShimmerHost(
                enabled = state.isLoading,
                modifier = Modifier.fillMaxSize().hazeSource(pageHaze).background(MaterialTheme.colorScheme.surface),
            ) {
                if (!state.isLoading && uiData.cover.isNotEmpty()) {
                    PlaylistBackground(
                        coverUrl = uiData.cover,
                        emphasized = uiData.headerStyle != PlaylistHeaderStyle.Standard,
                    )
                }
                val layout = PlaylistLayoutScope(
                    state = state,
                    actions = actions,
                    listState = listState,
                    padding = padding,
                    detailPaneWidth = metrics.detailWidthDp.dp,
                    onTrackMore = { track, anchor -> actions.onOverlayChange(OverlayState.TrackActionMenu(track, anchor)) },
                    header = { compact ->
                        if (customHeader != null) customHeader() else PlaylistDetailHeader(
                            playlist = uiData,
                            isLoading = state.isLoading,
                            loadingHeaderStyle = state.loadingHeaderStyle,
                            onPlayAll = actions.onPlayAll,
                            onHeaderAction = actions.onHeaderAction,
                            onDownload = actions.onDownload,
                            actionIcon = state.headerActionIcon,
                            actionLabel = state.headerActionLabel,
                            compact = compact,
                        )
                    },
                )
                if (splitPane) TwoPanePlaylistLayout(layout) else SinglePanePlaylistLayout(layout)
            }
            PlaylistActionMenu(
                overlay = state.overlay,
                isCreator = uiData.isCreator,
                queueSource = if (uiData.id == -1L) QueueSource(QueueSource.Type.DAILY, "dailySongs", uiData.title)
                    else QueueSource(QueueSource.Type.PLAYLIST, uiData.id.toString(), uiData.title),
                allMePlaylist = state.playlists,
                onDismiss = { actions.onOverlayChange(OverlayState.None) },
                onUpdateOverlay = actions.onOverlayChange,
                onDownloadTrack = actions.onTrackDownload,
                actions = actions.menu,
            )
        }
    }
}
