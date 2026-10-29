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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.ui.component.glass.MeiHazeTopBar
import com.ljyh.mei.ui.component.glass.meiArtworkTopBarBackground
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
    customHeader: (@Composable (titleModifier: Modifier) -> Unit)? = null,
) {
    val uiData = state.uiData
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val metrics = playlistLayoutMetrics(maxWidth.value, LocalDensity.current.fontScale)
        val splitPane = metrics.layout == PlaylistLayout.TwoPane
        val listState = rememberPlaylistScrollState(state, splitPane)
        val searchInTopBar = !splitPane
        val titleBottom = remember(state.playlistId, splitPane, uiData.title, state.isLoading) {
            mutableStateOf<Float?>(null)
        }
        var toolbarBottom by remember { mutableFloatStateOf(0f) }
        val showTitle by remember(listState, splitPane, titleBottom) {
            derivedStateOf {
                splitPane || listState.firstVisibleItemIndex > 0 ||
                    (toolbarBottom > 0f && titleBottom.value?.let { it <= toolbarBottom } == true)
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
                    backgroundProgress = 1f,
                    backgroundMode = meiArtworkTopBarBackground(),
                    centerTitle = !splitPane,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        toolbarBottom = coordinates.positionInRoot().y + coordinates.size.height
                    },
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
                        val titleModifier = if (splitPane) Modifier else Modifier.onGloballyPositioned { coordinates ->
                            titleBottom.value = coordinates.positionInRoot().y + coordinates.size.height
                        }
                        if (customHeader != null) customHeader(titleModifier) else PlaylistDetailHeader(
                            playlist = uiData,
                            isLoading = state.isLoading,
                            loadingHeaderStyle = state.loadingHeaderStyle,
                            onPlayAll = actions.onPlayAll,
                            onHeaderAction = actions.onHeaderAction,
                            onDownload = actions.onDownload,
                            actionIcon = state.headerActionIcon,
                            actionLabel = state.headerActionLabel,
                            compact = compact,
                            titleModifier = titleModifier,
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
