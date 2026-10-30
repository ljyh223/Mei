package com.ljyh.mei.ui.component.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.component.glass.LocalMeiPageProgress
import com.ljyh.mei.ui.component.glass.MeiHazeTopBar
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.ljyh.mei.ui.component.layout.meiPageGutter

/**
 * Shared chrome and states for complete collections. One column supports track/row lists;
 * multiple columns support artwork grids. Data fetching and item rendering stay with the caller.
 * Indexed access also supports Paging without copying or eagerly reading its items.
 */
@Composable
fun MeiCollectionPage(
    title: String,
    itemCount: Int,
    itemKey: (Int) -> Any,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    columns: GridCells = GridCells.Fixed(1),
    state: LazyGridState = rememberLazyGridState(),
    loading: Boolean = false,
    failed: Boolean = false,
    emptyText: String = "暂时没有内容",
    emptyActionLabel: String? = "重新加载",
    horizontalContentPadding: Dp? = null,
    horizontalItemSpacing: Dp? = null,
    verticalItemSpacing: Dp? = null,
    showLargeTitle: Boolean = true,
    titleContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    controls: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    loadingItem: (@Composable () -> Unit)? = null,
    itemContent: @Composable (Int) -> Unit,
) {
    val source = rememberHazeState()
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val titleGutter = meiPageGutter()
    val gutter = horizontalContentPadding ?: titleGutter
    val collapseDistance = with(LocalDensity.current) { 56.dp.toPx() }
    val progress by remember(state, collapseDistance) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) 1f
            else (state.firstVisibleItemScrollOffset / collapseDistance).coerceIn(0f, 1f)
        }
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Use this collection's actual width, including when hosted in a resized window.
        val wideArtworkGrid = maxWidth >= 600.dp && columns != GridCells.Fixed(1)
        val columnSpacing = horizontalItemSpacing ?: if (wideArtworkGrid) 24.dp else 12.dp
        val rowSpacing = verticalItemSpacing ?: if (wideArtworkGrid) 28.dp else 16.dp
        CompositionLocalProvider(LocalMeiPageProgress provides progress) {
            LazyVerticalGrid(
                columns = columns,
                state = state,
                modifier = Modifier.fillMaxSize().hazeSource(source).background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(
                    start = gutter,
                    end = gutter,
                    top = top + 64.dp,
                    bottom = insets.calculateBottomPadding() + 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                verticalArrangement = Arrangement.spacedBy(rowSpacing),
            ) {
                if (showLargeTitle) item(key = "collection_title", span = { GridItemSpan(maxLineSpan) }) {
                    MeiLargeTitle(title, startPadding = (titleGutter - gutter).coerceAtLeast(0.dp))
                }
                if (controls != null) item(key = "collection_controls", span = { GridItemSpan(maxLineSpan) }) {
                    controls()
                }
                when {
                    itemCount > 0 -> items(count = itemCount, key = itemKey) { itemContent(it) }
                    loading && loadingItem != null -> items(6, key = { "collection_loading_$it" }) { loadingItem() }
                    else -> item(key = "collection_status", span = { GridItemSpan(maxLineSpan) }) {
                        MeiCollectionStatus(loading, failed, onRetry, emptyText, emptyActionLabel)
                    }
                }
                if (itemCount > 0 && footer != null) item(key = "collection_footer", span = { GridItemSpan(maxLineSpan) }) {
                    footer()
                }
            }
        }
        MeiHazeTopBar(
            state = source, title = title, progress = if (showLargeTitle) progress else 1f,
            onNavigateBack = onNavigateBack, actions = actions, titleContent = titleContent,
        )
    }
}

@Composable
fun MeiCollectionStatus(loading: Boolean, failed: Boolean, onRetry: () -> Unit, emptyText: String = "暂时没有内容", emptyActionLabel: String? = "重新加载") {
    if (loading) {
        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    } else {
        MeiStatePanel(
            title = if (failed) "暂时无法加载" else emptyText,
            icon = if (failed) Icons.Rounded.CloudOff else Icons.Rounded.LibraryMusic,
            actionLabel = if (failed) "重新加载" else emptyActionLabel, onAction = onRetry,
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        )
    }
}

@Composable
fun MeiCollectionLoadMore(hasMore: Boolean, loading: Boolean, failed: Boolean, onLoadMore: () -> Unit) {
    if (!hasMore && !loading && !failed) return
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = onLoadMore, enabled = !loading) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text(if (failed) "加载失败，重试" else "加载更多")
        }
    }
}
