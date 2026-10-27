package com.ljyh.mei.ui.component.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.model.api.CommentSortType
import com.ljyh.mei.ui.screen.comment.CommentViewModel
import com.ljyh.mei.ui.screen.comment.component.CommentItem

/** Compact, in-player comments panel for the tablet layout. */
@Composable
fun PlayerCommentsContent(
    songId: String,
    onViewAllComments: () -> Unit,
    modifier: Modifier = Modifier,
    onControlsVisibilityChange: (Boolean) -> Unit = {},
    viewModel: CommentViewModel = hiltViewModel(),
) {
    val expandedCommentId by viewModel.expandedCommentId.collectAsState()
    val floorComments by viewModel.floorComments.collectAsState()
    val floorHasMore by viewModel.floorHasMore.collectAsState()
    val floorLoadMoreError by viewModel.floorLoadMoreError.collectAsState()
    val floorWaitingForReveal by viewModel.floorWaitingForReveal.collectAsState()
    val pagingItems = viewModel.pagingData.collectAsLazyPagingItems()
    val currentOnControlsVisibilityChange by rememberUpdatedState(onControlsVisibilityChange)
    val scrollThreshold = with(LocalDensity.current) { 10.dp.toPx() }
    val scrollConnection = remember(scrollThreshold) {
        object : NestedScrollConnection {
            private var accumulatedScroll = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val delta = available.y
                if (accumulatedScroll * delta < 0f) accumulatedScroll = 0f
                accumulatedScroll += delta
                if (kotlin.math.abs(accumulatedScroll) >= scrollThreshold) {
                    currentOnControlsVisibilityChange(accumulatedScroll > 0f)
                    accumulatedScroll = 0f
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(songId) {
        if (songId.isNotBlank()) {
            viewModel.setSortType(CommentSortType.HOT)
            viewModel.setSongId(songId)
        }
    }

    LazyColumn(modifier = modifier.nestedScroll(scrollConnection)) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                IconButton(onClick = onViewAllComments) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "查看全部评论", tint = Color.White)
                }
            }
        }

        when (val refresh = pagingItems.loadState.refresh) {
            is LoadState.Loading -> item {
                LoadingIndicator()
            }

            is LoadState.Error -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("评论加载失败", color = Color(0xFFFFDAD6))
                }
            }

            is LoadState.NotLoading -> {
                if (pagingItems.itemCount == 0) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("暂无评论", color = Color.White.copy(alpha = 0.82f))
                        }
                    }
                }
                items(
                    count = pagingItems.itemCount,
                    key = { index -> pagingItems.peek(index)?.id ?: index },
                ) { index ->
                    val comment = pagingItems[index] ?: return@items
                    val isExpanded = expandedCommentId == comment.id
                    CommentItem(
                        comment = comment,
                        isExpanded = isExpanded,
                        floorComments = if (isExpanded) floorComments else Resource.Loading,
                        onToggleFloor = viewModel::toggleFloorComments,
                        onShowMoreFloor = viewModel::showMoreFloorComments,
                        floorHasMore = isExpanded && floorHasMore,
                        floorLoadMoreError = floorLoadMoreError,
                        floorWaitingForReveal = floorWaitingForReveal,
                        horizontalPadding = 4.dp,
                        lightText = true,
                    )
                }
                if (pagingItems.loadState.append is LoadState.Loading) {
                    item { LoadingIndicator() }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
    }
}
