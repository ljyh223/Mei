package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.glass.MeiGlassRole
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.component.item.AlbumItem
import com.ljyh.mei.ui.component.item.PlaylistItem
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.model.toAlbumEntity
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.component.glass.MeiGlassIconButton
import com.ljyh.mei.ui.component.layout.meiPageGutter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMobileLayout(
    userNickname: String,
    userAvatarUrl: String,
    userPhoto: String,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    tabTitles: List<String>,
    createdPlaylists: List<Playlist>,
    collectedPlaylists: List<Playlist>,
    albums: List<Album>,
    onAvatarClick: () -> Unit,
    onPlaylistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val navController = LocalNavController.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 20.dp
        )
    ) {
        item {
            BigUserHeader(userNickname, userAvatarUrl, onAvatarClick) {
                Screen.Setting.navigate(navController)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            BentoDashboard(
                onHistoryClick = { Screen.History.navigate(navController)},
                onLocalClick = {
                    Screen.LocalMusic.navigate(navController)
                },
                onDownloadClick = {
                    Screen.DownloadManage.navigate(navController)
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        stickyHeader {
            // 之前的吸顶 Tab 逻辑
            Box(modifier = Modifier.padding(horizontal = meiPageGutter(), vertical = 8.dp)) {
                MeiGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    role = MeiGlassRole.Navigation,
                ) {
                    ModernCapsuleTabs(tabTitles, selectedTabIndex, onTabSelect)
                }
            }
        }

        when (selectedTabIndex) {
            0 -> { // 创建歌单
                if (createdPlaylists.isEmpty()) item { EmptyState("暂无创建歌单") }
                else items(createdPlaylists, key = { it.id }) { playlist ->
                    PlaylistItem(playlist) {
                        Screen.PlayList.navigate(navController) { addPath(playlist.id) }
                    }
                }
            }

            1 -> { // 收藏歌单
                if (collectedPlaylists.isEmpty()) item { EmptyState("暂无收藏歌单") }
                else items(collectedPlaylists, key = { it.id }) { playlist ->
                    PlaylistItem(playlist) {
                        onPlaylistClick(playlist.id)

                    }
                }
            }

            2 -> { // 收藏专辑
//                val albums = (albumList as? Resource.Success)?.data?.data ?: emptyList()

                if (albums.isEmpty()) item { EmptyState("暂无收藏专辑") }
                else{
//                    albums.map { al->
//                        al.toAlbumEntity().let {
//                            viewModel.insertAlbum(it.first, it.second)
//                        }
//                    }
                    items(albums, key = { it.id }) { album ->
                        AlbumItem(album) {
                            onAlbumClick(album.id.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(text: String) {
    com.ljyh.mei.ui.component.MeiStatePanel(
        title = text,
        icon = Icons.Rounded.Folder,
        modifier = Modifier.padding(vertical = 32.dp),
    )
}

@Composable
fun BigUserHeader(nickname: String, avatarUrl: String, onAvatarClick: () -> Unit, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = meiPageGutter()),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onAvatarClick,
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            AsyncImage(model = avatarUrl, contentDescription = "更换音乐库背景图片",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f)) {
            Text(
                "音乐库",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(nickname.ifBlank { "我的收藏" }, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        MeiGlassIconButton(onClick = onSettingsClick) {
            Icon(Icons.Rounded.Settings, contentDescription = "设置")
        }
    }
}
