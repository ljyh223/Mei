package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ljyh.mei.constants.PlaylistTrackTableHeaderKey
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.item.Track
import com.ljyh.mei.ui.component.item.TrackPlaceholder
import com.ljyh.mei.ui.screen.playlist.PlaylistOrderItem
import com.ljyh.mei.ui.screen.playlist.displayTrack
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.utils.preferences.rememberPreference
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PlaylistTrackList(
    modifier: Modifier = Modifier,
    pagingItems: LazyPagingItems<MediaMetadata>? = null,
    staticTracks: List<MediaMetadata> = emptyList(),
    isTablet: Boolean = false,
    headerContent: (@Composable () -> Unit)? = null, // 新增：可选的头部内容
    onTrackClick: (MediaMetadata, Int) -> Unit,
    onMoreClick: (MediaMetadata) -> Unit,
    reorderItems: List<PlaylistOrderItem>? = null,
    reorderSaving: Boolean = false,
    onMoveTrack: (Int, Int) -> Unit = { _, _ -> },
    onReorderFinished: () -> Unit = {},
    onTrackDownload: ((MediaMetadata) -> Unit)? = null,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    emptyMessage: String? = null,
    isLoading: Boolean = false,
    loadingItemCount: Int = 8,
) {

    val playlistTrackTableHeader by rememberPreference(PlaylistTrackTableHeaderKey,  false)
    val currentOnMove by rememberUpdatedState(onMoveTrack)
    val currentReorderItems by rememberUpdatedState(reorderItems)
    val currentSaving by rememberUpdatedState(reorderSaving)
    val haptics = LocalHapticFeedback.current
    val headerOffset = if (headerContent == null) 0 else 1
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = from.index - headerOffset
        val toIndex = to.index - headerOffset
        val items = currentReorderItems
        if (!currentSaving && items != null && fromIndex in items.indices && toIndex in items.indices) {
            currentOnMove(fromIndex, toIndex)
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        if (isTablet && playlistTrackTableHeader) {
            TrackTableHeader(isLoading = isLoading)
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f), // 占据剩余空间
            contentPadding = contentPadding
        ) {
            if (headerContent != null) {
                item {
                    headerContent()
                }
            }
            // 如果是平板，可以在这里加一个 StickyHeader 作为“表头”

            if (isLoading) {
                items(
                    count = loadingItemCount,
                    key = { "playlist_track_placeholder_$it" },
                ) { index ->
                    TrackPlaceholder(index = index, isTablet = isTablet)
                }
            } else if (reorderItems != null) {
                itemsIndexed(reorderItems, key = { _, item -> item.id }) { index, item ->
                    ReorderableItem(reorderState, key = item.id) { _ ->
                        Track(
                            track = item.displayTrack,
                            index = index,
                            isTablet = isTablet,
                            onClick = { if (item.track != null) onTrackClick(item.track, index) },
                            onMoreClick = { if (item.track != null) onMoreClick(item.track) },
                            moreButtonModifier = if (reorderSaving) Modifier else
                                Modifier.longPressDraggableHandle(
                                    onDragStarted = {
                                        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    },
                                    onDragStopped = {
                                        haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                        onReorderFinished()
                                    },
                                ).semantics {
                                    customActions = buildList {
                                        if (index > 0) add(CustomAccessibilityAction("上移") {
                                            onMoveTrack(index, index - 1)
                                            onReorderFinished()
                                            true
                                        })
                                        if (index < reorderItems.lastIndex) add(CustomAccessibilityAction("下移") {
                                            onMoveTrack(index, index + 1)
                                            onReorderFinished()
                                            true
                                        })
                                    }
                                },
                            moreContentDescription = "更多，长按拖动调整顺序",
                        )
                    }
                }
            } else if (pagingItems != null) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.id },
                    contentType = pagingItems.itemContentType { "Track" }
                ) { index ->
                    val track = pagingItems[index]
                    if (track != null) {
                        Track(
                            track = track,
                            index = index,
                            isTablet = isTablet,
                            onClick = { onTrackClick(track, index) },
                            onMoreClick = { onMoreClick(track) }
                        )
                    }
                }

                when (pagingItems.loadState.append) {
                    is LoadState.Loading -> {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            }
                        }
                    }

                    is LoadState.Error -> {
                        item { Text("加载更多失败，点击重试") }
                    }

                    else -> {}
                }

                if (
                    emptyMessage != null &&
                    pagingItems.itemCount == 0 &&
                    pagingItems.loadState.refresh is LoadState.NotLoading
                ) {
                    item {
                        Text(
                            text = emptyMessage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(staticTracks, key = { _, item -> item.id }) { index, track ->
                    Track(
                        track = track,
                        index = index,
                        isTablet = isTablet,
                        onClick = { onTrackClick(track, index) },
                        onMoreClick = { onMoreClick(track) }
                    )
                }
                if (emptyMessage != null && staticTracks.isEmpty()) {
                    item {
                        Text(
                            text = emptyMessage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

}


@Composable
fun TrackTableHeader(isLoading: Boolean = false) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", Modifier.width(36.dp).skeleton(isLoading), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // 这里的 paddingStart 必须和 Track 里的封面宽度 + 间距对齐
            // 40.dp (封面) + 16.dp (间距) = 56.dp
            Text("标题", Modifier.weight(4f).padding(start = 56.dp).skeleton(isLoading),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("专辑", Modifier.weight(3f).padding(horizontal = 8.dp).skeleton(isLoading),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("时长", Modifier.width(60.dp).skeleton(isLoading), textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.width(40.dp))
        }
        // 加一条极细的分割线，让层次感出来
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
    }
}
