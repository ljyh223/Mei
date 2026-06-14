package com.ljyh.mei.ui.screen.comment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.comment.component.CommentItem
import com.ljyh.mei.ui.screen.comment.component.CommentTopBar
import androidx.compose.foundation.layout.asPaddingValues
import timber.log.Timber
import java.util.Timer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    songId: String,
    viewModel: CommentViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    val sortType by viewModel.sortType.collectAsState()
    val total by viewModel.total.collectAsState()
    val expandedCommentId by viewModel.expandedCommentId.collectAsState()
    val floorComments by viewModel.floorComments.collectAsState()

    val pagingItems = viewModel.pagingData.collectAsLazyPagingItems()

    LaunchedEffect(songId) {
        viewModel.setSongId(songId)
    }

    val insets = LocalPlayerAwareWindowInsets.current

    Scaffold(
        topBar = {
            CommentTopBar(
                total = total,
                sortType = sortType,
                onSortTypeChange = { viewModel.setSortType(it) },
                onBack = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        when (val refreshState = pagingItems.loadState.refresh) {
            is LoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LoadState.Error -> {
                Timber.tag("CommentScreen").d(refreshState.error)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "加载失败",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is LoadState.NotLoading -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        bottom = insets.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    items(
                        count = pagingItems.itemCount,
                        key = { index -> pagingItems.peek(index)?.commentId ?: index }
                    ) { index ->
                        val comment = pagingItems[index] ?: return@items
                        val isExpanded = expandedCommentId == comment.commentId

                        CommentItem(
                            comment = comment,
                            isExpanded = isExpanded,
                            floorComments = if (isExpanded) floorComments
                            else com.ljyh.mei.data.network.Resource.Loading,
                            onToggleFloor = { commentId, count ->
                                viewModel.toggleFloorComments(commentId, count)
                            }
                        )
                    }

                    if (pagingItems.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}