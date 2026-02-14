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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ljyh.mei.constants.PlaylistTrackTableHeaderKey
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.ui.component.item.Track
import com.ljyh.mei.utils.rememberPreference

@Composable
fun PlaylistTrackList(
    modifier: Modifier = Modifier,
    pagingItems: LazyPagingItems<MediaMetadata>? = null,
    staticTracks: List<MediaMetadata> = emptyList(),
    isTablet: Boolean = false,
    headerContent: (@Composable () -> Unit)? = null, // 新增：可选的头部内容
    onTrackClick: (MediaMetadata, Int) -> Unit,
    onMoreClick: (MediaMetadata) -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {

    val playlistTrackTableHeader by rememberPreference(PlaylistTrackTableHeaderKey,  false)

    Column(modifier = modifier.fillMaxSize()) {

        if (isTablet && playlistTrackTableHeader) {
            TrackTableHeader()
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

            if (pagingItems != null) {
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
            }
        }
    }

}


@Composable
fun TrackTableHeader() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", Modifier.width(36.dp), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // 这里的 paddingStart 必须和 Track 里的封面宽度 + 间距对齐
            // 40.dp (封面) + 16.dp (间距) = 56.dp
            Text("标题", Modifier.weight(4f).padding(start = 56.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("专辑", Modifier.weight(3f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("时长", Modifier.width(60.dp), textAlign = TextAlign.End,
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