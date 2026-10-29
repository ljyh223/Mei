package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.playlist.PlaylistLayoutScope

/** Keep paging, filtering, reordering and menu behavior identical across the two layouts. */
@Composable
internal fun PlaylistTracks(
    content: PlaylistLayoutScope,
    modifier: Modifier = Modifier,
    tableRows: Boolean = false,
    headerContent: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    scrollingControls: (@Composable () -> Unit)? = null,
) {
    val state = content.state
    val actions = content.actions
    PlaylistTrackList(
        modifier = modifier,
        pagingItems = state.pagingItems,
        staticTracks = state.uiData.tracks,
        tableRows = tableRows,
        headerContent = headerContent,
        scrollingControls = scrollingControls,
        lazyListState = content.listState,
        onTrackClick = actions.onTrackClick,
        onMoreClick = content.onTrackMore,
        onTrackDownload = actions.onTrackDownload,
        reorderItems = state.reorderItems.takeIf { !state.isPlaylistSearchActive },
        reorderSaving = state.reorderSaving,
        onMoveTrack = actions.onMoveTrack,
        onReorderFinished = actions.onReorderFinished,
        emptyMessage = state.playlistSearchQuery.takeIf { it.isNotBlank() }?.let { "未找到匹配的歌曲" },
        contentPadding = contentPadding ?: PaddingValues(bottom =
            (LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() -
                content.padding.calculateBottomPadding()).coerceAtLeast(0.dp)),
        isLoading = state.isLoading,
        loadingItemCount = if (tableRows) 10 else 8,
    )
}
