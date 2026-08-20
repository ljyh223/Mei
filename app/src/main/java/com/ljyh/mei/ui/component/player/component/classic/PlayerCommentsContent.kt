package com.ljyh.mei.ui.component.player.component.classic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.screen.comment.CommentViewModel
import com.ljyh.mei.ui.screen.comment.component.CommentItem

/** Compact, in-player comments panel for the tablet layout. */
@Composable
fun PlayerCommentsContent(
    songId: String,
    onViewAllComments: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CommentViewModel = hiltViewModel(),
) {
    val total by viewModel.total.collectAsState()
    val expandedCommentId by viewModel.expandedCommentId.collectAsState()
    val floorComments by viewModel.floorComments.collectAsState()
    val pagingItems = viewModel.pagingData.collectAsLazyPagingItems()

    LaunchedEffect(songId) {
        if (songId.isNotBlank()) viewModel.setSongId(songId)
    }

    LazyColumn(modifier = modifier) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (total > 0) "评论 $total" else "评论",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onViewAllComments) {
                    Text("查看全部")
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
                    Text("评论加载失败", color = MaterialTheme.colorScheme.error)
                }
            }

            is LoadState.NotLoading -> {
                if (pagingItems.itemCount == 0) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("暂无评论", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(
                    count = pagingItems.itemCount,
                    key = { index -> pagingItems.peek(index)?.commentId ?: index },
                ) { index ->
                    val comment = pagingItems[index] ?: return@items
                    val isExpanded = expandedCommentId == comment.commentId
                    CommentItem(
                        comment = comment,
                        isExpanded = isExpanded,
                        floorComments = if (isExpanded) floorComments else Resource.Loading,
                        onToggleFloor = viewModel::toggleFloorComments,
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
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}
