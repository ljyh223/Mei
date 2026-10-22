package com.ljyh.mei.ui.screen.main.findmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.data.model.domain.DiscoverOrder
import com.ljyh.mei.ui.component.MeiFilterPill
import com.ljyh.mei.ui.component.card.PlaylistCard
import com.ljyh.mei.ui.component.collection.MeiCollectionLoadMore
import com.ljyh.mei.ui.component.collection.MeiCollectionPage
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen

@Composable
fun DiscoverPlaylistsScreen(viewModel: DiscoverPlaylistsViewModel = hiltViewModel()) {
    val nav = LocalNavController.current
    val page by viewModel.playlists.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val scroll = rememberLazyGridState()
    var categorySheet by rememberSaveable { mutableStateOf(false) }
    var lastCategory by rememberSaveable { mutableStateOf(page.query.category) }
    var lastOrder by rememberSaveable { mutableStateOf(page.query.order.wireValue) }
    // A detail -> collection pop restores scroll; only an actual filter change resets it.
    LaunchedEffect(page.query) {
        if (lastCategory != page.query.category || lastOrder != page.query.order.wireValue) scroll.scrollToItem(0)
        lastCategory = page.query.category
        lastOrder = page.query.order.wireValue
    }
    MeiCollectionPage(
        title = if (page.query.category == "全部") "全部歌单" else page.query.category,
        itemCount = page.items.size, itemKey = { page.items[it].id },
        onNavigateBack = { nav.navigateUp() }, onRetry = viewModel::retryPlaylists,
        columns = if (rememberDeviceInfo().isTablet) GridCells.Adaptive(164.dp) else GridCells.Fixed(2),
        state = scroll, loading = page.loading, failed = page.failed, emptyText = "这个分类还没有歌单",
        controls = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DiscoverOrder.entries.forEach { order ->
                    MeiFilterPill(order.label, page.query.order == order, { viewModel.selectOrder(order) }, compact = true)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { categorySheet = true }) { Text("分类") }
            }
        },
        loadingItem = {
            SkeletonShimmerHost(enabled = true) {
                PlaylistCard("", "", "", modifier = Modifier.fillMaxWidth(), isLoading = true, onClick = {})
            }
        },
        footer = { MeiCollectionLoadMore(page.hasMore, page.loadingMore, page.failed, viewModel::loadMore) },
    ) { index ->
        val playlist = page.items[index]
        DiscoverPlaylistCard(playlist, Modifier.fillMaxWidth()) {
            Screen.PlayList.navigate(nav) { addPath(playlist.id.toString()) }
        }
    }
    if (categorySheet) DiscoverCategorySheet(categories, page.query.category, { categorySheet = false }, viewModel::selectCategory, viewModel::loadCategories)
}
