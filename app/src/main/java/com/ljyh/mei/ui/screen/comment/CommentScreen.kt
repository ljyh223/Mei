package com.ljyh.mei.ui.screen.comment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.glass.meiTopBarContentClearance
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.comment.component.CommentItem
import com.ljyh.mei.ui.screen.comment.component.CommentTopBar
import androidx.compose.foundation.layout.asPaddingValues
import timber.log.Timber
import androidx.compose.foundation.background
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    songId: String,
    viewModel: CommentViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    val pageHaze = rememberHazeState()
    val sortType by viewModel.sortType.collectAsState()
    val total by viewModel.total.collectAsState()
    val expandedReplies by viewModel.expandedReplies.collectAsState()

    val pagingItems = viewModel.pagingData.collectAsLazyPagingItems()

    LaunchedEffect(songId) {
        viewModel.setSongId(songId)
    }

    val insets = LocalPlayerAwareWindowInsets.current
    val layoutDirection = LocalLayoutDirection.current
    val topBarClearance = meiTopBarContentClearance()
    val showCollapseFab = expandedReplies.values.any { it.replyCount > 10 && it.comments is Resource.Success }

    Scaffold(
        topBar = {
            CommentTopBar(
                total = total,
                hazeState = pageHaze,
                sortType = sortType,
                onSortTypeChange = { viewModel.setSortType(it) },
                onBack = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showCollapseFab,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                FloatingActionButton(
                    onClick = viewModel::collapseAllFloorComments,
                    modifier = Modifier.padding(bottom = insets.asPaddingValues().calculateBottomPadding()),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "收起全部回复",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        when (val refreshState = pagingItems.loadState.refresh) {
            is LoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(pageHaze)
                        .background(MaterialTheme.colorScheme.surface)
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
                        .hazeSource(pageHaze)
                        .background(MaterialTheme.colorScheme.surface)
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
                        .hazeSource(pageHaze)
                        .background(MaterialTheme.colorScheme.surface),
                    contentPadding = PaddingValues(
                        // Keep the viewport behind the toolbar for sampling while the first
                        // comment initially clears both the toolbar and its progressive fade.
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                        top = paddingValues.calculateTopPadding() + topBarClearance,
                        bottom = paddingValues.calculateBottomPadding() +
                            insets.asPaddingValues().calculateBottomPadding(),
                    )
                ) {
                    items(
                        count = pagingItems.itemCount,
                        key = { index -> pagingItems.peek(index)?.id ?: index }
                    ) { index ->
                        val comment = pagingItems[index] ?: return@items
                        CommentItem(
                            comment = comment,
                            replies = expandedReplies[comment.id],
                            onToggleFloor = viewModel::toggleFloorComments,
                            onRetryFloor = viewModel::retryFloorComments,
                            onShowMoreFloor = viewModel::showMoreFloorComments,
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
