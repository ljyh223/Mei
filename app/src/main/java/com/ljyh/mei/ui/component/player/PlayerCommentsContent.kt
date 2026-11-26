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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.ljyh.mei.data.model.api.CommentSortType
import com.ljyh.mei.ui.screen.comment.CommentViewModel
import com.ljyh.mei.ui.screen.comment.component.CommentItem

/** In-player comments panel shared by phone and tablet layouts. */
@Composable
fun PlayerCommentsContent(
    songId: String,
    onViewAllComments: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CommentViewModel = hiltViewModel(),
    foreground: Color = Color.White,
) {
    if (songId.toLongOrNull()?.let { it < 0 } == true) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("当前音源不提供评论", color = foreground)
        }
        return
    }
    val expandedReplies by viewModel.expandedReplies.collectAsState()
    val pagingItems = viewModel.pagingData.collectAsLazyPagingItems()

    LaunchedEffect(songId) {
        if (songId.isNotBlank()) {
            viewModel.setSortType(CommentSortType.HOT)
            viewModel.setSongId(songId)
        }
    }

    LazyColumn(modifier = modifier) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                IconButton(onClick = onViewAllComments) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "查看全部评论", tint = foreground)
                }
            }
        }

        when (val refresh = pagingItems.loadState.refresh) {
            is LoadState.Loading -> item {
                LoadingIndicator(foreground)
            }

            is LoadState.Error -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("评论加载失败", color = if (foreground == Color.White) Color(0xFFFFDAD6) else foreground)
                }
            }

            is LoadState.NotLoading -> {
                if (pagingItems.itemCount == 0) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("暂无评论", color = foreground.copy(alpha = 0.82f))
                        }
                    }
                }
                items(
                    count = pagingItems.itemCount,
                    key = { index -> pagingItems.peek(index)?.id ?: index },
                ) { index ->
                    val comment = pagingItems[index] ?: return@items
                    CommentItem(
                        comment = comment,
                        replies = expandedReplies[comment.id],
                        onToggleFloor = viewModel::toggleFloorComments,
                        onRetryFloor = viewModel::retryFloorComments,
                        onShowMoreFloor = viewModel::showMoreFloorComments,
                        horizontalPadding = 4.dp,
                        lightText = foreground == Color.White,
                    )
                }
                if (pagingItems.loadState.append is LoadState.Loading) {
                    item { LoadingIndicator(foreground) }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator(foreground: Color) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = foreground, strokeWidth = 2.dp)
    }
}
