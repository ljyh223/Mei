package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.screen.Screen


@Composable
fun LibraryTabletLayout(
    userNickname: String,
    userAvatarUrl: String,
    userPhoto: String,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    createdPlaylists: List<Playlist>,
    collectedPlaylists: List<Playlist>,
    albums: List<Album>,
) {
    val navController = LocalNavController.current
    Row(modifier = Modifier.fillMaxSize()) {
        // --- 左侧：固定侧边栏 (30% 宽度) ---
        Column(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(vertical = 48.dp, horizontal = 24.dp)
        ) {
            BigUserHeader(userNickname, userAvatarUrl, onAvatarClick = {})

            Spacer(modifier = Modifier.height(48.dp))

            // 平板端 Bento 盒子改为垂直排布
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 最近播放做成大方块
                BentoLargeCard(userPhoto, onClick = {
                    Screen.History.navigate(navController)
                })

                // 本地/云盘/下载 做成三个小方块
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardSmallCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Folder,
                        label = "本地音乐",
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                        onClick = {},
                        hideLabel = false
                    )
                    DashboardSmallCard(
                        modifier = Modifier.weight(1f), icon = Icons.Rounded.Cloud, label = "云盘",
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                        onClick = {},
                        hideLabel = false
                    )
                    DashboardSmallCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Download,
                        label = "下载",
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        onClick = {},
                        hideLabel = false
                    )
                }
            }
        }

        // --- 右侧：内容网格区 (70% 宽度) ---
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .padding(top = 24.dp)
        ) {
            // Tab 栏
            ModernCapsuleTabs(
                tabs = listOf("创建", "收藏", "专辑"),
                selectedIndex = selectedTabIndex,
                onTabClick = onTabSelect
            )

            // 使用 LazyVerticalGrid 实现响应式网格
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp), // 自动计算列数，每列至少 160dp
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val currentList = when (selectedTabIndex) {
                    0 -> createdPlaylists
                    1 -> collectedPlaylists
                    else -> emptyList()
                }

                if (selectedTabIndex < 2) {
                    items(currentList) { playlist ->
                        // 平板网格项：封面在上，文字在下
                        LibraryGridItem(
                            title = playlist.title,
                            subtitle = "${playlist.count}首",
                            cover = playlist.cover,
                            onClick = {
                                Screen.PlayList.navigate(navController) { addPath(playlist.id) }
                            }
                        )
                    }
                } else {
                    items(albums) { album ->
                        LibraryGridItem(
                            title = album.title,
                            subtitle = album.artist.joinToString(", "),
                            cover = album.cover,
                            onClick = {
                                Screen.Album.navigate(navController) { addPath(album.id.toString()) }
                            }
                        )
                    }
                }
            }
        }
    }
}
