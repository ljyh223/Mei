package com.ljyh.mei.ui.screen.main.findmusic


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.weapi.Playlists
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.home.PlaylistCard as SharedPlaylistCard
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
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
    LaunchedEffect(selectedCategory) {
        listState.scrollToItem(0)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("发现音乐") }
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is Resource.Success -> {
                    PlaylistGrid(
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "加载失败", style = MaterialTheme.typography.titleMedium)
                            Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
                        }
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
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(
            horizontal = 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * 歌单网格列表
 */
@Composable
fun PlaylistGrid(
    playlists: List<Playlists>,
    listState: LazyGridState,
    onPlaylistClick: (Long) -> Unit
) {
    val systemBarsPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val device = rememberDeviceInfo()
    val gridPadding = if (device.isTablet) 28.dp else 16.dp
    val minCardWidth = if (device.isTablet) 180.dp else 140.dp
    val horizontalSpacing = if (device.isTablet) 20.dp else 12.dp
    val verticalSpacing = if (device.isTablet) 24.dp else 16.dp
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCardWidth),
        contentPadding = PaddingValues(
            start = gridPadding,
            end = gridPadding,
            bottom = systemBarsPadding.calculateBottomPadding() + gridPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        state = listState,
    ) {
        items(
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
