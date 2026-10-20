package com.ljyh.mei.ui.screen.main.findmusic

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LibraryMusic
import com.ljyh.mei.ui.component.MeiFilterPill
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.data.model.domain.FeaturedPlaylist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.screen.main.home.PlaylistCard as SharedPlaylistCard
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindMusicScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: FindMusicViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    val playlistState by viewModel.highQualityPlaylist.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories = viewModel.categories

    val listState = rememberLazyGridState()
    val stateInsets = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    LaunchedEffect(selectedCategory) {
        listState.scrollToItem(0)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("发现", style = MaterialTheme.typography.headlineLarge) }
                )
                // 分类选择器
                CategorySelector(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = viewModel::onCategorySelected
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = playlistState) {
                is Resource.Loading -> {
                    SkeletonShimmerHost(enabled = true, modifier = Modifier.fillMaxSize()) {
                        PlaylistGrid(emptyList(), listState, onPlaylistClick = {}, isLoading = true)
                    }
                }

                is Resource.Success -> {
                    if (state.data.playlists.isEmpty()) {
                        Box(Modifier.fillMaxSize().windowInsetsPadding(stateInsets), contentAlignment = Alignment.Center) {
                            MeiStatePanel("这个分类还没有歌单", Icons.Rounded.LibraryMusic,
                                actionLabel = "重新加载",
                                onAction = { viewModel.loadCategoryData(selectedCategory, forceRefresh = true) })
                        }
                    } else PlaylistGrid(
                        playlists = state.data.playlists,
                        listState = listState,
                        onPlaylistClick = { playlistId ->
                            Screen.PlayList.navigate(navController) {
                                addPath(playlistId.toString())
                            }
                        }
                    )

                }

                is Resource.Error -> {
                    Box(Modifier.fillMaxSize().windowInsetsPadding(stateInsets), contentAlignment = Alignment.Center) {
                        MeiStatePanel("暂时无法加载歌单", Icons.Rounded.CloudOff,
                            actionLabel = "重试",
                            onAction = { viewModel.loadCategoryData(selectedCategory, forceRefresh = true) })
                    }
                }
            }
        }
    }
}

/**
 * 横向滚动的分类选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(
            horizontal = 20.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            MeiFilterPill(label = category, selected = isSelected, onClick = { onCategorySelected(category) })
        }
    }
}

/**
 * 歌单网格列表
 */
@Composable
fun PlaylistGrid(
    playlists: List<FeaturedPlaylist>,
    listState: LazyGridState,
    onPlaylistClick: (Long) -> Unit,
    isLoading: Boolean = false,
) {
    val systemBarsPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val device = rememberDeviceInfo()
    val gridPadding = if (device.isTablet) 28.dp else 20.dp
    val minCardWidth = if (device.isTablet) 180.dp else 140.dp
    val horizontalSpacing = if (device.isTablet) 20.dp else 12.dp
    val verticalSpacing = if (device.isTablet) 24.dp else 16.dp
    LazyVerticalGrid(
        columns = if (device.isTablet) GridCells.Adaptive(minSize = minCardWidth) else GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = gridPadding,
            end = gridPadding,
            top = 12.dp,
            bottom = systemBarsPadding.calculateBottomPadding() + gridPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        state = listState,
    ) {
        if (isLoading) items(6, key = { "discover_placeholder_$it" }) {
            SharedPlaylistCard(
                id = "discover_placeholder_$it", title = "", coverImg = "",
                modifier = Modifier.fillMaxWidth(), isLoading = true, onClick = {},
            )
        } else items(
            items = playlists,
            key = { it.id } // 优化性能，使用唯一ID
        ) { playlist ->
            SharedPlaylistCard(
                id = playlist.id.toString(),
                title = playlist.name,
                coverImg = playlist.coverImgUrl,
                extInfo = formatPlayCount(playlist.playCount),
                modifier = Modifier.fillMaxWidth(),
                onClick = { onPlaylistClick(playlist.id) }
            )
        }
    }
}

// 辅助函数：格式化播放量
fun formatPlayCount(count: Int): String {
    return when {
        count >= 1_0000_0000 -> String.format(Locale.getDefault(), "%.1f亿", count / 1_0000_0000.0)
        count >= 1_0000 -> String.format(Locale.getDefault(), "%.1f万", count / 1_0000.0)
        else -> count.toString()
    }
}
