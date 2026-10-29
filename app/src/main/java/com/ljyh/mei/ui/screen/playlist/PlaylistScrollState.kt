package com.ljyh.mei.ui.screen.playlist

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.paging.LoadState
import kotlinx.coroutines.flow.first

internal const val PlaylistHeaderKey = "playlist_detail_header"
internal const val PlaylistToolsKey = "playlist_track_tools"

internal data class PlaylistScrollAnchor(
    val songId: Long? = null,
    val songIndex: Int = 0,
    val offset: Int = 0,
    val header: Boolean = true,
)

internal val PlaylistScrollAnchorSaver = listSaver<PlaylistScrollAnchor, Any>(
    save = { listOf(it.songId ?: -1L, it.songIndex, it.offset, it.header) },
    restore = { PlaylistScrollAnchor((it[0] as Long).takeIf { id -> id != -1L }, it[1] as Int, it[2] as Int, it[3] as Boolean) },
)

internal sealed interface PlaylistAnchorTarget {
    data object Wait : PlaylistAnchorTarget
    data object LoadMore : PlaylistAnchorTarget
    data class Item(val index: Int, val offset: Int) : PlaylistAnchorTarget
}

/** ID first; ordinal is only a fallback when the song no longer belongs to the displayed set. */
internal fun resolvePlaylistAnchor(
    anchor: PlaylistScrollAnchor,
    ids: List<Long>,
    header: Boolean,
    loading: Boolean,
    hasMore: Boolean,
    allIds: List<Long>? = null,
    leadingItemCount: Int = if (header) 1 else 0,
): PlaylistAnchorTarget {
    if (loading) return PlaylistAnchorTarget.Wait
    if (anchor.header) return PlaylistAnchorTarget.Item(0, if (header) anchor.offset else 0)
    val index = ids.indexOf(anchor.songId)
    val headerOffset = leadingItemCount
    if (index >= 0) return PlaylistAnchorTarget.Item(index + headerOffset, anchor.offset)
    val removed = allIds != null && anchor.songId !in allIds
    if (removed && allIds!!.isNotEmpty()) {
        val fallback = allIds[anchor.songIndex.coerceIn(0, allIds.lastIndex)]
        val fallbackIndex = ids.indexOf(fallback)
        if (fallbackIndex >= 0) return PlaylistAnchorTarget.Item(fallbackIndex + headerOffset, 0)
        if (hasMore) return PlaylistAnchorTarget.LoadMore
    }
    if (!removed && hasMore) return PlaylistAnchorTarget.LoadMore
    if (ids.isEmpty()) return PlaylistAnchorTarget.Item(0, 0)
    return PlaylistAnchorTarget.Item(anchor.songIndex.coerceIn(0, ids.lastIndex) + headerOffset, 0)
}

/** Ignore structural placeholders and keep a header anchor through an untouched two-pane view. */
internal fun capturePlaylistAnchor(
    previous: PlaylistScrollAnchor,
    key: Any?,
    index: Int,
    offset: Int,
    splitPane: Boolean,
    userScrolling: Boolean,
    leadingItemCount: Int = if (splitPane) 0 else 1,
): PlaylistScrollAnchor? = when (key) {
    PlaylistHeaderKey -> PlaylistScrollAnchor(offset = offset)
    PlaylistToolsKey -> if (previous.header && splitPane && !userScrolling) null else PlaylistScrollAnchor()
    is Long -> if (previous.header && splitPane && index == leadingItemCount && offset == 0 && !userScrolling) null
        else PlaylistScrollAnchor(key, (index - leadingItemCount).coerceAtLeast(0), offset, false)
    else -> null
}

/** Saved semantic anchor survives recreation; the raw layout index is deliberately not saved. */
@Composable
internal fun rememberPlaylistScrollState(state: PlaylistUiState, splitPane: Boolean): LazyListState {
    val query = state.playlistSearchQuery
    val saved = rememberSaveable(state.playlistId, query, stateSaver = PlaylistScrollAnchorSaver) {
        mutableStateOf(PlaylistScrollAnchor())
    }
    val list = remember(state.playlistId, query) { LazyListState() }
    val current by rememberUpdatedState(state)
    val currentSplit by rememberUpdatedState(splitPane)
    var settledLayout by remember(state.playlistId, query) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(list, splitPane) {
        settledLayout = null
        val anchor = saved.value
        var target: PlaylistAnchorTarget.Item? = null
        snapshotFlow {
            val paging = current.pagingItems
            val ordered = current.reorderItems.takeIf { !current.isPlaylistSearchActive }
            val ids = ordered?.map(PlaylistOrderItem::id)
                ?: paging?.itemSnapshotList?.items?.map { it.id }
                ?: current.uiData.tracks.map { it.id }
            val loading = current.isLoading ||
                (ordered == null && paging != null && paging.loadState.refresh !is LoadState.NotLoading)
            val hasMore = ordered == null && paging != null && !paging.loadState.append.endOfPaginationReached
            val completeIds = ordered?.map(PlaylistOrderItem::id)
                ?: current.trackIds.takeIf { current.playlistSearchQuery.isBlank() }
            Triple(resolvePlaylistAnchor(anchor, ids, !splitPane, loading, hasMore, completeIds, leadingItemCount = 1),
                list.isScrollInProgress, list.layoutInfo.totalItemsCount)
        }.first { (resolution, userScrolling, _) ->
            if (userScrolling) true else when (resolution) {
                PlaylistAnchorTarget.Wait -> false
                PlaylistAnchorTarget.LoadMore -> {
                    current.pagingItems?.let { paging ->
                        if (paging.itemCount > 0 && paging.loadState.append !is LoadState.Error) paging[paging.itemCount - 1]
                    }
                    false
                }
                is PlaylistAnchorTarget.Item -> { target = resolution; true }
            }
        }
        target?.let {
            withFrameNanos { }
            list.scrollToItem(it.index, it.offset)
            withFrameNanos { }
        }
        settledLayout = splitPane
    }
    LaunchedEffect(list) {
        snapshotFlow {
            // Visible items also include rows behind the toolbar inside content padding. Match
            // the saved index/offset to its own key, not to an earlier row in that sampled area.
            val first = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == list.firstVisibleItemIndex }
            if (settledLayout != currentSplit || current.isLoading || first == null) null
            else capturePlaylistAnchor(saved.value, first.key, list.firstVisibleItemIndex,
                list.firstVisibleItemScrollOffset, currentSplit, list.isScrollInProgress, leadingItemCount = 1)
        }.collect { anchor -> if (anchor != null) saved.value = anchor }
    }
    return list
}
